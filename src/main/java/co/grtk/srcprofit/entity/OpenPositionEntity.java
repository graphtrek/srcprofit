package co.grtk.srcprofit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a snapshot of an open position from IBKR Flex Report.
 *
 * Open positions are snapshots of the portfolio at a point in time (reportDate).
 * Each position is uniquely identified by conid (contract ID).
 *
 * Upsert behavior: On re-import, existing positions are updated (not inserted again).
 * Natural key: conid (unique constraint enforced)
 *
 * Supports all asset classes: OPT (options), STK (stocks), CASH, BOND, FOP, etc.
 * Options-specific fields (strike, expiry, putCall) are nullable for non-OPT assets.
 *
 * CSV Source: IBKR Flex Report - Open Positions
 * All fields are optional in the CSV (depending on what columns you select in the Flex Query).
 * This entity includes ALL available columns from the IBKR Open Positions Flex Report.
 *
 * @see OpenPositionRepository for query methods
 * @see OpenPositionService for CSV parsing and persistence
 */
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(name = "OPEN_POSITION",
    indexes = {
        @Index(name = "op_conid_idx", columnList = "conid", unique = true),
        @Index(name = "op_symbol_idx", columnList = "symbol"),
        @Index(name = "op_asset_class_idx", columnList = "assetClass"),
        @Index(name = "op_report_date_idx", columnList = "reportDate"),
        @Index(name = "op_account_idx", columnList = "account")
    })
