package co.grtk.srcprofit.dto;

import java.time.LocalDate;

import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateAsString;

public class NetAssetValueDto {
    Long id;
    LocalDate reportDate;
    Double cash;
    Double stock;
    Double options;
    Double total;
    Double dividendAccruals;
    Double interestAccruals;
    Double dailyPremium;
    Double roi;
    Double averageCash;

    public String getReportDateStr() {
        return getLocalDateAsString(reportDate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public Double getCash() {
        return cash;
    }

    public void setCash(Double cash) {
        this.cash = cash;
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }

    public Double getOptions() {
        return options;
    }

    public void setOptions(Double options) {
        this.options = options;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getDividendAccruals() {
        return dividendAccruals;
    }

    public void setDividendAccruals(Double dividendAccruals) {
        this.dividendAccruals = dividendAccruals;
    }

    public Double getInterestAccruals() {
        return interestAccruals;
    }

    public void setInterestAccruals(Double interestAccruals) {
        this.interestAccruals = interestAccruals;
    }

    public Double getDailyPremium() {
        return dailyPremium;
    }

    public void setDailyPremium(Double dailyPremium) {
        this.dailyPremium = dailyPremium;
    }

    public Double getRoi() {
        return roi;
    }

    public void setRoi(Double roi) {
        this.roi = roi;
    }

    public Double getAverageCash() {
        return averageCash;
    }

    public void setAverageCash(Double averageCash) {
        this.averageCash = averageCash;
    }

    @Override
    public String toString() {
        return "NetAssetValueDto{" +
                "id=" + id +
                ", reportDate=" + reportDate +
                ", cash=" + cash +
                ", stock=" + stock +
                ", option=" + options +
                ", total=" + total +
                ", dividendAccruals=" + dividendAccruals +
                ", interestAccruals=" + interestAccruals +
                '}';
    }
}
