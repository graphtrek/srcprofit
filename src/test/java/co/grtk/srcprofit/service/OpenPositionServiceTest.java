package co.grtk.srcprofit.service;

import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OpenPositionEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OpenPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenPositionService, specifically testing instrument synchronization
 * during CSV import (ISSUE-043).
 *
 * Covers:
 * - Creating new instruments when position is imported
 * - Updating existing instruments when position is re-imported
 * - Falling back to symbol when description is empty
 * - Preserving existing instrument data (price, metadata, etc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenPositionService Instrument Synchronization Tests")
class OpenPositionServiceTest {

    @Mock
    private OpenPositionRepository openPositionRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private OpenPositionService openPositionService;

    @BeforeEach
    void setUp() {
        // Default mock behavior: positions don't exist yet (new import)
        when(openPositionRepository.findByConid(any())).thenReturn(null);
        // Default mock behavior: instruments don't exist yet
        when(instrumentRepository.findByConid(any())).thenReturn(null);
    }

    @Test
    @DisplayName("CSV import creates instrument when it does not exist")
    void testSaveCSV_createsInstrument_whenNotExists() throws IOException {
        // Given: CSV with position data (instrument doesn't exist yet)
        String csv = """
                ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
                U12345,265598,STK,AAPL,APPLE INC,100,2025-12-01,USD
                """;

        // Mock: position doesn't exist, instrument doesn't exist
        when(openPositionRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByConid(265598L)).thenReturn(null);

        // When: Import CSV
        int count = openPositionService.saveCSV(csv);

        // Then: 1 record processed
        assertThat(count).isEqualTo(1);

        // And: Instrument was created with correct values
        verify(instrumentRepository).save(argThat(instrument ->
                instrument.getConid() == 265598L &&
                "AAPL".equals(instrument.getTicker()) &&
                "APPLE INC".equals(instrument.getName())
        ));

        // And: Position was created
        verify(openPositionRepository).save(any(OpenPositionEntity.class));
    }

    @Test
    @DisplayName("CSV import updates existing instrument name from CSV")
    void testSaveCSV_updatesInstrument_whenExists() throws IOException {
        // Given: Instrument exists with old name
        InstrumentEntity existing = new InstrumentEntity();
        existing.setId(1L);
        existing.setConid(265598L);
        existing.setTicker("AAPL");
        existing.setName("Old Apple Name");
        existing.setPrice(150.0); // Existing price that should be preserved

        // Mock: instrument exists
        when(instrumentRepository.findByConid(265598L)).thenReturn(existing);

        // When: Import CSV with updated description
        String csv = """
                ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
                U12345,265598,STK,AAPL,APPLE INC - UPDATED,100,2025-12-01,USD
                """;
        int count = openPositionService.saveCSV(csv);

        // Then: 1 record processed
        assertThat(count).isEqualTo(1);

        // And: Instrument was updated
        verify(instrumentRepository).save(argThat(instrument ->
                instrument.getConid() == 265598L &&
                "AAPL".equals(instrument.getTicker()) &&
                "APPLE INC - UPDATED".equals(instrument.getName()) &&
                instrument.getPrice() == 150.0 // Price preserved
        ));
    }

    @Test
    @DisplayName("CSV import falls back to symbol when description is empty")
    void testSaveCSV_usesSymbolAsFallback_whenDescriptionEmpty() throws IOException {
        // Given: CSV with empty description field
        String csv = """
                ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
                U12345,265598,STK,AAPL,,100,2025-12-01,USD
                """;

        // Mock: position and instrument don't exist
        when(openPositionRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(null);

        // When: Import CSV
        int count = openPositionService.saveCSV(csv);

        // Then: 1 record processed
        assertThat(count).isEqualTo(1);

        // And: Instrument created with symbol as name (fallback)
        verify(instrumentRepository).save(argThat(instrument ->
                instrument.getConid() == 265598L &&
                "AAPL".equals(instrument.getTicker()) &&
                "AAPL".equals(instrument.getName()) // Fallback to symbol
        ));
    }

    @Test
    @DisplayName("CSV import updates conid when ticker exists with different conid")
    void testSaveCSV_updateConid_whenTickerExistsWithDifferentConid() throws IOException {
        // Given: Instrument exists with AAPL ticker but different conid
        InstrumentEntity existing = new InstrumentEntity();
        existing.setId(1L);
        existing.setConid(999999L); // Old conid
        existing.setTicker("AAPL");
        existing.setName("APPLE");
        existing.setPrice(150.0);

        // Mock: new conid not found, but ticker exists
        when(openPositionRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(existing);

        // When: Import CSV with correct conid
        String csv = """
                ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
                U12345,265598,STK,AAPL,APPLE INC,100,2025-12-01,USD
                """;
        int count = openPositionService.saveCSV(csv);

        // Then: 1 record processed
        assertThat(count).isEqualTo(1);

        // And: Instrument updated with new conid from CSV (IBKR ground truth)
        verify(instrumentRepository).save(argThat(instrument ->
                instrument.getConid() == 265598L && // Updated conid
                "AAPL".equals(instrument.getTicker()) &&
                "APPLE INC".equals(instrument.getName()) &&
                instrument.getPrice() == 150.0 // Price preserved
        ));
    }

    @Test
    @DisplayName("CSV import for options creates underlying instrument using underlyingConid")
    void testSaveCSV_createsUnderlyingInstrument_forOptions() throws IOException {
        // Given: CSV with option position (uses underlying stock info)
        String csv = """
                ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary,UnderlyingConid,UnderlyingSymbol
                U12345,123456,OPT,AAPL 100 C,AAPL 100 CALL,1,2025-12-01,USD,265598,AAPL
                """;

        // Mock: option conid doesn't exist, but underlying conid and symbol checked
        when(openPositionRepository.findByConid(123456L)).thenReturn(null);
        when(instrumentRepository.findByConid(265598L)).thenReturn(null);
        when(instrumentRepository.findByTicker("AAPL")).thenReturn(null);

        // When: Import CSV
        int count = openPositionService.saveCSV(csv);

        // Then: 1 record processed
        assertThat(count).isEqualTo(1);

        // And: Underlying instrument created with underlyingConid, not option's conid
        verify(instrumentRepository).save(argThat(instrument ->
                instrument.getConid() == 265598L &&  // Underlying conid, NOT option conid (123456)
                "AAPL".equals(instrument.getTicker()) &&
                "AAPL 100 CALL".equals(instrument.getName()) // Option description used as name
        ));
    }
}
