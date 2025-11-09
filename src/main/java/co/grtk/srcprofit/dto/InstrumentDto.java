package co.grtk.srcprofit.dto;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateAsString;
import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateTimeAsString;

public class InstrumentDto {

    private Long id;

    private Long conid;

    private String name;

    private String assetClass;

    private String ticker;

    private Double price;

    private LocalDateTime updated;

    private Double change;

    private Double changePercent;

    private Double realizedProfitOrLoss;
    private Double unRealizedProfitOrLoss;
    private Double collectedPremium;
    private LocalDate earningDate;

    private String alpacaExchange;

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

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getPrice() {
        return price == null ? 0 : price;
    }

    public void setPrice(Double price) {
        this.price = price;
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

    public String getUpdatedTimeStr() {
        return getLocalDateTimeAsString(updated);
    }

    public Double getRealizedProfitOrLoss() {
        return realizedProfitOrLoss;
    }

    public void setRealizedProfitOrLoss(Double realizedProfitOrLoss) {
        this.realizedProfitOrLoss = realizedProfitOrLoss;
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

    public LocalDate getEarningDate() {
        return earningDate;
    }

    public String getEarningDateStr() {
        return getLocalDateAsString(earningDate);
    }

    public void setEarningDate(LocalDate earningDate) {
        this.earningDate = earningDate;
    }

    public Integer getEarningDay() {
        return earningDate == null ? 999 : (int) ChronoUnit.DAYS.between(LocalDate.now(), earningDate.atStartOfDay());
    }

    public String getAlpacaExchange() {
        return alpacaExchange;
    }

    public void setAlpacaExchange(String alpacaExchange) {
        this.alpacaExchange = alpacaExchange;
    }
    @Override
    public String toString() {
        return "InstrumentDto{" +
                "id=" + id +
                ", conid=" + conid +
                ", name='" + name + '\'' +
                ", assetClass='" + assetClass + '\'' +
                ", ticker='" + ticker + '\'' +
                ", price=" + price +
                ", updated=" + updated +
                ", change=" + change +
                ", changePercent=" + changePercent +
                '}';
    }
}
