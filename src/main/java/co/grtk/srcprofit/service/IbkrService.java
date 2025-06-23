package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import co.grtk.srcprofit.dto.InstrumentDto;
import co.grtk.srcprofit.dto.MarketDataDto;
import co.grtk.srcprofit.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IbkrService {
    private static final Logger log = LoggerFactory.getLogger(IbkrService.class);

    private final RestClient ibkrRestClient;

    public IbkrService(RestClient ibkrRestClient,
                       ObjectMapper objectMapper,
                       InstrumentRepository instrumentRepository) {
        this.ibkrRestClient = ibkrRestClient;
    }

    public IbkrWatchlistDto getIbkrWatchlist() {
        IbkrWatchlistDto ibkrWatchlistDto = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/iserver/watchlist")
                        .queryParam("id", 100)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<IbkrWatchlistDto>() {});
        log.info("getIbkrWatchlist /v1/api/iserver/watchlist returned {}",
                (ibkrWatchlistDto != null) ? ibkrWatchlistDto.getName() : null);
        return ibkrWatchlistDto;
    }

    public List<MarketDataDto> getMarketDataSnapshots(String conidsCsv) {
        List<MarketDataDto> marketDataDtoList = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/iserver/marketdata/snapshot")
                        .queryParam("conids", conidsCsv)
                        .queryParam("fields", "31,55,82,83,7051")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MarketDataDto>>() {});
        log.info("getMarketDataSnapshots /v1/api/iserver/marketdata/snapshot returned {}", marketDataDtoList);
        return marketDataDtoList;
    }

    public String buildConidList(List<InstrumentDto> instruments) {
        return instruments.stream()
                .map(dto -> String.valueOf(dto.getConid()))
                .collect(Collectors.joining(","));
    }

}