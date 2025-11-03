package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Service for orchestrating FLEX report imports from Interactive Brokers.
 *
 * Handles the two-step FLEX API workflow:
 * 1. SendRequest - Initiates report generation, returns reference code
 * 2. GetStatement - Retrieves generated CSV report using reference code
 * 3. CSV Parsing - Parses CSV and saves to database
 * 4. Data Cleanup - Removes orphaned records
 *
 * Extracted from IbkrRestController to enable reuse from scheduled jobs
 * and improve separation of concerns.
 *
 * Database persistence (saving FLEX API metadata) is delegated to
 * FlexStatementPersistenceService to maintain clean separation of concerns.
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
 * @see FlexStatementPersistenceService for metadata persistence
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
    private final FlexStatementPersistenceService flexStatementPersistenceService;
    private final String userHome = System.getProperty("user.home");

    public FlexReportsService(IbkrService ibkrService,
                              OptionService optionService,
                              NetAssetValueService netAssetValueService,
                              Environment environment,
                              FlexStatementPersistenceService flexStatementPersistenceService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
        this.netAssetValueService = netAssetValueService;
        this.environment = environment;
        this.flexStatementPersistenceService = flexStatementPersistenceService;
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
     * Stateless Design:
     * - No instance variable state (thread-safe for concurrent calls)
     * - Each call is independent
     * - Retry logic should be handled by ScheduledJobsService (not here)
     *
     * Environment Variables Required:
     * - IBKR_FLEX_TRADES_ID: Query ID for trades report
     *
     * @return Success: "{csvRecords}/{dataFixRecords}/0" (e.g., "42/3/0")
     * @throws RuntimeException if API call fails or CSV parsing fails
     */
    public String importFlexTrades() {
        long start = System.currentTimeMillis();
        try {
            final String IBKR_FLEX_TRADES_ID = environment.getRequiredProperty("IBKR_FLEX_TRADES_ID");
            FlexStatementResponse flexTradesResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_TRADES_ID);

            // Save FLEX statement response metadata to database
            flexStatementPersistenceService.saveFlexStatementResponse(flexTradesResponse, "TRADES");

            log.info("importFlexTrades flexTradesResponse {}", flexTradesResponse);
            Thread.sleep(WAIT_FOR_REPORT_MS);

            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexTradesResponse.getUrl(), flexTradesResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_TRADES_" + flexTradesResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int csvRecords = optionService.saveCSV(flexTradesQuery);
            int dataFixRecords = optionService.dataFix();

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
     * Stateless Design:
     * - No instance variable state (thread-safe for concurrent calls)
     * - Each call is independent
     * - Retry logic should be handled by ScheduledJobsService (not here)
     *
     * Environment Variables Required:
     * - IBKR_FLEX_NET_ASSET_VALUE_ID: Query ID for NAV report
     *
     * @return Success: "{records}/0" (e.g., "30/0")
     * @throws RuntimeException if API call fails or CSV parsing fails
     */
    public String importFlexNetAssetValue() {
        long start = System.currentTimeMillis();
        try {
            final String IBKR_FLEX_NET_ASSET_VALUE_ID = environment.getRequiredProperty("IBKR_FLEX_NET_ASSET_VALUE_ID");
            FlexStatementResponse flexNetAssetValueResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_NET_ASSET_VALUE_ID);

            // Save FLEX statement response metadata to database
            flexStatementPersistenceService.saveFlexStatementResponse(flexNetAssetValueResponse, "NAV");

            log.info("importFlexNetAssetValue flexNetAssetValueResponse {}", flexNetAssetValueResponse);
            Thread.sleep(WAIT_FOR_REPORT_MS);

            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexNetAssetValueResponse.getUrl(), flexNetAssetValueResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_NET_ASSET_VALUE_" + flexNetAssetValueResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int records = netAssetValueService.saveCSV(flexTradesQuery);

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
