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
@Table(name = "NET_ASSET_VALUE",
        indexes = {
                @Index(name = "nav_report_date_idx", columnList = "reportDate"),
        })
public class NetAssetValueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false, unique = true)
    LocalDate reportDate;

    @Column
    private Double cash;

    @Column
    private Double stock;

    @Column
    private Double options;

    @Column
    private Double dividendAccruals;

    @Column
    private Double interestAccruals;

    @Column
    private Double total;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "NetAssetValueEntity{" +
                "id=" + id +
                ", reportDate=" + reportDate +
                ", cash=" + cash +
                ", stock=" + stock +
                ", options=" + options +
                ", dividendAccruals=" + dividendAccruals +
                ", interestAccruals=" + interestAccruals +
                ", total=" + total +
                '}';
    }
}
