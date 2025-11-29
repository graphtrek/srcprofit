package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Alpaca option snapshot response.
 *
 * Maps to a single option contract snapshot from the
 * /v1beta1/options/snapshots/{symbol} endpoint.
 */
public class AlpacaOptionSnapshotDto {
    public String symbol;  // OCC symbol, e.g., "AAPL230120C00150000"
    public LatestTradeDto latestTrade;
    public LatestQuoteDto latestQuote;
    public GreeksDto greeks;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setLatestTrade(LatestTradeDto latestTrade) {
        this.latestTrade = latestTrade;
    }

    public void setLatestQuote(LatestQuoteDto latestQuote) {
        this.latestQuote = latestQuote;
    }

    public void setGreeks(GreeksDto greeks) {
        this.greeks = greeks;
    }

    public String getSymbol() {
        return symbol;
    }

    public LatestTradeDto getLatestTrade() {
        return latestTrade;
    }

    public LatestQuoteDto getLatestQuote() {
        return latestQuote;
    }

    public GreeksDto getGreeks() {
        return greeks;
    }

    /**
     * Latest trade for this option contract.
     */
    public static class LatestTradeDto {
        @JsonProperty("t")
        public String timestamp;  // ISO 8601 timestamp
        @JsonProperty("x")
        public String exchange;   // Exchange code
        @JsonProperty("p")
        public String price;      // Trade price as string
        @JsonProperty("s")
        public Integer size;      // Trade size
        @JsonProperty("c")
        public String conditions;  // Trade conditions (e.g., "A")

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }
    }

    /**
     * Latest quote (bid/ask) for this option contract.
     */
    public static class LatestQuoteDto {
        @JsonProperty("t")
        public String timestamp;   // ISO 8601 timestamp
        @JsonProperty("ax")
        public String askExchange; // Ask exchange code
        @JsonProperty("ap")
        public String askPrice;    // Ask price as string
        @JsonProperty("as")
        public Integer askSize;    // Ask size
        @JsonProperty("bx")
        public String bidExchange; // Bid exchange code
        @JsonProperty("bp")
        public String bidPrice;    // Bid price as string
        @JsonProperty("bs")
        public Integer bidSize;    // Bid size
        @JsonProperty("c")
        public String conditions;  // Quote conditions (e.g., "A")

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setAskExchange(String askExchange) {
            this.askExchange = askExchange;
        }

        public void setAskPrice(String askPrice) {
            this.askPrice = askPrice;
        }

        public void setAskSize(Integer askSize) {
            this.askSize = askSize;
        }

        public void setBidExchange(String bidExchange) {
            this.bidExchange = bidExchange;
        }

        public void setBidPrice(String bidPrice) {
            this.bidPrice = bidPrice;
        }

        public void setBidSize(Integer bidSize) {
            this.bidSize = bidSize;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }
    }

    /**
     * Option Greeks (risk sensitivities) for this contract.
     * All values are doubles from Alpaca API.
     */
    public static class GreeksDto {
        public Double delta;     // Delta sensitivity to underlying price
        public Double gamma;     // Gamma sensitivity of delta
        public Double theta;     // Theta time decay
        public Double vega;      // Vega sensitivity to volatility
        public Double rho;       // Rho sensitivity to interest rates
        @JsonProperty("iv")
        public Double impliedVolatility;  // Implied volatility from API (mapped from "iv")

        public void setDelta(Double delta) {
            this.delta = delta;
        }

        public void setGamma(Double gamma) {
            this.gamma = gamma;
        }

        public void setTheta(Double theta) {
            this.theta = theta;
        }

        public void setVega(Double vega) {
            this.vega = vega;
        }

        public void setRho(Double rho) {
            this.rho = rho;
        }

        public void setImpliedVolatility(Double impliedVolatility) {
            this.impliedVolatility = impliedVolatility;
        }
    }
}
