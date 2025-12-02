# ISSUE-037: Alpaca Option Contracts Download and Storage

**Created**: 2025-11-28
**Completed**: 2025-12-02
**Status**: CLOSED
**Priority**: HIGH
**Category**: Feature
**Blocking**: None

---

## Problem

Option traders need access to available option contracts for underlying securities to:
- Identify trading opportunities matching portfolio heat and strike preferences
- Filter contracts by expiration date and strike price relative to current price
- Build dynamic option chains for analysis and execution

Currently, SrcProfit tracks open option positions but does not download or store available option contracts from Alpaca. This limits users to manual contract lookup on the Alpaca platform.

---

## Root Cause

No mechanism exists to:
1. Query Alpaca Options Contracts API (`GET /v1beta1/options/contracts`)
2. Store contract metadata in database
3. Filter contracts based on trader-defined criteria
4. Maintain fresh contract data via scheduled refresh

---

## Approach

Implement option contracts download feature following the `refreshAlpacaMarketData()` pattern in MarketDataService:

### Implementation Overview

1. **Database Layer**
   - Create `OptionContractEntity` with JPA mapping
   - Add migration `V002__Add_Option_Contract_Table.sql`
   - Create `OptionContractRepository` with filtering queries

2. **API Integration**
   - Create `AlpacaContractsResponseDto` wrapper
   - Add `getOptionContracts()` method to `AlpacaService`
   - Use `alpacaTradingRestClient` for Alpaca API calls

3. **Business Logic**
   - Create `OptionContractService` with orchestration methods
   - Implement batch refresh with per-instrument error tolerance
   - Calculate dynamic strike price ranges (-20% to +10% of current price)
   - Filter instruments by price < $100 and expiration < 3 months

4. **REST API**
   - Create `OptionContractRestController`
   - Endpoint for on-demand refresh: `POST /api/option-contracts/refresh`
   - Endpoint for ticker lookup: `GET /api/option-contracts/{ticker}`
   - Endpoint for cleanup: `DELETE /api/option-contracts/expired`

5. **Scheduled Jobs**
   - Add refresh job (every 12 hours)
   - Add cleanup job (every 24 hours)
   - Integrate into `ScheduledJobsService`

6. **Testing**
   - Unit tests with 80%+ coverage
   - Follow Mockito patterns from existing tests
   - Test filtering logic and error handling

### Filtering Criteria

**Eligible Instruments**:
- Price < $100 USD
- Price not null and > 0

**For Each Instrument, Download Contracts With**:
- Expiration date: Today to +3 months
- Strike price: (current price × 0.80) to (current price × 1.10)
- Option type: Both PUT and CALL
- Status: Active/tradable only

---

## Success Criteria

- [x] Plan documented and approved
- [ ] `OptionContractEntity` created with proper JPA annotations and indexes
- [ ] Database migration `V002__Add_Option_Contract_Table.sql` applied successfully
- [ ] `OptionContractRepository` implements all required query methods
- [ ] `AlpacaService.getOptionContracts()` successfully calls Alpaca API
- [ ] `OptionContractService` orchestrates batch refresh with error tolerance
- [ ] Strike price filtering logic correctly calculates -20% to +10% range
- [ ] Expiration filtering correctly limits to 3 months
- [ ] `OptionContractRestController` endpoints respond correctly
- [ ] Scheduled jobs register and execute on 12-hour and 24-hour intervals
- [ ] All unit tests pass with >80% coverage
- [ ] Integration test verifies contracts saved to database match Alpaca API response
- [ ] No duplicate contracts (upsert pattern prevents duplicates)
- [ ] Application runs without errors in development environment
- [ ] Database contains valid option contracts for test symbols

---

## Acceptance Tests

