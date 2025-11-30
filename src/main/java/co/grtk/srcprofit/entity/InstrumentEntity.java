package co.grtk.srcprofit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")


@Table(
        name = "INSTRUMENT",
        indexes = {
                @Index(name = "instr_ticker_idx", columnList = "ticker"),
                @Index(name = "instr_conid_idx", columnList = "conid"),
                @Index(name = "instr_name_idx", columnList = "name"),
                @Index(name = "instr_alpaca_asset_id_idx", columnList = "alpaca_asset_id")
        }

)
public class InstrumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long conid;

    @Column
    private String name;

    @Column(nullable = false, unique = true)
    private String ticker;

    @JsonIgnore
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OptionEntity> options;

    /**
     * Open positions where this instrument is the direct holding.
     * For stocks: positions where this instrument is the direct holding.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "instrument", fetch = FetchType.LAZY)
    private List<OpenPositionEntity> directPositions;

    /**
     * Open positions where this instrument is the underlying for options.
     * For stocks used as option underlyings: option positions referencing this as underlying.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "underlyingInstrument", fetch = FetchType.LAZY)
    private List<OpenPositionEntity> underlyingPositions;

    @Column
    private Double price;

    @Column
    private LocalDateTime updated;

    @Column
    private Double change;

    @Column
    private Double changePercent;

    @CreationTimestamp(source = SourceType.DB)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    private Instant updatedAt;

    private LocalDate earningDate;

    // Alpaca Asset Metadata Fields
    @Column(unique = true)
    private String alpacaAssetId;

    @Column
    private Boolean alpacaTradable;

    @Column
    private Boolean alpacaMarginable;

    @Column
    private Boolean alpacaShortable;

    @Column
    private Boolean alpacaEasyToBorrow;

    @Column
    private Boolean alpacaFractionable;

    @Column(precision = 10, scale = 4)
    private BigDecimal alpacaMaintenanceMarginRequirement;

    @Column
    private String alpacaExchange;

    @Column
    private String alpacaStatus;

    @Column
    private String alpacaAssetClass;

    @Column
    private Instant alpacaMetadataUpdatedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public List<OptionEntity> getOptions() {
        return options;
    }

    public void setOptions(List<OptionEntity> options) {
    }

    public List<OpenPositionEntity> getDirectPositions() {
        return directPositions;
    }

    public void setDirectPositions(List<OpenPositionEntity> directPositions) {
        this.directPositions = directPositions;
    }

    public List<OpenPositionEntity> getUnderlyingPositions() {
        return underlyingPositions;
    }

    public void setUnderlyingPositions(List<OpenPositionEntity> underlyingPositions) {
        this.underlyingPositions = underlyingPositions;
    }

    /**
     * Get all open positions related to this instrument.
     * Combines both direct positions (stocks) and underlying positions (options).
     *
     * @return Combined list of all related positions
     */
    public java.util.List<OpenPositionEntity> getAllRelatedPositions() {
        java.util.List<OpenPositionEntity> all = new java.util.ArrayList<>();
        if (directPositions != null) {
            all.addAll(directPositions);
        }
        if (underlyingPositions != null) {
            all.addAll(underlyingPositions);
        }
        return all;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getOptionPrice() {
        if (price == null) {
            return 0.0;
        }
        return Math.round(price * 100 * 100.0) / 100.0;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Double getChange() {
        return change;
    }

    public void setChange(Double change) {
        this.change = change;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    public LocalDate getEarningDate() {
        return earningDate;
    }

    public void setEarningDate(LocalDate earningsDate) {
        this.earningDate = earningsDate;
    }

    // Alpaca Asset Metadata Getters and Setters
    public String getAlpacaAssetId() {
        return alpacaAssetId;
    }

    public void setAlpacaAssetId(String alpacaAssetId) {
        this.alpacaAssetId = alpacaAssetId;
    }

    public Boolean getAlpacaTradable() {
        return alpacaTradable;
    }

    public void setAlpacaTradable(Boolean alpacaTradable) {
        this.alpacaTradable = alpacaTradable;
    }

    public Boolean getAlpacaMarginable() {
        return alpacaMarginable;
    }

    public void setAlpacaMarginable(Boolean alpacaMarginable) {
        this.alpacaMarginable = alpacaMarginable;
    }

    public Boolean getAlpacaShortable() {
        return alpacaShortable;
    }

    public void setAlpacaShortable(Boolean alpacaShortable) {
        this.alpacaShortable = alpacaShortable;
    }

    public Boolean getAlpacaEasyToBorrow() {
        return alpacaEasyToBorrow;
    }

    public void setAlpacaEasyToBorrow(Boolean alpacaEasyToBorrow) {
        this.alpacaEasyToBorrow = alpacaEasyToBorrow;
    }

    public Boolean getAlpacaFractionable() {
        return alpacaFractionable;
    }

    public void setAlpacaFractionable(Boolean alpacaFractionable) {
        this.alpacaFractionable = alpacaFractionable;
    }

    public BigDecimal getAlpacaMaintenanceMarginRequirement() {
        return alpacaMaintenanceMarginRequirement;
    }

    public void setAlpacaMaintenanceMarginRequirement(BigDecimal alpacaMaintenanceMarginRequirement) {
        this.alpacaMaintenanceMarginRequirement = alpacaMaintenanceMarginRequirement;
    }

    public String getAlpacaExchange() {
        return alpacaExchange;
    }

    public void setAlpacaExchange(String alpacaExchange) {
        this.alpacaExchange = alpacaExchange;
    }

    public String getAlpacaStatus() {
        return alpacaStatus;
    }

    public void setAlpacaStatus(String alpacaStatus) {
        this.alpacaStatus = alpacaStatus;
    }

    public String getAlpacaAssetClass() {
        return alpacaAssetClass;
    }

    public void setAlpacaAssetClass(String alpacaAssetClass) {
        this.alpacaAssetClass = alpacaAssetClass;
    }

    public Instant getAlpacaMetadataUpdatedAt() {
        return alpacaMetadataUpdatedAt;
    }

    public void setAlpacaMetadataUpdatedAt(Instant alpacaMetadataUpdatedAt) {
        this.alpacaMetadataUpdatedAt = alpacaMetadataUpdatedAt;
    }

    @Override
    public String toString() {
        return "InstrumentEntity{" +
                "id=" + id +
                ", conid=" + conid +
                ", name='" + name + '\'' +
                ", ticker='" + ticker + '\'' +
                ", options=" + options +
                ", price=" + price +
                ", updated=" + updated +
                ", change=" + change +
                ", changePercent=" + changePercent +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}