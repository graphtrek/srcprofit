---
name: spring-boot-scaffold
description: Generate Spring Boot 3.5.6 components (entities, repositories, services, controllers, configurations) following SrcProfit conventions, with JPA annotations, proper naming patterns, and integration with PostgreSQL. Use when creating new domain objects, REST endpoints, or Spring beans.
---

# Spring Boot Scaffold Skill

Quickly generate Spring Boot components following SrcProfit conventions and best practices.

## Instructions

### When to Use This Skill

Use this skill when you need to:
- Create new JPA entities with proper annotations and relationships
- Generate Spring Data JPA repositories with custom query methods
- Build service layer classes with business logic
- Create REST controllers with proper routing and error handling
- Write Spring @Configuration beans for application setup
- Generate custom exception classes and error handlers
- Build DTOs and request/response objects

### SrcProfit Conventions

**Package Structure:**
```
co.grtk.srcprofit
├── entity/          # JPA entities
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic services
├── controller/      # REST/MVC controllers
├── config/          # Spring configuration beans
├── exception/       # Custom exceptions
└── dto/             # Data Transfer Objects
```

**Naming Patterns:**
- Entities: Singular nouns (Position, Trade, Portfolio)
- Repositories: EntityRepository extending JpaRepository
- Services: EntityService for business logic
- Controllers: EntityController or EntityRestController
- Exceptions: EntityNotFoundException, InvalidEntityException
- DTOs: EntityRequest, EntityResponse

**Technology Stack:**
- Java 24 with latest features
- Spring Boot 3.5.6
- Spring Data JPA with Hibernate
- PostgreSQL 15 database
- Jakarta Validation (jakarta.validation.*)
- No Lombok - write explicit getters/setters/constructors

### Entity Creation Pattern

```java
@Entity
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL)
    private Set<Trade> trades = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Position() {
    }

    public Position(String symbol, BigDecimal quantity, Portfolio portfolio) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.portfolio = portfolio;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Set<Trade> getTrades() {
        return trades;
    }

    public void setTrades(Set<Trade> trades) {
        this.trades = trades;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

### Repository Creation Pattern

```java
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    @Query("SELECT p FROM Position p WHERE p.portfolio.id = :portfolioId AND p.quantity > 0")
    List<Position> findOpenPositions(@Param("portfolioId") Long portfolioId);

    @Query(value = "SELECT SUM(p.quantity * m.price) FROM positions p " +
                   "JOIN market_data m ON p.symbol = m.symbol " +
                   "WHERE p.portfolio_id = :portfolioId", nativeQuery = true)
    BigDecimal calculatePortfolioValue(@Param("portfolioId") Long portfolioId);
}
```

### Service Creation Pattern

```java
@Service
@Transactional
public class PositionService {

    private static final Logger logger = LoggerFactory.getLogger(PositionService.class);

    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;

    public PositionService(PositionRepository positionRepository,
                          PortfolioRepository portfolioRepository) {
        this.positionRepository = positionRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public Position createPosition(Long portfolioId, PositionRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found"));

        Position position = new Position();
        position.setSymbol(request.getSymbol());
        position.setQuantity(request.getQuantity());
        position.setPortfolio(portfolio);

        return positionRepository.save(position);
    }

    public Position updatePosition(Long id, PositionRequest request) {
        Position position = positionRepository.findById(id)
            .orElseThrow(() -> new PositionNotFoundException("Position not found"));

        position.setQuantity(request.getQuantity());
        return positionRepository.save(position);
    }

    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }

    public Position getPosition(Long id) {
        return positionRepository.findById(id)
            .orElseThrow(() -> new PositionNotFoundException("Position not found"));
    }

    public List<Position> getPositions(Long portfolioId) {
        return positionRepository.findByPortfolioIdAndSymbol(portfolioId, null);
    }
}
```

### Controller Creation Pattern

```java
@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private static final Logger logger = LoggerFactory.getLogger(PositionController.class);

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getPositions(
            @RequestParam Long portfolioId) {
        List<Position> positions = positionService.getPositions(portfolioId);
        return ResponseEntity.ok(positions.stream()
            .map(PositionResponse::fromEntity)
            .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionResponse> getPosition(@PathVariable Long id) {
        Position position = positionService.getPosition(id);
        return ResponseEntity.ok(PositionResponse.fromEntity(position));
    }

    @PostMapping
    public ResponseEntity<PositionResponse> createPosition(
            @Valid @RequestBody PositionRequest request) {
        Position position = positionService.createPosition(request.getPortfolioId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PositionResponse.fromEntity(position));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PositionResponse> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody PositionRequest request) {
        Position position = positionService.updatePosition(id, request);
        return ResponseEntity.ok(PositionResponse.fromEntity(position));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Configuration Pattern

```java
@Configuration
public class RepositoryConfiguration {

    @Bean
    public AuditingHandler auditingHandler() {
        return new AuditingHandler();
    }
}
```

### Exception Handling Pattern

```java
public class PositionNotFoundException extends RuntimeException {
    public PositionNotFoundException(String message) {
        super(message);
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PositionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePositionNotFound(
            PositionNotFoundException ex) {
        logger.error("Position not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

### DTO Creation Pattern (Using Records)

```java
public record PositionRequest(
    @NotBlank(message = "Symbol cannot be blank")
    String symbol,
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    BigDecimal quantity,
    @NotNull(message = "Portfolio ID cannot be null")
    Long portfolioId
) {}

public record PositionResponse(
    Long id,
    String symbol,
    BigDecimal quantity,
    LocalDateTime createdAt
) {
    public static PositionResponse fromEntity(Position position) {
        return new PositionResponse(
            position.getId(),
            position.getSymbol(),
            position.getQuantity(),
            position.getCreatedAt()
        );
    }
}
```

### Data Type Standards

**Always Use for Monetary Values:**
- `BigDecimal` for prices, P&L, portfolio values, cost basis
- 2 decimal places for currency display
- 4 decimal places for intermediate calculations

**Always Use for Timestamps:**
- `LocalDateTime` for application timestamps
- `Instant` for UTC timestamps
- `LocalDate` for expiration dates, trade dates

**Jakarta Validation Annotations:**
- `@NotNull` - field cannot be null
- `@NotBlank` - string cannot be blank
- `@DecimalMin(value = "0.01")` - minimum decimal value
- `@Email` - valid email format
- `@Size(min = 1, max = 100)` - string/collection size
- `@Pattern(regexp = "...")` - regex pattern matching
- `@Valid` - cascade validation to nested objects

## Key Integration Points

- **Persistence**: JPA/Hibernate with PostgreSQL 15
- **Validation**: Use @Valid and jakarta.validation.* annotations
- **Transactions**: @Transactional at service layer for atomic operations
- **Logging**: SLF4J with LoggerFactory
- **Error Handling**: Custom exceptions with @ControllerAdvice
- **DTO Conversion**: fromEntity() static methods
- **Relationship Management**: Proper fetch strategies (LAZY by default for many-to-one)
- **Constructor Injection**: Prefer constructor over @Autowired

## Testing Integration

- Create corresponding test classes for each component
- Use @SpringBootTest for integration tests
- Mock repositories in unit tests with Mockito
- Validate entity relationships and constraints
