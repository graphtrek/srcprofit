package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.PositionDto;
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
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static co.grtk.srcprofit.mapper.PositionMapper.calculateAndSetAnnualizedRoi;
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


    public PositionDto getOptionById(Long id) {
        log.info("getOptionById {}", id);
        OptionEntity optionEntity = optionRepository.findById(id).orElse(null);
        PositionDto positionDto = objectMapper.convertValue(optionEntity, PositionDto.class);
        positionDto.setTicker(optionEntity.getInstrument().getTicker());
        return positionDto;
    }

    public List<PositionDto> getOpenOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllOpenByTicker(ticker);
        return optionEntities.stream()
                .map(entity -> {
                            PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                            positionDto.setTicker(entity.getInstrument().getTicker());
                            return positionDto;
                        }

                ).toList();
    }

    public List<PositionDto> getClosedOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllClosedByTicker(ticker);
        return optionEntities.stream()
                .map(entity -> {
                            PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                            positionDto.setTicker(entity.getInstrument().getTicker());
                            return positionDto;
                        }

                ).toList();
    }
    
    
    public void calculatePosition(PositionDto positionDto,List<PositionDto> openPositions , List<PositionDto> closedPositions) {
        Double realizedProfitOrLoss = 0.0;
        Double collectedPremium = 0.0;
        for (PositionDto dto : closedPositions) {
            collectedPremium += dto.getTradePrice();
            if(OptionType.PUT.equals(dto.getType()))
                realizedProfitOrLoss += dto.getTradePrice();
        }
        positionDto.setRealizedProfitOrLoss(Math.round(realizedProfitOrLoss * 100.0) / 100.0);

        Double unRealizedProfitOrLoss = 0.0;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();

        Double positionValue = 0.0;
        for (PositionDto dto : openPositions) {
            if(dto.getTradeDate().isBefore(startDate)) {
                startDate = dto.getTradeDate();
            }
            if(dto.getExpirationDate().isAfter(endDate)) {
                endDate = dto.getExpirationDate();
            }

            collectedPremium += dto.getTradePrice();

            if(OptionType.PUT.equals(dto.getType())) {
                unRealizedProfitOrLoss += dto.getTradePrice();
                if (dto.getTradePrice() >= 0) {
                    positionValue += dto.getPositionValue();
                    log.info("ticker: {}, positionValue: {}", dto.getTicker(), dto.getPositionValue());
                    //unRealizedProfitOrLoss += dto.getTradePrice();
                } else {
                    //positionValue -= dto.getPositionValue();
                    //unRealizedProfitOrLoss -= dto.getTradePrice();
                }
            } else if(positionValue >= 0){
               // positionValue -= dto.getTradePrice();
            }
        }

        positionDto.setCollectedPremium(Math.round(collectedPremium * 100.0) / 100.0);

        if(positionDto.getUnRealizedProfitOrLoss() == null)
            positionDto.setUnRealizedProfitOrLoss(Math.round(unRealizedProfitOrLoss * 100.0) / 100.0);

        if(positionDto.getTradePrice() == null)
            positionDto.setTradePrice(Math.round(unRealizedProfitOrLoss * 100.0) / 100.0);

        if(positionDto.getPositionValue() == null)
            positionDto.setPositionValue(Math.round(positionValue * 100.0) / 100.0);

        if(positionDto.getExpirationDate() == null)
            positionDto.setExpirationDate(endDate);

        if(positionDto.getTradeDate() == null)
            positionDto.setTradeDate(startDate);

        calculateAndSetAnnualizedRoi(positionDto);
    }

    public List<PositionDto> getAllOpenOptions(LocalDate startDate) {
        List<OptionEntity> optionEntities;
        if(Objects.isNull(startDate))
            optionEntities = optionRepository.findAllOpen();
        else
            optionEntities = optionRepository.findAllOpenFromTradeDate(startDate);

        return optionEntities.stream()
                .map(entity -> {
                        String ticker = entity.getInstrument().getTicker();
                        PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                        positionDto.setTicker(ticker);
                        return positionDto;
                        }

                ).toList();
    }

    public List<PositionDto> getAllClosedOptions(LocalDate startDate) {
        List<OptionEntity> optionEntities;

        if(Objects.isNull(startDate))
           optionEntities = optionRepository.findAllClosed();
        else
            optionEntities = optionRepository.findAllClosedFromTradeDate(startDate);

        return optionEntities.stream()
                .map(entity -> {
                            String ticker = entity.getInstrument().getTicker();
                            PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                            positionDto.setTicker(ticker);
                            return positionDto;
                        }

                ).toList();
    }
    @Transactional
    public PositionDto saveOption(PositionDto positionDto) {
        log.info("Saving option {}", positionDto);
        OptionEntity optionEntity = objectMapper.convertValue(positionDto, OptionEntity.class);
        optionEntity.setType(
                Optional.ofNullable(optionEntity.getType())
                        .orElse(OptionType.PUT)
        );
        optionEntity.setStatus(
                Optional.ofNullable(optionEntity.getStatus())
                        .orElse(OptionStatus.PENDING)
        );

        optionEntity.setAssetClass(AssetClass.OPT);

        InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(positionDto.getTicker());
        if(instrumentEntity == null) {
            instrumentEntity = new InstrumentEntity();
            instrumentEntity.setTicker(positionDto.getTicker());
            instrumentRepository.save(instrumentEntity);
        }

        optionEntity.setInstrument(instrumentEntity);
        optionEntity = optionRepository.save(optionEntity);
        positionDto = objectMapper.convertValue(optionEntity, PositionDto.class);
        positionDto.setTicker(optionEntity.getInstrument().getTicker());

        return positionDto;
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
            for (CSVRecord csvRecord : csvRecords) {
                 //csvRecords.forEach(csvRecord -> {
                String assetClass = csvRecord.get("AssetClass");
                String ticker = csvRecord.get("UnderlyingSymbol");
                String putCall = csvRecord.get("Put/Call");
                String status = csvRecord.get("Open/CloseIndicator");
                String tradeDate = csvRecord.get("TradeDate");
                String expirationDate = csvRecord.get("Expiry");
                String strike = csvRecord.get("Strike");
                String quantity = csvRecord.get("Quantity");
                String underlyingConid = csvRecord.get("UnderlyingConid");
                Long conid = Long.parseLong(csvRecord.get("Conid"));
                String netCash = csvRecord.get("NetCash");
                String code = csvRecord.get("Symbol");
                String fifoPnlRealized = csvRecord.get("FifoPnlRealized");

                if(AssetClass.OPT.getCode().equals(assetClass) &&
                        Objects.nonNull(ticker) &&
                        Objects.nonNull(putCall) &&
                        Objects.nonNull(status)) {

                    OptionStatus optionStatus = OptionStatus.PENDING;
                    if("C".equals(status))
                        optionStatus = OptionStatus.CLOSED;
                    else
                       optionStatus = OptionStatus.OPEN;

                    OptionEntity optionEntity = optionRepository.findByConidAndStatus(conid, optionStatus);
                    if(optionEntity != null)
                        continue;

                    optionEntity = new OptionEntity();
                    optionEntity.setConid(conid);
                    optionEntity.setStatus(optionStatus);
                    optionEntity.setAssetClass(AssetClass.OPT);

                    if("C".equals(putCall))
                        optionEntity.setType(OptionType.CALL);
                    else
                        optionEntity.setType(OptionType.PUT);

                    optionEntity.setTradeDate(LocalDate.parse(tradeDate));
                    optionEntity.setExpirationDate(LocalDate.parse(expirationDate));
                    optionEntity.setPositionValue(Double.parseDouble(strike) * 100);
                    optionEntity.setQuantity(Integer.parseInt(quantity));

                    InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(ticker);
                    if(instrumentEntity == null || instrumentEntity.getId() == null) {
                        instrumentEntity = new InstrumentEntity();
                        instrumentEntity.setTicker(ticker);
                        instrumentEntity.setConid(Long.parseLong(underlyingConid));
                        instrumentRepository.save(instrumentEntity);
                    }


                    optionEntity.setTicker(ticker);
                    optionEntity.setCode(code);

                    double pnl = Math.round(Double.parseDouble(fifoPnlRealized) * 100.0) / 100.0;
                    optionEntity.setRealizedProfitOrLoss(pnl);

                    double tradePrice = Math.round(Double.parseDouble(netCash) * 100.0) / 100.0;
                    optionEntity.setTradePrice(tradePrice);
                    optionEntity.setInstrument(instrumentEntity);

                    double marketValue = Math.round(((Double.parseDouble(strike) * 100) + tradePrice)  * 100.0) / 100.0;
                    optionEntity.setMarketValue(marketValue);

                    int daysBetween =
                            (int) ChronoUnit.DAYS.between(optionEntity.getTradeDate(),optionEntity.getExpirationDate()
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
            }//);

        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

}