package co.grtk.srcprofit.dto;

import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;

import java.time.LocalDate;
import java.util.Objects;

public class PositionDto {

    Long id;
    Long parentId;
    Long conid;
    String code;
    LocalDate tradeDate;

    String note;
    String color = "black";

    Integer quantity = 1;
    Double fee;

    Integer annualizedRoiPercent;
    Integer probability;
    Integer daysBetween;
    Integer daysLeft;
    AssetClass assetClass = AssetClass.OPT;
    OptionType type;
    OptionStatus status;

    String ticker;
    Double tradePrice;
    Double realizedProfitOrLoss;
    Double unRealizedProfitOrLoss;
    Double positionValue;
    Double coveredPositionValue;
    Double collectedPremium;
    LocalDate positionsFromDate;
    LocalDate expirationDate;
    Double marketValue;
    Double marketVsPositionsPercentage;
    Double breakEven;


    public String getTradeDateString() {
        return Objects.isNull(tradeDate) ? "" : tradeDate.toString();
    }

    public String getPositionsFromDateString() {
        return Objects.isNull(positionsFromDate) ? "" : positionsFromDate.toString();
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

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
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
        this.ticker = ticker != null ? ticker.toUpperCase() : null;
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

    public Double getRealizedProfitOrLoss() {
        return realizedProfitOrLoss;
    }

    public void setRealizedProfitOrLoss(Double realizedProfitOrLoss) {
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

    public Long getConid() {
        return conid;
    }

    public void setConid(Long conid) {
        this.conid = conid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getUnRealizedProfitOrLoss() {
        return unRealizedProfitOrLoss;
    }

    public void setUnRealizedProfitOrLoss(Double unRealizedProfitOrLoss) {
        this.unRealizedProfitOrLoss = unRealizedProfitOrLoss;
    }

    public Double getCollectedPremium() {
        return collectedPremium;
    }

    public void setCollectedPremium(Double collectedPremium) {
        this.collectedPremium = collectedPremium;
    }

    public LocalDate getPositionsFromDate() {
        return positionsFromDate;
    }

    public void setPositionsFromDate(LocalDate positionsFromDate) {
        this.positionsFromDate = positionsFromDate;
    }

    public Double getCoveredPositionValue() {
        return coveredPositionValue;
    }

    public void setCoveredPositionValue(Double coveredPositionValue) {
        this.coveredPositionValue = coveredPositionValue;
    }

    public Double getMarketVsPositionsPercentage() {
        return marketVsPositionsPercentage;
    }

    public void setMarketVsPositionsPercentage(Double marketVsPositionsPercentage) {
        this.marketVsPositionsPercentage = marketVsPositionsPercentage;
    }

    public Double getBreakEven() {
        return breakEven;
    }

    public void setBreakEven(Double breakEven) {
        this.breakEven = breakEven;
    }

    @Override
    public String toString() {
        return "PositionDto{" +
                "id=" + id +
                ", parentId=" + parentId +
                ", conid=" + conid +
                ", code='" + code + '\'' +
                ", tradeDate=" + tradeDate +
                ", note='" + note + '\'' +
                ", color='" + color + '\'' +
                ", quantity=" + quantity +
                ", fee=" + fee +
                ", annualizedRoiPercent=" + annualizedRoiPercent +
                ", probability=" + probability +
                ", daysBetween=" + daysBetween +
                ", daysLeft=" + daysLeft +
                ", assetClass=" + assetClass +
                ", type=" + type +
                ", status=" + status +
                ", ticker='" + ticker + '\'' +
                ", tradePrice=" + tradePrice +
                ", realizedProfitOrLoss=" + realizedProfitOrLoss +
                ", unRealizedProfitOrLoss=" + unRealizedProfitOrLoss +
                ", positionValue=" + positionValue +
                ", coveredPositionValue=" + coveredPositionValue +
                ", collectedPremium=" + collectedPremium +
                ", positionsFromDate=" + positionsFromDate +
                ", expirationDate=" + expirationDate +
                ", marketValue=" + marketValue +
                ", marketVsPositionsPercentage=" + marketVsPositionsPercentage +
                ", breakEven=" + breakEven +
                '}';
    }
}