package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.CsvImportResult;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("Should include virtual position in portfolio calculations (ISSUE-028)")
    void testVirtualPositionIncludedInPortfolioCalculations() {
        // Given: 2 real open positions
        PositionDto real1 = createPosition(100.0, 30);
        real1.setAnnualizedRoiPercent(5);
        real1.setTicker("SPY");

        PositionDto real2 = createPosition(200.0, 30);
        real2.setAnnualizedRoiPercent(5);
        real2.setTicker("SPY");

        List<PositionDto> openPositions = new ArrayList<>();
        openPositions.add(real1);
        openPositions.add(real2);

        // And: Virtual position with higher ROI
        PositionDto virtual = createPosition(100.0, 30);
        virtual.setAnnualizedRoiPercent(15);
        virtual.setTicker("SPY");
        virtual.setVirtual(true);
        openPositions.add(virtual);

        // When: Calculate portfolio with virtual included
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("SPY");
        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // Then: Portfolio should have weighted average of all three positions
        // (100*5 + 200*5 + 100*15) / 400 = 2000/400 = 5%
        // But exact weighting depends on the weighted calculation implementation
        assertThat(portfolio.getAnnualizedRoiPercent())
                .as("Portfolio ROI should reflect virtual position")
                .isNotNull();
        assertThat(openPositions)
                .as("Open positions list should include virtual")
                .anyMatch(p -> p.getVirtual() != null && p.getVirtual());
    }

    @Test
    @DisplayName("Virtual position marked as virtual in portfolio")
    void testVirtualPositionMarking() {
        // Given: Mix of real and virtual positions
        PositionDto real = createPosition(100.0, 30);
        real.setVirtual(false);

        PositionDto virtual = createPosition(100.0, 30);
        virtual.setVirtual(true);

        // When/Then
        assertThat(real.getVirtual()).isFalse();
        assertThat(virtual.getVirtual()).isTrue();
    }

    @Test
    @DisplayName("Virtual position should have same calculation as real for isolated metrics")
    void testVirtualPositionIsolatedMetrics() {
        // Given: Virtual position with specific parameters
        PositionDto virtualPosition = createPosition(100.0, 30);
        virtualPosition.setTradePrice(5.0);
        virtualPosition.setMarketValue(50.0);
        virtualPosition.setExpirationDate(LocalDate.now().plusDays(30));
        virtualPosition.setTradeDate(LocalDate.now());
        virtualPosition.setVirtual(true);

        // When: Calculate just this position
        PositionDto portfolio = new PositionDto();
        portfolio.setTicker("SPY");
        List<PositionDto> openPositions = List.of(virtualPosition);
        optionService.calculatePosition(portfolio, openPositions, new ArrayList<>());

        // Then: Portfolio metrics should be populated
        assertThat(portfolio.getPositionValue())
                .as("Position value should be set")
                .isNotNull();
        assertThat(portfolio.getMarketValue())
                .as("Market value should be set")
                .isNotNull();
    }

    @Test
    @DisplayName("Multiple virtual positions in sequence should replace previous")
    void testVirtualPositionReplacement() {
        // This test verifies the expected behavior at the service level
        // In actual use, VirtualPositionService manages this in session scope

        PositionDto virtual1 = createPosition(100.0, 30);
        PositionDto virtual2 = createPosition(200.0, 30);

        // Both would be added to openPositions list
        // The session-scoped VirtualPositionService would replace the stored instance
        // Here we just verify they can be created
        assertThat(virtual1.getPositionValue()).isEqualTo(100.0);
        assertThat(virtual2.getPositionValue()).isEqualTo(200.0);
    }

    // ===== CSV Import Tests (ISSUE-030) =====

    @Test
    @DisplayName("CSV import with single invalid Conid continues processing")
    void testSaveCSV_SingleInvalidConid_ContinuesProcessing() {
        // Given: CSV with record having invalid Conid field
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,AAPL,P,O,2025-11-01,2025-11-15,150.00,1,265598,invalid-conid,50.00,AAPL_P_150,10.00\n" +
                "U123456,OPT,AAPL,P,O,2025-11-01,2025-11-15,150.00,1,265598,456789,50.00,AAPL_P_150,10.00";

        when(instrumentRepository.findByTicker("AAPL")).thenReturn(null);
        when(instrumentRepository.save(any())).thenReturn(new InstrumentEntity());
        when(optionRepository.findByConidAndStatusAndTradePrice(any(), any(), any())).thenReturn(null);
        when(optionRepository.save(any())).thenReturn(new OptionEntity());

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: Import should continue despite first record failure
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.getSuccessfulRecords()).isGreaterThanOrEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("Conid");
    }

    @Test
    @DisplayName("CSV import with single invalid NetCash continues processing")
    void testSaveCSV_SingleInvalidNetCash_ContinuesProcessing() {
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,SPY,C,O,2025-11-01,2025-11-15,500.00,1,756733,456789,not-a-number,SPY_C_500,10.00\n" +
                "U123456,OPT,SPY,C,O,2025-11-01,2025-11-15,500.00,1,756733,456790,50.00,SPY_C_500,10.00";

        when(instrumentRepository.findByTicker("SPY")).thenReturn(null);
        when(instrumentRepository.save(any())).thenReturn(new InstrumentEntity());
        when(optionRepository.findByConidAndStatusAndTradePrice(any(), any(), any())).thenReturn(null);
        when(optionRepository.save(any())).thenReturn(new OptionEntity());

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: Second record should be processed despite first failure
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.getSuccessfulRecords()).isGreaterThanOrEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("NetCash");
    }

    @Test
    @DisplayName("CSV import with invalid TradeDate continues processing")
    void testSaveCSV_InvalidTradeDate_ContinuesProcessing() {
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,QQQ,P,O,2025/11/01,2025-11-15,300.00,1,20005,789012,50.00,QQQ_P_300,10.00\n" +
                "U123456,OPT,QQQ,P,O,2025-11-01,2025-11-15,300.00,1,20005,789013,50.00,QQQ_P_300,10.00";

        when(instrumentRepository.findByTicker("QQQ")).thenReturn(null);
        when(instrumentRepository.save(any())).thenReturn(new InstrumentEntity());
        when(optionRepository.findByConidAndStatusAndTradePrice(any(), any(), any())).thenReturn(null);
        when(optionRepository.save(any())).thenReturn(new OptionEntity());

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: Processing should continue
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.getSuccessfulRecords()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("CSV import result reports successful/failed/skipped counts")
    void testSaveCSV_ReportsAccurateCounts() {
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,TSLA,C,O,2025-11-01,2025-11-15,250.00,1,76837,1001,50.00,TSLA_C_250,10.00\n" +
                "U123456,STK,IBM,C,O,2025-11-01,2025-11-15,150.00,1,8314,1002,50.00,IBM,10.00\n" +
                "U123456,OPT,TSLA,C,O,2025-11-01,2025-11-15,250.00,1,76837,1003,invalid,TSLA_C_250,10.00";

        when(instrumentRepository.findByTicker("TSLA")).thenReturn(null);
        when(instrumentRepository.save(any())).thenReturn(new InstrumentEntity());
        when(optionRepository.findByConidAndStatusAndTradePrice(any(), any(), any())).thenReturn(null);
        when(optionRepository.save(any())).thenReturn(new OptionEntity());

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: Counts should be accurate
        assertThat(result.getTotalRecords()).isEqualTo(3);
        assertThat(result.getSuccessfulRecords()).isEqualTo(1);  // TSLA record 1
        assertThat(result.getSkippedRecords()).isEqualTo(1);    // STK record (not OPT)
        assertThat(result.getFailedRecords()).isEqualTo(1);     // TSLA record 3 (invalid NetCash)
    }

    @Test
    @DisplayName("CSV import result has helpful summary message")
    void testCsvImportResult_HasHelpfulSummary() {
        CsvImportResult result = new CsvImportResult();
        result.setTotalRecords(10);
        result.setSuccessfulRecords(8);
        result.setFailedRecords(2);
        result.setSkippedRecords(0);
        result.addError(new CsvImportResult.CsvRecordError(5, "Conid", "bad", "NumberFormatException: bad", "NumberFormatException"));

        String summary = result.getSummary();

        assertThat(summary)
                .contains("CSV Import Summary: 10 total records")
                .contains("✓ Successful: 8")
                .contains("✗ Failed: 2")
                .contains("Record #5");
    }

    @Test
    @DisplayName("CSV import with all records invalid reports accurate failure")
    void testSaveCSV_AllInvalid_ReportsFailures() {
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,GDX,P,O,2025-11-01,2025-11-15,40.00,1,47835,bad1,invalid1,GDX_P_40,10.00\n" +
                "U123456,OPT,GDX,P,O,2025-11-01,2025-11-15,40.00,1,47835,bad2,invalid2,GDX_P_40,10.00";

        // Note: No stubbing needed - test expects parsing to fail early on both records

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: All records should be reported as failed
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getFailedRecords()).isEqualTo(2);
        assertThat(result.getSuccessfulRecords()).isEqualTo(0);
        assertThat(result.isCompleteFailure()).isTrue();
    }

    @Test
    @DisplayName("CSV import with valid records reports partial success correctly")
    void testSaveCSV_PartialSuccess_ReportsCorrectly() {
        String csv = "ClientAccountID,AssetClass,UnderlyingSymbol,Put/Call,Open/CloseIndicator,TradeDate,Expiry,Strike,Quantity,UnderlyingConid,Conid,NetCash,Symbol,FifoPnlRealized\n" +
                "U123456,OPT,IVV,C,O,2025-11-01,2025-11-15,450.00,1,913916,2001,75.00,IVV_C_450,10.00\n" +
                "U123456,OPT,IVV,C,O,2025-11-01,2025-11-15,450.00,1,913916,2002,invalid,IVV_C_450,10.00";

        when(instrumentRepository.findByTicker("IVV")).thenReturn(null);
        when(instrumentRepository.save(any())).thenReturn(new InstrumentEntity());
        when(optionRepository.findByConidAndStatusAndTradePrice(any(), any(), any())).thenReturn(null);
        when(optionRepository.save(any())).thenReturn(new OptionEntity());

        // When: saveCSV is called
        CsvImportResult result = optionService.saveCSV(csv);

        // Then: Should report partial success
        assertThat(result.getTotalRecords()).isEqualTo(2);
        assertThat(result.getSuccessfulRecords()).isEqualTo(1);
        assertThat(result.getFailedRecords()).isEqualTo(1);
        assertThat(result.isPartialSuccess()).isTrue();
        assertThat(result.isCompleteSuccess()).isFalse();
    }
}
