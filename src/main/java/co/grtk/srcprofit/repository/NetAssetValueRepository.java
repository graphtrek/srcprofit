package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.NetAssetValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NetAssetValueRepository extends JpaRepository<NetAssetValueEntity, Long> {
    NetAssetValueEntity findByReportDate(LocalDate reportDate);

    NetAssetValueEntity findTopByOrderByReportDateDesc();

    @Query("SELECT n " +
            "FROM NetAssetValueEntity n " +
            "WHERE n.reportDate BETWEEN :startDate AND :endDate " +
            "ORDER BY n.reportDate DESC")
    List<NetAssetValueEntity> findBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}