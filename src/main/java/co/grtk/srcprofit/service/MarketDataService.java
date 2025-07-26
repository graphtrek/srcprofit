package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuoteDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.entity.OptionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketDataService {
    public final AlpacaService alpacaService;
    private final InstrumentService instrumentService;
    private final IbkrService ibkrService;
    private final OptionService optionService;

    public MarketDataService(InstrumentService instrumentService, AlpacaService alpacaService, IbkrService ibkrService, OptionService optionService) {
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
        this.ibkrService = ibkrService;
        this.optionService = optionService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void refreshAlpacaMarketData() throws JsonProcessingException {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String tickerCsv = instrumentService.buildTickerCsv(instruments);
        AlpacaMarketDataDto alpacaMarketDataDto = alpacaService.getMarketDataSnapshot(tickerCsv);
        instrumentService.saveAlpacaMarketData(alpacaMarketDataDto);

        List<OptionEntity>  openOptions = optionService.getAllOpenOptions(null);
        String symbols = openOptions.stream().map(dto -> dto.getCode().replaceAll("\\s","")).collect(Collectors.joining(","));
        AlpacaQuotesDto alpacaQuotesDto = alpacaService.getOptionsLatestQuotes(symbols);
        for (OptionEntity option : openOptions) {
            String key = option.getCode().replaceAll("\\s", "");
            AlpacaQuoteDto alpacaQuoteDto = alpacaQuotesDto.getQuotes().get(key);
            if (alpacaQuoteDto != null) {
                option.setMarketPrice(alpacaQuoteDto.getMidPrice());
                optionService.saveOption(option);
            }
        }
    }

    public void refreshIbkrMarketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String conidCSV = instrumentService.buildConidCsv(instruments);
        List<IbkrMarketDataDto> ibkrMarketDataDtos = ibkrService.getMarketDataSnapshots(conidCSV);
        instrumentService.saveIbkrMarketData(ibkrMarketDataDtos);
    }
}