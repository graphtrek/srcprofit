package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.dto.IbkrTradeExecutionDto;
import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.NetAssetValueService;
import co.grtk.srcprofit.service.OptionService;
import com.ctc.wstx.io.CharsetNames;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
public class IbkrRestController {
    private static final Logger log = LoggerFactory.getLogger(IbkrRestController.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;
    private final Environment environment;
    private final NetAssetValueService netAssetValueService;
    private final String userHome = System.getProperty("user.home");
    private String netAssetValueReferenceCode = null;
    private int netAssetValueReferenceCodeCounter = 0;
    private String tradesReferenceCode = null;
    private int tradesReferenceCodeCounter = 0;

    public IbkrRestController(IbkrService ibkrService,
                              OptionService optionService,
                              Environment environment,
                              NetAssetValueService netAssetValueService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
        this.environment = environment;
        this.netAssetValueService = netAssetValueService;
    }

    @GetMapping("/ibkrFlexQuery/{referenceCode}")
    public String getFlexQuery(@PathVariable String referenceCode) {
        String flexQuery = ibkrService.getFlexQuery(referenceCode);
        log.info("getFlexQuery referenceCode {} returned {}", referenceCode, flexQuery);
        return flexQuery;
    }

    @GetMapping(value = "/ibkrFlexTradesImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexTradesImport() {
        long start = System.currentTimeMillis();
        try {
            if( tradesReferenceCode == null) {
                final String IBKR_FLEX_TRADES_ID = environment.getRequiredProperty("IBKR_FLEX_TRADES_ID");
                FlexStatementResponse flexStatementResponse = ibkrService.getFlexStatement(IBKR_FLEX_TRADES_ID);
                tradesReferenceCode = flexStatementResponse.getReferenceCode();
            }

            log.info("ibkrFlexTradesImport tradesReferenceCode {}", tradesReferenceCode);

            String flexTradesQuery = ibkrService.getFlexQuery(tradesReferenceCode);
            File file = new File(userHome + "/FLEX_TRADES_" + tradesReferenceCode + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int csvRecords = optionService.saveCSV(file.toPath());
            int dataFixRecords = optionService.dataFix();
            tradesReferenceCode = null;
            tradesReferenceCodeCounter = 0;
            long elapsed = System.currentTimeMillis() - start;
            log.info("ibkrFlexTradesImport file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return csvRecords + "/" + dataFixRecords + "/" + tradesReferenceCodeCounter;
        } catch (Exception e) {
            if( tradesReferenceCodeCounter >= 5) {
                tradesReferenceCode = null;
                tradesReferenceCodeCounter = 0;
            } else {
                tradesReferenceCodeCounter++;
            }
            log.error("ibkrFlexTradesImport tried:{} exception {}", tradesReferenceCodeCounter, e.getMessage());
            return "WAITING_FOR " + tradesReferenceCode+ "/" + tradesReferenceCodeCounter;
        }
    }

    @GetMapping(value = "/ibkrFlexNetAssetValueImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexNetAssetValueImport() {
        long start = System.currentTimeMillis();
        try {
            if( netAssetValueReferenceCode == null) {
                final String IBKR_FLEX_NET_ASSET_VALUE_ID = environment.getRequiredProperty("IBKR_FLEX_NET_ASSET_VALUE_ID");
                FlexStatementResponse flexStatementResponse = ibkrService.getFlexStatement(IBKR_FLEX_NET_ASSET_VALUE_ID);
                netAssetValueReferenceCode = flexStatementResponse.getReferenceCode();
                netAssetValueReferenceCodeCounter++;
            }

            log.info("ibkrFlexNetAssetValueImport netAssetValueReferenceCode {}", netAssetValueReferenceCode);

            String flexTradesQuery = ibkrService.getFlexQuery(netAssetValueReferenceCode);
            File file = new File(userHome + "/FLEX_NET_ASSET_VALUE_" + netAssetValueReferenceCode + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int records = netAssetValueService.saveCSV(file.toPath());
            netAssetValueReferenceCode = null;
            netAssetValueReferenceCodeCounter = 0;

            long elapsed = System.currentTimeMillis() - start;
            log.info("ibkrFlexNetAssetValueImport file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return String.valueOf(records) + "/" + netAssetValueReferenceCodeCounter;
        } catch (Exception e) {
            if ( netAssetValueReferenceCodeCounter >= 5) {
                netAssetValueReferenceCode = null;
                netAssetValueReferenceCodeCounter = 0;
            } else {
                netAssetValueReferenceCodeCounter++;
            }
            log.error("ibkrFlexNetAssetValueImport exception {}", e.getMessage());
            return "WAITING_FOR " + netAssetValueReferenceCode + "/" + netAssetValueReferenceCodeCounter;
        }
    }

    @GetMapping(value = "/ibkrLatestTrades", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IbkrTradeExecutionDto> getLatestTrades() {
        List<IbkrTradeExecutionDto> ibkrTradeExecutionDtoList = ibkrService.getLatestTrades();
        log.info("getLatestTrades returned {}", ibkrTradeExecutionDtoList);
        return ibkrTradeExecutionDtoList;
    }

}