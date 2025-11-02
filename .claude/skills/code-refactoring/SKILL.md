---
name: code-refactoring
description: Improve code quality, maintainability, and performance through targeted refactoring. Apply design patterns, reduce duplication, improve naming, modernize Java 24 code, and optimize financial calculations. Use when refactoring existing code, eliminating technical debt, or improving testability.
---

# Code Refactoring Skill

Improve code quality and maintainability through strategic refactoring.

## Instructions

### When to Use This Skill

Use this skill when you need to:
- Extract methods and classes to improve readability
- Replace if-else chains with polymorphism or strategy patterns
- Reduce constructor/method parameter bloat
- Improve testability through dependency injection
- Optimize N+1 query problems
- Reduce code duplication and consolidate logic
- Modernize deprecated patterns to Java 24 standards
- Improve financial calculation accuracy and clarity
- Apply SOLID principles (Single Responsibility, Open/Closed, etc.)
- Optimize algorithm efficiency and memory usage

### SrcProfit Refactoring Goals

**Financial Accuracy**: Ensure calculations match TastyTrade methodology
**Testability**: Make code easier to unit test with proper separation of concerns
**Performance**: Optimize database queries and computation-heavy calculations
**Maintainability**: Improve code clarity for trading domain logic
**Compliance**: Follow SrcProfit conventions and quality gates

### Common Refactoring Patterns

#### 1. Extract Method

**Before:**
```java
public void processTrade(Trade trade) {
    if (trade.getType().equals("BUY")) {
        Position position = positionRepository.findBySymbol(trade.getSymbol());
        position.setQuantity(position.getQuantity().add(trade.getQuantity()));
        position.setCostBasis(
            position.getCostBasis().add(
                trade.getPrice().multiply(trade.getQuantity())
            )
        );
        positionRepository.save(position);
    } else {
        // Sell logic...
    }
}
```

**After:**
```java
public void processTrade(Trade trade) {
    Position position = positionRepository.findBySymbol(trade.getSymbol());
    if (trade.getType().equals("BUY")) {
        updatePositionForBuy(position, trade);
    } else {
        updatePositionForSell(position, trade);
    }
    positionRepository.save(position);
}

private void updatePositionForBuy(Position position, Trade trade) {
    position.setQuantity(position.getQuantity().add(trade.getQuantity()));
    position.setCostBasis(
        position.getCostBasis().add(
            trade.getPrice().multiply(trade.getQuantity())
        )
    );
}

private void updatePositionForSell(Position position, Trade trade) {
    position.setQuantity(position.getQuantity().subtract(trade.getQuantity()));
    // Sell logic...
}
```

#### 2. Replace If-Else with Strategy Pattern

**Before:**
```java
public BigDecimal calculatePosition(Position position) {
    if (position.getType().equals("CALL")) {
        return calculateCallPosition(position);
    } else if (position.getType().equals("PUT")) {
        return calculatePutPosition(position);
    } else if (position.getType().equals("STOCK")) {
        return calculateStockPosition(position);
    }
    throw new IllegalArgumentException("Unknown type");
}
```

**After:**
```java
@FunctionalInterface
interface PositionCalculator {
    BigDecimal calculate(Position position);
}

@Service
public class PositionCalculationService {
    private final Map<String, PositionCalculator> calculators;

    public PositionCalculationService() {
        this.calculators = Map.of(
            "CALL", this::calculateCallPosition,
            "PUT", this::calculatePutPosition,
            "STOCK", this::calculateStockPosition
        );
    }

    public BigDecimal calculatePosition(Position position) {
        return calculators
            .getOrDefault(position.getType(), p -> {
                throw new IllegalArgumentException("Unknown type: " + p.getType());
            })
            .calculate(position);
    }
}
```

#### 3. Reduce Parameter Bloat with Builder Pattern

**Before:**
```java
public Position createPosition(String symbol, BigDecimal quantity,
                              Portfolio portfolio, LocalDateTime createdAt,
                              BigDecimal delta, BigDecimal gamma,
                              BigDecimal theta, BigDecimal vega) {
    // Implementation
}

// Usage
Position position = createPosition("AAPL", qty, portfolio, now, 0.5, 0.02, -0.03, 0.1);
```

