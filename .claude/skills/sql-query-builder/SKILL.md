---
name: sql-query-builder
description: Write and optimize SQL queries for PostgreSQL 15 with JPA/Hibernate integration, including JPQL, native queries, complex joins, aggregate functions, and performance optimization. Use when creating custom repository methods, optimizing slow queries, or writing data migration scripts.
---

# SQL Query Builder Skill

Write and optimize SQL queries for PostgreSQL with JPA/Hibernate.

## Instructions

### When to Use This Skill

Use this skill when you need to:
- Write custom Spring Data JPA @Query methods (JPQL)
- Create native SQL queries for complex operations
- Optimize slow queries and add proper indexes
- Write complex joins for multi-entity reports
- Create data migration scripts
- Set up proper indexing strategies
- Generate aggregate queries for analytics and reporting
- Implement pagination and sorting efficiently

### SrcProfit Database Context

**Key Entities:**
- `positions` - Current open/closed positions with Greeks
- `trades` - Individual buy/sell transactions
- `portfolios` - User portfolios
- `market_data` - Current market prices and volatility
- `net_asset_values` - Historical portfolio NAV tracking

**Key Relationships:**
- Portfolio → Position (one-to-many)
- Position → Trade (one-to-many)
- Position → Greeks (one-to-one)

### JPQL Repository Query Pattern

```java
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    // Simple queries
    List<Position> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    // JPQL with @Query - filtering open positions
    @Query("SELECT p FROM Position p " +
           "WHERE p.portfolio.id = :portfolioId " +
           "AND p.quantity > 0 " +
           "ORDER BY p.createdAt DESC")
    List<Position> findOpenPositions(@Param("portfolioId") Long portfolioId);

    // JPQL with projection - return only needed fields
    @Query("SELECT new map(p.id as id, p.symbol as symbol, p.quantity as quantity) " +
           "FROM Position p " +
           "WHERE p.portfolio.id = :portfolioId")
    List<Map<String, Object>> findPositionsProjection(@Param("portfolioId") Long portfolioId);

    // JPQL with aggregate functions - calculate portfolio value
    @Query("SELECT new co.grtk.srcprofit.dto.PortfolioSummary(" +
           "COUNT(p), SUM(p.quantity), MAX(p.createdAt)) " +
           "FROM Position p " +
           "WHERE p.portfolio.id = :portfolioId")
    PortfolioSummary getPortfolioSummary(@Param("portfolioId") Long portfolioId);

    // JPQL with JOIN - position with latest trade
    @Query("SELECT p FROM Position p " +
           "LEFT JOIN FETCH p.trades t " +
           "WHERE p.portfolio.id = :portfolioId " +
           "ORDER BY t.createdAt DESC")
    List<Position> findPositionsWithTrades(@Param("portfolioId") Long portfolioId);

    // JPQL with Pageable for large datasets
    @Query("SELECT p FROM Position p WHERE p.portfolio.id = :portfolioId")
    Page<Position> findByPortfolioIdPaged(@Param("portfolioId") Long portfolioId, Pageable pageable);
}
```

### Native SQL Query Pattern

