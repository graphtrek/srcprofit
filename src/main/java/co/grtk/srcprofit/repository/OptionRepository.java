package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {

    @Query("SELECT o " +
            "FROM OptionEntity o " +
            "JOIN FETCH o.instrument " +
            "ORDER BY o.tradeDate DESC, o.ticker ASC, o.status ASC")
    List<OptionEntity> findAllWithInstrument();

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "LEFT JOIN OptionEntity o2 " +
            "ON o1.conid = o2.conid AND o2.status = 'CLOSED' " +
            "WHERE o1.status = 'OPEN' AND o2.conid is NULL " +
            "ORDER BY o1.conid DESC, o1.status ASC, o1.tradeDate DESC")
    List<OptionEntity> findAllOpen();

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "LEFT JOIN OptionEntity o2 " +
            "ON o1.conid = o2.conid AND o2.status = 'CLOSED' " +
            "WHERE o1.status = 'OPEN' AND o1.tradeDate >= :tradeDate AND o2.conid is NULL " +
            "ORDER BY o1.conid DESC, o1.status ASC, o1.tradeDate DESC")
    List<OptionEntity> findAllOpenFromTradeDate(@Param("tradeDate") LocalDate tradeDate);

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "LEFT JOIN OptionEntity o2 " +
            "ON o1.conid = o2.conid AND o2.status = 'CLOSED' " +
            "WHERE o1.status = 'OPEN' AND o2.conid is NULL AND i.ticker = :ticker " +
            "ORDER BY o1.conid DESC, o1.status ASC, o1.tradeDate DESC")
    List<OptionEntity> findAllOpenByTicker(@Param("ticker") String ticker);

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "WHERE o1.conid IN (" +
            " SELECT o2.conid FROM OptionEntity o2 " +
            " GROUP by o2.conid " +
            " HAVING " +
            "   SUM(CASE WHEN o2.status = 'OPEN' THEN 1 ELSE 0 END) > 0 " +
            "   AND " +
            "   SUM(CASE WHEN o2.status = 'CLOSED' THEN 1 ELSE 0 END) > 0) "
    )
    List<OptionEntity> findAllClosed();

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "WHERE o1.tradeDate >= :tradeDate AND o1.conid IN (" +
            " SELECT o2.conid FROM OptionEntity o2 " +
            " GROUP by o2.conid " +
            " HAVING " +
            "   SUM(CASE WHEN o2.status = 'OPEN' THEN 1 ELSE 0 END) > 0 " +
            "   AND " +
            "   SUM(CASE WHEN o2.status = 'CLOSED' THEN 1 ELSE 0 END) > 0) "
    )
    List<OptionEntity> findAllClosedFromTradeDate(@Param("tradeDate") LocalDate tradeDate);

    @Query("SELECT o1 " +
            "FROM OptionEntity o1 " +
            "JOIN FETCH o1.instrument i " +
            "WHERE o1.ticker = :ticker AND o1.conid IN (" +
            " SELECT o2.conid FROM OptionEntity o2 " +
            " WHERE o2.ticker = :ticker " +
            " GROUP by o2.conid " +
            " HAVING " +
            "   SUM(CASE WHEN o2.status = 'OPEN' THEN 1 ELSE 0 END) > 0 " +
            "   AND " +
            "   SUM(CASE WHEN o2.status = 'CLOSED' THEN 1 ELSE 0 END) > 0) "
    )
    List<OptionEntity> findAllClosedByTicker(@Param("ticker") String ticker);

    @Query("SELECT o " +
            "FROM OptionEntity o " +
            "JOIN FETCH o.instrument i " +
            "WHERE i.ticker = :ticker " +
            "ORDER BY  o.code DESC, o.status ASC, o.tradeDate DESC")
    List<OptionEntity> findAllWithInstrumentByTicker(@Param("ticker") String ticker);

    @Query("SELECT o " +
            "FROM OptionEntity o " +
            "WHERE o.code = :code AND o.status = :status")
    OptionEntity findByCodeAndStatus(@Param("code") String code, @Param("status") OptionStatus status);

    @Query("SELECT o " +
            "FROM OptionEntity o " +
            "WHERE o.conid = :conid AND o.status = :status")
    OptionEntity findByConidAndStatus(@Param("conid") Long conid, @Param("status") OptionStatus status);

    List<OptionEntity> findByInstrumentTicker(String ticker, Sort sort);
}