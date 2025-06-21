package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.dto.OptionDto;
import co.grtk.srcprofit.entity.OptionEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {

    @Query("SELECT o FROM OptionEntity o JOIN FETCH o.instrument ORDER BY o.tradeDateTime DESC")
    List<OptionEntity> findAllWithInstrument();

    @Query("SELECT o " +
            "FROM OptionEntity o JOIN o.instrument i " +
            "WHERE i.ticker = :ticker " +
            "ORDER BY o.tradeDateTime DESC")
    List<OptionEntity> findAllWithInstrumentByTicker(@Param("ticker") String ticker);

    List<OptionEntity> findByInstrumentTicker(String ticker, Sort sort);
}