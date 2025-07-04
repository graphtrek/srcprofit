package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public class AlpacaTradeDto {
    @JsonProperty("t")
    private OffsetDateTime timestamp;

    @JsonProperty("x")
    private String exchange;

    @JsonProperty("p")
    private double price;

    @JsonProperty("s")
    private int size;

    @JsonProperty("c")
    private List<String> conditions;

    @JsonProperty("i")
    private long tradeId;

    @JsonProperty("z")
    private String tape;

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public long getTradeId() {
        return tradeId;
    }

    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    public String getTape() {
        return tape;
    }

    public void setTape(String tape) {
        this.tape = tape;
    }

    @Override
    public String toString() {
        return "AlpacaTradeDto{" +
                "timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                ", price=" + price +
                ", size=" + size +
                ", conditions=" + conditions +
                ", tradeId=" + tradeId +
                ", tape='" + tape + '\'' +
                '}';
    }
}