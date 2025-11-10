package co.grtk.srcprofit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO for Alpaca Assets API response.
 * Maps to GET /v2/assets/{symbol_or_asset_id}
 *
 * Reference: https://docs.alpaca.markets/api-references/assets-api/
 */
public class AlpacaAssetDto {

    /**
     * Unique identifier for the asset (UUID)
     */
    @JsonProperty("id")
    private String id;

    /**
     * Asset class (e.g., "us_equity", "crypto")
     */
    @JsonProperty("class")
    private String assetClass;

    /**
     * Exchange where the asset is traded
     */
    @JsonProperty("exchange")
    private String exchange;

    /**
     * Symbol (e.g., "AAPL", "BTCUSD")
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Full name of the asset
     */
    @JsonProperty("name")
    private String name;

    /**
     * Asset status (e.g., "active", "inactive")
     */
    @JsonProperty("status")
    private String status;

    /**
     * Whether the asset is tradable
     */
    @JsonProperty("tradable")
    private Boolean tradable;

    /**
     * Whether the asset is marginable
     */
    @JsonProperty("marginable")
    private Boolean marginable;

    /**
     * Whether the asset can be shorted
     */
    @JsonProperty("shortable")
    private Boolean shortable;

    /**
     * Whether the asset is easy to borrow (for short selling)
     */
    @JsonProperty("easy_to_borrow")
    private Boolean easyToBorrow;

    /**
     * Whether the asset supports fractional trading
     */
    @JsonProperty("fractionable")
    private Boolean fractionable;

    /**
     * Maintenance margin requirement (as a decimal, e.g., 0.2 = 20%)
     */
    @JsonProperty("maintenance_margin_requirement")
    private BigDecimal maintenanceMarginRequirement;

    // Constructors
    public AlpacaAssetDto() {
    }

    public AlpacaAssetDto(String id, String assetClass, String exchange, String symbol, String name,
                          String status, Boolean tradable, Boolean marginable, Boolean shortable,
                          Boolean easyToBorrow, Boolean fractionable, BigDecimal maintenanceMarginRequirement) {
        this.id = id;
        this.assetClass = assetClass;
        this.exchange = exchange;
        this.symbol = symbol;
        this.name = name;
        this.status = status;
        this.tradable = tradable;
        this.marginable = marginable;
        this.shortable = shortable;
        this.easyToBorrow = easyToBorrow;
        this.fractionable = fractionable;
        this.maintenanceMarginRequirement = maintenanceMarginRequirement;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getTradable() {
        return tradable;
    }

    public void setTradable(Boolean tradable) {
        this.tradable = tradable;
    }

    public Boolean getMarginable() {
        return marginable;
    }

    public void setMarginable(Boolean marginable) {
        this.marginable = marginable;
    }

    public Boolean getShortable() {
        return shortable;
    }

    public void setShortable(Boolean shortable) {
        this.shortable = shortable;
    }

    public Boolean getEasyToBorrow() {
        return easyToBorrow;
    }

    public void setEasyToBorrow(Boolean easyToBorrow) {
        this.easyToBorrow = easyToBorrow;
    }

    public Boolean getFractionable() {
        return fractionable;
    }

    public void setFractionable(Boolean fractionable) {
        this.fractionable = fractionable;
    }

    public BigDecimal getMaintenanceMarginRequirement() {
        return maintenanceMarginRequirement;
    }

    public void setMaintenanceMarginRequirement(BigDecimal maintenanceMarginRequirement) {
        this.maintenanceMarginRequirement = maintenanceMarginRequirement;
    }

    @Override
    public String toString() {
        return "AlpacaAssetDto{" +
                "id='" + id + '\'' +
                ", assetClass='" + assetClass + '\'' +
                ", exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", tradable=" + tradable +
                ", marginable=" + marginable +
                ", shortable=" + shortable +
                ", easyToBorrow=" + easyToBorrow +
                ", fractionable=" + fractionable +
                ", maintenanceMarginRequirement=" + maintenanceMarginRequirement +
                '}';
    }
}
