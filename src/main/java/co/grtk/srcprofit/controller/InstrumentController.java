package co.grtk.srcprofit.controller;

import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.service.IbkrService;
import co.grtk.srcprofit.service.InstrumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InstrumentController {
    private static final String INSTRUMENTS_PAGE_PATH = "instruments";
    private final InstrumentService instrumentService;
    private final IbkrService ibkrService;

    public InstrumentController(InstrumentService instrumentService, IbkrService ibkrService) {
        this.instrumentService = instrumentService;
        this.ibkrService = ibkrService;
    }

    @GetMapping("/instruments")
    public String ibkrLogin(Model model) {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute("instruments", instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

    @GetMapping("/watchlist")
    public String watchlist(Model model) {
        IbkrWatchlistDto ibkrWatchlistDto  = ibkrService.getWatchlist();
        instrumentService.refreshWatchlist(ibkrWatchlistDto.getInstruments());
        String conidCSV = instrumentService.buildConidCsv(ibkrWatchlistDto.getInstruments());
        List<IbkrMarketDataDto> ibkrMarketDataDtos = ibkrService.getMarketDataSnapshots(conidCSV);
        instrumentService.saveIbkrMarketData(ibkrMarketDataDtos);
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        model.addAttribute("instruments", instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

    @GetMapping("/marketData")
    public String marketData(Model model) {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String conidCSV = instrumentService.buildConidCsv(instruments);
        List<IbkrMarketDataDto> ibkrMarketDataDtos = ibkrService.getMarketDataSnapshots(conidCSV);
        instrumentService.saveIbkrMarketData(ibkrMarketDataDtos);
        instruments = instrumentService.loadAllInstruments();
        model.addAttribute("instruments", instruments);
        return INSTRUMENTS_PAGE_PATH;
    }

}