public class OpenPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CORE IDENTIFICATION ---

    /**
     * IBKR Contract ID - Natural key for upsert.
     * Unique constraint ensures one record per contract across all snapshots.
     * Populated from CSV column: CONID
     */
    @Column(nullable = false, unique = true)
    private Long conid;

    /**
     * Account identifier from IBKR.
     * Populated from CSV column: Account ID
     */
    @Column(nullable = false, length = 50)
    private String account;

    /**
     * Account alias (friendly name) if configured.
     * Populated from CSV column: Account Alias
     */
    @Column(length = 100)
    private String accountAlias;

    /**
     * Snapshot date (when IBKR generated the report).
     * Populated from CSV column: Report Date (format: YYYY-MM-DD after parsing)
     */
    @Column(nullable = false)
    private LocalDate reportDate;

    // --- POSITION CLASSIFICATION ---

    /**
     * Asset class: OPT (option), STK (stock), CASH, BOND, FOP (future), etc.
     * Populated from CSV column: Asset Class
     */
    @Column(nullable = false, length = 20)
    private String assetClass;

    /**
     * Additional asset subcategory classification.
     * Populated from CSV column: Sub Category
     */
    @Column(length = 50)
    private String subCategory;

    /**
     * Currency code (USD, EUR, GBP, etc.)
     * Populated from CSV column: Currency
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Position direction: "Long" or "Short"
     * Populated from CSV column: Side
     */
    @Column(length = 10)
    private String side;

    /**
     * Position level of detail: Summary or detailed lot-level reporting.
     * Populated from CSV column: Level of Detail
     */
    @Column(length = 50)
    private String levelOfDetail;

    // --- BASIC POSITION INFO ---

    /**
     * Symbol or ticker (SPY, AAPL, etc.)
     * Populated from CSV column: Symbol
     */
    @Column(length = 50)
    private String symbol;

    /**
     * Full description of the position (contract specification).
     * Populated from CSV column: Description
     */
    @Column(length = 500)
    private String description;

    /**
     * Contract multiplier (100 for equity options, 1 for stocks).
     * Populated from CSV column: Multiplier
     */
    @Column
    private Double multiplier;

    /**
     * Current quantity held.
     * Positive for long positions, negative for short positions.
     * Populated from CSV column: Quantity
     */
    @Column(nullable = false)
    private Integer quantity;

    // --- PRICING AND VALUE ---

    /**
     * Average acquisition cost per share/contract.
     * Populated from CSV column: Cost Basis Price
     */
    @Column
    private Double costBasisPrice;

    /**
     * Total acquisition cost = quantity * costBasisPrice.
     * Populated from CSV column: Cost Basis Money
     */
    @Column
    private Double costBasisMoney;

    /**
     * Current market price (closing mark as of report date).
     * Populated from CSV column: Mark Price
     */
    @Column
    private Double markPrice;

    /**
     * Position value = quantity * markPrice (in account currency).
     * Populated from CSV column: Position Value
     */
    @Column
    private Double positionValue;

    /**
     * Position value converted to base currency.
     * Populated from CSV column: Position Value in Base
     */
    @Column
    private Double positionValueInBase;

    /**
     * Currency conversion rate from asset currency to base currency.
     * Populated from CSV column: FX Rate to Base
     */
    @Column
    private Double fxRateToBase;

    /**
     * The average price of the position (open price).
     * Populated from CSV column: Open Price
     */
    @Column
    private Double openPrice;

    // --- PROFIT AND LOSS ---

    /**
     * Unrealized P&L based on FIFO cost basis.
     * = positionValue - costBasisMoney
     * Populated from CSV column: FIFO Unrealized PnL
     */
    @Column
    private Double fifoPnlUnrealized;

    /**
     * Unrealized capital gains component of P/L.
     * Populated from CSV column: Unrealized Capital Gains PnL
     */
    @Column
    private Double unrealizedCapitalGainsPnl;

    /**
     * Unrealized foreign exchange P/L component.
     * Populated from CSV column: Unrealized FX PnL
     */
    @Column
    private Double unrealizedFxPnl;

    /**
     * Position value as percentage of total NAV (Net Asset Value).
     * Populated from CSV column: Percent of NAV
     */
    @Column
    private Double percentOfNAV;

    // --- SECURITIES IDENTIFICATION ---

    /**
     * Security identifier code (varies by type).
     * Populated from CSV column: Security ID
     */
    @Column(length = 50)
    private String securityId;

    /**
     * Category of the security identifier (CUSIP, ISIN, etc.)
     * Populated from CSV column: Security ID Type
     */
    @Column(length = 20)
    private String securityIdType;

    /**
     * CUSIP identifier (for US securities).
     * Populated from CSV column: CUSIP
     */
    @Column(length = 50)
    private String cusip;

    /**
     * ISIN identifier (international securities).
     * Populated from CSV column: ISIN
     */
    @Column(length = 50)
    private String isin;

    /**
     * FIGI (Financial Instrument Global Identifier).
     * Populated from CSV column: FIGI
     */
    @Column(length = 50)
    private String figi;

    /**
     * SEDOL identifier (London Stock Exchange).
     * Populated from CSV column: SEDOL
     */
    @Column(length = 50)
    private String sedol;

    // --- OPTIONS-SPECIFIC FIELDS (nullable for non-OPT assets) ---

    /**
     * Expiration date for option contracts.
     * NULL for non-option positions (stocks, cash, etc.)
     * Populated from CSV column: Expiry (format: YYYY-MM-DD after parsing)
     */
    @Column
    private LocalDate expirationDate;

    /**
     * Strike price for option contracts.
     * NULL for non-option positions.
     * Populated from CSV column: Strike
     */
    @Column
    private Double strike;

    /**
     * Put/Call indicator: "P" (put) or "C" (call)
     * NULL for non-option positions.
     * Populated from CSV column: Put/Call
     */
    @Column(length = 1)
    private String putCall;

    /**
     * Underlying contract ID for derivatives.
     * For options: the underlying stock's conid
     * Populated from CSV column: Underlying CONID
     */
    @Column
    private Long underlyingConid;

    /**
     * Underlying symbol for derivatives.
     * For SPY 100 CALL: underlying symbol is SPY
     * Populated from CSV column: Underlying Symbol
     */
    @Column(length = 50)
    private String underlyingSymbol;

    /**
     * Security ID of underlying instrument (for derivatives).
     * Populated from CSV column: Underlying Security ID
     */
    @Column(length = 50)
    private String underlyingSecurityId;

    /**
     * Exchange listing for the underlying instrument.
     * Populated from CSV column: Underlying Listing Exchange
     */
    @Column(length = 50)
    private String underlyingListingExchange;

    // --- CORPORATE ACTIONS & ADJUSTMENTS ---

    /**
     * Principal adjustment factor for certain instruments.
     * Populated from CSV column: Principal Adjust Factor
     */
    @Column
    private Double principalAdjustFactor;

    /**
     * Accrued interest (primarily for bonds).
     * Populated from CSV column: Accrued Interest
     */
    @Column
    private Double accruedInterest;

    /**
     * Code abbreviations (WASH for wash sales, etc.)
     * Populated from CSV column: Code
     */
    @Column(length = 50)
    private String code;

    /**
     * Date/time indicator for wash sale holding periods.
     * Populated from CSV column: Holding Period Date Time
     */
    @Column
    private LocalDateTime holdingPeriodDateTime;

    /**
     * The order ID of lots originating from trades at IBKR.
     * Populated from CSV column: Originating Order ID
     */
    @Column(length = 50)
    private String originatingOrderId;

    /**
     * The transaction ID of lots originating from trades at IBKR.
     * Populated from CSV column: Originating Transaction ID
     */
    @Column(length = 50)
    private String originatingTransactionId;

    // --- BOND & STRUCTURED PRODUCTS ---

    /**
     * The company that issued the contract (structured products).
     * Populated from CSV column: Issuer
     */
    @Column(length = 200)
    private String issuer;

    /**
     * Country code of contract issuer.
     * Populated from CSV column: Issuer Country Code
     */
    @Column(length = 2)
    private String issuerCountryCode;

    // --- COMMODITIES ---

    /**
     * Type of commodity (for commodity positions).
     * Populated from CSV column: Commodity Type
     */
    @Column(length = 50)
    private String commodityType;

    /**
     * Fineness of commodity (for precious metals).
     * Populated from CSV column: Fineness
     */
    @Column
    private Double fineness;

    /**
     * Weight of physical commodity delivery.
     * Populated from CSV column: Weight
     */
    @Column(length = 50)
    private String weight;

    /**
     * Physical delivery type specification.
     * Populated from CSV column: Delivery Type
     */
    @Column(length = 50)
    private String deliveryType;

    /**
     * Serial number of commodity.
     * Populated from CSV column: Serial Number
     */
    @Column(length = 100)
    private String serialNumber;

    // --- METADATA ---

    /**
     * Model designation when applicable (for advisors).
     * Populated from CSV column: Model
     */
    @Column(length = 100)
    private String model;

    /**
     * Date and time of the initial trade.
     * Populated from CSV column: Open Date Time
     */
    @Column
    private LocalDateTime openDateTime;

    // --- JPA RELATIONSHIPS ---

    /**
     * For OPTIONS: Reference to underlying instrument (e.g., SPY for SPY options).
     * Joins via underlyingConid → InstrumentEntity.conid
     * NULL for non-option positions (STK, CASH, etc.)
     *
     * Note: foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) because this is a read-only
     * mapping and not all positions have corresponding instruments in the database.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underlyingConid", referencedColumnName = "conid",
                insertable = false, updatable = false, nullable = true,
                foreignKey = @jakarta.persistence.ForeignKey(value = jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private InstrumentEntity underlyingInstrument;

    /**
     * For STOCKS: Reference to the instrument itself.
     * Joins via conid → InstrumentEntity.conid
     * NULL for options (use underlyingInstrument instead) and other asset classes.
     *
     * Note: foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) because this is a read-only
     * mapping and not all positions have corresponding instruments in the database.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conid", referencedColumnName = "conid",
                insertable = false, updatable = false, nullable = true,
                foreignKey = @jakarta.persistence.ForeignKey(value = jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private InstrumentEntity instrument;

    /**
     * Helper method to get the appropriate instrument based on asset class.
     *
     * @return underlyingInstrument for OPTIONS, instrument for STOCKS, null for others
     */
    public InstrumentEntity getRelatedInstrument() {
        if ("OPT".equals(assetClass)) {
            return underlyingInstrument;
        } else if ("STK".equals(assetClass)) {
            return instrument;
        }
        return null;  // CASH, BOND, FOP, etc. don't have instrument relationships
    }

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConid() {
        return conid;
    }

    public void setConid(Long conid) {
        this.conid = conid;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAccountAlias() {
        return accountAlias;
    }

    public void setAccountAlias(String accountAlias) {
        this.accountAlias = accountAlias;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(String levelOfDetail) {
        this.levelOfDetail = levelOfDetail;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getCostBasisPrice() {
        return costBasisPrice;
    }

    public void setCostBasisPrice(Double costBasisPrice) {
        this.costBasisPrice = costBasisPrice;
    }

    public Double getCostBasisMoney() {
        return costBasisMoney;
    }

    public void setCostBasisMoney(Double costBasisMoney) {
        this.costBasisMoney = costBasisMoney;
    }

    public Double getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(Double markPrice) {
        this.markPrice = markPrice;
    }

    public Double getPositionValue() {
        return positionValue;
    }

    public void setPositionValue(Double positionValue) {
        this.positionValue = positionValue;
    }

    public Double getPositionValueInBase() {
        return positionValueInBase;
    }

    public void setPositionValueInBase(Double positionValueInBase) {
        this.positionValueInBase = positionValueInBase;
    }

    public Double getFxRateToBase() {
        return fxRateToBase;
    }

    public void setFxRateToBase(Double fxRateToBase) {
        this.fxRateToBase = fxRateToBase;
    }

    public Double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
    }

    public Double getFifoPnlUnrealized() {
        return fifoPnlUnrealized;
    }

    public void setFifoPnlUnrealized(Double fifoPnlUnrealized) {
        this.fifoPnlUnrealized = fifoPnlUnrealized;
    }

    public Double getUnrealizedCapitalGainsPnl() {
        return unrealizedCapitalGainsPnl;
    }

    public void setUnrealizedCapitalGainsPnl(Double unrealizedCapitalGainsPnl) {
        this.unrealizedCapitalGainsPnl = unrealizedCapitalGainsPnl;
    }

    public Double getUnrealizedFxPnl() {
        return unrealizedFxPnl;
    }

    public void setUnrealizedFxPnl(Double unrealizedFxPnl) {
        this.unrealizedFxPnl = unrealizedFxPnl;
    }

    public Double getPercentOfNAV() {
        return percentOfNAV;
    }

    public void setPercentOfNAV(Double percentOfNAV) {
        this.percentOfNAV = percentOfNAV;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public String getSecurityIdType() {
        return securityIdType;
    }

    public void setSecurityIdType(String securityIdType) {
        this.securityIdType = securityIdType;
    }

    public String getCusip() {
        return cusip;
    }

    public void setCusip(String cusip) {
        this.cusip = cusip;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getSedol() {
        return sedol;
    }

    public void setSedol(String sedol) {
        this.sedol = sedol;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getStrike() {
        return strike;
    }

    public void setStrike(Double strike) {
        this.strike = strike;
    }

    public String getPutCall() {
        return putCall;
    }

    public void setPutCall(String putCall) {
        this.putCall = putCall;
    }

    public Long getUnderlyingConid() {
        return underlyingConid;
    }

    public void setUnderlyingConid(Long underlyingConid) {
        this.underlyingConid = underlyingConid;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public String getUnderlyingSecurityId() {
        return underlyingSecurityId;
    }

    public void setUnderlyingSecurityId(String underlyingSecurityId) {
        this.underlyingSecurityId = underlyingSecurityId;
    }

    public String getUnderlyingListingExchange() {
        return underlyingListingExchange;
    }

    public void setUnderlyingListingExchange(String underlyingListingExchange) {
        this.underlyingListingExchange = underlyingListingExchange;
    }

    public Double getPrincipalAdjustFactor() {
        return principalAdjustFactor;
    }

    public void setPrincipalAdjustFactor(Double principalAdjustFactor) {
        this.principalAdjustFactor = principalAdjustFactor;
    }

    public Double getAccruedInterest() {
        return accruedInterest;
    }

    public void setAccruedInterest(Double accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getHoldingPeriodDateTime() {
        return holdingPeriodDateTime;
    }

    public void setHoldingPeriodDateTime(LocalDateTime holdingPeriodDateTime) {
        this.holdingPeriodDateTime = holdingPeriodDateTime;
    }

    public String getOriginatingOrderId() {
        return originatingOrderId;
    }

    public void setOriginatingOrderId(String originatingOrderId) {
        this.originatingOrderId = originatingOrderId;
    }

    public String getOriginatingTransactionId() {
        return originatingTransactionId;
    }

    public void setOriginatingTransactionId(String originatingTransactionId) {
        this.originatingTransactionId = originatingTransactionId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuerCountryCode() {
        return issuerCountryCode;
    }

    public void setIssuerCountryCode(String issuerCountryCode) {
        this.issuerCountryCode = issuerCountryCode;
    }

    public String getCommodityType() {
        return commodityType;
    }

    public void setCommodityType(String commodityType) {
        this.commodityType = commodityType;
    }

    public Double getFineness() {
        return fineness;
    }

    public void setFineness(Double fineness) {
        this.fineness = fineness;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getOpenDateTime() {
        return openDateTime;
    }

    public void setOpenDateTime(LocalDateTime openDateTime) {
        this.openDateTime = openDateTime;
    }

    public InstrumentEntity getUnderlyingInstrument() {
        return underlyingInstrument;
    }

    public void setUnderlyingInstrument(InstrumentEntity underlyingInstrument) {
        this.underlyingInstrument = underlyingInstrument;
    }

    public InstrumentEntity getInstrument() {
        return instrument;
    }

    public void setInstrument(InstrumentEntity instrument) {
        this.instrument = instrument;
    }

    @Override
    public String toString() {
        return "OpenPositionEntity{" +
                "id=" + id +
                ", conid=" + conid +
                ", account='" + account + '\'' +
                ", reportDate=" + reportDate +
                ", assetClass='" + assetClass + '\'' +
                ", currency='" + currency + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", markPrice=" + markPrice +
                ", positionValue=" + positionValue +
                ", side='" + side + '\'' +
                ", expirationDate=" + expirationDate +
                ", strike=" + strike +
                ", putCall='" + putCall + '\'' +
                '}';
    }
}
