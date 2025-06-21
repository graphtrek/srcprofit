package co.grtk.srcprofit.dto;

import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class OptionDto {

    Long id;
    Long parentId;
    LocalDateTime tradeDateTime = LocalDateTime.now();
    LocalDate expirationDate;
    String note;
    String color = "black";
    String ticker;
    Integer quantity = 1;
    Double tradePrice;
    Double positionValue;
    Double fee;
    Double marketValue;
    Integer realizedProfitOrLoss;
    Integer annualizedRoiPercent;
    Integer probability;
    Integer daysBetween;
    Integer daysLeft;
    AssetClass assetClass = AssetClass.OPT;
    OptionType type;
    OptionStatus status;

    public String getTradeDateTimeString() {
        return Objects.isNull(tradeDateTime) ? "" : tradeDateTime.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public String getTradeDateString() {
        return Objects.isNull(tradeDateTime) ? "" : tradeDateTime.toLocalDate().toString();
    }


    public String getExpirationDateString() {
        return Objects.isNull(expirationDate) ? "" : expirationDate.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTradeDateTime() {
        return tradeDateTime;
    }

    public void setTradeDateTime(LocalDateTime tradeDateTime) {
        this.tradeDateTime = tradeDateTime;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getTradePrice() {
        return tradePrice;
    }

    public void setTradePrice(Double tradePrice) {
        this.tradePrice = tradePrice;
    }

    public Double getPositionValue() {
        return positionValue;
    }

    public void setPositionValue(Double positionValue) {
        this.positionValue = positionValue;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }

    public Double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(Double marketValue) {
        this.marketValue = marketValue;
    }

    public Integer getRealizedProfitOrLoss() {
        return realizedProfitOrLoss;
    }

    public void setRealizedProfitOrLoss(Integer realizedProfitOrLoss) {
        this.realizedProfitOrLoss = realizedProfitOrLoss;
    }

    public Integer getAnnualizedRoiPercent() {
        return annualizedRoiPercent;
    }

    public void setAnnualizedRoiPercent(Integer roi) {
        this.annualizedRoiPercent = roi;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OptionType getType() {
        return type;
    }

    public void setType(OptionType optionType) {
        this.type = optionType;
    }

    public OptionStatus getStatus() {
        return status;
    }

    public void setStatus(OptionStatus optionStatus) {
        this.status = optionStatus;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "OptionDto{" +
                "id=" + id +
                ", tradeDateTime=" + tradeDateTime +
                ", expirationDate=" + expirationDate +
                ", note='" + note + '\'' +
                ", color='" + color + '\'' +
                ", ticker='" + ticker + '\'' +
                ", quantity=" + quantity +
                ", tradePrice=" + tradePrice +
                ", positionValue=" + positionValue +
                ", fee=" + fee +
                ", marketValue=" + marketValue +
                ", realizedProfitOrLoss=" + realizedProfitOrLoss +
                ", annualizedRoiPercent=" + annualizedRoiPercent +
                ", probability=" + probability +
                ", daysBetween=" + daysBetween +
                ", daysLeft=" + daysLeft +
                ", assetClass=" + assetClass +
                ", type=" + type +
                ", status=" + status +
                ", parentId=" + parentId +
                '}';
    }
}