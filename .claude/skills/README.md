# SrcProfit Skills

Specialized skills for Spring Boot and coding tasks in the SrcProfit project.

## Available Skills

### 1. financial-calculations
Generate and implement trading financial calculations following TastyTrade methodology.

**Includes:**
- Black-Scholes option pricing and Greeks calculation
- P&L analysis with FIFO cost basis tracking
- Portfolio heat and risk calculations
- Position sizing and probability of profit
- Validation against broker data

**Files:**
- `SKILL.md` - Main instructions and patterns
- `black-scholes-formulas.md` - Mathematical reference
- `tastytrade-reference.md` - TastyTrade methodology guide

### 2. spring-boot-scaffold
Generate Spring Boot 3.5.6 components following SrcProfit conventions.

**Includes:**
- JPA entity creation with proper annotations
- Spring Data repository scaffolding
- Service layer implementation patterns
- REST controller generation
- Exception handling and error responses
- DTO design with records and validation

**Standards:**
- No Lombok - explicit getters/setters/constructors
- Jakarta Validation (jakarta.validation.*)
- Constructor injection
- BigDecimal for all monetary values
- Proper fetch strategies for relationships

### 3. java-testing
Create comprehensive unit and integration tests with JUnit 5, Mockito, and AssertJ.

**Includes:**
- Unit test patterns with mocking
- Integration test setup with @SpringBootTest
- Repository test patterns with @DataJpaTest
- Controller test patterns with @WebMvcTest
- Test data builders and factories
- Parametrized tests (@ParameterizedTest)
- Financial calculation test patterns

**Standards:**
- 80%+ test coverage for service layer
- 100% coverage for financial calculations
- Fluent assertions with AssertJ
- Proper test organization with @Nested classes
- Real broker data validation for trading logic

### 4. sql-query-builder
Write and optimize SQL queries for PostgreSQL 15 with JPA/Hibernate.

**Includes:**
- JPQL query patterns with @Query
- Native SQL query development
- Pagination and sorting strategies
- Complex joins and aggregations
- Query optimization and indexing
- EXPLAIN ANALYZE performance tuning
- Data migration scripts

**Standards:**
- JOIN FETCH to prevent N+1 queries
- Projections for large result sets
- NUMERIC(19,4) for financial values
- Proper indexing strategy
- Test queries against real data

### 5. code-refactoring
Improve code quality through strategic refactoring and design patterns.

**Includes:**
- Extract method and class patterns
- Strategy pattern for if-else chains
- Builder pattern for complex objects
- Specification pattern for queries
- Domain objects for financial types (PnL, Greeks)
- Java 24 modernization (records, text blocks, var)
- N+1 query optimization

**Standards:**
- Maintain test coverage during refactoring
- Small, incremental changes
- Validate financial calculations before/after
- Follow SOLID principles
- Update documentation

## How Skills Work

Skills are **model-invoked** - Claude automatically decides to use them based on context and the skill description. Simply ask questions that match the skill's purpose:

- "Create a new JPA entity for..."
- "Generate a test for this service..."
- "Optimize this slow query..."
- "Help me refactor this method..."
- "Calculate Greeks for..."

## Integration with SrcProfit

All skills follow these standards:

- **Java 24** with Spring Boot 3.5.6
- **PostgreSQL 15** database
- **JPA/Hibernate** ORM
- **Jakarta Validation** for constraints
- **BigDecimal** for monetary values
- **SLF4J** for logging
- **Constructor Injection** in Spring components
- **FIFO Cost Basis** for position tracking
- **TastyTrade Methodology** for trading logic

## Support Files

Each skill includes:
- `SKILL.md` - Main instruction document with patterns and examples
- Supporting markdown files with detailed references
- Code examples matching SrcProfit conventions

## Quality Gates

Use these skills to support SrcProfit's 4-tier quality gates:
- **TIER 1** (`/commit`) - Fast TDD with good tests
- **TIER 2** (`/commit-review`) - Format + tests + coverage
- **TIER 3** (`/ship`) - Production-ready code

All refactoring and new code should:
1. Pass full test suite
2. Maintain or improve test coverage
3. Validate financial calculations
4. Follow SrcProfit conventions
5. Include proper documentation
