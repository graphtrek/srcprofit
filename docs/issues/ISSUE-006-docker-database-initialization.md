# ISSUE-006: Docker Database Schema Initialization

**Created**: 2025-11-03 (Session 06)
**Status**: CLOSED
**Completed**: 2025-11-03
**Priority**: CRITICAL
**Category**: Infrastructure | Bug Fix
**Blocking**: None (blocking issue resolved)

---

## Problem

When running `docker-compose up`, the application container failed with:
```
ERROR: relation "flex_statement_response" does not exist
Position: 13
```

This prevented the application from starting in Docker environments, though it worked locally with `./mvnw spring-boot:run`.

---

## Root Cause

Three layers of issues:

1. **Hibernate's `ddl-auto: update` mode unreliability**: In Docker, Hibernate's automatic schema creation doesn't reliably create tables on initial startup when the application connects before the database is fully ready.

2. **PostgreSQL init script context**: PostgreSQL's `/docker-entrypoint-initdb.d/` scripts execute against the default `postgres` database, not individual application databases. The schema wasn't being created in the `srcprofit` databases.

3. **Permission constraints**: Tables created by the `postgres` superuser weren't accessible to the `srcprofit` application user, causing `ERROR: permission denied for table` errors.

---

## Approach

Implemented explicit schema initialization in the Docker entrypoint process:

1. Modified `init/init-db.sh` to:
   - Create users and databases (existing)
   - Directly create all JPA entity tables with correct column definitions
   - Create all required indexes
   - Grant full permissions to the `srcprofit` user
   - Set default permissions for future schema changes

2. Schema creation happens in correct database contexts:
   - Runs for each srcprofit database: `srcprofit`, `srcprofit1`, `srcprofit2`
   - Executes before application attempts to connect

3. All operations are idempotent:
   - Uses `CREATE TABLE IF NOT EXISTS`
   - Safe to run multiple times without errors

---

## Solution Details

### Files Modified

**`init/init-db.sh`** - Added schema initialization function:
```bash
create_schema() {
    local db_name=$1
    # Creates all 5 tables with correct column types and indexes
    # Grants all permissions to srcprofit user
    # Sets default permissions for future tables
}

# Calls for each database
for db in srcprofit srcprofit1 srcprofit2; do
    create_schema "$db"
done
```

### Tables Initialized

| Table | Entity | Row Count |
|-------|--------|-----------|
| `instrument` | `InstrumentEntity` | ✓ |
| `option` | `OptionEntity` | ✓ |
| `net_asset_value` | `NetAssetValueEntity` | ✓ |
| `earning` | `EarningEntity` | ✓ |
| `flex_statement_response` | `FlexStatementResponseEntity` | ✓ |

### Indexes Created

- `instr_ticker_idx`, `instr_conid_idx`, `instr_name_idx`
- `opt_conid_idx`, `opt_conid_status_idx`, `opt_conid_status_price_idx`, `opt_code_idx`, `opt_ticker_idx`, `opt_instrument_id_idx`, `opt_status_idx`
- `nav_report_date_idx`
- `earning_report_date_idx`, `earning_symbol_idx`, `earning_idx`
- `fsr_reference_code_idx`, `fsr_request_date_idx`, `fsr_report_type_idx`

### Permission Grants

```sql
-- Existing tables
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO srcprofit;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO srcprofit;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO srcprofit;

-- Future tables (Hibernate-generated)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO srcprofit;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO srcprofit;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO srcprofit;
```

---

## Success Criteria

- [x] Application starts successfully with `docker-compose --env-file docker-compose.env up`
- [x] No `"relation does not exist"` errors
- [x] No `"permission denied"` errors
- [x] All 5 tables created in each database (srcprofit, srcprofit1, srcprofit2)
- [x] All indexes created correctly
- [x] Tomcat starts on port 8080
- [x] Database connectivity working end-to-end
- [x] Idempotent - safe to run multiple times

---

## Testing

### Verification Commands

**Check table creation**:
```bash
docker-compose --env-file docker-compose.env exec db psql -U srcprofit -d srcprofit -c "\dt"
```

