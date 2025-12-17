package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for OptionSnapshotEntity.
 *
 * Provides data access methods for querying and managing option snapshots.
 * Snapshots are keyed by OCC symbol (unique identifier).
 */
@Repository
public interface OptionSnapshotRepository extends JpaRepository<OptionSnapshotEntity, Long> {

    /**
     * Find an option snapshot by OCC symbol.
     *
     * @param symbol OCC symbol, e.g., "AAPL230120C00150000"
     * @return Optional containing the snapshot if found
     */
    Optional<OptionSnapshotEntity> findBySymbol(String symbol);

    /**
     * Find all snapshots for a specific instrument.
     *
     * @param instrument The instrument entity
     * @return List of snapshots for the instrument
     */
    List<OptionSnapshotEntity> findByInstrument(InstrumentEntity instrument);

    /**
     * Find all snapshots that expire before a specific date.
     *
     * Used for identifying expired contracts to delete.
     *
     * @param expirationDate The cutoff date
     * @return List of expired snapshots
     */
    List<OptionSnapshotEntity> findByExpirationDateBefore(LocalDate expirationDate);

    /**
     * Delete all snapshots that expire before a specific date.
     *
     * @param expirationDate The cutoff date
     * @return Number of deleted snapshots
     */
    @Modifying
    int deleteByExpirationDateBefore(LocalDate expirationDate);

    /**
     * Count total snapshots for a specific instrument.
     *
     * @param instrument The instrument entity
     * @return Total count
     */
    long countByInstrument(InstrumentEntity instrument);

    /**
     * Find snapshots for an instrument within an expiration date range.
     *
     * Useful for finding options expiring within a specific window
     * (e.g., 30-90 days out).
     *
     * @param instrument The instrument entity
     * @param dateGte Minimum expiration date (inclusive)
     * @param dateLte Maximum expiration date (inclusive)
     * @return Sorted list ordered by expiration date, then strike price
     */
    @Query("SELECT o FROM OptionSnapshotEntity o " +
           "WHERE o.instrument = :instrument " +
           "AND o.expirationDate >= :dateGte " +
           "AND o.expirationDate <= :dateLte " +
           "ORDER BY o.expirationDate ASC, o.strikePrice ASC")
    List<OptionSnapshotEntity> findByInstrumentAndExpirationRange(
            @Param("instrument") InstrumentEntity instrument,
            @Param("dateGte") LocalDate dateGte,
            @Param("dateLte") LocalDate dateLte
    );

    /**
     * Find snapshots for an instrument with specific option type.
     *
     * @param instrument The instrument entity
     * @param optionType "call" or "put"
     * @return List of snapshots matching the type
     */
    List<OptionSnapshotEntity> findByInstrumentAndOptionType(
            InstrumentEntity instrument,
            String optionType
    );
}
