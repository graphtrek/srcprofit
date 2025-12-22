package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuoteDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.entity.OptionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for refreshing market data from external APIs (Alpaca, IBKR).
 *
 * This service provides orchestration for market data updates. Scheduling is
 * handled by ScheduledJobsService via @Scheduled annotations.
 *
 * Methods:
 * - refreshAlpacaMarketData(): Updates stock quotes and option prices from Alpaca
 * - refreshIbkrMarketData(): Updates market data from Interactive Brokers (IBKR)
 *
 * @see ScheduledJobsService for @Scheduled annotations
 */
@Service
public class MarketDataService {
    public final AlpacaService alpacaService;
    private final InstrumentService instrumentService;
    private final IbkrService ibkrService;
    private final OptionService optionService;
    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    public MarketDataService(InstrumentService instrumentService, AlpacaService alpacaService, IbkrService ibkrService, OptionService optionService) {
        this.instrumentService = instrumentService;
        this.alpacaService = alpacaService;
        this.ibkrService = ibkrService;
        this.optionService = optionService;
    }

    /**
     * Refreshes market data from Alpaca API.
     *
     * Updates:
     * - Stock quotes for all instruments
     * - Option market prices for all open options
     *
     * Called by ScheduledJobsService every 60 seconds.
     *
     * @throws JsonProcessingException if JSON parsing fails
     */
    public void refreshAlpacaMarketData() throws JsonProcessingException {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String tickerCsv = instrumentService.buildTickerCsv(instruments);
        AlpacaMarketDataDto alpacaMarketDataDto = alpacaService.getMarketDataSnapshot(tickerCsv);
        instrumentService.saveAlpacaMarketData(alpacaMarketDataDto);

        List<OptionEntity>  openOptions = optionService.getAllOpenOptions(null);
        String optionSymbols = openOptions.stream().map(dto -> dto.getCode().replaceAll("\\s","")).collect(Collectors.joining(","));
        if(!optionSymbols.isEmpty()) {
            AlpacaQuotesDto alpacaQuotesDto = alpacaService.getOptionsLatestQuotes(optionSymbols);
            for (OptionEntity option : openOptions) {
                String key = option.getCode().replaceAll("\\s", "");
                AlpacaQuoteDto alpacaQuoteDto = alpacaQuotesDto.getQuotes().get(key);
                if (alpacaQuoteDto != null) {
                    option.setMarketPrice(alpacaQuoteDto.getMidPrice());
                    optionService.saveOption(option);
                }
            }
        } else {
            log.info("No open options found");
        }

    }

    public void refreshIbkrMarketData() {
        List<InstrumentDto> instruments = instrumentService.loadAllInstruments();
        String conidCSV = instrumentService.buildConidCsv(instruments);
        List<IbkrMarketDataDto> ibkrMarketDataDtos = ibkrService.getMarketDataSnapshots(conidCSV);
        instrumentService.saveIbkrMarketData(ibkrMarketDataDtos);
    }
}