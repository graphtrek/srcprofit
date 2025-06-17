package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.IbkrInstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IbkrInstrumentRepository extends JpaRepository<IbkrInstrumentEntity, Long> {
}