**After:**
```java
public class PositionBuilder {
    private String symbol;
    private BigDecimal quantity;
    private Portfolio portfolio;
    private LocalDateTime createdAt = LocalDateTime.now();
    private BigDecimal delta = BigDecimal.ZERO;
    private BigDecimal gamma = BigDecimal.ZERO;
    private BigDecimal theta = BigDecimal.ZERO;
    private BigDecimal vega = BigDecimal.ZERO;

    public PositionBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public PositionBuilder quantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public PositionBuilder portfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
        return this;
    }

    public PositionBuilder greeks(BigDecimal delta, BigDecimal gamma,
                                 BigDecimal theta, BigDecimal vega) {
        this.delta = delta;
        this.gamma = gamma;
        this.theta = theta;
        this.vega = vega;
        return this;
    }

    public Position build() {
        // Validation and creation
        return new Position(symbol, quantity, portfolio, createdAt, delta, gamma, theta, vega);
    }
}

// Usage
Position position = new PositionBuilder()
    .symbol("AAPL")
    .quantity(new BigDecimal("10"))
    .portfolio(portfolio)
    .greeks(new BigDecimal("0.5"), new BigDecimal("0.02"),
            new BigDecimal("-0.03"), new BigDecimal("0.1"))
    .build();
```

#### 4. Extract to Service for Testability

**Before:**
```java
@RestController
public class PositionController {
    private final PositionRepository repository;
    private final PriceService priceService;

    @GetMapping("/{id}")
    public PositionResponse getPosition(@PathVariable Long id) {
        Position position = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Position not found"));

        BigDecimal currentPrice = priceService.getCurrentPrice(position.getSymbol());
        BigDecimal currentValue = position.getQuantity().multiply(currentPrice);
        BigDecimal pnl = currentValue.subtract(position.getCostBasis());

        return new PositionResponse(position, pnl);
    }
}
```

**After:**
```java
@Service
public class PositionPnLService {
    private final PositionRepository repository;
    private final PriceService priceService;

    public BigDecimal calculateUnrealizedPnL(Position position) {
        BigDecimal currentPrice = priceService.getCurrentPrice(position.getSymbol());
        BigDecimal currentValue = position.getQuantity().multiply(currentPrice);
        return currentValue.subtract(position.getCostBasis());
    }
}

@RestController
public class PositionController {
    private final PositionRepository repository;
    private final PositionPnLService pnlService;

    @GetMapping("/{id}")
    public PositionResponse getPosition(@PathVariable Long id) {
        Position position = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Position not found"));

        BigDecimal pnl = pnlService.calculateUnrealizedPnL(position);
        return new PositionResponse(position, pnl);
    }
}
```

#### 5. Replace Magic Strings with Enums

**Before:**
```java
if (position.getStatus().equals("OPEN")) {
    // Handle open
} else if (position.getStatus().equals("CLOSED")) {
    // Handle closed
}
```

**After:**
```java
enum PositionStatus {
    OPEN("Open position"),
    CLOSED("Closed position"),
    ROLLED("Rolled to next expiration"),
    ASSIGNED("Assigned/exercised");

    private final String description;

    PositionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

if (position.getStatus() == PositionStatus.OPEN) {
    // Handle open
} else if (position.getStatus() == PositionStatus.CLOSED) {
    // Handle closed
}
```

#### 6. Use Specification Pattern for Complex Queries

**Before:**
```java
public List<Position> searchPositions(Long portfolioId, String symbol,
                                      BigDecimal minQuantity,
                                      LocalDateTime fromDate,
                                      LocalDateTime toDate) {
    // Lots of if-else logic to build query
}
```

**After:**
```java
public interface PositionSpecification {
    static Specification<Position> byPortfolio(Long portfolioId) {
        return (root, query, cb) -> cb.equal(root.get("portfolio").get("id"), portfolioId);
    }

    static Specification<Position> bySymbol(String symbol) {
        return (root, query, cb) -> cb.equal(root.get("symbol"), symbol);
    }

    static Specification<Position> minQuantity(BigDecimal minQty) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quantity"), minQty);
    }

    static Specification<Position> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }
}

// Usage
List<Position> positions = repository.findAll(
    PositionSpecification.byPortfolio(portfolioId)
        .and(PositionSpecification.bySymbol(symbol))
        .and(PositionSpecification.minQuantity(minQty))
        .and(PositionSpecification.createdBetween(fromDate, toDate))
);
```

#### 7. Optimize N+1 Queries

**Before:**
```java
List<Position> positions = positionRepository.findByPortfolioId(portfolioId);
for (Position position : positions) {
    List<Trade> trades = position.getTrades();  // N queries!
    calculatePnL(trades);
}
```

