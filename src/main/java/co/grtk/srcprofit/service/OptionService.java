package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.OptionDto;
import co.grtk.srcprofit.dto.OptionTreeDto;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.repository.OptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OptionService {
    Logger log = LoggerFactory.getLogger(OptionService.class);

    private final OptionRepository optionRepository;
    private final ObjectMapper objectMapper;

    public OptionService(OptionRepository optionRepository, ObjectMapper objectMapper) {
        this.optionRepository = optionRepository;
        this.objectMapper = objectMapper;
    }

    public static OptionTreeDto toTreeDto(OptionEntity optionEntity) {
        List<OptionTreeDto> children = optionEntity.getChildren().stream()
                .map(OptionService::toTreeDto)
                .toList();
        return new OptionTreeDto(optionEntity.getId(), optionEntity.getSymbol(), children);
    }

    public List<OptionTreeDto> getOptionTree() {
        List<OptionEntity> roots = optionRepository.findByParentIsNull();
        return roots.stream().map(OptionService::toTreeDto).toList();
    }

    public OptionDto getOptionById(Long id) {
        log.info("getOptionById {}", id);
        OptionEntity optionEntity = optionRepository.findById(id).orElse(null);
        return objectMapper.convertValue(optionEntity, OptionDto.class);
    }

    public List<OptionDto> getOptionsBySymbol(String  symbol) {
        List<OptionEntity> optionEntities = optionRepository.findBySymbol(symbol, Sort.by(Sort.Direction.ASC, "expirationDate"));
        return Optional.of(optionEntities)
                .map(entities -> objectMapper.convertValue(entities, new TypeReference<List<OptionDto>>() {}))
                .orElse(Collections.emptyList());
    }

    public List<OptionDto> getOptions() {
        List<OptionEntity> optionEntities = optionRepository.findAll(Sort.by(Sort.Direction.DESC, "expirationDate"));
        return Optional.of(optionEntities)
                .map(entities -> objectMapper.convertValue(entities, new TypeReference<List<OptionDto>>() {}))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public OptionDto saveOption(OptionDto optionDto) {
        log.info("Saving option {}", optionDto);
        OptionEntity optionEntity = objectMapper.convertValue(optionDto, OptionEntity.class);
        optionEntity.setType(
                Optional.ofNullable(optionEntity.getType())
                        .orElse(OptionType.PUT)
        );
        optionEntity.setStatus(
                Optional.ofNullable(optionEntity.getStatus())
                        .orElse(OptionStatus.PENDING)
        );
        optionEntity = optionRepository.save(optionEntity);
        optionDto = objectMapper.convertValue(optionEntity, OptionDto.class);

        return optionDto;
    }

}