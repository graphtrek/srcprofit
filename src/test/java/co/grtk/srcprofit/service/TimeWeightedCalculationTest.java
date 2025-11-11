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

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@DisplayName("Time-Weighted Portfolio Calculation Tests (ISSUE-025)")
class TimeWeightedCalculationTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OptionService optionService;

    private PositionDto createPosition(double positionValue, int daysLeft) {
        PositionDto position = new PositionDto();
        position.setPositionValue(positionValue);
        position.setDaysLeft(daysLeft);
        position.setQuantity(1);
        position.setTradeDate(LocalDate.now());
        position.setExpirationDate(LocalDate.now().plusDays(daysLeft));
        position.setTicker("TEST");
        position.setType(OptionType.PUT);
        position.setTradePrice(positionValue);
        position.setMarketPrice(positionValue);
        position.setMarketValue(positionValue);
        position.setFee(0.0);
        position.setBreakEven(0.0);
        return position;
    }

    /**
     * Helper to call private calculateNormalizedTimeWeight method via reflection
     */
    private double callCalculateNormalizedTimeWeight(Integer daysLeft) throws Exception {
        Method method = OptionService.class.getDeclaredMethod("calculateNormalizedTimeWeight", Integer.class);
        method.setAccessible(true);
        Object result = method.invoke(optionService, daysLeft);
        return (Double) result;
    }

    /**
     * Helper to call private calculateTimeWeightedROI method via reflection
     */
    private double callCalculateTimeWeightedROI(List<PositionDto> openPositions) throws Exception {
        Method method = OptionService.class.getDeclaredMethod("calculateTimeWeightedROI", List.class);
        method.setAccessible(true);
        Object result = method.invoke(optionService, openPositions);
        return (Double) result;
    }

    /**
     * Helper to call private calculateTimeWeightedProbability method via reflection
     */
    private double callCalculateTimeWeightedProbability(List<PositionDto> openPositions) throws Exception {
        Method method = OptionService.class.getDeclaredMethod("calculateTimeWeightedProbability", List.class);
        method.setAccessible(true);
        Object result = method.invoke(optionService, openPositions);
        return (Double) result;
    }

    // ========== TIME WEIGHT CALCULATION TESTS ==========

    @Test
    @DisplayName("Time Weight: 45 DTE baseline = 1.0")
    void testNormalizedTimeWeight_45DTE_Baseline() throws Exception {
        // 45 DTE should have normalized weight of 1.0
        double weight = callCalculateNormalizedTimeWeight(45);
        assertThat(weight)
                .as("45 DTE (TastyTrade standard) should have weight of 1.0")
                .isCloseTo(1.0, within(0.01));
    }

    @Test
    @DisplayName("Time Weight: 60 DTE has 15% more weight than 45 DTE")
    void testNormalizedTimeWeight_60DTE() throws Exception {
        // √(60/45) = 1.1547
        double weight = callCalculateNormalizedTimeWeight(60);
        assertThat(weight)
                .as("60 DTE should have √(60/45) = 1.15 weight")
                .isCloseTo(1.15, within(0.01));
    }

    @Test
    @DisplayName("Time Weight: 7 DTE has 61% less weight than 45 DTE")
    void testNormalizedTimeWeight_7DTE() throws Exception {
        // √(7/45) = 0.3944
        double weight = callCalculateNormalizedTimeWeight(7);
        assertThat(weight)
                .as("7 DTE should have √(7/45) = 0.39 weight")
                .isCloseTo(0.39, within(0.01));
    }

    @Test
    @DisplayName("Time Weight: 30 DTE has 18% less weight than 45 DTE")
    void testNormalizedTimeWeight_30DTE() throws Exception {
        // √(30/45) = 0.8165
        double weight = callCalculateNormalizedTimeWeight(30);
        assertThat(weight)
                .as("30 DTE should have √(30/45) = 0.82 weight")
                .isCloseTo(0.82, within(0.01));
    }

    @Test
    @DisplayName("Time Weight: 90 DTE has 41% more weight than 45 DTE")
    void testNormalizedTimeWeight_90DTE() throws Exception {
        // √(90/45) = 1.4142
        double weight = callCalculateNormalizedTimeWeight(90);
        assertThat(weight)
                .as("90 DTE should have √(90/45) = 1.41 weight")
                .isCloseTo(1.41, within(0.01));
    }

    @Test
    @DisplayName("Time Weight: Null daysLeft returns 0.0")
    void testNormalizedTimeWeight_Null() throws Exception {
        double weight = callCalculateNormalizedTimeWeight(null);
        assertThat(weight)
                .as("Null daysLeft should return 0.0")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Time Weight: Zero daysLeft returns 0.0")
    void testNormalizedTimeWeight_Zero() throws Exception {
        double weight = callCalculateNormalizedTimeWeight(0);
        assertThat(weight)
                .as("Zero daysLeft should return 0.0")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Time Weight: Negative daysLeft returns 0.0")
    void testNormalizedTimeWeight_Negative() throws Exception {
        double weight = callCalculateNormalizedTimeWeight(-5);
        assertThat(weight)
                .as("Negative daysLeft should return 0.0")
                .isEqualTo(0.0);
    }

    // ========== TIME-WEIGHTED ROI TESTS ==========

    @Test
    @DisplayName("Time-Weighted ROI: Longer DTE dominates portfolio")
    void testTimeWeightedROI_LongerDTEDominates() throws Exception {
        // Position 1: $10,000, 100% ROI, 7 DTE
        PositionDto shortPos = createPosition(10000, 7);
        shortPos.setAnnualizedRoiPercent(100);

        // Position 2: $10,000, 30% ROI, 60 DTE
        PositionDto longPos = createPosition(10000, 60);
        longPos.setAnnualizedRoiPercent(30);

        List<PositionDto> openPositions = List.of(shortPos, longPos);

        // Time-weighted ROI:
        // Weight1 = 10000 * √(7/45) = 3,944
        // Weight2 = 10000 * √(60/45) = 11,547
        // ROI = (100*3944 + 30*11547) / 15491 = 47.8%
        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);

        assertThat(timeWeightedROI)
                .as("Time-weighted ROI should be pulled down by longer-dated position")
                .isCloseTo(48.0, within(2.0));
    }

    @Test
    @DisplayName("Time-Weighted ROI: Single position returns its ROI")
    void testTimeWeightedROI_SinglePosition() throws Exception {
        PositionDto pos = createPosition(5000, 30);
        pos.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(pos);

        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);

        assertThat(timeWeightedROI)
                .as("Single position time-weighted ROI should equal its ROI")
                .isEqualTo(50.0);
    }

    @Test
    @DisplayName("Time-Weighted ROI: Empty list returns 0")
    void testTimeWeightedROI_EmptyList() throws Exception {
        double timeWeightedROI = callCalculateTimeWeightedROI(new ArrayList<>());

        assertThat(timeWeightedROI)
                .as("Empty position list should return 0")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Time-Weighted ROI: Equal DTE positions equal capital-weighted")
    void testTimeWeightedROI_EqualDTE() throws Exception {
        PositionDto pos1 = createPosition(5000, 45);
        pos1.setAnnualizedRoiPercent(40);

        PositionDto pos2 = createPosition(10000, 45);
        pos2.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(pos1, pos2);

        // With equal DTE, time weight cancels out
        // Capital-weighted: (40*5000 + 50*10000) / 15000 = 46.67%
        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);

        assertThat(timeWeightedROI)
                .as("Equal DTE should match capital-weighted calculation")
                .isCloseTo(47.0, within(1.0));
    }

    @Test
    @DisplayName("Time-Weighted ROI: Three positions with varied DTE")
    void testTimeWeightedROI_ThreePositions() throws Exception {
        // Position A: $10K, 50% ROI, 7 DTE   (short)
        PositionDto posA = createPosition(10000, 7);
        posA.setAnnualizedRoiPercent(50);

        // Position B: $10K, 50% ROI, 45 DTE  (baseline)
        PositionDto posB = createPosition(10000, 45);
        posB.setAnnualizedRoiPercent(50);

        // Position C: $10K, 50% ROI, 90 DTE  (long)
        PositionDto posC = createPosition(10000, 90);
        posC.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(posA, posB, posC);

        // All have same ROI, so result should be 50% regardless of weighting
        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);

        assertThat(timeWeightedROI)
                .as("Equal ROIs should result in that ROI regardless of time weighting")
                .isCloseTo(50.0, within(1.0));
    }

    @Test
    @DisplayName("Time-Weighted ROI: Null/zero daysLeft skipped gracefully")
    void testTimeWeightedROI_NullDaysLeft() throws Exception {
        PositionDto pos1 = createPosition(5000, 30);
        pos1.setAnnualizedRoiPercent(50);

        PositionDto pos2 = createPosition(5000, 0);  // Zero DTE
        pos2.setAnnualizedRoiPercent(100);

        List<PositionDto> openPositions = List.of(pos1, pos2);

        // Should only use pos1 (pos2 skipped due to zero daysLeft)
        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);

        assertThat(timeWeightedROI)
                .as("Should skip zero daysLeft and use only valid position")
                .isEqualTo(50.0);
    }

    // ========== TIME-WEIGHTED PROBABILITY TESTS ==========

    @Test
    @DisplayName("Time-Weighted Probability: Equal capital, unequal DTE")
    void testTimeWeightedProbability_UnequalDTE() throws Exception {
        // Position 1: $5,000, 80% probability, 7 DTE
        PositionDto shortPos = createPosition(5000, 7);
        shortPos.setProbability(80);
        shortPos.setAnnualizedRoiPercent(50);

        // Position 2: $5,000, 40% probability, 60 DTE
        PositionDto longPos = createPosition(5000, 60);
        longPos.setProbability(40);
        longPos.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(shortPos, longPos);

        // Time-weighted:
        // Weight1 = 5000 * √(7/45) = 1,972
        // Weight2 = 5000 * √(60/45) = 5,774
        // Prob = (80*1972 + 40*5774) / 7746 = 50.2%
        double timeWeightedProb = callCalculateTimeWeightedProbability(openPositions);

        assertThat(timeWeightedProb)
                .as("Time-weighted probability pulled toward longer-dated position")
                .isCloseTo(50.0, within(2.0));
    }

    @Test
    @DisplayName("Time-Weighted Probability: Single position returns its probability")
    void testTimeWeightedProbability_SinglePosition() throws Exception {
        PositionDto pos = createPosition(5000, 30);
        pos.setProbability(70);
        pos.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(pos);

        double timeWeightedProb = callCalculateTimeWeightedProbability(openPositions);

        assertThat(timeWeightedProb)
                .as("Single position time-weighted probability should equal its probability")
                .isEqualTo(70.0);
    }

    @Test
    @DisplayName("Time-Weighted Probability: Empty list returns 0")
    void testTimeWeightedProbability_EmptyList() throws Exception {
        double timeWeightedProb = callCalculateTimeWeightedProbability(new ArrayList<>());

        assertThat(timeWeightedProb)
                .as("Empty position list should return 0")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Time-Weighted Probability: Extreme DTE difference")
    void testTimeWeightedProbability_ExtremeDifference() throws Exception {
        // Position 1: $1,000, 95% probability, 1 DTE (expiring tomorrow)
        PositionDto veryShortPos = createPosition(1000, 1);
        veryShortPos.setProbability(95);
        veryShortPos.setAnnualizedRoiPercent(50);

        // Position 2: $1,000, 10% probability, 180 DTE (6 months)
        PositionDto veryLongPos = createPosition(1000, 180);
        veryLongPos.setProbability(10);
        veryLongPos.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(veryShortPos, veryLongPos);

        // Time weight ratio: √(180/45) / √(1/45) = 2.0 / 0.149 = 13.4×
        // Long position should heavily dominate
        double timeWeightedProb = callCalculateTimeWeightedProbability(openPositions);

        assertThat(timeWeightedProb)
                .as("Long-dated position should dominate with extreme DTE difference")
                .isLessThan(30); // Much closer to 10% than 95%
    }

    @Test
    @DisplayName("Time-Weighted Probability: Null probability skipped")
    void testTimeWeightedProbability_NullProbability() throws Exception {
        PositionDto pos1 = createPosition(5000, 30);
        pos1.setProbability(70);
        pos1.setAnnualizedRoiPercent(50);

        PositionDto pos2 = createPosition(5000, 30);
        pos2.setProbability(null);  // Null probability
        pos2.setAnnualizedRoiPercent(50);

        List<PositionDto> openPositions = List.of(pos1, pos2);

        // Should only use pos1
        double timeWeightedProb = callCalculateTimeWeightedProbability(openPositions);

        assertThat(timeWeightedProb)
                .as("Should skip null probability and use only valid position")
                .isEqualTo(70.0);
    }

    // ========== COMPARATIVE TESTS (Capital vs Time Weighting) ==========

    @Test
    @DisplayName("Comparison: Time-weighted differs from capital-weighted for unequal DTE")
    void testComparison_TimeVsCapital_UnequalDTE() throws Exception {
        // Create positions with same capital but different DTE and ROIs
        PositionDto pos1 = createPosition(10000, 10);  // 10 DTE, short-term
        pos1.setAnnualizedRoiPercent(80);
        pos1.setProbability(85);

        PositionDto pos2 = createPosition(10000, 90);  // 90 DTE, long-term
        pos2.setAnnualizedRoiPercent(40);
        pos2.setProbability(60);

        List<PositionDto> openPositions = List.of(pos1, pos2);

        // Capital-weighted (equal capital): (80+40)/2 = 60%, (85+60)/2 = 72.5%
        // Time-weighted: Long position gets √(90/45)/√(10/45) = 3× more weight
        // Should pull metrics toward long position (40%, 60%)

        double timeWeightedROI = callCalculateTimeWeightedROI(openPositions);
        double timeWeightedProb = callCalculateTimeWeightedProbability(openPositions);

        assertThat(timeWeightedROI)
                .as("Time-weighted ROI should be pulled toward long position")
                .isLessThan(60)  // Less than capital-weighted average
                .isGreaterThan(40);  // But not equal to long position

        assertThat(timeWeightedProb)
                .as("Time-weighted probability should be pulled toward long position")
                .isLessThan(72)  // Less than capital-weighted average
                .isGreaterThan(60);  // But not equal to long position
    }

    @Test
    @DisplayName("Verification: Time weighting matches theoretical sqrt(t) scaling")
    void testVerification_SqrtTimeScaling() throws Exception {
        // Verify that weight ratio matches sqrt ratio
        double weight30 = callCalculateNormalizedTimeWeight(30);
        double weight60 = callCalculateNormalizedTimeWeight(60);

        double ratio = weight60 / weight30;
        double expectedRatio = Math.sqrt(60.0 / 30.0);  // = √2 = 1.414

        assertThat(ratio)
                .as("Weight ratio should match sqrt(time) ratio")
                .isCloseTo(expectedRatio, within(0.01));
    }
}
