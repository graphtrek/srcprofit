package co.grtk.srcprofit.dto;

import co.grtk.srcprofit.mapper.Interval;
import co.grtk.srcprofit.mapper.MapperUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import static co.grtk.srcprofit.mapper.Interval.ALL;
import static co.grtk.srcprofit.mapper.MapperUtils.parseDouble;

public class ChartDataDto {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Interval interval;
    private Double lastDailyPremium;

    Map<LocalDate, BigDecimal> dailyPremium;
    String datesCsv;
    String navDatesCsv;
    String dailyPremiumCsv;

    Map<LocalDate, BigDecimal> dailyTotal;
    String dailyTotalCsv;

    Map<LocalDate, BigDecimal> dailyCash;
    String dailyCashCsv;

    Map<LocalDate, BigDecimal> dailyStock;
    String dailyStockCsv;

    Map<LocalDate, BigDecimal> dailyOptions;
    String dailyOptionsCsv;

    public ChartDataDto(Interval interval) {
        LocalDate now = LocalDate.now();
        switch (interval) {
            case WEEK:
                this.startDate = now.with(DayOfWeek.MONDAY);
                this.endDate = now.with(DayOfWeek.SUNDAY);
                this.interval = interval;
                break;
            case MONTH:
                this.startDate = now.with(TemporalAdjusters.firstDayOfMonth());
                this.endDate = now.with(TemporalAdjusters.lastDayOfMonth());
                this.interval = interval;
                break;
            case YEAR:
                this.startDate = now.with(TemporalAdjusters.firstDayOfYear());
                this.endDate = now.with(TemporalAdjusters.lastDayOfYear());
                this.interval = interval;
                break;
            default:
                this.startDate = null;
                this.endDate = null;
                this.interval = ALL;
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Interval getInterval() {
        return interval;
    }

    public Map<LocalDate, BigDecimal> getDailyPremium() {
        return dailyPremium;
    }

    public void setDailyPremium(Map<LocalDate, BigDecimal> dailyPremium) {
        this.dailyPremium = dailyPremium;
        datesCsv = MapperUtils.getDatesCsv(dailyPremium);
        dailyPremiumCsv = MapperUtils.getValuesCsv(dailyPremium);
        String [] premiums = dailyPremiumCsv.split(",");
        lastDailyPremium = parseDouble(premiums[premiums.length-1],0.0);
    }

    public String getDatesCsv() {
        return datesCsv;
    }

    public void setDatesCsv(String datesCsv) {
        this.datesCsv = datesCsv;
    }

    public String getDailyPremiumCsv() {
        return dailyPremiumCsv;
    }

    public void setDailyPremiumCsv(String dailyPremiumCsv) {
        this.dailyPremiumCsv = dailyPremiumCsv;
    }

    public Map<LocalDate, BigDecimal> getDailyTotal() {
        return dailyTotal;
    }

    public void setDailyTotal(Map<LocalDate, BigDecimal> dailyTotal) {
        this.dailyTotal = dailyTotal;
        navDatesCsv = MapperUtils.getDatesCsv(dailyTotal);
        dailyTotalCsv = MapperUtils.getValuesCsv(dailyTotal);
    }

    public String getDailyTotalCsv() {
        return dailyTotalCsv;
    }

    public Map<LocalDate, BigDecimal> getDailyCash() {
        return dailyCash;
    }

    public void setDailyCash(Map<LocalDate, BigDecimal> dailyCash) {
        this.dailyCash = dailyCash;
        dailyCashCsv = MapperUtils.getValuesCsv(dailyCash);
    }

    public String getDailyCashCsv() {
        return dailyCashCsv;
    }


    public Map<LocalDate, BigDecimal> getDailyStock() {
        return dailyStock;
    }

    public void setDailyStock(Map<LocalDate, BigDecimal> dailyStock) {
        this.dailyStock = dailyStock;
        dailyStockCsv = MapperUtils.getValuesCsv(dailyStock);
    }

    public String getDailyStockCsv() {
        return dailyStockCsv;
    }

    public Map<LocalDate, BigDecimal> getDailyOptions() {
        return dailyOptions;
    }

    public void setDailyOptions(Map<LocalDate, BigDecimal> dailyOptions) {
        this.dailyOptions = dailyOptions;
        dailyOptionsCsv = MapperUtils.getValuesCsv(dailyOptions);
    }

    public String getDailyOptionsCsv() {
        return dailyOptionsCsv;
    }

    public double getLastDailyPremium() {
        return lastDailyPremium == null ? 0 : lastDailyPremium;
    }

    public double getLastDailyTotal() {
        if (dailyTotalCsv == null || dailyTotalCsv.isEmpty()) {
            return 0.0;
        }
        String[] values = dailyTotalCsv.split(",");
        return parseDouble(values[values.length - 1], 0.0);
    }

    public double getLastDailyCash() {
        if (dailyCashCsv == null || dailyCashCsv.isEmpty()) {
            return 0.0;
        }
        String[] values = dailyCashCsv.split(",");
        return parseDouble(values[values.length - 1], 0.0);
    }

    public double getLastDailyStock() {
        if (dailyStockCsv == null || dailyStockCsv.isEmpty()) {
            return 0.0;
        }
        String[] values = dailyStockCsv.split(",");
        return parseDouble(values[values.length - 1], 0.0);
    }

    public double getLastDailyOptions() {
        if (dailyOptionsCsv == null || dailyOptionsCsv.isEmpty()) {
            return 0.0;
        }
        String[] values = dailyOptionsCsv.split(",");
        return parseDouble(values[values.length - 1], 0.0);
    }

    public String getNavDatesCsv() {
        return navDatesCsv;
    }
}