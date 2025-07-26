package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaMarketDataDto;
import co.grtk.srcprofit.dto.AlpacaQuotesDto;
import co.grtk.srcprofit.dto.AlpacaSingleAssetDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper mapper;

    public AlpacaService(RestClient alpacaRestClient, ObjectMapper mapper) {
        this.alpacaRestClient = alpacaRestClient;
        this.mapper = mapper;
    }

    public AlpacaQuotesDto getStocksLatestQuotes(String symbolsCsv) {
        AlpacaQuotesDto alpacaQuotesDto = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/quotes/latest")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<AlpacaQuotesDto>() {
                });
        int count = (alpacaQuotesDto != null && alpacaQuotesDto.getQuotes() != null) ? alpacaQuotesDto.getQuotes().size() : 0;
        log.info("getStocksLatestQuotes /v2/stocks/quotes/latest returned {}", count);
        return alpacaQuotesDto;
    }

    public AlpacaQuotesDto getOptionsLatestQuotes(String symbolsCsv) throws JsonProcessingException {
        String json = alpacaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta1/options/quotes/latest")
                        .queryParam("feed", "indicative")
                        .queryParam("symbols", symbolsCsv)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);


        AlpacaQuotesDto alpacaQuotesDto = mapper.readValue(json, AlpacaQuotesDto.class);

        //int count = (alpacaQuotesDto != null && alpacaQuotesDto.getQuotes() != null) ? alpacaQuotesDto.getQuotes().size() : 0;
        log.info("getOptionsLatestQuotes /v1beta1/options/quotes/latest returned {}", alpacaQuotesDto);
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
        int count = (quotes != null) ? quotes.size() : 0;
        log.info("getMarketData /v2/stocks/snapshots returned {}", count);
        AlpacaMarketDataDto alpacaMarketDataDto = new AlpacaMarketDataDto();
        alpacaMarketDataDto.setQuotes(quotes);
        return alpacaMarketDataDto;
    }


}