package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class IbkrService {
    private static final Logger log = LoggerFactory.getLogger(IbkrService.class);

    private final RestClient ibkrRestClient;

    public IbkrService(RestClient ibkrRestClient) {
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

    public List<IbkrMarketDataDto> getMarketDataSnapshots(String conidsCsv) {
        List<IbkrMarketDataDto> ibkrMarketDataDtoList = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/iserver/marketdata/snapshot")
                        .queryParam("conids", conidsCsv)
                        .queryParam("fields", "31,55,82,83,7051")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IbkrMarketDataDto>>() {});
        log.info("getMarketDataSnapshots /v1/api/iserver/marketdata/snapshot returned {}", ibkrMarketDataDtoList);
        return ibkrMarketDataDtoList;
    }

}