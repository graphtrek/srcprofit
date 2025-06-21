package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.OptionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.csv.CSVParser.parse;

@Service
public class OptionService {
    private final OptionRepository optionRepository;
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    Logger log = LoggerFactory.getLogger(OptionService.class);

    public OptionService(OptionRepository optionRepository, InstrumentRepository instrumentRepository, ObjectMapper objectMapper) {
        this.optionRepository = optionRepository;
        this.objectMapper = objectMapper;
        this.instrumentRepository = instrumentRepository;
    }


    public OptionDto getOptionById(Long id) {
        log.info("getOptionById {}", id);
        OptionEntity optionEntity = optionRepository.findById(id).orElse(null);
        OptionDto optionDto = objectMapper.convertValue(optionEntity, OptionDto.class);
        optionDto.setTicker(optionEntity.getInstrument().getTicker());
        return optionDto;
    }

    public List<OptionDto> getOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllWithInstrumentByTicker(ticker);
        return optionEntities.stream()
                .map(entity -> {
                            OptionDto optionDto = objectMapper.convertValue(entity, OptionDto.class);
                            optionDto.setTicker(entity.getInstrument().getTicker());
                            return optionDto;
                        }

                ).toList();
    }

    public List<OptionDto> getOptions() {
        List<OptionEntity> optionEntities = optionRepository.findAllWithInstrument();
        return optionEntities.stream()
                .map(entity -> {
                        String ticker = entity.getInstrument().getTicker();
                        OptionDto optionDto = objectMapper.convertValue(entity, OptionDto.class);
                        optionDto.setTicker(ticker);
                        return optionDto;
                        }

                ).toList();
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

        optionEntity.setAssetClass(AssetClass.OPT);

        InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(optionDto.getTicker());
        if(instrumentEntity == null) {
            instrumentEntity = new InstrumentEntity();
            instrumentEntity.setTicker(optionDto.getTicker());
            instrumentRepository.save(instrumentEntity);
        }

        optionEntity.setInstrument(instrumentEntity);
        optionEntity = optionRepository.save(optionEntity);
        optionDto = objectMapper.convertValue(optionEntity, OptionDto.class);
        optionDto.setTicker(optionEntity.getInstrument().getTicker());

        return optionDto;
    }

    @Transactional
    public void csvToOptions(Path path) {
        try( CSVParser csvRecords = parse(path, StandardCharsets.UTF_8,
                CSVFormat.Builder.create()
                        .setHeader()                   // első sor fejléc
                        .setSkipHeaderRecord(true)    // ne olvassa be újra a fejlécet
                        .setIgnoreHeaderCase(true)    // fejlécmezők kis/nagybetű érzéketlenek
                        .setTrim(true)                // whitespace-ek levágása
                        .get())) {

            csvRecords.forEach(csvRecord -> {
                String assetClass = csvRecord.get("AssetClass");
                String symbol = csvRecord.get("UnderlyingSymbol");
                String putCall = csvRecord.get("Put/Call");
                String status = csvRecord.get("Open/CloseIndicator");
                String tradeDate = csvRecord.get("TradeDate");
                String expirationDate = csvRecord.get("Expiry");
                String strike = csvRecord.get("Strike");
                String quantity = csvRecord.get("Quantity");
                String conid = csvRecord.get("UnderlyingConid");
                String netCash = csvRecord.get("NetCash");

                if(AssetClass.OPT.getCode().equals(assetClass) &&
                        Objects.nonNull(symbol) &&
                        Objects.nonNull(putCall) &&
                        Objects.nonNull(status)) {
                    OptionEntity optionEntity = new OptionEntity();
                    if("C".equals(putCall))
                        optionEntity.setType(OptionType.CALL);
                    else
                        optionEntity.setType(OptionType.PUT);
                    if("C".equals(status))
                        optionEntity.setStatus(OptionStatus.CLOSED);
                    else
                        optionEntity.setStatus(OptionStatus.OPEN);

                    optionEntity.setTradeDateTime(LocalDate.parse(tradeDate).atStartOfDay());
                    optionEntity.setExpirationDate(LocalDate.parse(expirationDate));
                    optionEntity.setAssetClass(AssetClass.fromCode(assetClass));
                    optionEntity.setPositionValue(Double.parseDouble(strike) * 100);
                    optionEntity.setQuantity(Integer.parseInt(quantity));
                    InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(symbol);
                    if(instrumentEntity == null || instrumentEntity.getId() == null) {
                        instrumentEntity = new InstrumentEntity();
                        instrumentEntity.setTicker(symbol);
                        instrumentEntity.setConid(Long.parseLong(conid));
                        instrumentRepository.save(instrumentEntity);
                    }

                    double tradePrice = Math.round(Double.parseDouble(netCash) * 100.0) / 100.0;
                    optionEntity.setTradePrice(tradePrice);
                    optionEntity.setInstrument(instrumentEntity);

                    double marketValue = Math.round(((Double.parseDouble(strike) * 100) + tradePrice)  * 100.0) / 100.0;
                    optionEntity.setMarketValue(marketValue);

                    int daysBetween =
                            (int) ChronoUnit.DAYS.between(optionEntity.getTradeDateTime(),optionEntity.getExpirationDate()
                                    .plusDays(1)
                                    .atStartOfDay());
                    optionEntity.setDaysBetween(daysBetween);

                    int daysLeft =
                            (int) ChronoUnit.DAYS.between(LocalDateTime.now(),optionEntity.getExpirationDate()
                                    .plusDays(1)
                                    .atStartOfDay());
                    optionEntity.setDaysLeft(daysLeft);

                    optionRepository.save(optionEntity);
                    log.info(csvRecord.toString());
                }
            });

        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

}