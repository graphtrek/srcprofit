package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.ChartDataDto;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.mapper.Interval;
import co.grtk.srcprofit.mapper.MapperUtils;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;
import static co.grtk.srcprofit.mapper.PositionMapper.calculateAndSetAnnualizedRoi;
import static java.lang.Math.abs;
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

    private List<PositionDto> getPositionDtos(List<OptionEntity> optionEntities) {
        return optionEntities.stream()
                .map(entity -> {
                    InstrumentEntity instrumentEntity = entity.getInstrument();
                    PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                    positionDto.setEarningDate(instrumentEntity.getEarningDate());
                    positionDto.setTicker(instrumentEntity.getTicker());
                    if(entity.getTradePrice() != null && entity.getMarketPrice() != null)
                        positionDto.setUnRealizedProfitOrLoss(round2Digits(entity.getTradePrice() - entity.getMarketPrice()));
                    if (OptionStatus.OPEN.equals(entity.getStatus())) {
                        positionDto.setMarketValue(instrumentEntity.getOptionPrice());
                    }
                    return positionDto;
                }).toList();
    }

    private List<PositionDto> getClosedPositionDtos(List<OptionEntity> optionEntities) {
        return optionEntities.stream()
                .map(entity -> {
                    InstrumentEntity instrumentEntity = entity.getInstrument();
                    PositionDto positionDto = objectMapper.convertValue(entity, PositionDto.class);
                    positionDto.setEarningDate(instrumentEntity.getEarningDate());
                    positionDto.setTicker(instrumentEntity.getTicker());
                    positionDto.setMarketValue(instrumentEntity.getOptionPrice());
                    return positionDto;
                }).toList();
    }

    public List<OptionEntity> getAllOpenOptions(LocalDate startDate) {
        List<OptionEntity> optionEntities;
        if (Objects.isNull(startDate))
            optionEntities = optionRepository.findAllOpen(LocalDate.now());
        else
            optionEntities = optionRepository.findAllOpenFromTradeDate(startDate);

        return optionEntities;
    }

    public List<PositionDto> getAllOpenPositions(LocalDate startDate) {
        List<OptionEntity> optionEntities = getAllOpenOptions(startDate);
        return getPositionDtos(optionEntities);
    }

    public List<PositionDto> getAllClosedOptions(LocalDate startDate) {
        List<OptionEntity> optionEntities;
        if (Objects.isNull(startDate))
            optionEntities = optionRepository.findAllClosed();
        else
            optionEntities = optionRepository.findAllClosedFromTradeDate(startDate);
        return getClosedPositionDtos(optionEntities);
    }

    public List<PositionDto> getOpenOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllOpenByTicker(ticker);
        return getPositionDtos(optionEntities);
    }

    public List<PositionDto> getClosedOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllClosedByTicker(ticker);
        return getClosedPositionDtos(optionEntities);
    }

    public List<PositionDto> getWeeklyOpenPositions(List<PositionDto> openPositions) {
        Map<LocalDate, List<PositionDto>> grouped = openPositions.stream()
                .filter(optionEntity -> optionEntity.getType().equals(OptionType.PUT))
                .collect(Collectors.groupingBy(PositionDto::getExpirationDate));

        List<PositionDto> weeklyOpenPositions = new ArrayList<>();
        grouped.forEach((date, posList) -> {
            log.info("Expiration: {}", date);
            PositionDto positionDto = new PositionDto();
            calculatePosition(positionDto,posList, Collections.emptyList());
            weeklyOpenPositions.add(positionDto);
//            posList.forEach(o -> {
//                log.info("  Value: {}", o.getPositionValue());
//                positionDto.setPositionValue(positionDto.getPositionValue() + o.getPositionValue());
//                positionDto.setPut(positionDto.getPut() + o.getPositionValue());
//            });
        });
        weeklyOpenPositions.sort(Comparator.comparing(PositionDto::getExpirationDate));
        return weeklyOpenPositions;
    }

    public void getDailyPremium(ChartDataDto chartDataDto) {
        if(Interval.ALL.equals(chartDataDto.getInterval()))
            chartDataDto.setDailyPremium(getDailyPremium());
        else
            chartDataDto.setDailyPremium(getDailyPremium(chartDataDto.getStartDate(), chartDataDto.getEndDate()));
    }

    public Map<LocalDate, BigDecimal> getDailyPremium(LocalDate startDate, LocalDate endDate) {
        List<OptionEntity> options =  optionRepository.findOptionsBetweenDates(startDate, endDate);
        return getDailyPremium(options);
    }

    public Map<LocalDate, BigDecimal> getDailyPremium() {
        List<OptionEntity> options =  optionRepository.findAll();
        return getDailyPremium(options);
    }

    private Map<LocalDate, BigDecimal> getDailyPremium( List<OptionEntity> options) {
        Map<LocalDate, BigDecimal> dailyPremium = options.stream()
                .collect(Collectors.groupingBy(
                        OptionEntity::getTradeDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                option -> BigDecimal.valueOf(option.getTradePrice() * abs(option.getQuantity())),
                                BigDecimal::add
                        )
                ));

        List<LocalDate> sortedDates = dailyPremium.keySet().stream()
                .sorted()
                .toList();

        Map<LocalDate, BigDecimal> cumulativePremiumPerDay = new LinkedHashMap<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (LocalDate date : sortedDates) {
            runningTotal = runningTotal.add(dailyPremium.getOrDefault(date, BigDecimal.ZERO).setScale(2, RoundingMode.UP));
            cumulativePremiumPerDay.put(date, runningTotal);
        }

        log.info("getDailyPremium options:{}, runningTotal:{}, cumulativePremiumPerDay {}",
                options.size(),
                runningTotal,
                cumulativePremiumPerDay);
        return cumulativePremiumPerDay;
    }


    public void calculatePosition(PositionDto positionDto, List<PositionDto> openPositions, List<PositionDto> closedPositions) {

        double realizedProfitOrLoss = 0.0;
        double collectedPremium = 0.0;
        double marketValue = 0.0;

        double unRealizedProfitOrLoss = 0.0;
        double put = 0.0;
        double call = 0.0;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();
        double positionValue = 0.0;
        double marketPrice = 0.0;
        double putMarketPrice = 0.0;

        for (PositionDto dto : closedPositions) {
            int qty = abs(dto.getQuantity());
            if (dto.getTradeDate().isBefore(startDate)) {
                startDate = dto.getTradeDate();
            }
            if (dto.getExpirationDate().isAfter(endDate)) {
                endDate = dto.getExpirationDate();
            }
            collectedPremium += dto.getTradePrice() * qty;
            realizedProfitOrLoss += dto.getTradePrice() * qty;
        }
        positionDto.setRealizedProfitOrLoss(round2Digits(realizedProfitOrLoss));

        for (PositionDto dto : openPositions) {
            if (dto.getTradeDate().isBefore(startDate)) {
                startDate = dto.getTradeDate();
            }
            if (dto.getExpirationDate().isAfter(endDate)) {
                endDate = dto.getExpirationDate();
            }


            int qty = abs(dto.getQuantity());
            collectedPremium += dto.getTradePrice() * qty;
            unRealizedProfitOrLoss += dto.getTradePrice() * qty;

            if (OptionType.PUT.equals(dto.getType())) {
                put += dto.getTradePrice() * qty;
                putMarketPrice += dto.getMarketPrice();
                if (dto.getTradePrice() >= 0) { //PUT SELL
                    marketValue += dto.getMarketValue() * qty;
                    positionValue += dto.getPositionValue() * qty;
                }
            } else {
                call += dto.getTradePrice() * qty;
            }

            positionDto.setType(dto.getType());
            calculateAndSetAnnualizedRoi(dto);
            if(dto.getTradePrice() > 0) {
                marketPrice += (dto.getMarketPrice() * qty);
            } else {
                marketPrice += (-1 * dto.getMarketPrice() * qty);
            }
        }

        positionDto.setPut(round2Digits(put));
        positionDto.setCall(round2Digits(call));

        if (positionDto.getTradePrice() == null)
            positionDto.setTradePrice(round2Digits(unRealizedProfitOrLoss));

        if (positionDto.getUnRealizedProfitOrLoss() == null)
            positionDto.setUnRealizedProfitOrLoss(round2Digits(unRealizedProfitOrLoss));

        if (positionDto.getPositionValue() == null)
            positionDto.setPositionValue(round2Digits(positionValue));

        if (positionDto.getExpirationDate() == null)
            positionDto.setExpirationDate(endDate);

        if (positionDto.getTradeDate() == null)
            positionDto.setTradeDate(startDate);

        if (positionDto.getMarketValue() == null)
            positionDto.setMarketValue(marketValue);

        positionDto.setCollectedPremium(round2Digits(collectedPremium));

        calculateAndSetAnnualizedRoi(positionDto);
        positionDto.setMarketPrice(round2Digits(marketPrice));
        positionDto.setPutMarketPrice(round2Digits(putMarketPrice));

        positionDto.setCoveredPositionValue(round2Digits(put - putMarketPrice));
        double marketVsPositionsPercentage = ((marketValue / positionValue) * 100) - 100;
        positionDto.setMarketVsPositionsPercentage(round2Digits(marketVsPositionsPercentage));
        log.info("positionDto: {}", positionDto);
    }

    @Transactional
    public void saveOption(OptionEntity optionEntity) {
        log.debug("Saving option {}", optionEntity);
        optionRepository.save(optionEntity);
    }


    @Transactional
    public PositionDto savePosition(PositionDto positionDto) {
        log.debug("Saving postion {}", positionDto);

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
        if (instrumentEntity == null) {
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
    public int saveCSV(Path path) {
        int rowCount = 0;
        long start = System.currentTimeMillis();
        try (CSVParser csvRecords = parse(path, StandardCharsets.UTF_8,
                CSVFormat.Builder.create()
                        .setHeader()                   // első sor fejléc
                        .setSkipHeaderRecord(true)    // ne olvassa be újra a fejlécet
                        .setIgnoreHeaderCase(true)    // fejlécmezők kis/nagybetű érzéketlenek
                        .setTrim(true)                // whitespace-ek levágása
                        .get())) {
            for (CSVRecord csvRecord : csvRecords) {
                String account = csvRecord.get("ClientAccountID");
                String assetClass = csvRecord.get("AssetClass");
                String ticker = csvRecord.get("UnderlyingSymbol");
                String putCall = csvRecord.get("Put/Call");
                String status = csvRecord.get("Open/CloseIndicator");
                String tradeDate = csvRecord.get("TradeDate");
                String expirationDate = csvRecord.get("Expiry");
                String strike = csvRecord.get("Strike");
                int quantity = MapperUtils.parseInt(csvRecord.get("Quantity"),1);
                String underlyingConid = csvRecord.get("UnderlyingConid");
                Long conid = Long.parseLong(csvRecord.get("Conid"));
                String netCash = csvRecord.get("NetCash");
                String code = csvRecord.get("Symbol");
                String fifoPnlRealized = csvRecord.get("FifoPnlRealized");

                if (AssetClass.OPT.getCode().equals(assetClass) &&
                        Objects.nonNull(ticker) &&
                        Objects.nonNull(putCall) &&
                        Objects.nonNull(status)) {

                    if("LCID1".equals(ticker))
                        ticker = "LCID";

                    OptionStatus optionStatus = OptionStatus.PENDING;
                    if ("C".equals(status))
                        optionStatus = OptionStatus.CLOSED;
                    else
                        optionStatus = OptionStatus.OPEN;

                    double tradePrice = Math.round(Double.parseDouble(netCash) * 100.0) / 100.0;

                    log.debug("ticker: {}, optionStatus: {}, conid:{} qty:{}, tradePrice:{}", ticker, optionStatus, conid, quantity, tradePrice);
                    OptionEntity optionEntity = optionRepository.findByConidAndStatusAndTradePrice(conid, optionStatus,tradePrice);
                    if( optionEntity == null)
                        optionEntity = new OptionEntity();
                    else
                        continue;
                    optionEntity.setAccount(account);
                    optionEntity.setConid(conid);
                    optionEntity.setStatus(optionStatus);
                    optionEntity.setAssetClass(AssetClass.OPT);

                    if ("C".equals(putCall))
                        optionEntity.setType(OptionType.CALL);
                    else
                        optionEntity.setType(OptionType.PUT);

                    optionEntity.setTradeDate(LocalDate.parse(tradeDate));
                    optionEntity.setExpirationDate(LocalDate.parse(expirationDate));
                    optionEntity.setPositionValue(Double.parseDouble(strike) * 100);
                    optionEntity.setQuantity(quantity);

                    InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(ticker);
                    if ((instrumentEntity == null || instrumentEntity.getId() == null)) {
                        instrumentEntity = new InstrumentEntity();
                        instrumentEntity.setTicker(ticker);
                        instrumentEntity.setConid(Long.parseLong(underlyingConid));
                        instrumentRepository.save(instrumentEntity);
                    }

                    optionEntity.setTicker(ticker);
                    optionEntity.setCode(code);

                    double pnl = Math.round(Double.parseDouble(fifoPnlRealized) * 100.0) / 100.0;
                    optionEntity.setRealizedProfitOrLoss(pnl);


                    optionEntity.setTradePrice(tradePrice);
                    optionEntity.setInstrument(instrumentEntity);

                    double marketValue = Math.round(((Double.parseDouble(strike) * 100) + tradePrice) * 100.0) / 100.0;
                    optionEntity.setMarketValue(marketValue);

                    int daysBetween =
                            (int) ChronoUnit.DAYS.between(optionEntity.getTradeDate(), optionEntity.getExpirationDate()
                                    .plusDays(1)
                                    .atStartOfDay());
                    optionEntity.setDaysBetween(daysBetween);

                    int daysLeft =
                            (int) ChronoUnit.DAYS.between(LocalDateTime.now(), optionEntity.getExpirationDate()
                                    .plusDays(1)
                                    .atStartOfDay());
                    optionEntity.setDaysLeft(daysLeft);

                    optionRepository.save(optionEntity);
                    log.info(csvRecord.toString());
                    rowCount++;
                }
            }
            long end = System.currentTimeMillis();
            int elapsedSeconds = (int) ((end - start) / 1000.0);
            int recordPerSecond = (int) (csvRecords.getRecordNumber() / elapsedSeconds);
            log.info("CSV file parsed in {} sec, records: {} recordPerSecond: {}", elapsedSeconds, csvRecords.getRecordNumber(), recordPerSecond);
            return rowCount;
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse CSV file: " + e.getMessage(),e);
        }
    }

    @Transactional
    public int dataFix(){
        int rowCount = 0;
        List<OptionEntity> options =  optionRepository.findAll();
        List<OptionEntity> closedOptions = optionRepository.findAllClosed();
        List<OptionEntity> openOptions = optionRepository.findAllOpen(LocalDate.now());

        for (OptionEntity optionEntity : options) {
            boolean toDelete = false;
            List<OptionEntity> closedPairs = closedOptions.stream().filter(o -> o.getConid().equals(optionEntity.getConid())).toList();
            List<OptionEntity> openPairs = openOptions.stream().filter(o -> o.getConid().equals(optionEntity.getConid())).toList();

            if (closedPairs.isEmpty() && openPairs.isEmpty())
                toDelete = true;

            if(toDelete) {
                log.warn("DataFix no pairs found for id:{}, symbol:{} conid:{} code:{} status:{} price:{} qty:{}",
                        optionEntity.getId(),
                        optionEntity.getTicker(),
                        optionEntity.getConid(),
                        optionEntity.getCode(),
                        optionEntity.getStatus(),
                        optionEntity.getTradePrice(),
                        optionEntity.getQuantity());
                optionRepository.delete(optionEntity);
                rowCount++;
            }
        }
        return rowCount;
    }
}