# Troubleshooting Guide

## Database Not Visible in pgAdmin

**Issue**: No databases appearing in pgAdmin after starting docker-compose.

**Root Cause**: Environment variables not being loaded when starting docker-compose, causing:
- Empty database credentials
- Database initialization script not running properly
- pgAdmin unable to authenticate

**Solution**:

1. Stop containers and remove volumes to start fresh:
```bash
docker-compose down -v
```

2. Always start containers with the `--env-file` flag:
```bash
docker-compose --env-file docker-compose.env up -d db pgadmin
```

3. Verify databases were created:
```bash
docker exec srcprofit_db psql -U postgres -c "\l"
```

Expected databases:
- srcprofit
- srcprofit1
- srcprofit2
- moneypenny
- stableips

**Accessing pgAdmin**:

1. URL: http://localhost:8888
2. Login with credentials from `docker-compose.env`:
   - Email: `${PGADMIN_DEFAULT_EMAIL}`
   - Password: `${PGADMIN_DEFAULT_PASSWORD}`
3. Add server connection:
   - Name: SrcProfit DB
   - Host: `srcprofit_db` (or `db`)
   - Port: `5432`
   - Username: `srcprofit` (or `postgres` for superuser)
   - Password: `srcprofit` (from SRCPROFIT_DB_PWD)

**Important**: The `init/init-db.sh` script only runs on first container startup (when volume is empty). If databases weren't created, you must remove the volume and restart.

## Date: 2025-10-20
