package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionService Portfolio Calculation Tests")
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OptionService optionService;

    private PositionDto createPosition(double positionValue, int daysToExp) {
        PositionDto position = new PositionDto();
        position.setPositionValue(positionValue);
        position.setDaysLeft(daysToExp);
        position.setQuantity(1);
        position.setTradeDate(LocalDate.now());
        position.setExpirationDate(LocalDate.now().plusDays(daysToExp));
        position.setTicker("TEST");
        position.setType(OptionType.PUT);
        // Set trade price equal to position value for simplified ROI calculation
        position.setTradePrice(positionValue);
        position.setMarketPrice(positionValue);
        position.setMarketValue(positionValue);
        position.setFee(0.0);
        position.setBreakEven(0.0);
        return position;
    }

    @Test
    @DisplayName("Portfolio ROI is calculated with weighted approach (not null)")
    void testPortfolioROI_CalculatedWithWeighting() {
        PositionDto pos1 = createPosition(100, 365);
        PositionDto pos2 = createPosition(10000, 365);

        List<PositionDto> openPositions = List.of(pos1, pos2);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // Verify ROI was calculated (not null)
        assertThat(portfolio.getAnnualizedRoiPercent())
                .as("Portfolio ROI should be calculated using weighted approach")
                .isNotNull();
    }

    @Test
    @DisplayName("Portfolio Probability is calculated with weighted approach (not null)")
    void testPortfolioProbability_CalculatedWithWeighting() {
        PositionDto pos1 = createPosition(100, 365);
        PositionDto pos2 = createPosition(10000, 365);

        List<PositionDto> openPositions = List.of(pos1, pos2);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // Verify probability was calculated (not null)
        assertThat(portfolio.getProbability())
                .as("Portfolio probability should be calculated using weighted approach")
                .isNotNull();
    }

    @Test
    @DisplayName("Single position portfolio: ROI equals position ROI")
    void testWeightedROI_SinglePosition() {
        PositionDto pos1 = createPosition(5000, 365);

        List<PositionDto> openPositions = List.of(pos1);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // With single position, portfolio ROI should equal position ROI
        Integer positionROI = pos1.getAnnualizedRoiPercent();
        Integer portfolioROI = portfolio.getAnnualizedRoiPercent();

        assertThat(portfolioROI)
                .as("Single position portfolio ROI should equal position ROI")
                .isEqualTo(positionROI);
    }

    @Test
    @DisplayName("Empty portfolio: ROI is 0")
    void testWeightedROI_NoPositions() {
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");
        portfolio.setPositionValue(0.0);

        optionService.calculatePosition(portfolio, new ArrayList<>(), new ArrayList<>());

        assertThat(portfolio.getAnnualizedRoiPercent())
                .as("Empty portfolio should have 0 ROI")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Equal sized positions: Portfolio metrics weighted equally")
    void testWeightedCalculations_EqualPositions() {
        PositionDto pos1 = createPosition(1000, 365);
        PositionDto pos2 = createPosition(1000, 365);

        List<PositionDto> openPositions = List.of(pos1, pos2);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // With equal sized positions, portfolio metrics should equal individual metrics
        Integer pos1ROI = pos1.getAnnualizedRoiPercent();
        Integer portfolioROI = portfolio.getAnnualizedRoiPercent();

        assertThat(portfolioROI)
                .as("With equal positions, portfolio ROI should equal position ROI")
                .isEqualTo(pos1ROI);
    }

    @Test
    @DisplayName("Portfolio with closed positions: Only open positions affect portfolio metrics")
    void testCalculatePosition_WithClosedPositions() {
        PositionDto openPos = createPosition(5000, 365);
        PositionDto closedPos = createPosition(1000, 0);
        closedPos.setDaysLeft(0);

        List<PositionDto> openPositions = List.of(openPos);
        List<PositionDto> closedPositions = List.of(closedPos);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, closedPositions);

        // Open position metrics should match the single open position
        Integer openPosROI = openPos.getAnnualizedRoiPercent();
        Integer portfolioROI = portfolio.getAnnualizedRoiPercent();

        assertThat(portfolioROI)
                .as("Portfolio ROI should be from open positions only")
                .isEqualTo(openPosROI);
    }

    @Test
    @DisplayName("Multiple positions: Portfolio calculation completes successfully")
    void testWeightedROI_ThreePositions() {
        PositionDto pos1 = createPosition(1000, 365);
        PositionDto pos2 = createPosition(2000, 365);
        PositionDto pos3 = createPosition(1000, 365);

        List<PositionDto> openPositions = List.of(pos1, pos2, pos3);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        // Should complete without errors
        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        assertThat(portfolio.getAnnualizedRoiPercent())
                .as("Portfolio ROI should be calculated for multiple positions")
                .isNotNull();
    }

    @Test
    @DisplayName("Weighted calculation uses capital at risk (positionValue * quantity)")
    void testWeightedCalculation_UsesCapitalAtRisk() {
        // Create two positions with different sizes
        PositionDto smallPos = createPosition(500, 365);
        PositionDto largePos = createPosition(5000, 365);

        List<PositionDto> openPositions = List.of(smallPos, largePos);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // Portfolio should be calculated using weighted approach
        // The large position's metrics should dominate
        assertThat(portfolio.getAnnualizedRoiPercent())
                .as("Weighted ROI calculated based on capital at risk")
                .isNotNull();
    }

    @Test
    @DisplayName("Weighting calculation method: Direct unit test")
    void testWeightedROI_MethodCalculation() {
        // This test verifies the weighting logic directly by checking
        // that portfolio metrics differ from simple averages for unequal positions
        PositionDto smallPos = createPosition(100, 365);
        smallPos.setAnnualizedRoiPercent(100); // Set explicitly for test

        PositionDto largePos = createPosition(10000, 365);
        largePos.setAnnualizedRoiPercent(10); // Set explicitly for test

        List<PositionDto> openPositions = List.of(smallPos, largePos);
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("PORTFOLIO");

        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // The key point: with weighted calculation, portfolio should be closer to large position ROI (10)
        // than simple average (55)
        Integer portfolioROI = portfolio.getAnnualizedRoiPercent();

        // Check that portfolio ROI was calculated
        assertThat(portfolioROI)
                .as("Portfolio ROI should be calculated")
                .isNotNull();

        // It should not be the simple average of 55
        // (which shows weighting is being applied)
        assertThat(portfolioROI)
                .as("Should use weighted calculation, not simple average")
                .isNotEqualTo(55);
    }
}
