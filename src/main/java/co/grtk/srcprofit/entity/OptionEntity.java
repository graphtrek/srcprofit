package co.grtk.srcprofit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(
        name = "OPTION",
        indexes = {
                @Index(name = "instrument_idx", columnList = "instrumentId"),
                @Index(name = "status_idx", columnList = "status")
        }
)
@DiscriminatorValue("OPT")
public class OptionEntity extends BaseAsset{

    @Enumerated(EnumType.STRING)
    private OptionStatus status;

    @Enumerated(EnumType.STRING)
    private OptionType type;

    @Column(nullable = false)
    LocalDate expirationDate;

    @Column
    Double fee;

    @Column
    Integer realizedProfitOrLoss;

    @Column
    Integer annualizedRoiPercent;

    @Column
    Integer probability;

    @Column
    Integer daysBetween;

    @Column
    Integer daysLeft;

    @Column
    String color;

    @Column
    String note;


    public OptionStatus getStatus() {
        return status;
    }

    public void setStatus(OptionStatus status) {
        this.status = status;
    }

    public OptionType getType() {
        return type;
    }

    public void setType(OptionType type) {
        this.type = type;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
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

    public void setAnnualizedRoiPercent(Integer annualizedRoiPercent) {
        this.annualizedRoiPercent = annualizedRoiPercent;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}