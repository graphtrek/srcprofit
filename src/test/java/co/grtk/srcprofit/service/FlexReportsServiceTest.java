package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.CsvImportResult;
import co.grtk.srcprofit.entity.FlexStatementResponseEntity;
import co.grtk.srcprofit.repository.FlexStatementResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlexReportsService, specifically testing FLEX import operation tracking.
 *
 * Covers:
 * - Failed and skipped record count persistence for TRADES imports
 * - Failed and skipped record count persistence for NAV imports
 * - Verification that CsvImportResult counts are correctly saved to FlexStatementResponseEntity
 * - Integration with FlexStatementResponseRepository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlexReportsService FLEX Import Tracking Tests")
class FlexReportsServiceTest {

    @Mock
    private IbkrService ibkrService;

    @Mock
    private OptionService optionService;

    @Mock
    private NetAssetValueService netAssetValueService;

    @Mock
    private Environment environment;

    @Mock
    private FlexStatementResponseRepository flexStatementResponseRepository;

    @InjectMocks
    private FlexReportsService flexReportsService;

    private FlexStatementResponseEntity testEntity;
    private CsvImportResult testTradesResult;

    @BeforeEach
    void setUp() {
        // Create a test entity that will be returned by repository
        testEntity = new FlexStatementResponseEntity();
        testEntity.setId(1L);
        testEntity.setReferenceCode("TEST-REF-001");
        testEntity.setRequestDate("2025-11-16 10:00:00");
        testEntity.setStatus("Success");
        testEntity.setUrl("https://ibkr.example.com/flex/report");
        testEntity.setReportType("TRADES");
        testEntity.setOriginalTimestamp("2025-11-16 10:00:00");
        testEntity.setDbUrl("jdbc:postgresql://localhost:5432/srcprofit");

        // Create a sample CsvImportResult with mixed outcomes
        testTradesResult = new CsvImportResult();
        testTradesResult.setTotalRecords(100);
        testTradesResult.setSuccessfulRecords(90);
        testTradesResult.setFailedRecords(5);
        testTradesResult.setSkippedRecords(5);
    }

