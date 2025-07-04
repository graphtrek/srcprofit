package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AlpacaMarketDataDto {
    @JsonProperty("")
    private Map<String, AlpacaSingleAssetDto> quotes;

    public Map<String, AlpacaSingleAssetDto> getQuotes() {
        return quotes;
    }

    public void setQuotes(Map<String, AlpacaSingleAssetDto> quotes) {
        this.quotes = quotes;
    }

    @Override
    public String toString() {
        return "AlpacaMarketDataDto{" +
                "quotes=" + quotes +
                '}';
    }
}