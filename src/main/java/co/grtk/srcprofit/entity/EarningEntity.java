package co.grtk.srcprofit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(name = "EARNING",
        indexes = {
                @Index(name = "earning_report_date_idx", columnList = "reportDate"),
                @Index(name = "earning_symbol_idx", columnList = "symbol"),
                @Index(name = "earning_idx", columnList = "symbol, reportDate, fiscalDateEnding")
        })
public class EarningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    LocalDate reportDate;

    @Column(nullable = false)
    String symbol;

    @Column(nullable = false)
    String name;

    @Column
    LocalDate fiscalDateEnding;

    @Column
    String estimate;

    @Column
    String currency;

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
}
