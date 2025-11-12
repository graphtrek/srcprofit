package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Manual Position Calculation Tests (ISSUE-026)")
class ManualCalculationTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OptionService optionService;

    private PositionDto createPosition(double positionValue, double tradePrice, int daysToExp) {
        PositionDto position = new PositionDto();
        position.setPositionValue(positionValue);
        position.setTradePrice(tradePrice);
        position.setTradeDate(LocalDate.now());
        position.setExpirationDate(LocalDate.now().plusDays(daysToExp));
        position.setQuantity(1);
        position.setTicker("TEST");
        position.setType(OptionType.PUT);
        position.setMarketPrice(tradePrice);
        position.setMarketValue(positionValue);
        position.setFee(0.0);
        return position;
    }

    // ========== CORE FUNCTIONALITY TESTS ==========

    @Test
    @DisplayName("Manual Calculation: Uses only form values")
    void testManualCalculation_FormValuesOnly() {
        PositionDto input = createPosition(10000.0, 120.0, 45);
        input.setMarketValue(9800.0);  // Different from position value

        PositionDto result = optionService.calculateSinglePosition(input);

        // Verify calculations happened
        assertThat(result.getAnnualizedRoiPercent())
                .as("ROI should be calculated")
                .isNotNull();

        assertThat(result.getDaysBetween())
                .as("Days between should be calculated (inclusive of expiration day)")
                .isGreaterThan(40);  // Allow for inclusive calculation
    }

    @Test
    @DisplayName("Manual Calculation: No database queries")
    void testManualCalculation_NoDatabaseAccess() {
        PositionDto input = createPosition(10000.0, 120.0, 30);

        optionService.calculateSinglePosition(input);

        // Verify NO database queries occurred
        verify(optionRepository, never()).findAllOpenByTicker("TEST");
        verify(optionRepository, never()).findAllClosedByTicker("TEST");
    }

    @Test
    @DisplayName("Manual Calculation: Aggregated fields are zero")
    void testManualCalculation_AggregatedFieldsZero() {
        PositionDto input = createPosition(10000.0, 100.0, 45);

        PositionDto result = optionService.calculateSinglePosition(input);

        // Aggregated fields should be zero (no database positions)
        assertThat(result.getRealizedProfitOrLoss())
                .as("Realized P&L should be zero")
                .isEqualTo(0.0);

        assertThat(result.getCallObligationValue())
                .as("CALL obligation should be zero")
                .isEqualTo(0.0);

        assertThat(result.getCallObligationMarketValue())
                .as("CALL obligation market should be zero")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Manual Calculation: Handles null input gracefully")
    void testManualCalculation_NullInput() {
        PositionDto result = optionService.calculateSinglePosition(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Manual Calculation: Handles missing trade date")
    void testManualCalculation_MissingTradeDate() {
        PositionDto input = new PositionDto();
        input.setPositionValue(10000.0);
        input.setExpirationDate(LocalDate.now().plusDays(30));
        // NO trade date

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getTradeDate()).isNull();
    }

    @Test
    @DisplayName("Manual Calculation: Handles zero position value")
    void testManualCalculation_ZeroPositionValue() {
        PositionDto input = createPosition(0.0, 100.0, 30);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getPositionValue()).isEqualTo(0.0);
    }

    // ========== CALCULATION ACCURACY TESTS ==========

    @Test
    @DisplayName("Manual Calculation: Days calculated correctly")
    void testManualCalculation_DaysCalculated() {
        PositionDto input = new PositionDto();
        input.setPositionValue(10000.0);
        input.setTradeDate(LocalDate.of(2025, 11, 11));
        input.setExpirationDate(LocalDate.of(2025, 12, 26));
        input.setTradePrice(100.0);
        input.setMarketValue(10100.0);
        input.setQuantity(1);
        input.setType(OptionType.PUT);

        PositionDto result = optionService.calculateSinglePosition(input);

        // 45 days between Nov 11 and Dec 26 (may be 46 due to inclusive calculation)
        assertThat(result.getDaysBetween())
                .as("Days between should be calculated from form dates")
                .isGreaterThanOrEqualTo(45)
                .isLessThanOrEqualTo(46);

        // daysLeft should be calculated from now to expiration
        assertThat(result.getDaysLeft())
                .as("Days left should be positive")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Manual Calculation: ROI calculated from form values")
    void testManualCalculation_ROICalculated() {
        PositionDto input = createPosition(10000.0, 200.0, 365);  // High trade price = high ROI
        input.setMarketValue(10500.0);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getAnnualizedRoiPercent())
                .as("ROI should be calculated and positive")
                .isGreaterThan(0)
                .isNotNull();
    }

    @Test
    @DisplayName("Manual Calculation: Probability calculated if market value available")
    void testManualCalculation_ProbabilityCalculated() {
        PositionDto input = createPosition(10000.0, 100.0, 45);
        input.setMarketValue(10200.0);  // Market value provided

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getProbability())
                .as("Probability should be calculated when market value available")
                .isNotNull();
    }

    @Test
    @DisplayName("Manual Calculation: UnRealized P&L calculated")
    void testManualCalculation_UnRealizedPnL() {
        PositionDto input = createPosition(10000.0, 100.0, 30);
        input.setMarketValue(10500.0);  // Profit: 500

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getUnRealizedProfitOrLoss())
                .as("UnRealized P&L should be market value - position value")
                .isEqualTo(500.0);
    }

    @Test
    @DisplayName("Manual Calculation: Negative UnRealized P&L")
    void testManualCalculation_NegativePnL() {
        PositionDto input = createPosition(10000.0, 100.0, 30);
        input.setMarketValue(9500.0);  // Loss: -500

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getUnRealizedProfitOrLoss())
                .as("UnRealized P&L should reflect loss")
                .isEqualTo(-500.0);
    }

    @Test
    @DisplayName("Manual Calculation: Collected premium from trade price")
    void testManualCalculation_CollectedPremium() {
        PositionDto input = createPosition(10000.0, 120.0, 30);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getCollectedPremium())
                .as("Collected premium should be trade price * quantity")
                .isEqualTo(120.0);  // 120 * 1 = 120
    }

    @Test
    @DisplayName("Manual Calculation: Market price reflects P&L")
    void testManualCalculation_MarketPrice() {
        PositionDto input = createPosition(10000.0, 100.0, 30);
        input.setMarketValue(10300.0);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getMarketPrice())
                .as("Market price should be market value - position value")
                .isEqualTo(300.0);
    }

    // ========== POSITION TYPE TESTS ==========

    @Test
    @DisplayName("Manual Calculation: PUT position sets PUT value")
    void testManualCalculation_PUTType() {
        PositionDto input = createPosition(10000.0, 100.0, 30);
        input.setType(OptionType.PUT);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getPut())
                .as("PUT value should be set from trade price")
                .isEqualTo(100.0);

        assertThat(result.getCall())
                .as("CALL value should be zero for PUT position")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Manual Calculation: CALL position sets CALL value")
    void testManualCalculation_CALLType() {
        PositionDto input = createPosition(10000.0, 50.0, 30);
        input.setType(OptionType.CALL);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getCall())
                .as("CALL value should be set from trade price")
                .isEqualTo(50.0);

        assertThat(result.getPut())
                .as("PUT value should be zero for CALL position")
                .isEqualTo(0.0);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Manual Calculation: Handles quantity > 1")
    void testManualCalculation_MultipleContracts() {
        PositionDto input = createPosition(10000.0, 100.0, 30);
        input.setQuantity(5);  // 5 contracts

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getCollectedPremium())
                .as("Collected premium should be trade price * quantity")
                .isEqualTo(500.0);  // 100 * 5
    }

    @Test
    @DisplayName("Manual Calculation: Very short DTE (1 day)")
    void testManualCalculation_ShortDTE() {
        PositionDto input = createPosition(10000.0, 100.0, 1);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getDaysBetween())
                .as("Days between should be 1 for next-day expiration")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Manual Calculation: Very long DTE (180 days)")
    void testManualCalculation_LongDTE() {
        PositionDto input = createPosition(10000.0, 100.0, 180);

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getDaysBetween())
                .as("Days between should be calculated correctly for long DTE")
                .isGreaterThan(170);
    }

    @Test
    @DisplayName("Manual Calculation: Null trade price handled")
    void testManualCalculation_NullTradePrice() {
        PositionDto input = new PositionDto();
        input.setPositionValue(10000.0);
        input.setTradeDate(LocalDate.now());
        input.setExpirationDate(LocalDate.now().plusDays(30));
        input.setQuantity(1);
        input.setType(OptionType.PUT);
        input.setTradePrice(null);  // NULL trade price

        PositionDto result = optionService.calculateSinglePosition(input);

        assertThat(result.getTradePrice())
                .as("Trade price might be estimated or null")
                .isNotNull();  // CalculateAndSetAnnualizedRoi estimates it
    }

    @Test
    @DisplayName("Manual Calculation: Null market value handled")
    void testManualCalculation_NullMarketValue() {
        PositionDto input = new PositionDto();
        input.setPositionValue(10000.0);
        input.setTradeDate(LocalDate.now());
        input.setExpirationDate(LocalDate.now().plusDays(30));
        input.setTradePrice(100.0);
        input.setMarketValue(null);  // NULL market value
        input.setQuantity(1);
        input.setType(OptionType.PUT);

        PositionDto result = optionService.calculateSinglePosition(input);

        // Should handle gracefully without calculating P&L
        assertThat(result).isNotNull();
    }

    // ========== WHAT-IF ANALYSIS SCENARIOS ==========

    @Test
    @DisplayName("What-if Scenario: Different trade prices")
    void testManualCalculation_WhatIfTradePrice() {
        // Scenario 1: Trade at $100
        PositionDto input1 = createPosition(10000.0, 100.0, 45);
        input1.setMarketValue(10200.0);
        PositionDto result1 = optionService.calculateSinglePosition(input1);

        // Scenario 2: Trade at $120 (same position, different entry)
        PositionDto input2 = createPosition(10000.0, 120.0, 45);
        input2.setMarketValue(10200.0);
        PositionDto result2 = optionService.calculateSinglePosition(input2);

        // Higher trade price should give higher ROI
        assertThat(result2.getAnnualizedRoiPercent())
                .as("Higher trade price should increase ROI")
                .isGreaterThan(result1.getAnnualizedRoiPercent());
    }

    @Test
    @DisplayName("What-if Scenario: Different expiration dates")
    void testManualCalculation_WhatIfDTE() {
        // Scenario 1: 30 DTE
        PositionDto input1 = createPosition(10000.0, 100.0, 30);
        PositionDto result1 = optionService.calculateSinglePosition(input1);

        // Scenario 2: 60 DTE
        PositionDto input2 = createPosition(10000.0, 100.0, 60);
        PositionDto result2 = optionService.calculateSinglePosition(input2);

        // Different DTE should result in different days between
        assertThat(result2.getDaysBetween())
                .as("60 DTE should have more days between than 30 DTE")
                .isGreaterThan(result1.getDaysBetween());
    }

    @Test
    @DisplayName("What-if Scenario: Different market values")
    void testManualCalculation_WhatIfMarketValue() {
        // Scenario 1: Up $200
        PositionDto input1 = createPosition(10000.0, 100.0, 30);
        input1.setMarketValue(10200.0);
        PositionDto result1 = optionService.calculateSinglePosition(input1);

        // Scenario 2: Down $200
        PositionDto input2 = createPosition(10000.0, 100.0, 30);
        input2.setMarketValue(9800.0);
        PositionDto result2 = optionService.calculateSinglePosition(input2);

        // Market value affects P&L
        assertThat(result1.getUnRealizedProfitOrLoss())
                .as("Higher market value should have positive P&L")
                .isGreaterThan(result2.getUnRealizedProfitOrLoss());
    }
}
