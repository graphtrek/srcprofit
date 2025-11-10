package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaAssetDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlpacaService, specifically the refreshStaleAssetMetadata() method.
 *
 * Covers:
 * - Finding stale assets and refreshing them
 * - Error handling (per-ticker failures don't abort batch)
 * - Timestamp updates
 * - Return count of refreshed assets
 */
@ExtendWith(MockitoExtension.class)
class AlpacaServiceTest {

    @Mock
    private RestClient alpacaRestClient;

    @Mock
    private RestClient alpacaTradingRestClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private AlpacaService alpacaService;

    private InstrumentEntity testInstrumentAAPL;
    private InstrumentEntity testInstrumentMSFT;
    private InstrumentEntity testInstrumentGOOGL;

    private AlpacaAssetDto testAssetDtoAAPL;
    private AlpacaAssetDto testAssetDtoMSFT;

    @BeforeEach
    void setUp() {
        // Setup test instruments
        testInstrumentAAPL = new InstrumentEntity();
        testInstrumentAAPL.setId(1L);
        testInstrumentAAPL.setTicker("AAPL");
        testInstrumentAAPL.setAlpacaMetadataUpdatedAt(Instant.now().minus(30, ChronoUnit.HOURS)); // Stale

        testInstrumentMSFT = new InstrumentEntity();
        testInstrumentMSFT.setId(2L);
        testInstrumentMSFT.setTicker("MSFT");
        testInstrumentMSFT.setAlpacaMetadataUpdatedAt(Instant.now().minus(25, ChronoUnit.HOURS)); // Stale

        testInstrumentGOOGL = new InstrumentEntity();
        testInstrumentGOOGL.setId(3L);
        testInstrumentGOOGL.setTicker("GOOGL");
        testInstrumentGOOGL.setAlpacaMetadataUpdatedAt(Instant.now().minus(1, ChronoUnit.HOURS)); // Fresh

        // Setup test asset DTOs
        testAssetDtoAAPL = new AlpacaAssetDto();
        testAssetDtoAAPL.setId("test-id-aapl");
        testAssetDtoAAPL.setSymbol("AAPL");
        testAssetDtoAAPL.setTradable(true);
        testAssetDtoAAPL.setMarginable(true);
        testAssetDtoAAPL.setShortable(true);
        testAssetDtoAAPL.setEasyToBorrow(true);
        testAssetDtoAAPL.setFractionable(true);
        testAssetDtoAAPL.setMaintenanceMarginRequirement(new BigDecimal("0.25"));
        testAssetDtoAAPL.setExchange("NASDAQ");
        testAssetDtoAAPL.setStatus("active");
        testAssetDtoAAPL.setAssetClass("us_equity");

        testAssetDtoMSFT = new AlpacaAssetDto();
        testAssetDtoMSFT.setId("test-id-msft");
        testAssetDtoMSFT.setSymbol("MSFT");
        testAssetDtoMSFT.setTradable(true);
        testAssetDtoMSFT.setMarginable(false);
        testAssetDtoMSFT.setShortable(false);
        testAssetDtoMSFT.setEasyToBorrow(false);
        testAssetDtoMSFT.setFractionable(true);
        testAssetDtoMSFT.setMaintenanceMarginRequirement(new BigDecimal("0.30"));
        testAssetDtoMSFT.setExchange("NASDAQ");
        testAssetDtoMSFT.setStatus("active");
        testAssetDtoMSFT.setAssetClass("us_equity");
    }

    /**
     * Test 1: Only stale instruments are refreshed
     * Verifies that fresh instruments are not included in the batch refresh
     */
    @Test
    void testRefreshOnlyStaleAssets() {
        // Setup: Return stale instruments, not fresh
        List<InstrumentEntity> staleInstruments = Arrays.asList(testInstrumentAAPL, testInstrumentMSFT);
        when(instrumentRepository.findStaleAlpacaAssets(any(Instant.class))).thenReturn(staleInstruments);

        // Create a real service instance with mocked repository
        AlpacaService realService = new AlpacaService(alpacaRestClient, alpacaTradingRestClient, objectMapper, instrumentRepository);
        AlpacaService spyService = spy(realService);

        doReturn(testAssetDtoAAPL).when(spyService).getAsset("AAPL");
        doReturn(testAssetDtoMSFT).when(spyService).getAsset("MSFT");
        when(instrumentRepository.save(any(InstrumentEntity.class))).thenReturn(testInstrumentAAPL, testInstrumentMSFT);

        // Execute
        int refreshedCount = spyService.refreshStaleAssetMetadata();

        // Assert
        assertEquals(2, refreshedCount);
        verify(instrumentRepository).findStaleAlpacaAssets(any(Instant.class));
        verify(instrumentRepository, times(2)).save(any(InstrumentEntity.class));
    }

    /**
     * Test 2: API failure for one ticker doesn't abort batch
     * Verifies that if one instrument fails to refresh, the batch continues
     */
    @Test
    void testBatchContinuesOnSingleFailure() {
        // Setup: Mock API to fail on MSFT
        List<InstrumentEntity> staleInstruments = Arrays.asList(testInstrumentAAPL, testInstrumentMSFT);
        when(instrumentRepository.findStaleAlpacaAssets(any(Instant.class))).thenReturn(staleInstruments);

        // Create a spy so we can verify real behavior
        AlpacaService spyAlpacaService = spy(alpacaService);
        doReturn(testAssetDtoAAPL).when(spyAlpacaService).getAsset("AAPL");
        doThrow(new RuntimeException("API Error")).when(spyAlpacaService).getAsset("MSFT");
        when(instrumentRepository.save(any(InstrumentEntity.class))).thenReturn(testInstrumentAAPL);

        // Execute
        int refreshedCount = spyAlpacaService.refreshStaleAssetMetadata();

        // Assert: Only AAPL refreshed, MSFT failed silently
        assertEquals(1, refreshedCount);
        verify(instrumentRepository).findStaleAlpacaAssets(any(Instant.class));
        verify(instrumentRepository, times(1)).save(any(InstrumentEntity.class));
    }

    /**
     * Test 3: Timestamp is updated on successful refresh
     * Verifies that alpacaMetadataUpdatedAt is set to current time
     */
    @Test
    void testTimestampUpdatedOnRefresh() {
        // Setup
        List<InstrumentEntity> staleInstruments = Collections.singletonList(testInstrumentAAPL);
        when(instrumentRepository.findStaleAlpacaAssets(any(Instant.class))).thenReturn(staleInstruments);

        // Create a real instance to test timestamp behavior
        AlpacaService realService = new AlpacaService(alpacaRestClient, alpacaTradingRestClient, objectMapper, instrumentRepository);
        AlpacaService spyService = spy(realService);
        doReturn(testAssetDtoAAPL).when(spyService).getAsset("AAPL");
        when(instrumentRepository.save(any(InstrumentEntity.class))).thenAnswer(invocation -> {
            InstrumentEntity saved = invocation.getArgument(0);
            assertNotNull(saved.getAlpacaMetadataUpdatedAt());
            assertTrue(saved.getAlpacaMetadataUpdatedAt().isBefore(Instant.now().plus(1, ChronoUnit.SECONDS)));
            return saved;
        });

        Instant before = Instant.now();

        // Execute
        spyService.refreshStaleAssetMetadata();

        Instant after = Instant.now();

        // Assert: Verify timestamp was set correctly
        ArgumentCaptor<InstrumentEntity> captor = ArgumentCaptor.forClass(InstrumentEntity.class);
        verify(instrumentRepository).save(captor.capture());
        InstrumentEntity saved = captor.getValue();
        assertTrue(saved.getAlpacaMetadataUpdatedAt().isAfter(before.minus(1, ChronoUnit.SECONDS)));
        assertTrue(saved.getAlpacaMetadataUpdatedAt().isBefore(after.plus(1, ChronoUnit.SECONDS)));
    }

    /**
     * Test 4: No stale assets returns 0
     * Verifies that when there are no stale assets, the method returns 0
     */
    @Test
    void testNoStaleAssetsReturnsZero() {
        // Setup: Return empty list
        when(instrumentRepository.findStaleAlpacaAssets(any(Instant.class))).thenReturn(Collections.emptyList());

        // Execute
        int refreshedCount = alpacaService.refreshStaleAssetMetadata();

        // Assert
        assertEquals(0, refreshedCount);
        verify(instrumentRepository).findStaleAlpacaAssets(any(Instant.class));
        verify(instrumentRepository, never()).save(any(InstrumentEntity.class));
    }

    /**
     * Test 5: Asset metadata fields are updated correctly
     * Verifies that saveAssetMetadata() correctly maps all fields from DTO to entity
     */
    @Test
    void testAssetMetadataFieldsUpdatedCorrectly() {
        // Setup: Create a fresh instrument
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setTicker("AAPL");

        // Execute: Call saveAssetMetadata
        alpacaService.saveAssetMetadata(testAssetDtoAAPL, instrument);

        // Assert: All fields are mapped correctly
        assertEquals("test-id-aapl", instrument.getAlpacaAssetId());
        assertTrue(instrument.getAlpacaTradable());
        assertTrue(instrument.getAlpacaMarginable());
        assertTrue(instrument.getAlpacaShortable());
        assertTrue(instrument.getAlpacaEasyToBorrow());
        assertTrue(instrument.getAlpacaFractionable());
        assertEquals(new BigDecimal("0.25"), instrument.getAlpacaMaintenanceMarginRequirement());
        assertEquals("NASDAQ", instrument.getAlpacaExchange());
        assertEquals("active", instrument.getAlpacaStatus());
        assertEquals("us_equity", instrument.getAlpacaAssetClass());
        assertNotNull(instrument.getAlpacaMetadataUpdatedAt());
    }

    /**
     * Test 6: Batch refresh returns correct count
     * Verifies that the return value correctly reflects number of successful refreshes
     */
    @Test
    void testBatchRefreshReturnsCorrectCount() {
        // Setup: 4 stale, 2 succeed, 2 fail
        List<InstrumentEntity> instruments = Arrays.asList(
                testInstrumentAAPL,
                testInstrumentMSFT,
                new InstrumentEntity(),
                new InstrumentEntity()
        );
        instruments.get(2).setTicker("TSLA");
        instruments.get(3).setTicker("NVDA");

        when(instrumentRepository.findStaleAlpacaAssets(any(Instant.class))).thenReturn(instruments);

        AlpacaService spyService = spy(alpacaService);
        doReturn(testAssetDtoAAPL).when(spyService).getAsset("AAPL");
        doReturn(testAssetDtoMSFT).when(spyService).getAsset("MSFT");
        doThrow(new RuntimeException("API Error")).when(spyService).getAsset("TSLA");
        doThrow(new RuntimeException("API Error")).when(spyService).getAsset("NVDA");
        when(instrumentRepository.save(any(InstrumentEntity.class))).thenReturn(testInstrumentAAPL, testInstrumentMSFT);

        // Execute
        int refreshedCount = spyService.refreshStaleAssetMetadata();

        // Assert: Only successful ones counted
        assertEquals(2, refreshedCount);
        verify(instrumentRepository).findStaleAlpacaAssets(any(Instant.class));
        verify(instrumentRepository, times(2)).save(any(InstrumentEntity.class));
    }
}
