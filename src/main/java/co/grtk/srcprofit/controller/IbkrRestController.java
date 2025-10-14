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
    private FlexStatementResponse flexNetAssetValueResponse = null;
    private int netAssetValueReferenceCodeCounter = 0;
    private FlexStatementResponse flexTradesResponse = null;
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


    @GetMapping(value = "/ibkrFlexTradesImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexTradesImport() {
        long start = System.currentTimeMillis();
        try {
            if(flexTradesResponse == null) {
                final String IBKR_FLEX_TRADES_ID = environment.getRequiredProperty("IBKR_FLEX_TRADES_ID");
                flexTradesResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_TRADES_ID);
                tradesReferenceCodeCounter++;
            }

            log.info("ibkrFlexTradesImport flexTradesResponse {}", flexTradesResponse);
            Thread.sleep(15000);
            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexTradesResponse.getUrl(), flexTradesResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_TRADES_" + flexTradesResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int csvRecords = optionService.saveCSV(file.toPath());
            int dataFixRecords = optionService.dataFix();
            flexTradesResponse = null;
            tradesReferenceCodeCounter = 0;
            long elapsed = System.currentTimeMillis() - start;
            log.info("ibkrFlexTradesImport file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return csvRecords + "/" + dataFixRecords + "/" + tradesReferenceCodeCounter;
        } catch (Exception e) {
            if( tradesReferenceCodeCounter >= 5) {
                flexTradesResponse = null;
                tradesReferenceCodeCounter = 0;
            } else {
                tradesReferenceCodeCounter++;
            }
            log.error("ibkrFlexTradesImport tried:{} exception {}", tradesReferenceCodeCounter, e.getMessage());
            return "WAITING_FOR REPORT /" + tradesReferenceCodeCounter;
        }
    }

    @GetMapping(value = "/ibkrFlexNetAssetValueImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexNetAssetValueImport() {
        long start = System.currentTimeMillis();
        try {
            if( flexNetAssetValueResponse == null) {
                final String IBKR_FLEX_NET_ASSET_VALUE_ID = environment.getRequiredProperty("IBKR_FLEX_NET_ASSET_VALUE_ID");
                flexNetAssetValueResponse = ibkrService.getFlexWebServiceSendRequest(IBKR_FLEX_NET_ASSET_VALUE_ID);
                netAssetValueReferenceCodeCounter++;
            }

            log.info("ibkrFlexNetAssetValueImport flexNetAssetValueResponse {}", flexNetAssetValueResponse);
            Thread.sleep(15000);
            String flexTradesQuery = ibkrService.getFlexWebServiceGetStatement(flexNetAssetValueResponse.getUrl(), flexNetAssetValueResponse.getReferenceCode());
            File file = new File(userHome + "/FLEX_NET_ASSET_VALUE_" + flexNetAssetValueResponse.getReferenceCode() + ".csv");
            FileUtils.write(file, flexTradesQuery, CharsetNames.CS_UTF8);
            int records = netAssetValueService.saveCSV(file.toPath());
            flexNetAssetValueResponse = null;
            netAssetValueReferenceCodeCounter = 0;

            long elapsed = System.currentTimeMillis() - start;
            log.info("ibkrFlexNetAssetValueImport file {} written elapsed:{}", file.getAbsolutePath(), elapsed);
            return String.valueOf(records) + "/" + netAssetValueReferenceCodeCounter;
        } catch (Exception e) {
            if ( netAssetValueReferenceCodeCounter >= 5) {
                flexNetAssetValueResponse = null;
                netAssetValueReferenceCodeCounter = 0;
            } else {
                netAssetValueReferenceCodeCounter++;
            }
            log.error("ibkrFlexNetAssetValueImport exception {}", e.getMessage());
            return "WAITING_FOR REPORT /" + netAssetValueReferenceCodeCounter;
        }
    }

    @GetMapping(value = "/ibkrLatestTrades", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IbkrTradeExecutionDto> getLatestTrades() {
        List<IbkrTradeExecutionDto> ibkrTradeExecutionDtoList = ibkrService.getLatestTrades();
        log.info("getLatestTrades returned {}", ibkrTradeExecutionDtoList);
        return ibkrTradeExecutionDtoList;
    }

}