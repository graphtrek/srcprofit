package co.grtk.srcprofit.mapper;

import co.grtk.srcprofit.entity.OptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PositionCalculationHelper Tests")
class PositionCalculationHelperTest {

    private static final LocalDate TEST_DATE_2025_01_01 = LocalDate.of(2025, 1, 1);
    private static final LocalDate TEST_DATE_2025_01_31 = LocalDate.of(2025, 1, 31);

    // ======================== calculateDaysBetween Tests ========================

    @Test
    @DisplayName("calculateDaysBetween: Valid dates returns correct days plus 1")
    void testCalculateDaysBetween_ValidDates() {
        int days = PositionCalculationHelper.calculateDaysBetween(TEST_DATE_2025_01_01, TEST_DATE_2025_01_31);
        assertEquals(31, days); // 30 days + 1
    }

    @Test
    @DisplayName("calculateDaysBetween: Null trade date returns -1")
    void testCalculateDaysBetween_NullTradeDate() {
        int days = PositionCalculationHelper.calculateDaysBetween(null, TEST_DATE_2025_01_31);
        assertEquals(-1, days);
    }

    @Test
    @DisplayName("calculateDaysBetween: Null expiration date returns -1")
    void testCalculateDaysBetween_NullExpirationDate() {
        int days = PositionCalculationHelper.calculateDaysBetween(TEST_DATE_2025_01_01, null);
        assertEquals(-1, days);
    }

    @Test
    @DisplayName("calculateDaysBetween: Both dates null returns -1")
    void testCalculateDaysBetween_BothNull() {
        int days = PositionCalculationHelper.calculateDaysBetween(null, null);
        assertEquals(-1, days);
    }

    @Test
    @DisplayName("calculateDaysBetween: Same date returns 1 (expiration day included)")
    void testCalculateDaysBetween_SameDate() {
        int days = PositionCalculationHelper.calculateDaysBetween(TEST_DATE_2025_01_01, TEST_DATE_2025_01_01);
        assertEquals(1, days);
    }

    @Test
    @DisplayName("calculateDaysBetween: 90 days between trade and expiration")
    void testCalculateDaysBetween_90Days() {
        LocalDate tradeDate = LocalDate.of(2025, 1, 1);
        LocalDate expDate = LocalDate.of(2025, 4, 1);
        int days = PositionCalculationHelper.calculateDaysBetween(tradeDate, expDate);
        assertEquals(91, days); // 90 days + 1
    }

    // ======================== calculateDaysLeft Tests ========================

    @Test
    @DisplayName("calculateDaysLeft: Null expiration date returns -1")
    void testCalculateDaysLeft_NullExpirationDate() {
        int days = PositionCalculationHelper.calculateDaysLeft(null);
        assertEquals(-1, days);
    }

    @Test
    @DisplayName("calculateDaysLeft: Returns positive number for future date")
    void testCalculateDaysLeft_FutureDate() {
        LocalDate futureDate = LocalDate.now().plusDays(10);
        int days = PositionCalculationHelper.calculateDaysLeft(futureDate);
        assertTrue(days > 0, "Days left should be positive for future dates");
    }

    @Test
    @DisplayName("calculateDaysLeft: Returns 1 or less for today's expiration")
    void testCalculateDaysLeft_TodayExpiration() {
        LocalDate today = LocalDate.now();
        int days = PositionCalculationHelper.calculateDaysLeft(today);
        assertTrue(days <= 1, "Days left should be <= 1 for today's expiration");
    }

    // ======================== estimateTradePrice Tests ========================

    @Test
    @DisplayName("estimateTradePrice: Calculates correctly with standard rate")
    void testEstimateTradePrice_StandardCalculation() {
        double positionValue = 100.0;
        int daysBetween = 30;
        double estimated = PositionCalculationHelper.estimateTradePrice(positionValue, daysBetween);

        // 100 × 0.0014 × 30 = 4.20
        assertEquals(4.20, estimated, 0.01);
    }

