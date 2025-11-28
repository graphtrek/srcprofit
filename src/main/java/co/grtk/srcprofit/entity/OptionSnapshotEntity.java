package co.grtk.srcprofit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA entity for option snapshot data from Alpaca Data API.
 *
 * Stores the latest trading data (prices, quotes, Greeks) for option contracts.
 * Data is updated via upsert pattern - finds by symbol, updates if exists.
 *
 * OCC Symbol Format: AAPL230120C00150000
 * - Root: AAPL (1-6 chars)
 * - Expiration: 230120 (YYMMDD)
 * - Type: C (call) or P (put)
 * - Strike: 00150000 (strike * 1000, padded to 8 digits)
 *
 * @see AlpacaOptionSnapshotDto Source DTO
 */
@Entity
@Table(name = "OPTION_SNAPSHOT", indexes = {
        @Index(name = "opt_snap_symbol_idx", columnList = "symbol", unique = true),
        @Index(name = "opt_snap_instrument_idx", columnList = "instrument_id"),
        @Index(name = "opt_snap_expiration_idx", columnList = "expiration_date"),
        @Index(name = "opt_snap_type_idx", columnList = "option_type")
})
public class OptionSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // ============ Contract Identification ============

    /**
     * OCC contract symbol, e.g., "AAPL230120C00150000"
     * Unique identifier for this option contract.
     * Used as natural key (unique constraint).
     */
    @Column(unique = true, nullable = false, length = 50)
    public String symbol;

    /**
     * Relationship to underlying instrument.
     * Eager loading not necessary for snapshots (separate queries preferred).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    public InstrumentEntity instrument;

    // ============ Parsed from OCC Symbol ============

    /**
     * Option type: "call" or "put"
     * Extracted from OCC symbol (6th character: C or P)
     */
    @Column(nullable = false, length = 10)
    public String optionType;

    /**
     * Strike price, e.g., 150.00
     * Extracted from OCC symbol and divided by 1000.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal strikePrice;

    /**
     * Expiration date, e.g., 2023-01-20
     * Extracted from OCC symbol (YYMMDD format).
     */
    @Column(nullable = false)
    public LocalDate expirationDate;

    // ============ Latest Trade ============

    /**
     * Timestamp of the last trade for this option.
     */
    public OffsetDateTime lastTradeTime;

    /**
     * Exchange code where the last trade occurred.
     */
    @Column(length = 10)
    public String lastTradeExchange;

    /**
     * Price of the last trade.
     * Stored with 4 decimal places for precision.
     */
    @Column(precision = 10, scale = 4)
    public BigDecimal lastTradePrice;

    /**
     * Size (quantity) of the last trade.
     */
    public Integer lastTradeSize;

    // ============ Latest Quote (Bid/Ask) ============

    /**
     * Timestamp of the last quote update.
     */
    public OffsetDateTime lastQuoteTime;

    /**
     * Ask exchange code.
     */
    @Column(length = 10)
    public String askExchange;

    /**
     * Current ask (offer) price.
     */
    @Column(precision = 10, scale = 4)
    public BigDecimal askPrice;

    /**
     * Current ask size (quantity available at ask price).
     */
    public Integer askSize;

    /**
     * Bid exchange code.
     */
    @Column(length = 10)
    public String bidExchange;

    /**
     * Current bid price.
     */
    @Column(precision = 10, scale = 4)
    public BigDecimal bidPrice;

    /**
     * Current bid size (quantity available at bid price).
     */
    public Integer bidSize;

    // ============ Greeks (Option Risk Sensitivities) ============

    /**
     * Delta: rate of change of option price vs underlying stock price.
     * Range: -1 to 1 for puts, 0 to 1 for calls.
     */
    @Column(precision = 8, scale = 6)
    public BigDecimal delta;

    /**
     * Gamma: rate of change of delta.
     * Always positive. Measures curvature of option price.
     */
    @Column(precision = 8, scale = 6)
    public BigDecimal gamma;

    /**
     * Theta: rate of change of option price vs time.
     * Usually negative (time decay hurts option holders).
     */
    @Column(precision = 8, scale = 6)
    public BigDecimal theta;

    /**
     * Vega: rate of change of option price vs volatility.
     * Positive for both puts and calls.
     */
    @Column(precision = 8, scale = 6)
    public BigDecimal vega;

    /**
     * Rho: rate of change of option price vs interest rates.
     * Positive for calls, negative for puts.
     */
    @Column(precision = 8, scale = 6)
    public BigDecimal rho;

    /**
     * Implied volatility: annualized volatility implied by the option price.
     * Range: typically 0 to 1 (0% to 100%).
     */
    @Column(precision = 6, scale = 4)
    public BigDecimal impliedVolatility;

    // ============ Timestamps ============

    /**
     * When this record was first created.
     * Auto-set by Hibernate.
     */
    @CreationTimestamp
    @Column(nullable = false)
    public Instant createdAt;

    /**
     * When this record was last updated.
     * Auto-updated by Hibernate.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    public Instant updatedAt;

    /**
     * When the snapshot data was last updated from Alpaca API.
     * Useful for tracking data freshness.
     */
    public Instant snapshotUpdatedAt;

    // ============ Getters and Setters ============

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public InstrumentEntity getInstrument() {
        return instrument;
    }

    public void setInstrument(InstrumentEntity instrument) {
        this.instrument = instrument;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    public BigDecimal getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(BigDecimal strikePrice) {
        this.strikePrice = strikePrice;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public OffsetDateTime getLastTradeTime() {
        return lastTradeTime;
    }

    public void setLastTradeTime(OffsetDateTime lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    public String getLastTradeExchange() {
        return lastTradeExchange;
    }

    public void setLastTradeExchange(String lastTradeExchange) {
        this.lastTradeExchange = lastTradeExchange;
    }

    public BigDecimal getLastTradePrice() {
        return lastTradePrice;
    }

    public void setLastTradePrice(BigDecimal lastTradePrice) {
        this.lastTradePrice = lastTradePrice;
    }

    public Integer getLastTradeSize() {
        return lastTradeSize;
    }

    public void setLastTradeSize(Integer lastTradeSize) {
        this.lastTradeSize = lastTradeSize;
    }

    public OffsetDateTime getLastQuoteTime() {
        return lastQuoteTime;
    }

    public void setLastQuoteTime(OffsetDateTime lastQuoteTime) {
        this.lastQuoteTime = lastQuoteTime;
    }

    public String getAskExchange() {
        return askExchange;
    }

    public void setAskExchange(String askExchange) {
        this.askExchange = askExchange;
    }

    public BigDecimal getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(BigDecimal askPrice) {
        this.askPrice = askPrice;
    }

    public Integer getAskSize() {
        return askSize;
    }

    public void setAskSize(Integer askSize) {
        this.askSize = askSize;
    }

    public String getBidExchange() {
        return bidExchange;
    }

    public void setBidExchange(String bidExchange) {
        this.bidExchange = bidExchange;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Integer getBidSize() {
        return bidSize;
    }

    public void setBidSize(Integer bidSize) {
        this.bidSize = bidSize;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }

    public BigDecimal getGamma() {
        return gamma;
    }

    public void setGamma(BigDecimal gamma) {
        this.gamma = gamma;
    }

    public BigDecimal getTheta() {
        return theta;
    }

    public void setTheta(BigDecimal theta) {
        this.theta = theta;
    }

    public BigDecimal getVega() {
        return vega;
    }

    public void setVega(BigDecimal vega) {
        this.vega = vega;
    }

    public BigDecimal getRho() {
        return rho;
    }

    public void setRho(BigDecimal rho) {
        this.rho = rho;
    }

    public BigDecimal getImpliedVolatility() {
        return impliedVolatility;
    }

    public void setImpliedVolatility(BigDecimal impliedVolatility) {
        this.impliedVolatility = impliedVolatility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSnapshotUpdatedAt() {
        return snapshotUpdatedAt;
    }

    public void setSnapshotUpdatedAt(Instant snapshotUpdatedAt) {
        this.snapshotUpdatedAt = snapshotUpdatedAt;
    }

    // ============ Helper Methods ============

    /**
     * Calculate mid price from bid and ask prices.
     *
     * @return Mid price = (bid + ask) / 2, or null if bid/ask not available
     */
    public BigDecimal getMidPrice() {
        if (bidPrice != null && askPrice != null) {
            return bidPrice.add(askPrice).divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    /**
     * Calculate bid-ask spread in basis points.
     *
     * @return Spread in basis points, or null if bid/ask not available
     */
    public BigDecimal getSpreadBps() {
        BigDecimal mid = getMidPrice();
        if (mid != null && mid.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spread = askPrice.subtract(bidPrice);
            return spread.divide(mid, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(10000));
        }
        return null;
    }
}
