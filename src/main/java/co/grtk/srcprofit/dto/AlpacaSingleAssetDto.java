package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public class AlpacaSingleAssetDto {

    @JsonProperty("latestTrade")
    private AlpacaTradeDto latestTrade;

    @JsonProperty("latestQuote")
    private AlpacaQuoteDto latestQuote;

    @JsonProperty("minuteBar")
    private AlpacaBarDto minuteBar;

    @JsonProperty("dailyBar")
    private AlpacaBarDto dailyBar;

    @JsonProperty("prevDailyBar")
    private AlpacaBarDto prevDailyBar;

    public AlpacaTradeDto getLatestTrade() {
        return latestTrade;
    }

    public void setLatestTrade(AlpacaTradeDto latestTrade) {
        this.latestTrade = latestTrade;
    }

    public AlpacaQuoteDto getLatestQuote() {
        return latestQuote;
    }

    public void setLatestQuote(AlpacaQuoteDto latestQuote) {
        this.latestQuote = latestQuote;
    }

    public AlpacaBarDto getMinuteBar() {
        return minuteBar;
    }

    public void setMinuteBar(AlpacaBarDto minuteBar) {
        this.minuteBar = minuteBar;
    }

    public AlpacaBarDto getDailyBar() {
        return dailyBar;
    }

    public void setDailyBar(AlpacaBarDto dailyBar) {
        this.dailyBar = dailyBar;
    }

    public AlpacaBarDto getPrevDailyBar() {
        return prevDailyBar;
    }

    public void setPrevDailyBar(AlpacaBarDto prevDailyBar) {
        this.prevDailyBar = prevDailyBar;
    }

    @Override
    public String toString() {
        return "AlpacaSingleAssetDto{" +
                "latestTrade=" + latestTrade +
                ", latestQuote=" + latestQuote +
                ", minuteBar=" + minuteBar +
                ", dailyBar=" + dailyBar +
                ", prevDailyBar=" + prevDailyBar +
                '}';
    }

}