**After:**
```java
@Query("SELECT DISTINCT p FROM Position p " +
       "LEFT JOIN FETCH p.trades " +
       "WHERE p.portfolio.id = :portfolioId")
List<Position> findWithTrades(@Param("portfolioId") Long portfolioId);

// Usage
List<Position> positions = positionRepository.findWithTrades(portfolioId);
for (Position position : positions) {
    List<Trade> trades = position.getTrades();  // No extra queries
    calculatePnL(trades);
}
```

#### 8. Improve Financial Calculations with Domain Objects

**Before:**
```java
public BigDecimal calculatePnL(BigDecimal cost, BigDecimal current, BigDecimal quantity) {
    return current.multiply(quantity).subtract(cost);
}

public BigDecimal calculateDelta(BigDecimal spot, BigDecimal strike,
                                BigDecimal tte, BigDecimal rate, BigDecimal vol) {
    // 30 lines of calculation
}
```

**After:**
```java
public class PnL {
    private final BigDecimal unrealized;
    private final BigDecimal realized;
    private final BigDecimal percentage;

    public PnL(BigDecimal costBasis, BigDecimal currentValue, BigDecimal realizedGains) {
        this.unrealized = currentValue.subtract(costBasis);
        this.realized = realizedGains;
        this.percentage = unrealized.divide(costBasis, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    public BigDecimal getUnrealized() { return unrealized; }
    public BigDecimal getRealized() { return realized; }
    public BigDecimal getPercentage() { return percentage; }
}

public class Greeks {
    private final BigDecimal delta;
    private final BigDecimal gamma;
    private final BigDecimal theta;
    private final BigDecimal vega;

    public Greeks(BigDecimal delta, BigDecimal gamma, BigDecimal theta, BigDecimal vega) {
        this.delta = delta;
        this.gamma = gamma;
        this.theta = theta;
        this.vega = vega;
    }

    public BigDecimal getDelta() { return delta; }
    public BigDecimal getGamma() { return gamma; }
    public BigDecimal getTheta() { return theta; }
    public BigDecimal getVega() { return vega; }
}

// Usage
PnL pnl = new PnL(position.getCostBasis(), currentValue, realizedGains);
log.info("P&L: {} ({}%)", pnl.getUnrealized(), pnl.getPercentage());

Greeks greeks = greeksCalculator.calculate(spotPrice, strike, tte, rate, volatility);
log.info("Greeks - Delta: {}, Gamma: {}, Theta: {}, Vega: {}",
         greeks.getDelta(), greeks.getGamma(), greeks.getTheta(), greeks.getVega());
```

### Refactoring Safety Measures

1. **Run Full Test Suite**
   - Before: `mvn test`
   - After: `mvn test`
   - Ensure all tests pass

2. **Use Small, Incremental Changes**
   - Refactor one method/class at a time
   - Commit after each successful refactoring
   - Make code reviews easier

3. **Validate Trading Calculations**
   - Compare results before/after
   - Test against broker data (Alpaca, IBKR)
   - Verify P&L calculations match

4. **Update Documentation and Comments**
   - Explain why, not what (code shows what)
   - Update ADRs for major changes
   - Keep README and docs current

5. **Git Strategy**
   - Create feature branch for refactoring
   - Small, focused commits
   - Pull request with clear description
   - Code review before merge

### Java 24 Modernization Patterns

**Use Records for Immutable Data:**
```java
// Old
public class PositionRequest {
    private String symbol;
    private BigDecimal quantity;

    public PositionRequest(String symbol, BigDecimal quantity) {
        this.symbol = symbol;
        this.quantity = quantity;
    }

    // Getters...
}

// New
public record PositionRequest(String symbol, BigDecimal quantity) {}
```

**Use Text Blocks for Multi-line Strings:**
```java
// Old
String query = "SELECT p FROM Position p " +
               "WHERE p.portfolio.id = :portfolioId " +
               "AND p.quantity > 0";

// New
String query = """
    SELECT p FROM Position p
    WHERE p.portfolio.id = :portfolioId
    AND p.quantity > 0
    """;
```

**Use var for Type Inference:**
```java
// Old
BigDecimal totalValue = portfolio.getPositions()
    .stream()
    .map(p -> p.getQuantity().multiply(priceService.getPrice(p.getSymbol())))
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// New
var totalValue = portfolio.getPositions()
    .stream()
    .map(p -> p.getQuantity().multiply(priceService.getPrice(p.getSymbol())))
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### Quality Gates Before Refactoring

- Code compiles without warnings
- All tests pass
- Code coverage ≥ 80% for business logic
- No security vulnerabilities (dependency check)
- Financial calculations validated
