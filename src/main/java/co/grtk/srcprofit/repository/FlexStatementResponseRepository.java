package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.FlexStatementResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for FlexStatementResponseEntity.
 *
 * Provides query methods for tracking FLEX report import history,
 * including lookups by reference code, report type, and date.
 *
 * @see FlexStatementResponseEntity
 * @see co.grtk.srcprofit.service.FlexReportsService
 */
@Repository
public interface FlexStatementResponseRepository extends JpaRepository<FlexStatementResponseEntity, Long> {

    /**
     * Find FLEX statement response by unique reference code.
     *
     * @param referenceCode the reference code from IBKR FLEX API
     * @return the entity, or null if not found
     */
    FlexStatementResponseEntity findByReferenceCode(String referenceCode);

    /**
     * Find all FLEX statement responses by report type.
     *
     * @param reportType the report type ("TRADES" or "NAV")
     * @return list of entities matching the report type
     */
    List<FlexStatementResponseEntity> findByReportType(String reportType);

    /**
     * Find the most recent FLEX statement response for a given report type.
     *
     * @param reportType the report type ("TRADES" or "NAV")
     * @return the most recent entity, or null if none exist
     */
    FlexStatementResponseEntity findTopByReportTypeOrderByRequestDateDesc(String reportType);

    /**
     * Find all FLEX statement responses requested on a specific date.
     *
     * @param requestDate the request date
     * @return list of entities matching the request date
     */
    List<FlexStatementResponseEntity> findByRequestDate(LocalDate requestDate);
}
