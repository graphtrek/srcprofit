package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.MarketDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InstrumentController {
    public static final String MODEL_ATTRIBUTE_INSTRUMENTS = "instruments";
    private static final String INSTRUMENTS_PAGE_PATH = "instruments";
    private final InstrumentService instrumentService;
    private final MarketDataService marketDataService;

    public InstrumentController(InstrumentService instrumentService, MarketDataService marketDataService) {
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/instruments")
    public String ibkrLogin(Model model) {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute(MODEL_ATTRIBUTE_INSTRUMENTS, instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

    @GetMapping("/ibkrWatchlist")
    public String ibkrWatchlist(Model model) {
        marketDataService.refreshIbkrMarketData();
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute(MODEL_ATTRIBUTE_INSTRUMENTS, instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

    @GetMapping("/ibkrMarketData")
    public String ibkrMarketData(Model model) {
        marketDataService.refreshIbkrMarketData();
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute(MODEL_ATTRIBUTE_INSTRUMENTS, instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

    @GetMapping("/alpacaMarketData")
    public String alpacaMarketData(Model model) {
        marketDataService.refreshAlpacaMarketData();
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute(MODEL_ATTRIBUTE_INSTRUMENTS, instruments);
        return INSTRUMENTS_PAGE_PATH;
    }
}