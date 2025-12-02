package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.AlpacaService;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.OptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping(value = "/alpacaStocksQuotes", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlpacaQuotesDto getStocksQuotes() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        return alpacaService.getStocksLatestQuotes(instrumentService.buildTickerCsv(instruments));
    }

    @GetMapping(value = "/alpacaOptionsQuotes", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlpacaQuotesDto getOptionsQuotes() throws JsonProcessingException {
        List<PositionDto>  openOptions = optionService.getAllOpenOptionDtos(null);
        String symbols = openOptions.stream().map(dto -> dto.getCode().replaceAll("\\s","")).collect(Collectors.joining(","));
        return alpacaService.getOptionsLatestQuotes(symbols);
    }

    @GetMapping(value = "/alpacaMarketData", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlpacaMarketDataDto marketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        return alpacaService.getMarketDataSnapshot(instrumentService.buildTickerCsv(instruments));
    }


}