package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.FlexStatementResponse;
import co.grtk.srcprofit.entity.FlexStatementResponseEntity;
import co.grtk.srcprofit.repository.FlexStatementResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service for persisting FLEX statement response metadata to database.
 *
 * Responsibilities:
 * - Save FLEX API request metadata for audit trail
 * - Convert timestamp strings to LocalDate for database storage
 * - Maintain historical record of FLEX report imports
 *
 * This service provides persistence-only functionality, with scheduling and
 * orchestration handled by FlexReportsService.
 *
 * @see FlexReportsService for FLEX API orchestration and scheduling
 * @see FlexStatementResponseRepository for database access
 * @see FlexStatementResponseEntity for database schema
 */
@Service
public class FlexStatementPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(FlexStatementPersistenceService.class);

    private final FlexStatementResponseRepository flexStatementResponseRepository;

    public FlexStatementPersistenceService(FlexStatementResponseRepository flexStatementResponseRepository) {
        this.flexStatementResponseRepository = flexStatementResponseRepository;
    }

    /**
     * Converts FLEX API timestamp string to LocalDate.
     *
     * @param timestamp timestamp string from IBKR (e.g., "2025-11-02 20:55:44")
     * @return LocalDate representing the date portion
     * @throws IllegalArgumentException if timestamp is invalid
     */
    private LocalDate parseTimestampToLocalDate(String timestamp) {
        if (timestamp == null || timestamp.length() < 10) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        }
        // Extract date portion "YYYY-MM-DD" from timestamp
        return LocalDate.parse(timestamp.substring(0, 10));
    }

    /**
     * Saves FlexStatementResponse metadata to database (transactional).
     *
     * Creates a persistence record of FLEX API request for audit trail,
     * converting timestamp String to LocalDate for database storage.
     *
     * @param response the FLEX API response containing reference code, timestamp, status, URL
     * @param reportType the report type ("TRADES" for options trades, "NAV" for net asset value)
     */
    @Transactional
    public void saveFlexStatementResponse(FlexStatementResponse response, String reportType) {
        try {
            FlexStatementResponseEntity entity = new FlexStatementResponseEntity();
            entity.setReferenceCode(response.getReferenceCode());
            entity.setRequestDate(parseTimestampToLocalDate(response.getTimestamp()));
            entity.setStatus(response.getStatus());
            entity.setUrl(response.getUrl());
            entity.setReportType(reportType);
            entity.setOriginalTimestamp(response.getTimestamp());

            flexStatementResponseRepository.save(entity);
            log.info("Saved FlexStatementResponse to database: referenceCode={}, reportType={}, requestDate={}",
                    entity.getReferenceCode(), entity.getReportType(), entity.getRequestDate());
        } catch (Exception e) {
            // Log error but don't fail the import process
            log.error("Failed to save FlexStatementResponse to database: {}", e.getMessage(), e);
        }
    }
}
