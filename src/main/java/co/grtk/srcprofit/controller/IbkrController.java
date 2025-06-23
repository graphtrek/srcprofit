package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class IbkrController {
    private static final Logger LOG = LoggerFactory.getLogger(IbkrController.class);

    private final IbkrService ibkrService;
    private final OptionService optionService;
    private final InstrumentService instrumentService;

    public IbkrController(IbkrService ibkrService, OptionService optionService, InstrumentService instrumentService) {
        this.ibkrService = ibkrService;
        this.optionService = optionService;
        this.instrumentService = instrumentService;
    }

    @GetMapping("/import")
    public void importCSV() {
        optionService.csvToOptions(new File("/Users/Imre/tmp/ITATAI_OPTIONS.csv").toPath());
    }

}