package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

import static co.grtk.srcprofit.mapper.PositionMapper.round2Digits;

public class AlpacaQuoteDto {
    @JsonProperty("ap")
    private double askPrice;

    @JsonProperty("as")
    private int askSize;

    @JsonProperty("ax")
    private String askExchange;

    @JsonProperty("bp")
    private double bidPrice;

    @JsonProperty("bs")
    private int bidSize;

    @JsonProperty("bx")
    private String bidExchange;

    @JsonProperty("c")
    private List<String> conditions;

    @JsonProperty("t")
    private OffsetDateTime timestamp;

    @JsonProperty("z")
    private String tape;

    public double getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(double askPrice) {
        this.askPrice = askPrice;
    }

    public int getAskSize() {
        return askSize;
    }

    public void setAskSize(int askSize) {
        this.askSize = askSize;
    }

    public String getAskExchange() {
        return askExchange;
    }

    public void setAskExchange(String askExchange) {
        this.askExchange = askExchange;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public int getBidSize() {
        return bidSize;
    }

    public void setBidSize(int bidSize) {
        this.bidSize = bidSize;
    }

    public String getBidExchange() {
        return bidExchange;
    }

    public void setBidExchange(String bidExchange) {
        this.bidExchange = bidExchange;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTape() {
        return tape;
    }

    public void setTape(String tape) {
        this.tape = tape;
    }

    public double getMidPrice() {
        return round2Digits(((bidPrice * bidSize) + (askPrice * askSize)) / (askSize + bidSize));
    }

    @Override
    public String toString() {
        return "AlpacaQuoteDto{" +
                "askPrice=" + askPrice +
                ", askSize=" + askSize +
                ", askExchange='" + askExchange + '\'' +
                ", bidPrice=" + bidPrice +
                ", bidSize=" + bidSize +
                ", bidExchange='" + bidExchange + '\'' +
                ", conditions=" + conditions +
                ", timestamp=" + timestamp +
                ", tape='" + tape + '\'' +
                '}';
    }
}