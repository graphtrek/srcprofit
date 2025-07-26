package co.grtk.srcprofit.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateAsString;

public class EarningDto {
    private Long id;
    LocalDate reportDate;
    String symbol;
    String name;
    LocalDate fiscalDateEnding;
    String estimate;
    String currency;
    public String getReportDateStr() {
        return getLocalDateAsString(reportDate);
    }
    public String getFiscalDateEndingStr() {
        return getLocalDateAsString(fiscalDateEnding);
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

    public LocalDate getFiscalDateEnding() {
        return fiscalDateEnding;
    }

    public void setFiscalDateEnding(LocalDate fiscalDateEnding) {
        this.fiscalDateEnding = fiscalDateEnding;
    }

    public String getEstimate() {
        return estimate;
    }

    public void setEstimate(String estimate) {
        this.estimate = estimate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getEarningDay() {
        return reportDate == null ? 999 : (int) ChronoUnit.DAYS.between(LocalDate.now(), reportDate.atStartOfDay());
    }

    @Override
    public String toString() {
        return "EarningDto{" +
                "id=" + id +
                ", reportDate=" + reportDate +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", fiscalDateEnding='" + fiscalDateEnding + '\'' +
                ", estimate='" + estimate + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
}