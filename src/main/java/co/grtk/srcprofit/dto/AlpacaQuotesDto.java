package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AlpacaQuotesDto {
    @JsonProperty("quotes")
    private Map<String, AlpacaQuoteDto> quotes;

    public Map<String, AlpacaQuoteDto> getQuotes() {
        return quotes;
    }

    public void setQuotes(Map<String, AlpacaQuoteDto> quotes) {
        this.quotes = quotes;
    }

    @Override
    public String toString() {
        return "AlpacaQuotesDto{" +
                "quotes=" + quotes +
                '}';
    }
}