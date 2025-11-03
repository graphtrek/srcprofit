package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.entity.FlexStatementResponseEntity;
import co.grtk.srcprofit.repository.FlexStatementResponseRepository;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

/**
 * Service for orchestrating FLEX report imports from Interactive Brokers.
 *
 * Handles the complete FLEX import workflow:
 * 1. SendRequest - Initiates report generation, returns reference code
 * 2. Metadata Persistence - Saves FLEX API response to database (audit trail)
 * 3. Wait - 15 seconds for report generation
 * 4. GetStatement - Retrieves generated CSV report using reference code
 * 5. File Write - Saves CSV to local filesystem
 * 6. CSV Parsing - Parses CSV and saves to database
 * 7. Data Cleanup - Removes orphaned records
 *
 * Transaction Management:
 * - Both import methods are @Transactional
 * - Entire workflow is atomic (API call + metadata + CSV parsing)
 * - Rollback occurs if any step fails
 *
 * IMPORTANT: This service is stateless orchestration only. Scheduling is handled
 * by ScheduledJobsService. Use this service for the actual FLEX API orchestration logic.
 *
 * Note: This implementation uses a thread-safe, stateless approach. Each call is
 * independent with no shared mutable state (unlike the previous instance variable approach).
 * This makes it safe for concurrent execution from both scheduled jobs and manual API endpoints.
 *
 * @see ScheduledJobsService for @Scheduled annotations (job scheduling)
 * @see IbkrService for FLEX API methods
 * @see OptionService for trades CSV parsing
 * @see NetAssetValueService for NAV CSV parsing
 * @see FlexStatementResponseRepository for FLEX metadata persistence
 */
@Service
public class FlexReportsService {
    private static final Logger log = LoggerFactory.getLogger(FlexReportsService.class);
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long WAIT_FOR_REPORT_MS = 15000;  // 15 seconds

    private final IbkrService ibkrService;
    private final OptionService optionService;
    private final NetAssetValueService netAssetValueService;
    private final Environment environment;
    private final FlexStatementResponseRepository flexStatementResponseRepository;
    private final String userHome = System.getProperty("user.home");

