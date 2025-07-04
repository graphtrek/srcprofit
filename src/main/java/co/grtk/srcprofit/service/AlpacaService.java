package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.AlpacaSingleAssetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AlpacaService {
    private static final Logger log = LoggerFactory.getLogger(AlpacaService.class);

    private final RestClient alpacaRestClient;

    public AlpacaService(RestClient alpacaRestClient) {
        this.alpacaRestClient = alpacaRestClient;
    }

    public AlpacaQuotesDto getLatestQuotes(String symbolsCsv) {
        AlpacaQuotesDto alpacaQuotesDto = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/quotes/latest")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<AlpacaQuotesDto>() {
                });
        log.info("getMarketData /v2/stocks/quotes/latest returned {}", alpacaQuotesDto);
        return alpacaQuotesDto;
    }

    public AlpacaMarketDataDto getMarketDataSnapshot(String symbolsCsv) {
        Map<String, AlpacaSingleAssetDto> quotes = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/snapshots")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, AlpacaSingleAssetDto>>() {
                });
        log.info("getMarketData /v2/stocks/snapshots returned {}", quotes);
        AlpacaMarketDataDto alpacaMarketDataDto = new AlpacaMarketDataDto();
        alpacaMarketDataDto.setQuotes(quotes);
        return alpacaMarketDataDto;
    }
}
