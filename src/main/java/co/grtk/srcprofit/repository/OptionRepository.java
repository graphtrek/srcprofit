package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {
    List<OptionEntity> findByParentIsNull(); // csak gyökér kategóriák
}