    public FlexReportsService(IbkrService ibkrService,
                              OptionService optionService,
                              NetAssetValueService netAssetValueService,
                              Environment environment,
                              FlexStatementResponseRepository flexStatementResponseRepository) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
        this.netAssetValueService = netAssetValueService;
        this.environment = environment;
        this.flexStatementResponseRepository = flexStatementResponseRepository;
    }

    /**
     * Saves FlexStatementResponse metadata to database.
     *
     * Creates a persistence record of FLEX API request for audit trail,
     * storing the full timestamp string from FLEX API response.
     *
     * @param response the FLEX API response containing reference code, timestamp, status, URL
     * @param reportType the report type ("TRADES" for options trades, "NAV" for net asset value)
     */
    private void saveFlexStatementResponse(FlexStatementResponse response, String reportType) {
        try {
            FlexStatementResponseEntity entity = new FlexStatementResponseEntity();
            entity.setReferenceCode(response.getReferenceCode());
            entity.setRequestDate(response.getTimestamp());
            entity.setStatus(response.getStatus());
            entity.setUrl(response.getUrl());
            entity.setReportType(reportType);
            entity.setOriginalTimestamp(response.getTimestamp());
            entity.setDbUrl(environment.getProperty("SRCPROFIT_DB_URL"));

            flexStatementResponseRepository.save(entity);
            log.info("Saved FlexStatementResponse to database: referenceCode={}, reportType={}, requestDate={}, dbUrl={}",
                    entity.getReferenceCode(), entity.getReportType(), entity.getRequestDate(), entity.getDbUrl());
        } catch (Exception e) {
            // Log error but don't fail the import process
            log.error("Failed to save FlexStatementResponse to database: {}", e.getMessage(), e);
        }
    }

    /**
     * Imports FLEX Trades report from IBKR.
     *
     * Workflow:
     * 1. Calls FLEX SendRequest API to initiate report generation
     * 2. Saves response metadata to database
     * 3. Waits 15 seconds for report generation
     * 4. Calls FLEX GetStatement API to retrieve CSV
     * 5. Writes CSV to file: ~/FLEX_TRADES_{referenceCode}.csv
     * 6. Parses CSV and saves trades to database (OptionService)
     * 7. Runs data cleanup to remove orphaned records (OptionService.dataFix)
     *
     * Transactional:
     * - Entire workflow is atomic (API call + metadata + CSV parsing)
     * - Rollback occurs if any step fails
     *
     * Environment Variables Required:
     * - IBKR_FLEX_TRADES_ID: Query ID for trades report
     *
     * @return Success: "{csvRecords}/{dataFixRecords}/0" (e.g., "42/3/0")
     * @throws RuntimeException if API call fails or CSV parsing fails
     */
    @Transactional
    public String importFlexTrades() {
        long start = System.currentTimeMillis();
        try {
            final String IBKR_FLEX_TRADES_ID = environment.getRequiredProperty("IBKR_FLEX_TRADES_ID");
            FlexStatementResponse flexTradesResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_TRADES_ID);

            // Save FLEX statement response metadata to database
            saveFlexStatementResponse(flexTradesResponse, "TRADES");

            log.info("importFlexTrades flexTradesResponse {}", flexTradesResponse);
            Thread.sleep(WAIT_FOR_REPORT_MS);

            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexTradesResponse.getUrl(), flexTradesResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_TRADES_" + flexTradesResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int csvRecords = optionService.saveCSV(flexTradesQuery);
            int dataFixRecords = optionService.dataFix();

            // Update entity with monitoring fields
            FlexStatementResponseEntity entity = flexStatementResponseRepository.findByReferenceCode(flexTradesResponse.getReferenceCode());
            if (entity != null) {
                entity.setCsvFilePath(file.getAbsolutePath());
                entity.setCsvRecordsCount(csvRecords);
                entity.setDataFixRecordsCount(dataFixRecords);
                flexStatementResponseRepository.save(entity);
                log.info("Updated FlexStatementResponse with monitoring fields: csvRecords={}, dataFixRecords={}, csvFilePath={}",
                        csvRecords, dataFixRecords, file.getAbsolutePath());
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("importFlexTrades file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return csvRecords + "/" + dataFixRecords + "/0";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for FLEX report", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import FLEX trades", e);
        }
    }

    /**
     * Imports FLEX Net Asset Value report from IBKR.
     *
     * Workflow:
     * 1. Calls FLEX SendRequest API to initiate report generation
     * 2. Saves response metadata to database
     * 3. Waits 15 seconds for report generation
     * 4. Calls FLEX GetStatement API to retrieve CSV
     * 5. Writes CSV to file: ~/FLEX_NET_ASSET_VALUE_{referenceCode}.csv
     * 6. Parses CSV and saves NAV records to database (NetAssetValueService)
     *
     * Transactional:
     * - Entire workflow is atomic (API call + metadata + CSV parsing)
     * - Rollback occurs if any step fails
     *
     * Environment Variables Required:
     * - IBKR_FLEX_NET_ASSET_VALUE_ID: Query ID for NAV report
     *
     * @return Success: "{records}/0" (e.g., "30/0")
     * @throws RuntimeException if API call fails or CSV parsing fails
     */
    @Transactional
    public String importFlexNetAssetValue() {
        long start = System.currentTimeMillis();
        try {
            final String IBKR_FLEX_NET_ASSET_VALUE_ID = environment.getRequiredProperty("IBKR_FLEX_NET_ASSET_VALUE_ID");
            FlexStatementResponse flexNetAssetValueResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_NET_ASSET_VALUE_ID);

            // Save FLEX statement response metadata to database
            saveFlexStatementResponse(flexNetAssetValueResponse, "NAV");

            log.info("importFlexNetAssetValue flexNetAssetValueResponse {}", flexNetAssetValueResponse);
            Thread.sleep(WAIT_FOR_REPORT_MS);

            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexNetAssetValueResponse.getUrl(), flexNetAssetValueResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_NET_ASSET_VALUE_" + flexNetAssetValueResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int records = netAssetValueService.saveCSV(flexTradesQuery);

            // Update entity with monitoring fields
            FlexStatementResponseEntity entity = flexStatementResponseRepository.findByReferenceCode(flexNetAssetValueResponse.getReferenceCode());
            if (entity != null) {
                entity.setCsvFilePath(file.getAbsolutePath());
                entity.setCsvRecordsCount(records);
                entity.setDataFixRecordsCount(null); // NAV reports don't have data fix
                flexStatementResponseRepository.save(entity);
                log.info("Updated FlexStatementResponse with monitoring fields: csvRecords={}, csvFilePath={}",
                        records, file.getAbsolutePath());
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("importFlexNetAssetValue file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return String.valueOf(records) + "/0";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for FLEX report", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import FLEX Net Asset Value", e);
        }
    }
}