**Expected output**:
```
               List of relations
 Schema |          Name           | Type  |  Owner
--------+-------------------------+-------+----------
 public | earning                 | table | postgres
 public | flex_statement_response | table | postgres
 public | instrument              | table | postgres
 public | net_asset_value         | table | postgres
 public | option                  | table | postgres
(5 rows)
```

**Check permissions**:
```bash
docker-compose --env-file docker-compose.env exec db psql -U srcprofit -d srcprofit -c "\dp instrument"
```

**Verify application startup**:
```bash
docker-compose --env-file docker-compose.env logs app | grep "Started SrcProfitApplication"
```

### Acceptance Test

```bash
#!/bin/bash
# Test Docker deployment

# Start fresh
docker-compose --env-file docker-compose.env down -v

# Start services
docker-compose --env-file docker-compose.env up -d

# Wait for initialization
sleep 30

# Check if app started successfully
if docker-compose logs app | grep -q "Started SrcProfitApplication"; then
    echo "✅ Application started successfully"
else
    echo "❌ Application failed to start"
    exit 1
fi

# Check if any table errors
if docker-compose logs app | grep -q "relation.*does not exist"; then
    echo "❌ Table not found error"
    exit 1
else
    echo "✅ No table errors"
fi

# Check if any permission errors
if docker-compose logs app | grep -q "permission denied"; then
    echo "❌ Permission error"
    exit 1
else
    echo "✅ No permission errors"
fi

echo "✅ All tests passed!"
```

---

## Related Issues

- Related: ISSUE-005 (FLEX Reports Monitoring) - This was the issue that exposed the Docker schema problem

---

## Implementation Details

### Commits

| Hash | Message |
|------|---------|
| `590413b` | fix(database): Initialize schema with all tables on startup |
| `80fd99c` | fix(database): Create schema in all srcprofit databases on startup |
| `684502d` | fix(database): Initialize schema directly in init-db.sh |
| `d201ea4` | fix(database): Add permission grants to srcprofit user on init |

### Changes Summary

- **Files Modified**: 1 (`init/init-db.sh`)
- **Files Created**: 1 (`.md` doc)
- **Files Deleted**: 1 (duplicate schema script)
- **Lines Added**: 140+ (schema DDL + permissions)

---

## Notes

### Why Not Just Use Flyway/Liquibase?

While migrations frameworks are generally good practice, for this project:
- Simple schema (5 tables, relatively stable)
- Single entrypoint (Docker init scripts)
- No schema evolution/versioning needed yet
- Init script approach is transparent and maintainable

If schema complexity increases significantly, consider migrating to Flyway.

### Why Not Just `ddl-auto: create`?

The `create` mode would drop and recreate tables on every startup, losing data. The `update` mode is correct but unreliable in Docker timing scenarios.

### Environment Configuration

The fix requires proper environment variables in `docker-compose.env`:
```
SRCPROFIT_DB_URL=jdbc:postgresql://db:5432/srcprofit
SRCPROFIT_DB_USER=srcprofit
SRCPROFIT_DB_PWD=srcprofit
```

Note: Uses `db` (Docker service name) not `localhost`.

### Multi-Database Setup

The initialization handles all 3 databases:
- `srcprofit` - Primary (Imre's account)
- `srcprofit1` - Secondary (Krisztian's account)
- `srcprofit2` - Tertiary (GraphTrek's account)

Each gets identical schema initialization.

---

## Future Improvements

1. **Schema versioning**: Add migration metadata table if/when schema changes become frequent
2. **Seed data**: Could add initial data loading in the same script
3. **Health checks**: Add database connectivity checks before app startup
4. **Monitoring**: Log initialization performance metrics

---

## References

- PostgreSQL Docker Documentation: https://hub.docker.com/_/postgres
- Hibernate DDL Documentation: https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#configurations-hbmddl
- Spring Boot JPA Configuration: https://docs.spring.io/spring-boot/docs/3.5.6/reference/html/application-properties.html#application-properties.data.spring.jpa

---

## Sign-Off

**Fixed By**: Claude Code
**Date**: 2025-11-03
**Verification**: ✅ All success criteria met
**Ready for**: Production deployment with Docker
