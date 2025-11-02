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
                '}';
    }
}