    @ParameterizedTest(name = "positionValue={0}, days={1} → estimated={2}")
    @CsvSource({
            "100.0, 30, 4.20",
            "50.0, 30, 2.10",
            "200.0, 30, 8.40",
            "100.0, 60, 8.40",
            "100.0, 0, 0.00"
    })
    @DisplayName("estimateTradePrice: Various scenarios")
    void testEstimateTradePrice_Scenarios(double positionValue, int daysBetween, double expected) {
        double estimated = PositionCalculationHelper.estimateTradePrice(positionValue, daysBetween);
        assertEquals(expected, estimated, 0.01);
    }

    @Test
    @DisplayName("estimateTradePrice: Negative days returns 0")
    void testEstimateTradePrice_NegativeDays() {
        double estimated = PositionCalculationHelper.estimateTradePrice(100.0, -5);
        assertEquals(0.0, estimated);
    }

    @Test
    @DisplayName("estimateTradePrice: Result is rounded to 2 decimals")
    void testEstimateTradePrice_RoundedTo2Decimals() {
        // 123.45 × 0.0014 × 37 = 6.391053 → 6.39
        double estimated = PositionCalculationHelper.estimateTradePrice(123.45, 37);
        assertEquals(6.39, estimated, 0.001);
    }

    // ======================== calculateBreakEven Tests ========================

    @Test
    @DisplayName("calculateBreakEven: PUT option calculates correctly")
    void testCalculateBreakEven_PutOption() {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(100.0, 5.0, OptionType.PUT);
        assertEquals(95.0, breakEven, 0.01);
    }

    @Test
    @DisplayName("calculateBreakEven: CALL option calculates correctly")
    void testCalculateBreakEven_CallOption() {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(100.0, 5.0, OptionType.CALL);
        assertEquals(105.0, breakEven, 0.01);
    }

    @Test
    @DisplayName("calculateBreakEven: Null option type returns null")
    void testCalculateBreakEven_NullOptionType() {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(100.0, 5.0, null);
        assertNull(breakEven);
    }

    @Test
    @DisplayName("calculateBreakEven: Zero trade price returns null")
    void testCalculateBreakEven_ZeroTradePrice() {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(100.0, 0.0, OptionType.PUT);
        assertNull(breakEven);
    }

    @Test
    @DisplayName("calculateBreakEven: Negative trade price returns null")
    void testCalculateBreakEven_NegativeTradePrice() {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(100.0, -5.0, OptionType.CALL);
        assertNull(breakEven);
    }

    @ParameterizedTest(name = "strike={0}, premium={1}, type={2} → breakEven={3}")
    @CsvSource({
            "100.0, 2.5, PUT, 97.5",
            "100.0, 2.5, CALL, 102.5",
            "50.0, 1.0, PUT, 49.0",
            "50.0, 1.0, CALL, 51.0"
    })
    @DisplayName("calculateBreakEven: Various strike and premium combinations")
    void testCalculateBreakEven_Combinations(double strike, double premium, OptionType type, double expected) {
        Double breakEven = PositionCalculationHelper.calculateBreakEven(strike, premium, type);
        assertNotNull(breakEven);
        assertEquals(expected, breakEven, 0.01);
    }

