package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaOptionSnapshotDto;
import co.grtk.srcprofit.dto.AlpacaOptionSnapshotsResponseDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionSnapshotEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OptionRepository;
import co.grtk.srcprofit.repository.OptionSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OptionSnapshotService.
 *
 * Tests filtering logic, OCC parsing, error handling, and database operations.
 */
@ExtendWith(MockitoExtension.class)
class OptionSnapshotServiceTest {

    @Mock
    private AlpacaService alpacaService;

    @Mock
    private OptionSnapshotRepository optionSnapshotRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private OptionRepository optionRepository;

    private OptionSnapshotService optionSnapshotService;

    private InstrumentEntity testInstrumentAAPL;
    private InstrumentEntity testInstrumentMSFT;
    private InstrumentEntity testInstrumentTSLA;

    @BeforeEach
    void setUp() {
        optionSnapshotService = new OptionSnapshotService(
                alpacaService,
                optionSnapshotRepository,
                instrumentRepository,
                optionRepository
        );

        // Setup test instruments
        testInstrumentAAPL = new InstrumentEntity();
        testInstrumentAAPL.setId(1L);
        testInstrumentAAPL.setTicker("AAPL");
        testInstrumentAAPL.setPrice(95.50);

        testInstrumentMSFT = new InstrumentEntity();
        testInstrumentMSFT.setId(2L);
        testInstrumentMSFT.setTicker("MSFT");
        testInstrumentMSFT.setPrice(150.00);  // Price >= 100, should be filtered out

        testInstrumentTSLA = new InstrumentEntity();
        testInstrumentTSLA.setId(3L);
        testInstrumentTSLA.setTicker("TSLA");
        testInstrumentTSLA.setPrice(85.25);
    }

