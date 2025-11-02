package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.IbkrTradeExecutionDto;
import co.grtk.srcprofit.service.FlexReportsService;
import co.grtk.srcprofit.service.IbkrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IbkrRestController {
    private static final Logger log = LoggerFactory.getLogger(IbkrRestController.class);

    private final IbkrService ibkrService;
    private final FlexReportsService flexReportsService;

    public IbkrRestController(IbkrService ibkrService,
                              FlexReportsService flexReportsService) {
        this.ibkrService = ibkrService;
        this.flexReportsService = flexReportsService;
    }


    @GetMapping(value = "/ibkrFlexTradesImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexTradesImport() {
        return flexReportsService.importFlexTrades();
    }

    @GetMapping(value = "/ibkrFlexNetAssetValueImport", produces = MediaType.APPLICATION_XML_VALUE)
    public String ibkrFlexNetAssetValueImport() {
        return flexReportsService.importFlexNetAssetValue();
    }

    @GetMapping(value = "/ibkrLatestTrades", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IbkrTradeExecutionDto> getLatestTrades() {
        List<IbkrTradeExecutionDto> ibkrTradeExecutionDtoList = ibkrService.getLatestTrades();
        log.info("getLatestTrades returned {}", ibkrTradeExecutionDtoList);
        return ibkrTradeExecutionDtoList;
    }

}