package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
 * This service owns @Scheduled annotations for automatic FLEX report imports,
 * running every 30 minutes with 1 minute initial delay.
 *
 * @see IbkrService for FLEX API methods
 * @see OptionService for trades CSV parsing
 * @see NetAssetValueService for NAV CSV parsing
 * @see FlexStatementPersistenceService for metadata persistence
 */
@Service
public class FlexReportsService {
    private static final Logger log = LoggerFactory.getLogger(FlexReportsService.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;
    private final NetAssetValueService netAssetValueService;
    private final Environment environment;
    private final FlexStatementPersistenceService flexStatementPersistenceService;

    // State for retry logic
    // TODO: Consider thread-safe implementation for concurrent scheduled jobs
    private FlexStatementResponse flexTradesResponse = null;
    private int tradesReferenceCodeCounter = 0;
    private FlexStatementResponse flexNetAssetValueResponse = null;
    private int netAssetValueReferenceCodeCounter = 0;
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
     * 2. Calls ScheduledReportsService to save response metadata
     * 3. Waits 15 seconds for report generation
     * 4. Calls FLEX GetStatement API to retrieve CSV
     * 5. Writes CSV to file: ~/FLEX_TRADES_{referenceCode}.csv
     * 6. Parses CSV and saves trades to database (OptionService)
     * 7. Runs data cleanup to remove orphaned records (OptionService.dataFix)
     *
     * Retry Logic:
     * - Uses instance variable state to track retries across calls
     * - Max 5 retry attempts (controlled by tradesReferenceCodeCounter)
     * - Returns "WAITING_FOR REPORT /{counter}" on failure
     * - Resets state after max retries exceeded
     *
     * Environment Variables Required:
     * - IBKR_FLEX_TRADES_ID: Query ID for trades report
     *
     * @return Success: "{csvRecords}/{dataFixRecords}/{counter}" (e.g., "42/3/0")
     *         Failure: "WAITING_FOR REPORT /{counter}" (e.g., "WAITING_FOR REPORT /2")
     */
    @Scheduled(fixedDelay = 30, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public String importFlexTrades() {
        long start = System.currentTimeMillis();
        try {
            if(flexTradesResponse == null) {
                final String IBKR_FLEX_TRADES_ID = environment.getRequiredProperty("IBKR_FLEX_TRADES_ID");
                flexTradesResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_TRADES_ID);
                tradesReferenceCodeCounter++;
            }

            // Save FLEX statement response metadata to database
            flexStatementPersistenceService.saveFlexStatementResponse(flexTradesResponse, "TRADES");

            log.info("importFlexTrades flexTradesResponse {}", flexTradesResponse);
            Thread.sleep(15000);
            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexTradesResponse.getUrl(), flexTradesResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_TRADES_" + flexTradesResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int csvRecords = optionService.saveCSV(flexTradesQuery);
            int dataFixRecords = optionService.dataFix();
            flexTradesResponse = null;
            tradesReferenceCodeCounter = 0;
            long elapsed = System.currentTimeMillis() - start;
            log.info("importFlexTrades file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return csvRecords + "/" + dataFixRecords + "/" + tradesReferenceCodeCounter;
        } catch (Exception e) {
            if( tradesReferenceCodeCounter >= 5) {
                flexTradesResponse = null;
                tradesReferenceCodeCounter = 0;
            } else {
                tradesReferenceCodeCounter++;
            }
            log.error("importFlexTrades tried:{} exception {}", tradesReferenceCodeCounter, e.getMessage());
            return "WAITING_FOR REPORT /" + tradesReferenceCodeCounter;
        }
    }

    /**
     * Imports FLEX Net Asset Value report from IBKR.
     *
     * Workflow:
     * 1. Calls FLEX SendRequest API to initiate report generation
     * 2. Calls ScheduledReportsService to save response metadata
     * 3. Waits 15 seconds for report generation
     * 4. Calls FLEX GetStatement API to retrieve CSV
     * 5. Writes CSV to file: ~/FLEX_NET_ASSET_VALUE_{referenceCode}.csv
     * 6. Parses CSV and saves NAV records to database (NetAssetValueService)
     *
     * Retry Logic:
     * - Uses instance variable state to track retries across calls
     * - Max 5 retry attempts (controlled by netAssetValueReferenceCodeCounter)
     * - Returns "WAITING_FOR REPORT /{counter}" on failure
     * - Resets state after max retries exceeded
     *
     * Environment Variables Required:
     * - IBKR_FLEX_NET_ASSET_VALUE_ID: Query ID for NAV report
     *
     * @return Success: "{records}/{counter}" (e.g., "30/0")
     *         Failure: "WAITING_FOR REPORT /{counter}" (e.g., "WAITING_FOR REPORT /3")
     */
    @Scheduled(fixedDelay = 30, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public String importFlexNetAssetValue() {
        long start = System.currentTimeMillis();
        try {
            if( flexNetAssetValueResponse == null) {
                final String IBKR_FLEX_NET_ASSET_VALUE_ID = environment.getRequiredProperty("IBKR_FLEX_NET_ASSET_VALUE_ID");
                flexNetAssetValueResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_NET_ASSET_VALUE_ID);
                netAssetValueReferenceCodeCounter++;
            }

            // Save FLEX statement response metadata to database
            flexStatementPersistenceService.saveFlexStatementResponse(flexNetAssetValueResponse, "NAV");

            log.info("importFlexNetAssetValue flexNetAssetValueResponse {}", flexNetAssetValueResponse);
            Thread.sleep(15000);
            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexNetAssetValueResponse.getUrl(), flexNetAssetValueResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_NET_ASSET_VALUE_" + flexNetAssetValueResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int records = netAssetValueService.saveCSV(flexTradesQuery);
            flexNetAssetValueResponse = null;
            netAssetValueReferenceCodeCounter = 0;

            long elapsed = System.currentTimeMillis() - start;
            log.info("importFlexNetAssetValue file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return String.valueOf(records) + "/" + netAssetValueReferenceCodeCounter;
        } catch (Exception e) {
            if ( netAssetValueReferenceCodeCounter >= 5) {
                flexNetAssetValueResponse = null;
                netAssetValueReferenceCodeCounter = 0;
            } else {
                netAssetValueReferenceCodeCounter++;
            }
            log.error("importFlexNetAssetValue exception {}", e.getMessage());
            return "WAITING_FOR REPORT /" + netAssetValueReferenceCodeCounter;
        }
    }
}
