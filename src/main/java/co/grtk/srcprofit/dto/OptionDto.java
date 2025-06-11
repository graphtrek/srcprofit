package co.grtk.srcprofit.dto;

import co.grtk.srcprofit.model.AssetClass;
import co.grtk.srcprofit.model.OptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class OptionDto {

    Long id;
    LocalDateTime startDateTime;
    LocalDate expirationDate;
    boolean active;
    String description;
    String color;
    String symbol;
    Integer quantity;
    Double tradePrice;
    Double positionValue;
    Double fee;
    Double marketValue;
    Integer realizedProfitOrLoss;
    Integer annualizedRoiPercent;
    Integer probability;
    Integer daysBetween;
    Integer daysLeft;
    AssetClass assetClass;
    OptionType optionType;


    public String getStartDateTimeString() {
        return Objects.isNull(startDateTime) ? "" : startDateTime.toString();
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

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

    public OptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }

    @Override
    public String toString() {
        return "OptionDto{" +
                "id=" + id +
                ", startDateTime=" + startDateTime +
                ", expirationDate=" + expirationDate +
                ", active=" + active +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                ", symbol='" + symbol + '\'' +
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
                ", optionType=" + optionType +
                '}';
    }
}