package co.grtk.srcprofit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AlphaVintageService {

    private static final Logger log = LoggerFactory.getLogger(AlphaVintageService.class);
    private final RestClient alphaVintageRestClient;
    private final Environment environment;

    public AlphaVintageService(RestClient alphaVintageRestClient, Environment environment) {
        this.alphaVintageRestClient = alphaVintageRestClient;
        this.environment = environment;
    }

    public String getEarningsCalendar() {
        return alphaVintageRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("query")
                        .queryParam("apikey", environment.getRequiredProperty("ALPHA_VINTAGE_API_KEY"))
                        .queryParam("function", "EARNINGS_CALENDAR")
                        .queryParam("horizon", "12month")
                        .build())
                .retrieve()
                .body(String.class);
    }

}