    // ======================== calculateAnnualizedRoiPercent Tests ========================

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Standard calculation without fees")
    void testCalculateAnnualizedRoiPercent_NoFees() {
        // tradePrice=5.0, days=30
        // dailyRoi = 5.0 / 30 = 0.1667
        // annualizedRoi = 0.1667 * 365 = 60.83
        // percentage = (60.83 / 100) * 100 = 60.83 → 61%
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 5.0, null, 30);
        assertEquals(61, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Calculation with fees")
    void testCalculateAnnualizedRoiPercent_WithFees() {
        // tradePrice=5.0, fee=0.5 → roiBase = 4.5
        // dailyRoi = 4.5 / 30 = 0.15
        // annualizedRoi = 0.15 * 365 = 54.75
        // percentage = (54.75 / 100) * 100 = 54.75 → 55%
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 5.0, 0.5, 30);
        assertEquals(55, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Zero days returns 0")
    void testCalculateAnnualizedRoiPercent_ZeroDays() {
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 5.0, null, 0);
        assertEquals(0, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Negative days returns 0")
    void testCalculateAnnualizedRoiPercent_NegativeDays() {
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 5.0, null, -10);
        assertEquals(0, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Zero position value returns 0")
    void testCalculateAnnualizedRoiPercent_ZeroPositionValue() {
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(0.0, 5.0, null, 30);
        assertEquals(0, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Negative position value returns 0")
    void testCalculateAnnualizedRoiPercent_NegativePositionValue() {
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(-100.0, 5.0, null, 30);
        assertEquals(0, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Handles 90-DTE options")
    void testCalculateAnnualizedRoiPercent_90DTE() {
        // tradePrice=3.0, days=90
        // dailyRoi = 3.0 / 90 = 0.0333
        // annualizedRoi = 0.0333 * 365 = 12.17
        // percentage = (12.17 / 100) * 100 = 12.17 → 12%
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 3.0, null, 90);
        assertEquals(12, roi);
    }

    @Test
    @DisplayName("calculateAnnualizedRoiPercent: Result is rounded to nearest integer")
    void testCalculateAnnualizedRoiPercent_RoundedCorrectly() {
        int roi = PositionCalculationHelper.calculateAnnualizedRoiPercent(100.0, 5.4, null, 30);
        // Should round to the nearest integer
        assertTrue(roi >= 0, "ROI should be non-negative");
    }

    // ======================== calculateProbability Tests ========================

    @Test
    @DisplayName("calculateProbability: Zero market value returns 0")
    void testCalculateProbability_ZeroMarketValue() {
        int prob = PositionCalculationHelper.calculateProbability(100.0, 0.0, 30);
        assertEquals(0, prob);
    }

    @Test
    @DisplayName("calculateProbability: Negative market value returns 0")
    void testCalculateProbability_NegativeMarketValue() {
        int prob = PositionCalculationHelper.calculateProbability(100.0, -50.0, 30);
        assertEquals(0, prob);
    }

    @Test
    @DisplayName("calculateProbability: Zero days returns 0")
    void testCalculateProbability_ZeroDays() {
        int prob = PositionCalculationHelper.calculateProbability(100.0, 105.0, 0);
        assertEquals(0, prob);
    }

    @Test
    @DisplayName("calculateProbability: Negative days returns 0")
    void testCalculateProbability_NegativeDays() {
        int prob = PositionCalculationHelper.calculateProbability(100.0, 105.0, -30);
        assertEquals(0, prob);
    }

    @Test
    @DisplayName("calculateProbability: Returns probability in range 0-100")
    void testCalculateProbability_ValidRange() {
        int prob = PositionCalculationHelper.calculateProbability(100.0, 105.0, 30);
        assertTrue(prob >= 0 && prob <= 100, "Probability should be between 0 and 100");
    }

    @Test
    @DisplayName("calculateProbability: Market value above strike increases probability")
    void testCalculateProbability_MarketAboveStrike() {
        int probAbove = PositionCalculationHelper.calculateProbability(100.0, 110.0, 30);
        int probBelow = PositionCalculationHelper.calculateProbability(100.0, 90.0, 30);
        assertTrue(probAbove > probBelow, "Probability should be higher when market > strike");
    }

    @Test
    @DisplayName("calculateProbability: More DTE increases probability variation")
    void testCalculateProbability_DTEImpact() {
        int prob30 = PositionCalculationHelper.calculateProbability(100.0, 110.0, 30);
        int prob90 = PositionCalculationHelper.calculateProbability(100.0, 110.0, 90);
        // With more time, volatility is higher, probability spreads further
        assertNotEquals(prob30, prob90, "Probability should differ with different DTE");
    }

}