```java
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    // Native query for complex calculations
    @Query(value = "SELECT p.id, p.symbol, p.quantity, " +
                   "SUM(t.price * t.quantity) as cost_basis, " +
                   "m.current_price * p.quantity as current_value, " +
                   "(m.current_price * p.quantity) - (SUM(t.price * t.quantity)) as unrealized_pnl " +
                   "FROM positions p " +
                   "LEFT JOIN trades t ON p.id = t.position_id " +
                   "LEFT JOIN market_data m ON p.symbol = m.symbol " +
                   "WHERE p.portfolio_id = :portfolioId " +
                   "GROUP BY p.id, p.symbol, p.quantity, m.current_price",
           nativeQuery = true)
    List<PositionWithPnL> getPositionsWithPnL(@Param("portfolioId") Long portfolioId);

    // Native query for portfolio heat calculation
    @Query(value = "SELECT SUM(CASE " +
                   "WHEN p.position_type = 'PUT' THEN p.strike * p.quantity * 100 " +
                   "WHEN p.position_type = 'CALL' THEN p.strike * p.quantity * 100 * 0.2 " +
                   "ELSE 0 END) as portfolio_heat " +
                   "FROM positions p " +
                   "WHERE p.portfolio_id = :portfolioId " +
                   "AND p.quantity > 0",
           nativeQuery = true)
    BigDecimal calculatePortfolioHeat(@Param("portfolioId") Long portfolioId);

    // Native query with date range - performance reports
    @Query(value = "SELECT DATE_TRUNC('day', t.created_at) as trade_date, " +
                   "SUM(CASE WHEN t.type = 'BUY' THEN -1 ELSE 1 END * t.price * t.quantity) as daily_pnl " +
                   "FROM trades t " +
                   "WHERE t.position_id IN (SELECT id FROM positions WHERE portfolio_id = :portfolioId) " +
                   "AND t.created_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE_TRUNC('day', t.created_at) " +
                   "ORDER BY trade_date DESC",
           nativeQuery = true)
    List<DailyPnL> getDailyPnL(@Param("portfolioId") Long portfolioId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
```

### Pagination and Sorting

```java
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    // Paginated query
    Page<Position> findByPortfolioId(Long portfolioId, Pageable pageable);

    // Usage in service
    @Service
    public class PositionService {
        public Page<Position> getPositionsPaginated(Long portfolioId, int page, int size) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return positionRepository.findByPortfolioId(portfolioId, pageable);
        }

        public Page<Position> getPositionsSorted(Long portfolioId, String sortBy) {
            Pageable pageable = PageRequest.of(0, 100, Sort.by(sortBy).ascending());
            return positionRepository.findByPortfolioId(portfolioId, pageable);
        }
    }
}
```

### Common Query Patterns

**Find with Multiple Conditions (AND/OR):**
```java
@Query("SELECT p FROM Position p " +
       "WHERE p.portfolio.id = :portfolioId " +
       "AND (p.symbol = :symbol OR p.quantity > :minQuantity) " +
       "AND p.createdAt >= :startDate")
List<Position> findWithMultipleConditions(
    @Param("portfolioId") Long portfolioId,
    @Param("symbol") String symbol,
    @Param("minQuantity") BigDecimal minQuantity,
    @Param("startDate") LocalDateTime startDate);
```

**COUNT and Aggregates:**
```java
@Query("SELECT COUNT(p) FROM Position p WHERE p.portfolio.id = :portfolioId")
long countPositions(@Param("portfolioId") Long portfolioId);

@Query("SELECT SUM(p.quantity) FROM Position p WHERE p.portfolio.id = :portfolioId")
BigDecimal sumQuantities(@Param("portfolioId") Long portfolioId);

@Query("SELECT MAX(p.createdAt) FROM Position p WHERE p.portfolio.id = :portfolioId")
LocalDateTime getLatestPositionDate(@Param("portfolioId") Long portfolioId);
```

**GROUP BY with HAVING:**
```java
@Query("SELECT new map(p.symbol as symbol, COUNT(p) as count, SUM(p.quantity) as total) " +
       "FROM Position p " +
       "WHERE p.portfolio.id = :portfolioId " +
       "GROUP BY p.symbol " +
       "HAVING COUNT(p) > 1")
List<Map<String, Object>> findSymbolsWithMultiplePositions(@Param("portfolioId") Long portfolioId);
```

**Date/Time Operations:**
```java
@Query("SELECT p FROM Position p " +
       "WHERE p.portfolio.id = :portfolioId " +
       "AND YEAR(p.createdAt) = :year")
List<Position> findPositionsByYear(@Param("portfolioId") Long portfolioId, @Param("year") int year);

@Query("SELECT p FROM Position p " +
       "WHERE p.portfolio.id = :portfolioId " +
       "AND p.createdAt >= CURRENT_TIMESTAMP - INTERVAL '30 days'")
List<Position> findPositionsFromLast30Days(@Param("portfolioId") Long portfolioId);
```

### Performance Optimization

