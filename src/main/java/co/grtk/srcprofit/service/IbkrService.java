package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.dto.IbkrMarketDataDto;
import co.grtk.srcprofit.dto.IbkrTradeExecutionDto;
import co.grtk.srcprofit.dto.IbkrWatchlistDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class IbkrService {
    private static final Logger log = LoggerFactory.getLogger(IbkrService.class);

    private final RestClient ibkrRestClient;
    private final RestClient ibkrFlexRestClient;
    private final Environment environment;

    public IbkrService(RestClient ibkrRestClient, RestClient ibkrFlexRestClient, Environment environment) {
        this.ibkrRestClient = ibkrRestClient;
        this.ibkrFlexRestClient = ibkrFlexRestClient;
        this.environment = environment;
    }

    public IbkrWatchlistDto getWatchlist() {
        IbkrWatchlistDto ibkrWatchlistDto = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/iserver/watchlist")
                        .queryParam("id", 100)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<IbkrWatchlistDto>() {
                });
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
                .body(new ParameterizedTypeReference<List<IbkrMarketDataDto>>() {
                });
        log.info("getMarketDataSnapshots /v1/api/iserver/marketdata/snapshot returned {}", ibkrMarketDataDtoList);
        return ibkrMarketDataDtoList;
    }

    public FlexStatementResponse getFlexStatement() {
        return ibkrFlexRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/SendRequest")
                        .queryParam("t", environment.getRequiredProperty("IBKR_FLEX_API_TOKEN"))
                        .queryParam("q", environment.getRequiredProperty("IBKR_FLEX_STATEMENT_ID"))
                        .queryParam("v", "3")
                        .build())
                .retrieve()
                .body(FlexStatementResponse.class);
    }

    public String getFlexQuery(String referenceCode) {
        return ibkrFlexRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/GetStatement")
                        .queryParam("t", environment.getRequiredProperty("IBKR_FLEX_API_TOKEN"))
                        .queryParam("q", referenceCode)
                        .queryParam("v", "3")
                        .build())
                .retrieve()
                .body(String.class);
    }

    public List<IbkrTradeExecutionDto> getLatestTrades() {
        List<IbkrTradeExecutionDto> ibkrTradeExecutionDtoList = ibkrRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/iserver/account/trades")
                        .queryParam("days", 7)
                        .queryParam("accountId", environment.getRequiredProperty("IBKR_ACCOUNT_ID"))
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IbkrTradeExecutionDto>>() {
                });
        log.info("getLatestTrades /v1/api/iserver/account/trades returned {}", ibkrTradeExecutionDtoList);
        return ibkrTradeExecutionDtoList;
    }

}