    /**
     * Test 1: importFlexTrades persists successful record count
     * Verifies that csvRecordsCount is set to the number of successful records from CsvImportResult
     */
    @Test
    @DisplayName("importFlexTrades persists successful record count")
    void testImportFlexTradesPersistsSuccessfulRecordCount() throws InterruptedException {
        // Setup: Mock the IBKR and OptionService responses
        when(environment.getProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-001"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-001")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(3);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-001")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify: save() is called twice - once in saveFlexStatementResponse() and once in importFlexTrades()
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        // Verify the last save has the monitoring fields
        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvRecordsCount())
                .as("csvRecordsCount should equal successful records from CsvImportResult")
                .isEqualTo(90);
    }

    /**
     * Test 2: importFlexTrades persists failed record count
     * Verifies that csvFailedRecordsCount is extracted from CsvImportResult and persisted
     */
    @Test
    @DisplayName("importFlexTrades persists failed record count")
    void testImportFlexTradesPersistsFailedRecordCount() throws InterruptedException {
        // Setup
        when(environment.getProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-001"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-001")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(3);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-001")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify: save() is called twice - get last call which has monitoring fields
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvFailedRecordsCount())
                .as("csvFailedRecordsCount should equal failed records from CsvImportResult")
                .isEqualTo(5);
    }

    /**
     * Test 3: importFlexTrades persists skipped record count
     * Verifies that csvSkippedRecordsCount is extracted from CsvImportResult and persisted
     */
    @Test
    @DisplayName("importFlexTrades persists skipped record count")
    void testImportFlexTradesPersistsSkippedRecordCount() throws InterruptedException {
        // Setup
        when(environment.getProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-001"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-001")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(3);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-001")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify: save() is called twice - get last call which has monitoring fields
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvSkippedRecordsCount())
                .as("csvSkippedRecordsCount should equal skipped records from CsvImportResult")
                .isEqualTo(5);
    }

    /**
     * Test 4: importFlexTrades persists all counts together
     * Verifies that all three counts (successful, failed, skipped) are persisted in a single save
     */
    @Test
    @DisplayName("importFlexTrades persists all counts together")
    void testImportFlexTradesPersistsAllCountsTogether() throws InterruptedException {
        // Setup
        when(environment.getProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-001"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-001")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(3);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-001")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify all three counts in the final update call
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvRecordsCount()).isEqualTo(90);
        assertThat(lastSaved.getCsvFailedRecordsCount()).isEqualTo(5);
        assertThat(lastSaved.getCsvSkippedRecordsCount()).isEqualTo(5);
        assertThat(lastSaved.getDataFixRecordsCount()).isEqualTo(3);
    }

    /**
     * Test 5: importFlexTrades handles zero failed records
     * Verifies behavior when import is completely successful (no failures)
     */
    @Test
    @DisplayName("importFlexTrades handles zero failed records")
    void testImportFlexTradesHandlesZeroFailedRecords() throws InterruptedException {
        // Setup: All successful, no failures or skips
        CsvImportResult perfectResult = new CsvImportResult();
        perfectResult.setTotalRecords(100);
        perfectResult.setSuccessfulRecords(100);
        perfectResult.setFailedRecords(0);
        perfectResult.setSkippedRecords(0);

        when(environment.getRequiredProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-002"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-002")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(perfectResult);
        when(optionService.dataFix()).thenReturn(0);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-002")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvFailedRecordsCount()).isEqualTo(0);
        assertThat(lastSaved.getCsvSkippedRecordsCount()).isEqualTo(0);
    }

    /**
     * Test 6: importFlexNetAssetValue sets failed/skipped counts to 0
     * Verifies that NAV imports set failed/skipped to 0 (not applicable for NAV)
     */
    @Test
    @DisplayName("importFlexNetAssetValue sets failed/skipped counts to 0")
    void testImportFlexNetAssetValueSetsCountsToZero() throws Exception {
        // Setup
        FlexStatementResponseEntity navEntity = new FlexStatementResponseEntity();
        navEntity.setId(2L);
        navEntity.setReferenceCode("TEST-REF-NAV-001");
        navEntity.setRequestDate("2025-11-16 10:00:00");
        navEntity.setStatus("Success");
        navEntity.setUrl("https://ibkr.example.com/flex/nav");
        navEntity.setReportType("NAV");

        when(environment.getProperty("IBKR_FLEX_NET_ASSET_VALUE_ID")).thenReturn("TEST_NAV_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_NAV_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-NAV-001"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-NAV-001")))
                .thenReturn("CSV,NAV,CONTENT");
        when(netAssetValueService.saveCSV("CSV,NAV,CONTENT")).thenReturn(30);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-NAV-001")).thenReturn(navEntity);

        // Execute
        flexReportsService.importFlexNetAssetValue();

        // Verify: save() is called twice - get last call which has monitoring fields
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvRecordsCount()).isEqualTo(30);
        assertThat(lastSaved.getCsvFailedRecordsCount())
                .as("NAV imports should set failed records to 0")
                .isEqualTo(0);
        assertThat(lastSaved.getCsvSkippedRecordsCount())
                .as("NAV imports should set skipped records to 0")
                .isEqualTo(0);
        assertThat(lastSaved.getDataFixRecordsCount())
                .as("NAV imports should set dataFix records to null")
                .isNull();
    }

    /**
     * Test 7: importFlexTrades with high failure rate
     * Verifies behavior when most records fail (10% success, 60% failed, 30% skipped)
     */
    @Test
    @DisplayName("importFlexTrades handles high failure rate")
    void testImportFlexTradesHandlesHighFailureRate() throws InterruptedException {
        // Setup: 10 successful, 60 failed, 30 skipped out of 100
        CsvImportResult poorResult = new CsvImportResult();
        poorResult.setTotalRecords(100);
        poorResult.setSuccessfulRecords(10);
        poorResult.setFailedRecords(60);
        poorResult.setSkippedRecords(30);

        when(environment.getRequiredProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-003"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-003")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(poorResult);
        when(optionService.dataFix()).thenReturn(0);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-003")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvRecordsCount()).isEqualTo(10);
        assertThat(lastSaved.getCsvFailedRecordsCount()).isEqualTo(60);
        assertThat(lastSaved.getCsvSkippedRecordsCount()).isEqualTo(30);
    }

    /**
     * Test 8: importFlexTrades when repository lookup returns null
     * Verifies graceful handling when entity is not found (shouldn't crash)
     */
    @Test
    @DisplayName("importFlexTrades handles null entity from repository")
    void testImportFlexTradesHandlesNullEntity() throws InterruptedException {
        // Setup: Repository returns null (entity not found)
        when(environment.getRequiredProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-004"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-004")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(3);
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-004")).thenReturn(null);

        // Execute: Should not throw even though entity is null
        String result = flexReportsService.importFlexTrades();

        // Verify: Should complete successfully
        // save() is called once in saveFlexStatementResponse() for initial metadata
        // but NOT called again in the update section (since entity lookup returns null)
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, times(1)).save(captor.capture());

        // Verify the return value contains the expected data
        assertThat(result).contains("90/3/5");
    }

    /**
     * Test 9: importFlexTrades preserves data fix records count
     * Verifies that dataFixRecordsCount is correctly set along with the new counts
     */
    @Test
    @DisplayName("importFlexTrades preserves data fix records count")
    void testImportFlexTradesPreservesDataFixCount() throws InterruptedException {
        // Setup
        when(environment.getProperty("IBKR_FLEX_TRADES_ID")).thenReturn("TEST_QUERY_ID");
        when(ibkrService.getFlexWebServiceSendRequest("TEST_QUERY_ID"))
                .thenReturn(createMockFlexResponse("TEST-REF-005"));
        when(ibkrService.getFlexWebServiceGetStatement(anyString(), eq("TEST-REF-005")))
                .thenReturn("CSV,CONTENT,HERE");
        when(optionService.saveCSV("CSV,CONTENT,HERE")).thenReturn(testTradesResult);
        when(optionService.dataFix()).thenReturn(7); // 7 records fixed
        when(flexStatementResponseRepository.findByReferenceCode("TEST-REF-005")).thenReturn(testEntity);

        // Execute
        flexReportsService.importFlexTrades();

        // Verify all four counts are present
        ArgumentCaptor<FlexStatementResponseEntity> captor = ArgumentCaptor.forClass(FlexStatementResponseEntity.class);
        verify(flexStatementResponseRepository, atLeast(2)).save(captor.capture());

        FlexStatementResponseEntity lastSaved = captor.getValue();
        assertThat(lastSaved.getCsvRecordsCount()).isEqualTo(90);
        assertThat(lastSaved.getCsvFailedRecordsCount()).isEqualTo(5);
        assertThat(lastSaved.getCsvSkippedRecordsCount()).isEqualTo(5);
        assertThat(lastSaved.getDataFixRecordsCount()).isEqualTo(7);
    }

    // Helper method to create mock FlexStatementResponse
    private co.grtk.srcprofit.dto.FlexStatementResponse createMockFlexResponse(String referenceCode) {
        co.grtk.srcprofit.dto.FlexStatementResponse response = new co.grtk.srcprofit.dto.FlexStatementResponse();
        response.setReferenceCode(referenceCode);
        response.setStatus("Success");
        response.setTimestamp("2025-11-16 10:00:00");
        response.setUrl("https://ibkr.example.com/flex/report");
        return response;
    }
}
