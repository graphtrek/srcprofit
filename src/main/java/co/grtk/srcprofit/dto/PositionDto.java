package co.grtk.srcprofit.dto;

import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateAsString;

public class PositionDto {

    Long id;
    Long parentId;
    Long conid;
    String code;
    LocalDate tradeDate;

    String note;
    String color = "black";

    int quantity = 1;
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
    Double cash;
    Double put;
    Double call;
    Double stock;
    Double marketPrice;
    Double putMarketPrice;
    Double callMarketPrice;
    LocalDate earningDate;

    public String getTradeDateString() {
        return getLocalDateAsString(tradeDate);
    }

    public String getPositionsFromDateString() {
        return getLocalDateAsString(positionsFromDate);
    }

    public String getExpirationDateString() {
        return getLocalDateAsString(expirationDate);
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
        return realizedProfitOrLoss == null ? 0 : realizedProfitOrLoss;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
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

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getPut() {
        return put;
    }

    public void setPut(Double put) {
        this.put = put;
    }

    public Double getCall() {
        return call;
    }

    public void setCall(Double call) {
        this.call = call;
    }

    public Double getMarketPrice() {
        return marketPrice == null ? 0 : marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }

    public Double getPutMarketPrice() {
        return putMarketPrice;
    }

    public void setPutMarketPrice(Double putMarketPrice) {
        this.putMarketPrice = putMarketPrice;
    }

    public Double getCallMarketPrice() {
        return callMarketPrice;
    }

    public void setCallMarketPrice(Double callMarketPrice) {
        this.callMarketPrice = callMarketPrice;
    }

    public LocalDate getEarningDate() {
        return earningDate;
    }

    public void setEarningDate(LocalDate earningDate) {
        this.earningDate = earningDate;
    }

    public Integer getEarningDay() {
        return earningDate == null ? 999 : (int) ChronoUnit.DAYS.between(LocalDate.now(), earningDate.atStartOfDay());
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