package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    @Query("SELECT i " +
            "FROM InstrumentEntity i " +
            "ORDER BY i.ticker ASC")
    public List<InstrumentEntity> findAllInstrument();

    public InstrumentEntity findByTicker(String ticker);
}
