---
name: java-testing
description: Create comprehensive unit and integration tests for Java/Spring Boot code using JUnit 5, Mockito, and AssertJ. Generate test fixtures, parametrized tests, and test data builders. Use when writing tests for services, controllers, repositories, and financial calculations.
---

# Java Testing Skill

Create comprehensive unit and integration tests for SrcProfit components.

## Instructions

### When to Use This Skill

Use this skill when you need to:
- Write unit tests for service and controller classes
- Create integration tests with Spring context
- Generate mock objects and test doubles
- Build test data factories for complex entities
- Verify financial calculations and trading logic
- Test error handling and edge cases
- Create parametrized tests for multiple scenarios
- Validate business rules and constraints

### Testing Framework Stack

- **Test Framework**: JUnit 5 (Jupiter)
- **Mocking**: Mockito 5.x
- **Assertions**: AssertJ for fluent assertions
- **Spring Testing**: @SpringBootTest, @WebMvcTest, @DataJpaTest
- **Test Organization**: Parallel package structure in src/test/java

### Unit Test Pattern

```java
class PositionServiceTest {

    private PositionRepository positionRepository;
    private PortfolioRepository portfolioRepository;
    private PositionService positionService;

    @BeforeEach
    void setUp() {
        positionRepository = mock(PositionRepository.class);
        portfolioRepository = mock(PortfolioRepository.class);
        positionService = new PositionService(positionRepository, portfolioRepository);
    }

    @Test
    void createPosition_withValidRequest_shouldCreateAndReturnPosition() {
        // Arrange
        Long portfolioId = 1L;
        Portfolio portfolio = new Portfolio();
        portfolio.setId(portfolioId);

        PositionRequest request = new PositionRequest("AAPL", new BigDecimal("10"), portfolioId);

        when(portfolioRepository.findById(portfolioId))
            .thenReturn(Optional.of(portfolio));

        Position savedPosition = new Position("AAPL", new BigDecimal("10"), portfolio);
        savedPosition.setId(1L);

        when(positionRepository.save(any(Position.class)))
            .thenReturn(savedPosition);

        // Act
        Position result = positionService.createPosition(portfolioId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));

        verify(portfolioRepository).findById(portfolioId);
        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void createPosition_withInvalidPortfolioId_shouldThrowNotFoundException() {
        // Arrange
        Long portfolioId = 999L;
        PositionRequest request = new PositionRequest("AAPL", new BigDecimal("10"), portfolioId);

        when(portfolioRepository.findById(portfolioId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> positionService.createPosition(portfolioId, request))
            .isInstanceOf(PortfolioNotFoundException.class)
            .hasMessageContaining("Portfolio not found");

        verify(positionRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "GOOGL", "MSFT", "TSLA"})
    void updatePosition_withDifferentSymbols_shouldUpdateSuccessfully(String symbol) {
        // Arrange
        Long positionId = 1L;
        Position existingPosition = new Position(symbol, new BigDecimal("5"), null);
        existingPosition.setId(positionId);

        PositionRequest request = new PositionRequest(symbol, new BigDecimal("10"), 1L);

        when(positionRepository.findById(positionId))
            .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(existingPosition))
            .thenReturn(existingPosition);

        // Act
        Position result = positionService.updatePosition(positionId, request);

        // Assert
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
    }
}
```

### Integration Test Pattern

```java
@SpringBootTest
@Transactional
class PositionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        portfolioRepository.deleteAll();

        testPortfolio = new Portfolio();
        testPortfolio.setName("Test Portfolio");
        testPortfolio = portfolioRepository.save(testPortfolio);
    }

    @Test
    void createPosition_withValidRequest_shouldReturnCreatedPosition() throws Exception {
        // Arrange
        PositionRequest request = new PositionRequest("AAPL", new BigDecimal("10"), testPortfolio.getId());

        // Act & Assert
        mockMvc.perform(post("/api/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.symbol").value("AAPL"))
            .andExpect(jsonPath("$.quantity").value(10));

        Position savedPosition = positionRepository.findAll().getFirst();
        assertThat(savedPosition.getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void getPosition_withExistingId_shouldReturnPosition() throws Exception {
        // Arrange
        Position position = new Position("GOOGL", new BigDecimal("5"), testPortfolio);
        Position savedPosition = positionRepository.save(position);

        // Act & Assert
        mockMvc.perform(get("/api/positions/{id}", savedPosition.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("GOOGL"))
            .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void deletePosition_withExistingId_shouldDeleteSuccessfully() throws Exception {
        // Arrange
        Position position = new Position("MSFT", new BigDecimal("3"), testPortfolio);
        Position savedPosition = positionRepository.save(position);

        // Act & Assert
        mockMvc.perform(delete("/api/positions/{id}", savedPosition.getId()))
            .andExpect(status().isNoContent());

        assertThat(positionRepository.findById(savedPosition.getId())).isEmpty();
    }

    private String asJsonString(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }
}
```

### Repository Test Pattern

```java
@DataJpaTest
class PositionRepositoryTest {

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setName("Test Portfolio");
        testPortfolio = portfolioRepository.save(testPortfolio);
    }

    @Test
    void findOpenPositions_shouldReturnOnlyPositionsWithPositiveQuantity() {
        // Arrange
        Position openPosition = new Position("AAPL", new BigDecimal("10"), testPortfolio);
        Position closedPosition = new Position("GOOGL", new BigDecimal("0"), testPortfolio);

        positionRepository.save(openPosition);
        positionRepository.save(closedPosition);

        // Act
        List<Position> openPositions = positionRepository.findOpenPositions(testPortfolio.getId());

        // Assert
        assertThat(openPositions)
            .hasSize(1)
            .extracting(Position::getSymbol)
            .contains("AAPL");
    }
}
```

