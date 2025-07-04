package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.AlpacaService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlpacaRestController {
    private static final Logger LOG = LoggerFactory.getLogger(AlpacaRestController.class);

    private final AlpacaService alpacaService;
    private final OptionService optionService;
    private final InstrumentService instrumentService;

    public AlpacaRestController(AlpacaService alpacaService, OptionService optionService, InstrumentService instrumentService) {
        this.alpacaService = alpacaService;
        this.optionService = optionService;
        this.instrumentService = instrumentService;
    }

    @GetMapping(value = "/alpacaQuotes", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlpacaQuotesDto getQuotes() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        return alpacaService.getLatestQuotes(instrumentService.buildTickerCsv(instruments));
    }

    @GetMapping(value = "/alpacaMarketData", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlpacaMarketDataDto marketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        return alpacaService.getMarketDataSnapshot(instrumentService.buildTickerCsv(instruments));
    }

}