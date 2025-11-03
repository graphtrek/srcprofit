package co.grtk.srcprofit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Entity representing FLEX report API request metadata from Interactive Brokers.
 *
 * Stores audit trail of FLEX API SendRequest calls, including reference codes,
 * timestamps, and report types for tracking import history.
 *
 * @see co.grtk.srcprofit.dto.FlexStatementResponse
 * @see co.grtk.srcprofit.service.FlexReportsService
 */
@Entity
@Table(name = "FLEX_STATEMENT_RESPONSE",
        indexes = {
                @Index(name = "fsr_reference_code_idx", columnList = "referenceCode"),
                @Index(name = "fsr_request_date_idx", columnList = "requestDate"),
                @Index(name = "fsr_report_type_idx", columnList = "reportType")
        })
public class FlexStatementResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique reference code from IBKR FLEX API.
     * Used to retrieve generated CSV report via GetStatement API.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String referenceCode;

    /**
     * Date when FLEX report was requested.
     * Converted from FlexStatementResponse.timestamp String to LocalDate.
     */
    @Column(nullable = false)
    private LocalDate requestDate;

    /**
     * Status from FLEX API response (e.g., "Success", "Fail").
     */
    @Column(nullable = false, length = 50)
    private String status;

    /**
     * URL for retrieving the generated FLEX report.
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Type of FLEX report requested.
     * Values: "TRADES" (options trades) or "NAV" (net asset value).
     */
    @Column(nullable = false, length = 20)
    private String reportType;

    /**
     * Original timestamp string from FLEX API response.
     * Preserved for debugging and audit purposes.
     * Format: typically "YYYY-MM-DD HH:mm:ss"
     */
    @Column(length = 50)
    private String originalTimestamp;

    /**
     * Database connection URL where the import was processed.
     * Captures which database instance received the imported data.
     * Useful for multi-environment deployments (dev/staging/prod).
     */
    @Column(name = "db_url", length = 500)
    private String dbUrl;

    /**
     * File system path to the saved CSV file.
     * Path to the CSV file written after GetStatement API call.
     * Example: ~/FLEX_TRADES_ABC123.csv or ~/FLEX_NET_ASSET_VALUE_XYZ789.csv
     */
    @Column(name = "csv_file_path", length = 500)
    private String csvFilePath;

    /**
     * Number of records imported from the CSV file.
     * Count returned from saveCSV() method during import processing.
     */
    @Column(name = "csv_records_count")
    private Integer csvRecordsCount;

    /**
     * Number of records processed in data fix operation.
     * Only applicable to FLEX Trades imports (null for NAV reports).
     * Count returned from dataFix() method.
     */
    @Column(name = "data_fix_records_count")
    private Integer dataFixRecordsCount;

    // Constructors

    public FlexStatementResponseEntity() {
    }

    public FlexStatementResponseEntity(String referenceCode, LocalDate requestDate, String status,
                                       String url, String reportType, String originalTimestamp) {
        this.referenceCode = referenceCode;
        this.requestDate = requestDate;
        this.status = status;
        this.url = url;
        this.reportType = reportType;
        this.originalTimestamp = originalTimestamp;
    }

    // Getters and Setters

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

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getOriginalTimestamp() {
        return originalTimestamp;
    }

    public void setOriginalTimestamp(String originalTimestamp) {
        this.originalTimestamp = originalTimestamp;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public Integer getCsvRecordsCount() {
        return csvRecordsCount;
    }

    public void setCsvRecordsCount(Integer csvRecordsCount) {
        this.csvRecordsCount = csvRecordsCount;
    }

    public Integer getDataFixRecordsCount() {
        return dataFixRecordsCount;
    }

    public void setDataFixRecordsCount(Integer dataFixRecordsCount) {
        this.dataFixRecordsCount = dataFixRecordsCount;
    }

    @Override
    public String toString() {
        return "FlexStatementResponseEntity{" +
                "id=" + id +
                ", referenceCode='" + referenceCode + '\'' +
                ", requestDate=" + requestDate +
                ", status='" + status + '\'' +
                ", url='" + url + '\'' +
                ", reportType='" + reportType + '\'' +
                ", originalTimestamp='" + originalTimestamp + '\'' +
                ", dbUrl='" + dbUrl + '\'' +
                ", csvFilePath='" + csvFilePath + '\'' +
                ", csvRecordsCount=" + csvRecordsCount +
                ", dataFixRecordsCount=" + dataFixRecordsCount +
                '}';
    }
}
