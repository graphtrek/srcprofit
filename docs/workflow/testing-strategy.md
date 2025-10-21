# Testing Strategy - SrcProfit

**Purpose**: Ground Truth TDD for financial accuracy in options trading
**Last Updated**: 2025-10-21 (Session 1)

---

## 🎯 Philosophy

**Ground Truth TDD**: Validate calculations against broker data, not assumptions

Traditional TDD doesn't work for financial calculations where "correct answer" exists externally.

---

## 📊 Test Pyramid

```
        /\
       /E2E\      10% - Full workflows (Selenium/RestAssured)
      /------\
     /Integr.\   20% - API + Database (TestContainers)
    /----------\
   /   Unit     \ 70% - Business logic (JUnit 5 + Mockito)
  /--------------\
```

### Distribution
- **Unit**: 70% - Fast, isolated, business logic
- **Integration**: 20% - Database, external APIs
- **E2E**: 10% - Full user workflows

---

## 🧪 Ground Truth TDD

### The Problem with Traditional TDD

```java
// ❌ Traditional TDD - What should the result be?
@Test
void shouldCalculateOptionProfitCorrectly() {
    OptionEntity option = createOption();
    BigDecimal result = service.calculateProfit(option);
    assertThat(result).isEqualTo(???);  // What's correct?
}
```

### The Solution - Ground Truth

```java
// ✅ Ground Truth TDD - Compare to broker data
@Test
void shouldCalculateProfitMatchingIBKR() {
    // Given - Real trade from IBKR Flex report
    OptionEntity option = loadGroundTruth("ibkr-trade-12345.json");
    BigDecimal expectedProfit = option.getGroundTruthProfit();

    // When - Our calculation
    BigDecimal actualProfit = service.calculateProfit(option);

    // Then - Must match broker exactly
    assertThat(actualProfit)
        .usingComparator(BigDecimal::compareTo)
        .isEqualTo(expectedProfit);
}
```

### Ground Truth Process

1. **Collect**: Get known-correct values from broker (IBKR, Alpaca)
2. **Store**: Save in `src/test/resources/ground-truth/`
3. **Test**: Write test comparing calculation to ground truth
4. **Implement**: Make test pass
5. **Validate**: Verify against live broker data

---

## 🗂️ Test Organization

### Structure

```
src/test/
├── java/
│   └── co/grtk/srcprofit/
│       ├── entity/          # Entity tests
│       ├── repository/      # JPA tests (@DataJpaTest)
│       ├── service/         # Service tests (Mock + Integration)
│       ├── controller/      # Controller tests (@WebMvcTest)
│       └── integration/     # Full integration tests
└── resources/
    ├── ground-truth/        # Broker data (JSON/CSV)
    │   ├── ibkr-trades.json
    │   ├── alpaca-quotes.json
    │   └── black-scholes-baseline.json
    ├── test-data/           # Test fixtures
    └── application-test.yaml
```

### Ground Truth Files

**IBKR Trade Example** (`ground-truth/ibkr-trade-12345.json`):
```json
{
  "tradeId": "12345",
  "symbol": "SPY 450 PUT 2024-03-15",
  "openDate": "2024-01-15",
  "openPrice": 1.25,
  "quantity": -10,
  "closeDate": "2024-02-10",
  "closePrice": 0.50,
  "commission": 6.50,
  "expectedProfit": 743.50,
  "source": "IBKR Flex Report 2024-01-15"
}
```

---

## 🧩 Testing Patterns

### Unit Tests (JUnit 5 + AssertJ)

```java
@ExtendWith(MockitoExtension.class)
class OptionServiceTest {
    @Mock
    private OptionRepository repository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private OptionService service;

    @Test
    @DisplayName("Should calculate annualized ROI using Black-Scholes")
    void shouldCalculateAnnualizedRoi() {
        // Given
        OptionEntity option = OptionEntityBuilder.create()
            .withPremium(new BigDecimal("125.50"))
            .withRisk(new BigDecimal("500.00"))
            .withDaysToExpiration(45)
            .build();

        BigDecimal expectedRoi = new BigDecimal("15.25");  // From ground truth

        // When
        BigDecimal actualRoi = service.calculateAnnualizedRoi(option);

        // Then
        assertThat(actualRoi)
            .usingComparator(BigDecimal::compareTo)
            .isEqualByComparingTo(expectedRoi);
    }
}
```

### Integration Tests (TestContainers + PostgreSQL)

```java
@SpringBootTest
@Testcontainers
@Transactional
class OptionRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("srcprofit_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OptionRepository repository;

    @Test
    void shouldFindAllOpenPositions() {
        // Given
        OptionEntity open = createOption(OptionStatus.OPEN);
        OptionEntity closed = createOption(OptionStatus.CLOSED);
        repository.saveAll(List.of(open, closed));

        // When
        List<OptionEntity> result = repository.findAllOpen();

        // Then
        assertThat(result)
            .hasSize(1)
            .first()
            .extracting(OptionEntity::getStatus)
            .isEqualTo(OptionStatus.OPEN);
    }
}
```

### Controller Tests (@WebMvcTest)

```java
@WebMvcTest(PositionController.class)
class PositionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OptionService optionService;

    @Test
    void shouldReturnAllOpenPositions() throws Exception {
        // Given
        List<PositionDto> positions = List.of(
            new PositionDto("SPY", 10, new BigDecimal("125.50"), LocalDate.now())
        );
        when(optionService.getAllOpen()).thenReturn(positions);

        // When/Then
        mockMvc.perform(get("/positions"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("positions", hasSize(1)))
            .andExpect(view().name("positions"));
    }
}
```