**Use JOIN FETCH to Prevent N+1 Queries:**
```java
// Problem: N+1 queries (1 for positions, N for each trade)
@Query("SELECT p FROM Position p WHERE p.portfolio.id = :portfolioId")
List<Position> findPositions(@Param("portfolioId") Long portfolioId);

// Solution: JOIN FETCH to load trades in single query
@Query("SELECT DISTINCT p FROM Position p " +
       "LEFT JOIN FETCH p.trades t " +
       "WHERE p.portfolio.id = :portfolioId")
List<Position> findPositionsWithTrades(@Param("portfolioId") Long portfolioId);
```

**Use Projections for Large Result Sets:**
```java
// Instead of loading full entities, fetch only needed fields
@Query("SELECT new co.grtk.srcprofit.dto.PositionSummary(" +
       "p.id, p.symbol, p.quantity) " +
       "FROM Position p WHERE p.portfolio.id = :portfolioId")
List<PositionSummary> findPositionSummaries(@Param("portfolioId") Long portfolioId);
```

### Indexing Strategy

**Create indexes for frequently queried columns:**
```sql
-- Index for portfolio lookups
CREATE INDEX idx_positions_portfolio_id ON positions(portfolio_id);

-- Index for symbol searches
CREATE INDEX idx_positions_symbol ON positions(symbol);

-- Composite index for common queries
CREATE INDEX idx_positions_portfolio_symbol ON positions(portfolio_id, symbol);

-- Index for time-based queries
CREATE INDEX idx_trades_created_at ON trades(created_at);

-- Index for status filters
CREATE INDEX idx_positions_quantity ON positions(quantity) WHERE quantity > 0;
```

### Migration Scripts Pattern

```sql
-- Add new column with default value
ALTER TABLE positions ADD COLUMN greeks_delta NUMERIC(10, 4) DEFAULT 0.0;

-- Update existing data
UPDATE positions SET greeks_delta = 0.5 WHERE symbol = 'AAPL';

-- Create view for commonly used query
CREATE VIEW open_positions AS
SELECT p.*, m.current_price, m.volatility
FROM positions p
LEFT JOIN market_data m ON p.symbol = m.symbol
WHERE p.quantity > 0;

-- Create materialized view for expensive calculations
CREATE MATERIALIZED VIEW portfolio_heat_summary AS
SELECT p.portfolio_id, SUM(CASE
    WHEN p.position_type = 'PUT' THEN p.strike * p.quantity * 100
    ELSE 0 END) as total_heat
FROM positions p
WHERE p.quantity > 0
GROUP BY p.portfolio_id;

-- Refresh materialized view
REFRESH MATERIALIZED VIEW portfolio_heat_summary;
```

### EXPLAIN ANALYZE for Query Optimization

```sql
-- Analyze query performance
EXPLAIN ANALYZE
SELECT p.*, SUM(t.quantity) as total_quantity
FROM positions p
LEFT JOIN trades t ON p.id = t.position_id
WHERE p.portfolio_id = 1
GROUP BY p.id;

-- Check if indexes are being used
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM positions WHERE portfolio_id = 1 AND symbol = 'AAPL';
```

### Data Type Considerations

**Use NUMERIC for Financial Values:**
```sql
-- Correct for financial calculations
price NUMERIC(19, 4),      -- Up to 15 digits before decimal, 4 after
quantity NUMERIC(18, 8),   -- For partial shares
portfolio_heat NUMERIC(20, 2)  -- Sum of multiple positions

-- Avoid
price FLOAT,  -- Precision loss
cost_basis DOUBLE  -- Rounding errors
```

**Use TIMESTAMP for Audit Trails:**
```sql
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
expires_at TIMESTAMP NOT NULL  -- For options expiration
```

### Query Best Practices for SrcProfit

1. **Always Use Parameterized Queries**: Prevent SQL injection
2. **Use BigDecimal for Results**: Map NUMERIC columns to BigDecimal
3. **Filter Early**: Apply WHERE clauses before GROUP BY
4. **Limit Result Sets**: Use pagination for large datasets
5. **Validate Financial Calculations**: Compare query results to application calculations
6. **Test Against Real Data**: Queries must handle edge cases (nulls, zeros, negatives)
7. **Monitor Query Performance**: Use EXPLAIN ANALYZE regularly
