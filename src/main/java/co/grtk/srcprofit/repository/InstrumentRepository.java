package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {
    public InstrumentEntity findByTicker(String ticker);
}
