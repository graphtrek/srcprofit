package co.grtk.srcprofit.dto;

import java.time.LocalDateTime;

import static co.grtk.srcprofit.mapper.MapperUtils.getLocalDateTimeAsString;

public class FlexImportHistoryDto {
    Long id;
    String referenceCode;
    String reportType;
    String status;
    LocalDateTime updatedAt;
    Integer csvRecordsCount;
    Integer csvFailedRecordsCount;
    Integer csvSkippedRecordsCount;
    Integer dataFixRecordsCount;

    public String getUpdatedAtStr() {
        return getLocalDateTimeAsString(updatedAt);
    }

    public Integer getCsvRecordsCountSafe() {
        return csvRecordsCount != null ? csvRecordsCount : 0;
    }

    public Integer getCsvFailedRecordsCountSafe() {
        return csvFailedRecordsCount != null ? csvFailedRecordsCount : 0;
    }

    public Integer getCsvSkippedRecordsCountSafe() {
        return csvSkippedRecordsCount != null ? csvSkippedRecordsCount : 0;
    }

    public Integer getDataFixRecordsCountSafe() {
        return dataFixRecordsCount != null ? dataFixRecordsCount : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCsvRecordsCount() {
        return csvRecordsCount;
    }

    public void setCsvRecordsCount(Integer csvRecordsCount) {
        this.csvRecordsCount = csvRecordsCount;
    }

    public Integer getCsvFailedRecordsCount() {
        return csvFailedRecordsCount;
    }

    public void setCsvFailedRecordsCount(Integer csvFailedRecordsCount) {
        this.csvFailedRecordsCount = csvFailedRecordsCount;
    }

    public Integer getCsvSkippedRecordsCount() {
        return csvSkippedRecordsCount;
    }

    public void setCsvSkippedRecordsCount(Integer csvSkippedRecordsCount) {
        this.csvSkippedRecordsCount = csvSkippedRecordsCount;
    }

    public Integer getDataFixRecordsCount() {
        return dataFixRecordsCount;
    }

    public void setDataFixRecordsCount(Integer dataFixRecordsCount) {
        this.dataFixRecordsCount = dataFixRecordsCount;
    }

    @Override
    public String toString() {
        return "FlexImportHistoryDto{" +
                "id=" + id +
                ", referenceCode='" + referenceCode + '\'' +
                ", reportType='" + reportType + '\'' +
                ", status='" + status + '\'' +
                ", updatedAt=" + updatedAt +
                ", csvRecordsCount=" + csvRecordsCount +
                ", csvFailedRecordsCount=" + csvFailedRecordsCount +
                ", csvSkippedRecordsCount=" + csvSkippedRecordsCount +
                ", dataFixRecordsCount=" + dataFixRecordsCount +
                '}';
    }
}
