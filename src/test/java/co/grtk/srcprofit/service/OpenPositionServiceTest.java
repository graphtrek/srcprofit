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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

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
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

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
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

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
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

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
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

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
}