```java
// Test 1: Batch refresh filters instruments correctly
@Test
void testRefreshOptionContracts_FiltersInstrumentsByPrice() {
    // Create instruments with prices: $50, $150, $80
    // Run refreshOptionContracts()
    // Assert: only $50 and $80 instruments processed
    assertEquals(2, instrumentsProcessed);
}

// Test 2: Strike price range calculation
@Test
void testRefreshContractsForInstrument_CalculatesStrikePriceRange() {
    // Instrument price: $100
    // Call refreshContractsForInstrument()
    // Assert: API called with strikeGte=$80, strikeLte=$110
    ArgumentCaptor<String> strikeGte = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> strikeLte = ArgumentCaptor.forClass(String.class);
    verify(alpacaService).getOptionContracts(eq("AAPL"), any(), any(),
                                            strikeLte.capture(), strikeGte.capture());
    assertEquals("80.00", strikeGte.getValue());
    assertEquals("110.00", strikeLte.getValue());
}

// Test 3: Expiration date range (3 months)
@Test
void testRefreshContractsForInstrument_CalculatesExpirationRange() {
    LocalDate today = LocalDate.now();
    LocalDate threeMonthsOut = today.plusMonths(3);
    // Assert: API called with expiration_date_gte=today, expiration_date_lte=3months
    ArgumentCaptor<String> dateGte = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> dateLte = ArgumentCaptor.forClass(String.class);
    verify(alpacaService).getOptionContracts(eq("AAPL"), dateGte.capture(),
                                            dateLte.capture(), any(), any());
    assertEquals(today.toString(), dateGte.getValue());
    assertEquals(threeMonthsOut.toString(), dateLte.getValue());
}

// Test 4: Upsert prevents duplicates
@Test
void testSaveOrUpdateContract_PreventsDoubleSave() {
    AlpacaContractDto dto = createTestContract(alpacaId="test-123");

    // First save
    optionContractService.saveOrUpdateContract(dto, instrument);
    assertEquals(1, optionContractRepository.count());

    // Save same contract again
    optionContractService.saveOrUpdateContract(dto, instrument);
    assertEquals(1, optionContractRepository.count());  // Still 1, not 2
}

// Test 5: Batch continues on single failure
@Test
void testRefreshOptionContracts_HandlesPerInstrumentErrors() {
    List<InstrumentEntity> instruments = Arrays.asList(
        instrumentAAPL,  // Will succeed
        instrumentMSFT   // Will fail (API error)
    );
    when(alpacaService.getOptionContracts(eq("MSFT"), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("API Error"));

    int count = optionContractService.refreshOptionContracts();

    // Should continue processing even though MSFT failed
    assertEquals(1, count);  // Only AAPL succeeded
}
```

---

## Implementation Plan Details

See `/Users/Imre/.claude/plans/purrfect-dancing-hamster.md` for detailed implementation steps.

### Files to Create
1. `src/main/java/co/grtk/srcprofit/entity/OptionContractEntity.java`
2. `src/main/resources/db/migration/V002__Add_Option_Contract_Table.sql`
3. `src/main/java/co/grtk/srcprofit/repository/OptionContractRepository.java`
4. `src/main/java/co/grtk/srcprofit/dto/AlpacaContractsResponseDto.java`
5. `src/main/java/co/grtk/srcprofit/service/OptionContractService.java`
6. `src/main/java/co/grtk/srcprofit/controller/OptionContractRestController.java`
7. `src/test/java/co/grtk/srcprofit/service/OptionContractServiceTest.java`

### Files to Modify
1. `src/main/java/co/grtk/srcprofit/service/AlpacaService.java` - Add `getOptionContracts()` method
2. `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java` - Add 2 scheduled methods

---

## Related Issues

- Depends on: ISSUE-016 (Scheduled Alpaca assets refresh) - provides pattern for scheduled jobs
- Related: ISSUE-014 (Alpaca assets API implementation) - similar API integration pattern
- Blocks: Future option trading analysis and reporting features

---

## Notes

### Alpaca API Reference
- **Endpoint**: `GET /v1beta1/options/contracts`
- **Documentation**: https://docs.alpaca.markets/reference/get-options-contracts-1
- **Query Parameters**: `underlying_symbols`, `status`, `expiration_date_gte/lte`, `strike_price_gte/lte`
- **Response**: Array of option contract objects

### Pattern References
- **refreshAlpacaMarketData()**: MarketDataService.java:55-77 (batch refresh pattern)
- **AlpacaService**: AlpacaService.java (API integration, error handling)
- **ScheduledJobsService**: ScheduledJobsService.java (scheduling pattern)
- **Testing**: AlpacaServiceTest.java (Mockito patterns)

### Configuration
- Uses `alpacaTradingRestClient` bean (from RestClientConfig.java)
- Alpaca credentials from environment: `ALPACA_API_KEY`, `ALPACA_API_SECRET_KEY`

### Database
- Uses Flyway migrations (V001, V002, etc.)
- PostgreSQL 15
- Cascade delete ensures contract cleanup when instrument deleted

### Estimated Effort
- Implementation: ~4 hours
- Testing: ~1 hour
- Documentation: ~30 min
- **Total**: ~5.5 hours

---

## Completed Date

[To be filled when issue closed]
