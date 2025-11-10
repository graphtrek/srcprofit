package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    @Query("SELECT i " +
            "FROM InstrumentEntity i " +
            "ORDER BY i.ticker ASC")
    List<InstrumentEntity> findAllInstrument();

    @Query("SELECT i FROM InstrumentEntity i WHERE i.ticker IN (:symbols)")
    List<InstrumentEntity> findByTickers(@Param("symbols") List<String> symbols);

    InstrumentEntity findByTicker(@Param("ticker") String ticker);

    /**
     * Find all instruments with stale Alpaca metadata.
     *
     * Returns instruments where metadata was last updated before the given threshold
     * or has never been updated (null timestamp). Results are ordered by staleness
     * (oldest first) to prioritize the most out-of-date instruments.
     *
     * @param threshold The cutoff instant; instruments updated before this are considered stale
     * @return List of stale instruments ordered by update time (oldest first)
     */
    @Query("SELECT i FROM InstrumentEntity i " +
            "WHERE i.alpacaMetadataUpdatedAt < :threshold OR i.alpacaMetadataUpdatedAt IS NULL " +
            "ORDER BY i.alpacaMetadataUpdatedAt ASC NULLS FIRST")
    List<InstrumentEntity> findStaleAlpacaAssets(@Param("threshold") Instant threshold);
}
