package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.ChartDataDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.mapper.MapperUtils;
import co.grtk.srcprofit.repository.OptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for ISSUE-008: Dashboard Put Premium Displaying NaN Values
 *
 * This test suite ensures that:
 * 1. Null tradePrice values are handled gracefully
 * 2. Division by zero doesn't produce NaN
 * 3. CSV output doesn't contain "NaN" or "Infinity"
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ISSUE-008: Dashboard NaN Fix Tests")
class OptionServiceNanFixTest {

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private OptionService optionService;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();
    }

    @Test
    @DisplayName("Should handle null tradePrice gracefully in getDailyPremium")
    void testGetDailyPremium_WithNullTradePrice() {
        // Given: Options with null and valid tradePrices
        OptionEntity optionWithNullPrice = new OptionEntity();
        optionWithNullPrice.setTradePrice(null);
        optionWithNullPrice.setQuantity(-1);
        optionWithNullPrice.setTradeDate(testDate);

        OptionEntity optionWithValidPrice = new OptionEntity();
        optionWithValidPrice.setTradePrice(100.0);
        optionWithValidPrice.setQuantity(-1);
        optionWithValidPrice.setTradeDate(testDate);

        when(optionRepository.findAll()).thenReturn(List.of(optionWithNullPrice, optionWithValidPrice));

        // When: Calculate daily premium
        Map<LocalDate, BigDecimal> result = optionService.getDailyPremium();

        // Then: Should only include valid prices, not NaN
        assertThat(result).isNotEmpty();
        BigDecimal dailyTotal = result.get(testDate);
        assertThat(dailyTotal).isNotNull();
        assertThat(dailyTotal.toString()).doesNotContain("NaN");
        assertThat(dailyTotal.toString()).doesNotContain("Infinity");
        assertThat(dailyTotal).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should skip all options with null tradePrice")
    void testGetDailyPremium_AllNullTradePrice() {
        // Given: Only options with null tradePrices
        OptionEntity option = new OptionEntity();
        option.setTradePrice(null);
        option.setQuantity(-1);
        option.setTradeDate(testDate);

        when(optionRepository.findAll()).thenReturn(List.of(option));

        // When: Calculate daily premium
        Map<LocalDate, BigDecimal> result = optionService.getDailyPremium();

        // Then: Should return empty map (no valid options to sum)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty map when no options exist")
    void testGetDailyPremium_EmptyOptionsList() {
        // Given: No options
        when(optionRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Calculate daily premium
        Map<LocalDate, BigDecimal> result = optionService.getDailyPremium();

        // Then: Should return empty map gracefully
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("MapperUtils.getValuesCsv should never contain NaN")
    void testGetValuesCsv_NoNaNInOutput() {
        // Given: Map with valid BigDecimal values
        Map<LocalDate, BigDecimal> data = new LinkedHashMap<>();
        data.put(LocalDate.of(2025, 1, 1), new BigDecimal("100.50"));
        data.put(LocalDate.of(2025, 1, 2), new BigDecimal("200.75"));
        data.put(LocalDate.of(2025, 1, 3), new BigDecimal("50.25"));

        // When: Convert to CSV
        String csv = MapperUtils.getValuesCsv(data);

        // Then: Should not contain NaN or Infinity
        assertThat(csv).doesNotContain("NaN");
        assertThat(csv).doesNotContain("Infinity");
        assertThat(csv).contains("100.50");
        assertThat(csv).contains("200.75");
        assertThat(csv).contains("50.25");
    }

    @Test
    @DisplayName("MapperUtils.getValuesCsv should handle edge case BigDecimals")
    void testGetValuesCsv_WithEdgeCases() {
        // Given: Map with zero and very small values
        Map<LocalDate, BigDecimal> data = new LinkedHashMap<>();
        data.put(LocalDate.of(2025, 1, 1), BigDecimal.ZERO);
        data.put(LocalDate.of(2025, 1, 2), new BigDecimal("0.01"));
        data.put(LocalDate.of(2025, 1, 3), new BigDecimal("999999.99"));

        // When: Convert to CSV
        String csv = MapperUtils.getValuesCsv(data);

        // Then: Should handle all values correctly
        assertThat(csv).doesNotContain("NaN");
        assertThat(csv).doesNotContain("Infinity");
        assertThat(csv).contains("0");
        assertThat(csv).contains("0.01");
        assertThat(csv).contains("999999.99");
    }

    @Test
    @DisplayName("Should handle zero positionValue in percentage calculation without division by zero")
    void testCalculatePosition_ZeroPositionValue() {
        // Given: Position data with zero position value (positionValue = 0)
        // This tests that division by zero is prevented
        PositionDto positionDto = new PositionDto();

        // Simulating zero position value scenario
        List<PositionDto> openPositions = new ArrayList<>();
        List<PositionDto> closedPositions = new ArrayList<>();

        // When: Calculate position with empty positions (zero position value)
        // This would previously cause: marketValue / 0 = NaN or Infinity
        optionService.calculatePosition(positionDto, openPositions, closedPositions);

        // Then: Should not produce NaN in percentage (should default to 0.0)
        assertThat(positionDto.getMarketVsPositionsPercentage()).isNotNull();
        // The fixed code returns 0.0 when positionValue <= 0
        assertThat(positionDto.getMarketVsPositionsPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getDailyPremium should filter out null tradePrices from mixed list")
    void testGetDailyPremium_MixedNullAndValidPrices_MultipleDates() {
        // Given: Options with mixed null/valid prices across multiple dates
        OptionEntity option1 = createOption(null, -1, LocalDate.of(2025, 1, 1));
        OptionEntity option2 = createOption(100.0, -1, LocalDate.of(2025, 1, 1));
        OptionEntity option3 = createOption(50.0, -1, LocalDate.of(2025, 1, 2));
        OptionEntity option4 = createOption(null, -1, LocalDate.of(2025, 1, 2));
        OptionEntity option5 = createOption(75.0, -1, LocalDate.of(2025, 1, 3));

        when(optionRepository.findAll()).thenReturn(List.of(option1, option2, option3, option4, option5));

        // When: Calculate daily premium (returns cumulative daily premium)
        Map<LocalDate, BigDecimal> result = optionService.getDailyPremium();

        // Then: Should only sum valid prices, and values should be cumulative
        assertThat(result).hasSize(3);
        // Cumulative: day 1 = 100.00
        assertThat(result.get(LocalDate.of(2025, 1, 1))).isEqualTo(new BigDecimal("100.00"));
        // Cumulative: day 1 + day 2 = 100.00 + 50.00 = 150.00
        assertThat(result.get(LocalDate.of(2025, 1, 2))).isEqualTo(new BigDecimal("150.00"));
        // Cumulative: day 1 + day 2 + day 3 = 100.00 + 50.00 + 75.00 = 225.00
        assertThat(result.get(LocalDate.of(2025, 1, 3))).isEqualTo(new BigDecimal("225.00"));

        // Verify no NaN in any value
        result.values().forEach(value ->
                assertThat(value.toString()).doesNotContain("NaN")
        );
    }

    @Test
    @DisplayName("MapperUtils.getValuesCsv creates valid CSV without NaN")
    void testMapperUtilsValuesCsv_NoNaN() {
        // Given: Map with valid BigDecimal values
        Map<LocalDate, BigDecimal> dailyPremium = new LinkedHashMap<>();
        dailyPremium.put(LocalDate.of(2025, 1, 1), new BigDecimal("100.00"));
        dailyPremium.put(LocalDate.of(2025, 1, 2), new BigDecimal("250.00"));

        // When: Convert to CSV
        String premiumCsv = MapperUtils.getValuesCsv(dailyPremium);

        // Then: CSV output should not contain NaN
        assertThat(premiumCsv).doesNotContain("NaN");
        assertThat(premiumCsv).doesNotContain("Infinity");
        assertThat(premiumCsv).contains("100.00");
        assertThat(premiumCsv).contains("250.00");
    }

    // Helper method to create test options
    private OptionEntity createOption(Double tradePrice, int quantity, LocalDate tradeDate) {
        OptionEntity option = new OptionEntity();
        option.setTradePrice(tradePrice);
        option.setQuantity(quantity);
        option.setTradeDate(tradeDate);
        return option;
    }
}
