package co.grtk.srcprofit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(
        name = "OPTION",
        indexes = {
                @Index(name = "opt_conid_idx", columnList = "conid"),
                @Index(name = "opt_conid_status_idx", columnList = "conid, status"),
                @Index(name = "opt_conid_status_price_idx", columnList = "conid, status, tradePrice"),
                @Index(name = "opt_code_idx", columnList = "code"),
                @Index(name = "opt_ticker_idx", columnList = "ticker"),
                @Index(name = "opt_instrument_id_idx", columnList = "instrumentId"),
                @Index(name = "opt_status_idx", columnList = "status")
        }
)
@DiscriminatorValue("OPT")
public class OptionEntity extends BaseAsset {

    @Column(nullable = false)
    private String account;
    @Column(nullable = false)
    private LocalDate expirationDate;
    @Column
    private Double fee;
    @Column
    private Double realizedProfitOrLoss;
    @Column
    private Integer annualizedRoiPercent;
    @Column
    private Integer probability;
    @Column
    private Integer daysBetween;
    @Column
    private Integer daysLeft;
    @Column
    private String color;
    @Column
    private String note;
    @Column
    private Long conid;
    @Column(nullable = false)
    private String ticker;
    @Column(nullable = false)
    private String code;
    @Enumerated(EnumType.STRING)
    private OptionStatus status;
    @Enumerated(EnumType.STRING)
    private OptionType type;
    @Column
    private Double marketPrice;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public OptionStatus getStatus() {
        return status;
    }

    public void setStatus(OptionStatus status) {
        this.status = status;
    }

    public OptionType getType() {
        return type;
    }

    public void setType(OptionType type) {
        this.type = type;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public Double getRealizedProfitOrLoss() {
        return realizedProfitOrLoss;
    }

    public void setRealizedProfitOrLoss(Double realizedProfitOrLoss) {
        this.realizedProfitOrLoss = realizedProfitOrLoss;
    }

    public Integer getAnnualizedRoiPercent() {
        return annualizedRoiPercent;
    }

    public void setAnnualizedRoiPercent(Integer annualizedRoiPercent) {
        this.annualizedRoiPercent = annualizedRoiPercent;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getDaysBetween() {
        return daysBetween;
    }

    public void setDaysBetween(Integer daysBetween) {
        this.daysBetween = daysBetween;
    }

    public Integer getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Integer daysLeft) {
        this.daysLeft = daysLeft;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getConid() {
        return conid;
    }

    public void setConid(Long conid) {
        this.conid = conid;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }

}