package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.OpenPositionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.repository.OpenPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenPositionServiceTest {

    @Mock
    private OpenPositionRepository openPositionRepository;

    private OpenPositionService openPositionService;

    @BeforeEach
    void setUp() {
        openPositionService = new OpenPositionService(openPositionRepository, null);
    }

    /**
     * Helper method to create test OpenPositionEntity
     */
    private OpenPositionEntity buildTestEntity(String underlyingSymbol, Double strike, String putCall, Integer quantity, LocalDate expirationDate) {
        OpenPositionEntity entity = new OpenPositionEntity();
        entity.setId(1L);
        entity.setConid(12345L);
        entity.setAccount("DU12345");
        entity.setAssetClass("OPT");
        entity.setSymbol("SPY 250120C00600000");  // Full option symbol
        entity.setUnderlyingSymbol(underlyingSymbol);
        entity.setQuantity(quantity);
        entity.setReportDate(LocalDate.now());
        entity.setExpirationDate(expirationDate);
        entity.setStrike(strike);  // In dollars
        entity.setPutCall(putCall);  // "P" or "C"
        entity.setCostBasisPrice(5.0);  // Trade price per contract
        entity.setMarkPrice(4.5);  // Current market price
        entity.setFifoPnlUnrealized(-50.0);  // Unrealized P&L
        entity.setCurrency("USD");
        return entity;
    }

    @Test
    void getAllOpenOptionDtos_shouldReturnCalculatedDtos() {
        // Arrange
        OpenPositionEntity spy = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        OpenPositionEntity aapl = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        aapl.setUnderlyingSymbol("AAPL");

        when(openPositionRepository.findAllOptions()).thenReturn(List.of(spy, aapl));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).hasSize(2);

        // First position: SPY PUT
        PositionDto spyDto = result.get(0);
        assertThat(spyDto.getTicker()).isEqualTo("SPY");
        assertThat(spyDto.getType()).isEqualTo(OptionType.PUT);
        assertThat(spyDto.getPositionValue()).isEqualTo(600.0);
        assertThat(spyDto.getTradePrice()).isEqualTo(5.0);
        assertThat(spyDto.getMarketPrice()).isEqualTo(4.5);
        assertThat(spyDto.getQuantity()).isEqualTo(1);
        assertThat(spyDto.getStatus()).isEqualTo(OptionStatus.OPEN);
        assertThat(spyDto.getAssetClass()).isEqualTo(AssetClass.OPT);
        assertThat(spyDto.getAnnualizedRoiPercent()).isNotNull();
        assertThat(spyDto.getDaysLeft()).isGreaterThan(0);
        assertThat(spyDto.getUnRealizedProfitOrLoss()).isEqualTo(-50.0);

        // Second position: AAPL CALL
        PositionDto aaplDto = result.get(1);
        assertThat(aaplDto.getTicker()).isEqualTo("AAPL");
        assertThat(aaplDto.getType()).isEqualTo(OptionType.CALL);
        assertThat(aaplDto.getPositionValue()).isEqualTo(200.0);
        assertThat(aaplDto.getQuantity()).isEqualTo(-1);
        assertThat(aaplDto.getAnnualizedRoiPercent()).isNotNull();
    }

    @Test
    void getOpenOptionsByTickerDto_shouldFilterByUnderlyingTicker() {
        // Arrange
        OpenPositionEntity spy1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        OpenPositionEntity spy2 = buildTestEntity("SPY", 605.0, "C", -1, LocalDate.now().plusDays(30));

        when(openPositionRepository.findOptionsByUnderlyingTicker("SPY")).thenReturn(List.of(spy1, spy2));

        // Act
        List<PositionDto> result = openPositionService.getOpenOptionsByTickerDto("SPY");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> dto.getTicker().equals("SPY"));
        assertThat(result.get(0).getType()).isEqualTo(OptionType.PUT);
        assertThat(result.get(1).getType()).isEqualTo(OptionType.CALL);
    }

    @Test
    void getAllOpenOptionDtos_emptyRepository_shouldReturnEmptyList() {
        // Arrange
        when(openPositionRepository.findAllOptions()).thenReturn(Collections.emptyList());

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
    }

    @Test
    void convertToPositionDto_handlesNullFields() {
        // Arrange
        OpenPositionEntity entity = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        entity.setCostBasisPrice(null);  // Null trade price - will be estimated by PositionMapper
        entity.setMarkPrice(null);       // Null market price - will be set to 0.0 by DTO
        entity.setFifoPnlUnrealized(null);  // Null P&L

        when(openPositionRepository.findAllOptions()).thenReturn(List.of(entity));

        // Act - should not throw
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).hasSize(1);
        PositionDto dto = result.get(0);
        assertThat(dto.getTicker()).isEqualTo("SPY");
        // PositionMapper estimates tradePrice if null and converts null marketPrice to 0.0
        assertThat(dto.getUnRealizedProfitOrLoss()).isNull();
        assertThat(dto.getPositionValue()).isEqualTo(600.0);
        // Verify the method doesn't crash with null fields
        assertThat(dto.getType()).isEqualTo(OptionType.PUT);
    }

    @Test
    void convertToPositionDto_fieldMappingAccuracy() {
        // Arrange
        OpenPositionEntity entity = buildTestEntity("QQQ", 350.0, "C", 2, LocalDate.now().plusDays(60));
        entity.setReportDate(LocalDate.of(2025, 1, 1));
        entity.setFifoPnlUnrealized(250.0);

        when(openPositionRepository.findAllOptions()).thenReturn(List.of(entity));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).hasSize(1);
        PositionDto dto = result.get(0);

        // Verify all field mappings
        assertThat(dto.getTicker()).isEqualTo("QQQ");  // underlyingSymbol
        assertThat(dto.getQuantity()).isEqualTo(2);  // quantity
        assertThat(dto.getTradeDate()).isEqualTo(LocalDate.of(2025, 1, 1));  // reportDate
        assertThat(dto.getExpirationDate()).isEqualTo(LocalDate.now().plusDays(60));  // expirationDate
        assertThat(dto.getPositionValue()).isEqualTo(350.0);  // strike
        assertThat(dto.getTradePrice()).isEqualTo(5.0);  // costBasisPrice
        assertThat(dto.getMarketPrice()).isEqualTo(4.5);  // markPrice
        assertThat(dto.getMarketValue()).isEqualTo(4.5);  // markPrice
        assertThat(dto.getType()).isEqualTo(OptionType.CALL);  // putCall "C"
        assertThat(dto.getUnRealizedProfitOrLoss()).isEqualTo(250.0);  // fifoPnlUnrealized
        assertThat(dto.getStatus()).isEqualTo(OptionStatus.OPEN);  // static
        assertThat(dto.getAssetClass()).isEqualTo(AssetClass.OPT);  // static
    }

    @Test
    void getAllOpenOptionDtos_putCallTypeConversion() {
        // Arrange: Test both PUT and CALL conversions
        OpenPositionEntity put = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        OpenPositionEntity call = buildTestEntity("SPY", 610.0, "C", -1, LocalDate.now().plusDays(30));

        when(openPositionRepository.findAllOptions()).thenReturn(List.of(put, call));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(OptionType.PUT);
        assertThat(result.get(1).getType()).isEqualTo(OptionType.CALL);
    }

    @Test
    void getOpenOptionsByTickerDto_singleTicker() {
        // Arrange
        OpenPositionEntity entity = buildTestEntity("TSLA", 250.0, "P", 5, LocalDate.now().plusDays(20));

        when(openPositionRepository.findOptionsByUnderlyingTicker("TSLA")).thenReturn(List.of(entity));

        // Act
        List<PositionDto> result = openPositionService.getOpenOptionsByTickerDto("TSLA");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("TSLA");
        assertThat(result.get(0).getQuantity()).isEqualTo(5);
    }

    // ===== Date Filtering Tests (ISSUE-045) =====

    @Test
    void getAllOpenOptionDtos_withStartDate_shouldFilterByReportDate() {
        // Arrange: Create positions with different report dates
        LocalDate cutoffDate = LocalDate.of(2025, 12, 1);

        OpenPositionEntity recentPosition = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        recentPosition.setReportDate(LocalDate.of(2025, 12, 3));

        OpenPositionEntity olderPosition = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        olderPosition.setReportDate(LocalDate.of(2025, 11, 20));

        when(openPositionRepository.findAllOptionsByDate(cutoffDate))
            .thenReturn(List.of(recentPosition));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(cutoffDate);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("SPY");
        assertThat(result.get(0).getTradeDate()).isEqualTo(LocalDate.of(2025, 12, 3));

        // Verify the correct repository method was called
        verify(openPositionRepository).findAllOptionsByDate(cutoffDate);
        verify(openPositionRepository, never()).findAllOptions();
    }

    @Test
    void getAllOpenOptionDtos_withNullDate_shouldReturnAllPositions() {
        // Arrange
        OpenPositionEntity position1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        position1.setReportDate(LocalDate.of(2025, 12, 3));

        OpenPositionEntity position2 = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        position2.setReportDate(LocalDate.of(2025, 11, 20));

        when(openPositionRepository.findAllOptions()).thenReturn(List.of(position1, position2));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

        // Assert
        assertThat(result).hasSize(2);

        // Verify the correct repository method was called
        verify(openPositionRepository).findAllOptions();
        verify(openPositionRepository, never()).findAllOptionsByDate(any());
    }

    @Test
    void getAllOpenOptionDtos_withFutureStartDate_shouldReturnEmptyList() {
        // Arrange: Start date in the future, so no positions match
        LocalDate futureDate = LocalDate.now().plusDays(30);

        when(openPositionRepository.findAllOptionsByDate(futureDate))
            .thenReturn(Collections.emptyList());

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(futureDate);

        // Assert
        assertThat(result).isEmpty();
        assertThat(result).isNotNull();
        verify(openPositionRepository).findAllOptionsByDate(futureDate);
    }

    @Test
    void getAllOpenOptionDtos_withPastStartDate_shouldReturnMultiplePositions() {
        // Arrange: Past date should return all recent positions
        LocalDate pastDate = LocalDate.of(2025, 1, 1);

        OpenPositionEntity position1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        position1.setReportDate(LocalDate.of(2025, 12, 3));

        OpenPositionEntity position2 = buildTestEntity("QQQ", 350.0, "C", 2, LocalDate.now().plusDays(45));
        position2.setReportDate(LocalDate.of(2025, 11, 15));

        OpenPositionEntity position3 = buildTestEntity("TSLA", 250.0, "P", -1, LocalDate.now().plusDays(20));
        position3.setReportDate(LocalDate.of(2025, 10, 1));

        when(openPositionRepository.findAllOptionsByDate(pastDate))
            .thenReturn(List.of(position1, position2, position3));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(pastDate);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).extracting(PositionDto::getTicker)
            .containsExactly("SPY", "QQQ", "TSLA");
        verify(openPositionRepository).findAllOptionsByDate(pastDate);
    }

    @Test
    void getAllOpenOptionDtos_dateFiltering_calculatesMetricsCorrectly() {
        // Arrange: Verify that calculated fields work with date filtering
        LocalDate filterDate = LocalDate.of(2025, 12, 1);

        OpenPositionEntity position = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        position.setReportDate(LocalDate.of(2025, 12, 3));

        when(openPositionRepository.findAllOptionsByDate(filterDate))
            .thenReturn(List.of(position));

        // Act
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos(filterDate);

        // Assert
        assertThat(result).hasSize(1);
        PositionDto dto = result.get(0);

        // Verify calculated fields are populated
        assertThat(dto.getAnnualizedRoiPercent()).isNotNull();
        assertThat(dto.getDaysLeft()).isGreaterThan(0);
        assertThat(dto.getBreakEven()).isNotNull();

        // Verify code field is set (critical for AlpacaRestController)
        assertThat(dto.getCode()).isNotNull();
        assertThat(dto.getCode()).isEqualTo("SPY 250120C00600000");
    }

    // ===== saveCSV Deletion Tests (ISSUE-046) =====

    @Test
    void saveCSV_returnFormat_shouldBeSavedSlashDeleted() throws IOException {
        // Arrange: 2 existing positions, CSV has 1 (1 will be deleted)
        OpenPositionEntity pos1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        pos1.setConid(100L);
        pos1.setAccount("DU12345");

        OpenPositionEntity pos2 = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        pos2.setConid(200L);
        pos2.setAccount("DU12345");

        when(openPositionRepository.findByConid(100L)).thenReturn(pos1);  // Update
        when(openPositionRepository.findByConid(999L)).thenReturn(null);  // Insert
        when(openPositionRepository.findByAccount("DU12345")).thenReturn(List.of(pos1, pos2));

        String csv = "ClientAccountID,Conid,AssetClass,Symbol,ReportDate,Quantity,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol\n" +
                "DU12345,100,OPT,SPY 250120P00600000,2025-12-04,1,USD,100,SPY\n" +
                "DU12345,999,OPT,AAPL 250120C00200000,2025-12-04,1,USD,20,AAPL";

        // When
        String result = openPositionService.saveCSV(csv);

        // Then: Should have 2 saved (1 update + 1 insert) and 1 deleted
        assertThat(result).isEqualTo("2/1");
        verify(openPositionRepository).deleteAll(any(List.class));
    }

    @Test
    void saveCSV_accountScoped_shouldPreserveOtherAccounts() throws IOException {
        // Arrange: Positions from two accounts, CSV only has one account
        OpenPositionEntity du12345_pos1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        du12345_pos1.setConid(100L);
        du12345_pos1.setAccount("DU12345");

        OpenPositionEntity du12345_pos2 = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        du12345_pos2.setConid(200L);
        du12345_pos2.setAccount("DU12345");

        OpenPositionEntity du99999_pos1 = buildTestEntity("TSLA", 250.0, "P", 2, LocalDate.now().plusDays(20));
        du99999_pos1.setConid(300L);
        du99999_pos1.setAccount("DU99999");

        when(openPositionRepository.findByConid(100L)).thenReturn(du12345_pos1);
        when(openPositionRepository.findByAccount("DU12345")).thenReturn(List.of(du12345_pos1, du12345_pos2));

        // CSV only contains DU12345 account
        String csv = "ClientAccountID,Conid,AssetClass,Symbol,ReportDate,Quantity,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol\n" +
                "DU12345,100,OPT,SPY 250120P00600000,2025-12-04,1,USD,100,SPY";

        // When
        String result = openPositionService.saveCSV(csv);

        // Then: DU12345 pos2 deleted (1 deleted), but DU99999 positions never queried
        assertThat(result).isEqualTo("1/1");
        verify(openPositionRepository).findByAccount("DU12345");
        verify(openPositionRepository, never()).findByAccount("DU99999");
    }

    @Test
    void saveCSV_emptyData_shouldReturnZeroZero() throws IOException {
        // Arrange: CSV header only, no data rows
        String csv = "ClientAccountID,Conid,AssetClass,Symbol,ReportDate,Quantity,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol";

        // When
        String result = openPositionService.saveCSV(csv);

        // Then: No rows processed, no deletions
        assertThat(result).isEqualTo("0/0");
        verify(openPositionRepository, never()).findByAccount(any());
        verify(openPositionRepository, never()).deleteAll(any());
    }

    @Test
    void saveCSV_allPositionsStillOpen_shouldReturnZeroDeleted() throws IOException {
        // Arrange: All positions in CSV still exist, nothing to delete
        OpenPositionEntity pos1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        pos1.setConid(100L);
        pos1.setAccount("DU12345");

        when(openPositionRepository.findByConid(100L)).thenReturn(pos1);  // Update
        when(openPositionRepository.findByAccount("DU12345")).thenReturn(List.of(pos1));

        String csv = "ClientAccountID,Conid,AssetClass,Symbol,ReportDate,Quantity,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol\n" +
                "DU12345,100,OPT,SPY 250120P00600000,2025-12-04,1,USD,100,SPY";

        // When
        String result = openPositionService.saveCSV(csv);

        // Then: 1 saved, 0 deleted
        assertThat(result).isEqualTo("1/0");
        verify(openPositionRepository, never()).deleteAll(any());
    }

    @Test
    void saveCSV_multipleAccounts_shouldDeleteFromAllCsvAccounts() throws IOException {
        // Arrange: CSV has two accounts with 2 positions total, but 4 exist in DB (2 per account)
        OpenPositionEntity du11111_pos1 = buildTestEntity("SPY", 600.0, "P", 1, LocalDate.now().plusDays(30));
        du11111_pos1.setConid(1L);
        du11111_pos1.setAccount("DU11111");

        OpenPositionEntity du11111_pos2 = buildTestEntity("AAPL", 200.0, "C", -1, LocalDate.now().plusDays(45));
        du11111_pos2.setConid(2L);
        du11111_pos2.setAccount("DU11111");

        OpenPositionEntity du22222_pos1 = buildTestEntity("TSLA", 250.0, "P", 2, LocalDate.now().plusDays(20));
        du22222_pos1.setConid(3L);
        du22222_pos1.setAccount("DU22222");

        OpenPositionEntity du22222_pos2 = buildTestEntity("QQQ", 350.0, "C", -2, LocalDate.now().plusDays(50));
        du22222_pos2.setConid(4L);
        du22222_pos2.setAccount("DU22222");

        when(openPositionRepository.findByConid(1L)).thenReturn(du11111_pos1);  // Update
        when(openPositionRepository.findByConid(3L)).thenReturn(du22222_pos1);  // Update
        when(openPositionRepository.findByAccount("DU11111")).thenReturn(List.of(du11111_pos1, du11111_pos2));
        when(openPositionRepository.findByAccount("DU22222")).thenReturn(List.of(du22222_pos1, du22222_pos2));

        String csv = "ClientAccountID,Conid,AssetClass,Symbol,ReportDate,Quantity,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol\n" +
                "DU11111,1,OPT,SPY 250120P00600000,2025-12-04,1,USD,100,SPY\n" +
                "DU22222,3,OPT,TSLA 250120P00250000,2025-12-04,2,USD,175,TSLA";

        // When
        String result = openPositionService.saveCSV(csv);

        // Then: 2 saved, 2 deleted (pos2 from each account)
        assertThat(result).isEqualTo("2/2");
        verify(openPositionRepository).findByAccount("DU11111");
        verify(openPositionRepository).findByAccount("DU22222");
    }
}