    /**
     * Test 1: Batch refresh processes all instruments with open positions
     */
    @Test
    void testRefreshOptionSnapshots_ProcessesInstrumentsWithOpenPositions() {
        // Setup: Three instruments with open positions
        List<InstrumentEntity> instrumentsWithOpenPositions = Arrays.asList(
                testInstrumentAAPL,    // $95.50
                testInstrumentMSFT,    // $150.00
                testInstrumentTSLA     // $85.25
        );
        when(optionRepository.findInstrumentsWithOpenPositions()).thenReturn(instrumentsWithOpenPositions);

        // Mock empty API responses
        AlpacaOptionSnapshotsResponseDto emptyResponse = new AlpacaOptionSnapshotsResponseDto();
        emptyResponse.setSnapshots(new HashMap<>());
        when(alpacaService.getOptionSnapshots(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(emptyResponse);

        // Execute
        int count = optionSnapshotService.refreshOptionSnapshots();

        // Verify: All 3 instruments were processed
        // Each instrument gets 2 calls: one for calls, one for puts
        verify(alpacaService, times(6)).getOptionSnapshots(anyString(), anyString(), anyString(), anyString());
        assertTrue(count >= 0);
    }

    /**
     * Test 2: OCC symbol parsing extracts strike, expiration, and type
     */
    @Test
    void testRefreshOptionSnapshots_ParsesOccSymbolCorrectly() {
        // Setup: Single instrument with one snapshot
        List<InstrumentEntity> instruments = Arrays.asList(testInstrumentAAPL);
        when(optionRepository.findInstrumentsWithOpenPositions()).thenReturn(instruments);

        // Create snapshot with OCC symbol: AAPL230120C00150000
        // Expiration: 2023-01-20, Type: Call, Strike: 150.00
        AlpacaOptionSnapshotDto snapshot = createTestSnapshot("AAPL230120C00150000", "call");
        Map<String, AlpacaOptionSnapshotDto> snapshots = new HashMap<>();
        snapshots.put("AAPL230120C00150000", snapshot);

        AlpacaOptionSnapshotsResponseDto response = new AlpacaOptionSnapshotsResponseDto();
        response.setSnapshots(snapshots);

        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("call"), anyString(), anyString()))
                .thenReturn(response);
        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("put"), anyString(), anyString()))
                .thenReturn(new AlpacaOptionSnapshotsResponseDto(new HashMap<>()));

        when(optionSnapshotRepository.findBySymbol("AAPL230120C00150000"))
                .thenReturn(Optional.empty());

        // Execute
        int count = optionSnapshotService.refreshOptionSnapshots();

        // Verify: Snapshot was saved with correct parsed values
        ArgumentCaptor<OptionSnapshotEntity> captor = ArgumentCaptor.forClass(OptionSnapshotEntity.class);
        verify(optionSnapshotRepository).save(captor.capture());

        OptionSnapshotEntity saved = captor.getValue();
        assertEquals("AAPL230120C00150000", saved.getSymbol());
        assertEquals("call", saved.getOptionType());
        assertEquals(new BigDecimal("150.00"), saved.getStrikePrice());
        assertEquals(LocalDate.of(2023, 1, 20), saved.getExpirationDate());
        assertEquals(1, count);
    }

    /**
     * Test 3: Batch filters snapshots by expiration (3 months)
     */
    @Test
    void testRefreshOptionSnapshots_FiltersSnapshotsByExpiration() {
        List<InstrumentEntity> instruments = Arrays.asList(testInstrumentAAPL);
        when(optionRepository.findInstrumentsWithOpenPositions()).thenReturn(instruments);

        LocalDate today = LocalDate.now();
        LocalDate withinRange = today.plusDays(60);      // 60 days - should be included
        LocalDate beyondRange = today.plusDays(120);     // 120 days - should be filtered out

        // Create two snapshots with different expiration dates
        AlpacaOptionSnapshotDto withinSnapshot = createTestSnapshot(
                "AAPL" + formatDate(withinRange) + "C00150000", "call");
        AlpacaOptionSnapshotDto beyondSnapshot = createTestSnapshot(
                "AAPL" + formatDate(beyondRange) + "C00150000", "call");

        Map<String, AlpacaOptionSnapshotDto> snapshots = new HashMap<>();
        snapshots.put(withinSnapshot.getSymbol(), withinSnapshot);
        snapshots.put(beyondSnapshot.getSymbol(), beyondSnapshot);

        AlpacaOptionSnapshotsResponseDto response = new AlpacaOptionSnapshotsResponseDto();
        response.setSnapshots(snapshots);

        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("call"), anyString(), anyString()))
                .thenReturn(response);
        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("put"), anyString(), anyString()))
                .thenReturn(new AlpacaOptionSnapshotsResponseDto(new HashMap<>()));

        when(optionSnapshotRepository.findBySymbol(anyString()))
                .thenReturn(Optional.empty());

        // Execute
        int count = optionSnapshotService.refreshOptionSnapshots();

        // Verify: Only within-range snapshot was saved
        assertEquals(1, count);
    }

    /**
     * Test 4: Batch continues on per-instrument error
     */
    @Test
    void testRefreshOptionSnapshots_HandlesPerInstrumentErrors() {
        List<InstrumentEntity> instruments = Arrays.asList(testInstrumentAAPL, testInstrumentTSLA);
        when(optionRepository.findInstrumentsWithOpenPositions()).thenReturn(instruments);

        // AAPL succeeds
        AlpacaOptionSnapshotDto snapshot = createTestSnapshot("AAPL230120C00150000", "call");
        Map<String, AlpacaOptionSnapshotDto> snapshotMap = new HashMap<>();
        snapshotMap.put("AAPL230120C00150000", snapshot);

        AlpacaOptionSnapshotsResponseDto successResponse = new AlpacaOptionSnapshotsResponseDto();
        successResponse.setSnapshots(snapshotMap);

        // TSLA fails on first call
        when(alpacaService.getOptionSnapshots(eq("AAPL"), anyString(), anyString(), anyString()))
                .thenReturn(successResponse);
        when(alpacaService.getOptionSnapshots(eq("TSLA"), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API Error"));

        when(optionSnapshotRepository.findBySymbol(anyString()))
                .thenReturn(Optional.empty());

        // Execute
        int count = optionSnapshotService.refreshOptionSnapshots();

        // Verify: AAPL succeeded (1 snapshot saved), TSLA failed but process continued
        // AAPL: 1 call for calls + 1 for puts = 2 calls
        // TSLA: 1 call for calls (fails) = 1 call (PUT call not attempted due to exception)
        // Total: 3 API calls
        verify(alpacaService, times(3)).getOptionSnapshots(anyString(), anyString(), anyString(), anyString());
        assertTrue(count > 0);  // At least AAPL's snapshot was saved
    }

    /**
     * Test 5: Delete expired snapshots removes only expired
     */
    @Test
    void testDeleteExpiredSnapshots_RemovesExpiredOnly() {
        LocalDate today = LocalDate.now();

        when(optionSnapshotRepository.deleteByExpirationDateBefore(today))
                .thenReturn(5);

        // Execute
        int deleted = optionSnapshotService.deleteExpiredSnapshots();

        // Verify: Only snapshots expiring before today are deleted
        verify(optionSnapshotRepository).deleteByExpirationDateBefore(today);
        assertEquals(5, deleted);
    }

    /**
     * Test 6: Get snapshots for instrument
     */
    @Test
    void testGetSnapshotsForInstrument_ReturnsSnapshots() {
        OptionSnapshotEntity snapshot1 = new OptionSnapshotEntity();
        snapshot1.setSymbol("AAPL230119C00095000");
        snapshot1.setOptionType("call");

        OptionSnapshotEntity snapshot2 = new OptionSnapshotEntity();
        snapshot2.setSymbol("AAPL230119P00090000");
        snapshot2.setOptionType("put");

        when(instrumentRepository.findByTicker("AAPL")).thenReturn(testInstrumentAAPL);
        when(optionSnapshotRepository.findByInstrument(testInstrumentAAPL))
                .thenReturn(Arrays.asList(snapshot1, snapshot2));

        // Execute
        List<OptionSnapshotEntity> snapshots = optionSnapshotService.getSnapshotsForInstrument("AAPL");

        // Verify
        assertEquals(2, snapshots.size());
        verify(instrumentRepository).findByTicker("AAPL");
        verify(optionSnapshotRepository).findByInstrument(testInstrumentAAPL);
    }

    /**
     * Test 7: Upsert pattern updates existing snapshot
     */
    @Test
    void testRefreshOptionSnapshots_UpdatesExistingSnapshot() {
        List<InstrumentEntity> instruments = Arrays.asList(testInstrumentAAPL);
        when(optionRepository.findInstrumentsWithOpenPositions()).thenReturn(instruments);

        // Create snapshot
        AlpacaOptionSnapshotDto newSnapshot = createTestSnapshot("AAPL230120C00150000", "call");
        newSnapshot.getGreeks().setDelta(0.50);  // Different delta
        newSnapshot.getLatestQuote().setAskPrice("2.55");

        Map<String, AlpacaOptionSnapshotDto> snapshots = new HashMap<>();
        snapshots.put("AAPL230120C00150000", newSnapshot);

        AlpacaOptionSnapshotsResponseDto response = new AlpacaOptionSnapshotsResponseDto();
        response.setSnapshots(snapshots);

        // Existing snapshot
        OptionSnapshotEntity existing = new OptionSnapshotEntity();
        existing.setSymbol("AAPL230120C00150000");
        existing.setDelta(new BigDecimal("0.45"));  // Old value

        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("call"), anyString(), anyString()))
                .thenReturn(response);
        when(alpacaService.getOptionSnapshots(eq("AAPL"), eq("put"), anyString(), anyString()))
                .thenReturn(new AlpacaOptionSnapshotsResponseDto(new HashMap<>()));

        when(optionSnapshotRepository.findBySymbol("AAPL230120C00150000"))
                .thenReturn(Optional.of(existing));

        // Execute
        int count = optionSnapshotService.refreshOptionSnapshots();

        // Verify: Existing snapshot was updated
        ArgumentCaptor<OptionSnapshotEntity> captor = ArgumentCaptor.forClass(OptionSnapshotEntity.class);
        verify(optionSnapshotRepository).save(captor.capture());

        OptionSnapshotEntity saved = captor.getValue();
        assertEquals(new BigDecimal("0.500000"), saved.getDelta());  // Updated value
        assertEquals(1, count);
    }

    // ============ Helper Methods ============

    private AlpacaOptionSnapshotDto createTestSnapshot(String symbol, String type) {
        AlpacaOptionSnapshotDto dto = new AlpacaOptionSnapshotDto();
        dto.setSymbol(symbol);

        // Latest Trade
        AlpacaOptionSnapshotDto.LatestTradeDto trade = new AlpacaOptionSnapshotDto.LatestTradeDto();
        trade.setTimestamp("2023-01-15T20:00:00Z");
        trade.setExchange("C");
        trade.setPrice("2.50");
        trade.setSize(10);
        dto.setLatestTrade(trade);

        // Latest Quote
        AlpacaOptionSnapshotDto.LatestQuoteDto quote = new AlpacaOptionSnapshotDto.LatestQuoteDto();
        quote.setTimestamp("2023-01-15T20:00:00Z");
        quote.setAskExchange("C");
        quote.setAskPrice("2.55");
        quote.setAskSize(100);
        quote.setBidExchange("C");
        quote.setBidPrice("2.45");
        quote.setBidSize(200);
        dto.setLatestQuote(quote);

        // Greeks
        AlpacaOptionSnapshotDto.GreeksDto greeks = new AlpacaOptionSnapshotDto.GreeksDto();
        greeks.setDelta(0.45);
        greeks.setGamma(0.03);
        greeks.setTheta(-0.05);
        greeks.setVega(0.15);
        greeks.setRho(0.02);
        greeks.setImpliedVolatility(0.25);
        dto.setGreeks(greeks);

        return dto;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d%02d%02d",
                date.getYear() % 100,
                date.getMonthValue(),
                date.getDayOfMonth());
    }
}
