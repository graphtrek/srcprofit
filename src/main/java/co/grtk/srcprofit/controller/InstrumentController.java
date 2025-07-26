package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.service.InstrumentService;
import co.grtk.srcprofit.service.MarketDataService;
import co.grtk.srcprofit.service.OptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InstrumentController {
    public static final String MODEL_ATTRIBUTE_INSTRUMENTS = "instruments";
    private static final String INSTRUMENTS_PAGE_PATH = "instruments_jte";
    private final InstrumentService instrumentService;
    private final MarketDataService marketDataService;
    private final OptionService optionService;

    public InstrumentController(InstrumentService instrumentService, MarketDataService marketDataService, OptionService optionService) {
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
        this.optionService = optionService;
    }

    @GetMapping("/instruments")
    public String getInstruments(Model model) {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        List<PositionDto> optionHistory = optionService.getAllClosedOptions(null);
        List<PositionDto> openOptions = optionService.getAllOpenPositions(null);
        for (InstrumentDto instrument : instruments) {
            PositionDto positionDto = new PositionDto();
            positionDto.setTicker(instrument.getTicker());
            List<PositionDto> instrumentOpenPositions =
                    openOptions.stream().filter(p -> p.getTicker().equals(instrument.getTicker())).toList();
            List<PositionDto> instrumentClosedPositions =
                    optionHistory.stream().filter(p -> p.getTicker().equals(instrument.getTicker())).toList();
            optionService.calculatePosition(positionDto, instrumentOpenPositions, instrumentClosedPositions);
            instrument.setUnRealizedProfitOrLoss(positionDto.getUnRealizedProfitOrLoss());
            instrument.setRealizedProfitOrLoss(positionDto.getRealizedProfitOrLoss());
            instrument.setCollectedPremium(positionDto.getCollectedPremium());
        }
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
    public String alpacaMarketData(Model model) throws JsonProcessingException {
        marketDataService.refreshAlpacaMarketData();
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute(MODEL_ATTRIBUTE_INSTRUMENTS, instruments);
        return INSTRUMENTS_PAGE_PATH;
    }
}