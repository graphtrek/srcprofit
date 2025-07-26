package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.EarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EarningRepository extends JpaRepository<EarningEntity, Long> {
    @Query("SELECT e " +
            "FROM EarningEntity e " +
            "WHERE e.symbol = :symbol AND e.reportDate = :reportDate AND e.fiscalDateEnding = :fiscalDateEnding")
    EarningEntity findBySymbolAndReportDateAndFiscalDateEnding(@Param("symbol") String symbol, @Param("reportDate")LocalDate reportDate, @Param("fiscalDateEnding")LocalDate fiscalDateEnding);

    @Query("SELECT e " +
            "FROM EarningEntity e " +
            "WHERE e.symbol = :symbol ORDER BY e.reportDate ASC")
    List<EarningEntity> findAllBySymbol(@Param("symbol") String symbol);

}