### Test Data Builder Pattern

```java
public class PositionTestBuilder {

    private String symbol = "AAPL";
    private BigDecimal quantity = new BigDecimal("10");
    private Portfolio portfolio;
    private LocalDateTime createdAt = LocalDateTime.now();

    public PositionTestBuilder withSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public PositionTestBuilder withQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public PositionTestBuilder withPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
        return this;
    }

    public PositionTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Position build() {
        Position position = new Position();
        position.setSymbol(this.symbol);
        position.setQuantity(this.quantity);
        position.setPortfolio(this.portfolio);
        position.setCreatedAt(this.createdAt);
        return position;
    }
}

// Usage
Position position = new PositionTestBuilder()
    .withSymbol("GOOGL")
    .withQuantity(new BigDecimal("5"))
    .withPortfolio(testPortfolio)
    .build();
```

### Financial Calculation Test Pattern

```java
class PositionGreeksCalculationTest {

    private GreeksCalculationService greeksService;

    @BeforeEach
    void setUp() {
        greeksService = new GreeksCalculationService();
    }

    @Test
    void calculateGreeks_withValidInput_shouldReturnCorrectDelta() {
        // Arrange
        BigDecimal spotPrice = new BigDecimal("100.00");
        BigDecimal strike = new BigDecimal("105.00");
        BigDecimal timeToExpiration = new BigDecimal("0.0822"); // 30 days
        BigDecimal riskFreeRate = new BigDecimal("0.05");
        BigDecimal volatility = new BigDecimal("0.25");

        // Act
        Greeks greeks = greeksService.calculateGreeks(
            spotPrice, strike, timeToExpiration, riskFreeRate, volatility);

        // Assert
        assertThat(greeks.getDelta())
            .isGreaterThan(BigDecimal.ZERO)
            .isLessThan(BigDecimal.ONE);
        assertThat(greeks.getGamma()).isGreaterThan(BigDecimal.ZERO);
        assertThat(greeks.getTheta()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
        "100, 100, 0.0822, 0.05, 0.25",
        "100, 110, 0.0822, 0.05, 0.25",
        "100, 90, 0.0822, 0.05, 0.25"
    })
    void calculateGreeks_withMultipleStrikes_shouldCalculateSuccessfully(
            String spot, String strike, String tte, String rate, String vol) {
        // Arrange
        BigDecimal spotPrice = new BigDecimal(spot);
        BigDecimal strikePrice = new BigDecimal(strike);
        BigDecimal timeToExp = new BigDecimal(tte);
        BigDecimal riskRate = new BigDecimal(rate);
        BigDecimal volatility = new BigDecimal(vol);

        // Act
        Greeks greeks = greeksService.calculateGreeks(
            spotPrice, strikePrice, timeToExp, riskRate, volatility);

        // Assert
        assertThat(greeks).isNotNull();
        assertThat(greeks.getDelta()).isBetween(BigDecimal.valueOf(-1), BigDecimal.ONE);
    }
}
```

### Testing Best Practices for SrcProfit

**Use AssertJ for Fluent Assertions:**
```java
// Good
assertThat(position.getQuantity())
    .isEqualByComparingTo(new BigDecimal("10"))
    .isGreaterThan(BigDecimal.ZERO);

// Avoid
assertEquals(position.getQuantity(), new BigDecimal("10"));
```

**Verify Interactions with Mockito:**
```java
// Verify method was called once
verify(repository).save(position);

// Verify method was never called
verify(repository, never()).delete(any());

// Verify call order
InOrder inOrder = inOrder(repo1, repo2);
inOrder.verify(repo1).save(entity);
inOrder.verify(repo2).save(entity);
```

**Use @ParameterizedTest for Multiple Scenarios:**
```java
@ParameterizedTest
@ValueSource(strings = {"AAPL", "GOOGL", "MSFT"})
void testMultipleSymbols(String symbol) {
    // Test code
}
```

**Organize Tests with Nested Classes:**
```java
class PositionServiceTest {

    @Nested
    class CreatePosition {
        @Test
        void shouldCreateSuccessfully() { }

        @Test
        void shouldThrowIfPortfolioNotFound() { }
    }

    @Nested
    class UpdatePosition {
        @Test
        void shouldUpdateSuccessfully() { }
    }
}
```

### Test Coverage Goals

- **Unit Tests**: 80%+ coverage for service/business logic
- **Integration Tests**: Key workflows and API endpoints
- **Financial Calculations**: 100% coverage with real broker data validation
- **Edge Cases**: Dividends, stock splits, expirations, boundary conditions
- **Error Paths**: All exception handling scenarios

### Common Test Annotations

| Annotation | Purpose |
|-----------|---------|
| @Test | Marks a test method |
| @BeforeEach | Runs before each test |
| @AfterEach | Runs after each test |
| @BeforeAll | Runs once before all tests |
| @AfterAll | Runs once after all tests |
| @DisplayName | Custom test display name |
| @Disabled | Skip test |
| @ParameterizedTest | Test with multiple parameters |
| @ValueSource | Provide parameter values |
| @CsvSource | CSV test data |
| @SpringBootTest | Load full Spring context |
| @WebMvcTest | Load web layer only |
| @DataJpaTest | Load JPA context |
