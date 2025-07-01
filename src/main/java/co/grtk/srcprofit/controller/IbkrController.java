package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class IbkrController {
    private static final Logger log = LoggerFactory.getLogger(IbkrController.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;
    private final InstrumentService instrumentService;

    public IbkrController(IbkrService ibkrService, OptionService optionService, InstrumentService instrumentService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
        this.instrumentService = instrumentService;
    }

    @GetMapping("/flexStatement")
    public FlexStatementResponse getFlexStatement() {
        return ibkrService.getFlexStatement();
    }

    @GetMapping("/flexQuery/{referenceCode}")
    public String getFlexQuery(@PathVariable String referenceCode) {
        String flexQuery = ibkrService.getFlexQuery(referenceCode);
        log.info("getFlexQuery referenceCode {} returned {}", referenceCode, flexQuery);
        return flexQuery;
    }

    @GetMapping("/import")
    public void importCSV() {
        optionService.csvToOptions(new File("/Users/Imre/tmp/ITATAI_OPTIONS.csv").toPath());
    }

}