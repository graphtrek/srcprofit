package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketDataService {
    public final AlpacaService alpacaService;
    private final InstrumentService instrumentService;
    private final IbkrService ibkrService;

    public MarketDataService(InstrumentService instrumentService, AlpacaService alpacaService, IbkrService ibkrService) {
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
        this.ibkrService = ibkrService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void refreshAlpacaMarketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String tickerCsv = instrumentService.buildTickerCsv(instruments);
        AlpacaMarketDataDto alpacaMarketDataDto = alpacaService.getMarketDataSnapshot(tickerCsv);
        instrumentService.saveAlpacaMarketData(alpacaMarketDataDto);
    }

    public void refreshIbkrMarketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String conidCSV = instrumentService.buildConidCsv(instruments);
        List<IbkrMarketDataDto> ibkrMarketDataDtos = ibkrService.getMarketDataSnapshots(conidCSV);
        instrumentService.saveIbkrMarketData(ibkrMarketDataDtos);
    }
}