---

## 💰 Financial Calculation Testing

### Decimal Precision

```java
@Test
void shouldHandleDecimalPrecisionCorrectly() {
    // Given - Exact values from broker
    BigDecimal premium = new BigDecimal("125.50");
    BigDecimal risk = new BigDecimal("500.00");

    // When
    BigDecimal roi = premium.divide(risk, 4, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));

    // Then - Exact comparison
    assertThat(roi).isEqualByComparingTo("25.1000");
}
```

### FIFO Cost Basis

```java
@Test
void shouldCalculateFifoCostBasisCorrectly() {
    // Given - Trades from ground truth CSV
    List<Trade> trades = loadGroundTruthTrades("ibkr-spy-trades.csv");

    // When
    BigDecimal costBasis = service.calculateFifoCostBasis(trades);

    // Then - Must match broker calculation
    BigDecimal expected = trades.get(0).getIbkrCostBasis();
    assertThat(costBasis).isEqualByComparingTo(expected);
}
```

### Black-Scholes Validation

```java
@Test
void shouldMatchBlackScholesBaseline() {
    // Given - Known Black-Scholes values
    BlackScholesInput input = loadGroundTruth("black-scholes-baseline.json");

    // When
    double optionPrice = blackScholesCalculator.calculate(
        input.getSpotPrice(),
        input.getStrike(),
        input.getTimeToExpiration(),
        input.getRiskFreeRate(),
        input.getVolatility()
    );

    // Then - Within 0.01 of expected
    assertThat(optionPrice).isCloseTo(input.getExpectedPrice(), within(0.01));
}
```

---

## 🔧 Test Configuration

### Maven Surefire (Unit Tests)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <groups>unit</groups>
        <excludedGroups>integration</excludedGroups>
        <argLine>@{argLine} -Xmx1024m</argLine>
    </configuration>
</plugin>
```

### Maven Failsafe (Integration Tests)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <groups>integration</groups>
        <includes>
            <include>**/*IT.java</include>
        </includes>
    </configuration>
</plugin>
```

### JaCoCo Coverage

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

---

## 🎯 Coverage Targets

| Component | Target | Rationale |
|-----------|--------|-----------|
| **Service Layer** | >90% | Business logic critical |
| **Repository** | >85% | Data access important |
| **Entity** | >95% | Domain model critical |
| **Controller** | >70% | Focus on key workflows |
| **Mapper** | >85% | Transformation logic |
| **Overall** | >80% | **Minimum for PR** |

---

## 🚨 Critical Test Rules

### For Financial Calculations
- ✅ ALWAYS use BigDecimal comparisons, not double
- ✅ ALWAYS validate against broker data
- ✅ ALWAYS test FIFO ordering for P&L
- ✅ ALWAYS test edge cases (zero, negative, null)
- ✅ ALWAYS document source of ground truth

### For Database Tests
- ✅ ALWAYS use @Transactional for cleanup
- ✅ ALWAYS test lazy loading behavior
- ✅ ALWAYS verify N+1 query prevention
- ✅ ALWAYS test with realistic data volumes

---

## 📚 Test Data Management

### Builders

```java
public class OptionEntityBuilder {
    public static OptionEntity create() {
        return OptionEntity.builder()
            .ticker("SPY")
            .strike(new BigDecimal("450.00"))
            .quantity(-10)
            .status(OptionStatus.OPEN)
            .type(OptionType.PUT)
            .build();
    }

    public static OptionEntity createFromGroundTruth(String filename) {
        return JsonLoader.load("ground-truth/" + filename, OptionEntity.class);
    }
}
```

### Ground Truth Loader

```java
public class GroundTruthLoader {
    public static <T> T load(String path, Class<T> type) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return objectMapper.readValue(resource.getInputStream(), type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ground truth: " + path, e);
        }
    }
}
```

---

## 🔄 TDD Workflow

### Red-Green-Refactor

```bash
# 1. RED - Write failing test with ground truth
@Test
void shouldCalculateProfitMatchingBroker() {
    // Load broker data
    Trade trade = loadGroundTruth("ibkr-trade-12345.json");

    // Our calculation
    BigDecimal profit = service.calculateProfit(trade);

    // Must match broker
    assertThat(profit).isEqualByComparingTo(trade.getExpectedProfit());
}

# Run test
./mvnw test -Dtest=OptionServiceTest
# ❌ FAILED - method not implemented

# 2. GREEN - Implement
# ... write code ...

./mvnw test -Dtest=OptionServiceTest
# ✅ PASSED

# 3. REFACTOR - Clean up
# ... improve code ...

./mvnw test
# ✅ All tests pass

# 4. COMMIT
/commit  # TIER 1 - fast feedback
```

---

## 📈 Continuous Improvement

### Test Maintenance
- **Weekly**: Add new ground truth data from broker
- **Monthly**: Review coverage, add missing tests
- **Quarterly**: Update test fixtures, clean up obsolete tests

### Metrics to Track
- Test execution time (<30s for unit, <2min for integration)
- Coverage trends (should increase over time)
- Ground truth validation rate (100% for financial calculations)

---

**Version**: 1.0 (Session 1)
**Source**: Contrarian Trading Portfolio System (Ground Truth TDD methodology)
**Next Review**: After first 10 tests written
