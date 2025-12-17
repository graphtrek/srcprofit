package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.ChartDataDto;
import co.grtk.srcprofit.dto.CsvImportResult;
import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.mapper.Interval;
import co.grtk.srcprofit.mapper.MapperUtils;
import co.grtk.srcprofit.mapper.PositionCalculationHelper;
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
    private final VirtualPositionService virtualPositionService;
    Logger log = LoggerFactory.getLogger(OptionService.class);

    public OptionService(OptionRepository optionRepository, InstrumentRepository instrumentRepository, ObjectMapper objectMapper, VirtualPositionService virtualPositionService) {
        this.optionRepository = optionRepository;
        this.objectMapper = objectMapper;
        this.instrumentRepository = instrumentRepository;
        this.virtualPositionService = virtualPositionService;
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

    public List<PositionDto> getAllOpenOptionDtos(LocalDate startDate) {
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

    /**
     * Get open positions for a ticker, including any virtual position from the session.
     * Virtual positions are included in portfolio calculations (weighted ROI, probability, etc.).
     *
     * @param ticker the ticker symbol
     * @return list of open positions including virtual position if one exists for this ticker
     */
    public List<PositionDto> getOpenOptionsByTickerWithVirtual(String ticker) {
        // Use a mutable list to support adding virtual position
        List<PositionDto> openPositions = new ArrayList<>(getOpenOptionsByTicker(ticker));

        // Add virtual position if it exists for this ticker
        virtualPositionService.getVirtualPosition(ticker).ifPresent(virtualEntity -> {
            PositionDto virtualDto = objectMapper.convertValue(virtualEntity, PositionDto.class);
            virtualDto.setEarningDate(virtualEntity.getInstrument().getEarningDate());
            virtualDto.setTicker(ticker);
            virtualDto.setVirtual(true); // Mark as virtual for UI distinction
            openPositions.add(virtualDto);
        });

        return openPositions;
    }

    public List<PositionDto> getClosedOptionsByTicker(String ticker) {
        List<OptionEntity> optionEntities = optionRepository.findAllClosedByTicker(ticker);
        return getClosedPositionDtos(optionEntities);
    }

    public List<PositionDto> getWeeklyOpenOptionDtos(List<PositionDto> openPositions) {
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
                .filter(option -> option.getTradePrice() != null)
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

    /**
     * Calculate position-weighted ROI for portfolio.
     * Weight each position's ROI by its capital at risk (positionValue * quantity).
     * Formula: sum(roi * positionValue * quantity) / sum(positionValue * quantity)
     *
     * @param openPositions List of open positions
     * @return Weighted ROI percentage, or 0 if no valid positions
     */
    private double calculateWeightedROI(List<PositionDto> openPositions) {
        if (openPositions == null || openPositions.isEmpty()) {
            return 0.0;
        }

        double totalWeightedROI = 0.0;
        double totalCapitalAtRisk = 0.0;

        for (PositionDto position : openPositions) {
            Integer roi = position.getAnnualizedRoiPercent();
            Double posValue = position.getPositionValue();
            int qty = abs(position.getQuantity());

            if (roi != null && posValue != null && posValue > 0) {
                double capitalAtRisk = posValue * qty;
                totalWeightedROI += (roi * capitalAtRisk);
                totalCapitalAtRisk += capitalAtRisk;
            }
        }

        if (totalCapitalAtRisk == 0) {
            return 0.0;
        }

        double weightedROI = totalWeightedROI / totalCapitalAtRisk;
        return round2Digits(weightedROI);
    }

    /**
     * Calculate position-weighted probability for portfolio.
     * Weight each position's probability by its capital at risk (positionValue * quantity).
     * Formula: sum(probability * positionValue * quantity) / sum(positionValue * quantity)
     *
     * @param openPositions List of open positions
     * @return Weighted probability (0-100), or 0 if no valid positions
     */
    private double calculateWeightedProbability(List<PositionDto> openPositions) {
        if (openPositions == null || openPositions.isEmpty()) {
            return 0.0;
        }

        double totalWeightedProbability = 0.0;
        double totalCapitalAtRisk = 0.0;

        for (PositionDto position : openPositions) {
            Integer probability = position.getProbability();
            Double posValue = position.getPositionValue();
            int qty = abs(position.getQuantity());

            if (probability != null && posValue != null && posValue > 0) {
                double capitalAtRisk = posValue * qty;
                totalWeightedProbability += (probability * capitalAtRisk);
                totalCapitalAtRisk += capitalAtRisk;
            }
        }

        if (totalCapitalAtRisk == 0) {
            return 0.0;
        }

        double weightedProbability = totalWeightedProbability / totalCapitalAtRisk;
        return round2Digits(weightedProbability);
    }

    /**
     * Calculate normalized time weight using square root scaling.
     * Reference: 45 DTE = 1.0 (TastyTrade mechanical trading standard)
     *
     * Formula: √(daysLeft / 45)
     * - Longer-dated options have MORE uncertainty (wider distributions)
     * - Matches volatility scaling in options theory (σ × √t)
     * - Consistent with existing probability calculation (PositionMapper.java:158)
     *
     * Examples:
     * -  7 DTE → 0.39 (61% less weight than baseline)
     * - 30 DTE → 0.82 (18% less weight)
     * - 45 DTE → 1.00 (baseline)
     * - 60 DTE → 1.15 (15% more weight)
     * - 90 DTE → 1.41 (41% more weight)
     *
     * @param daysLeft days remaining until expiration
     * @return normalized time weight (0.0 if invalid)
     */
    private double calculateNormalizedTimeWeight(Integer daysLeft) {
        if (daysLeft == null || daysLeft <= 0) {
            return 0.0;
        }
        // Normalize to 45 DTE reference period
        return Math.sqrt(daysLeft / 45.0);
    }

    /**
     * Calculate time-weighted ROI for portfolio.
     * Weights each position by: positionValue × quantity × √(daysLeft / 45)
     *
     * This reflects time-based uncertainty:
     * - Longer-dated positions have wider probability distributions (more risk)
     * - Weight scaling matches Black-Scholes volatility term (σ√t)
     * - 45 DTE baseline aligns with TastyTrade mechanical trading standard
     *
     * Formula: sum(roi × posValue × qty × √(days/45)) / sum(posValue × qty × √(days/45))
     *
     * @param openPositions List of open positions
     * @return Time-weighted ROI percentage, or 0 if no valid positions
     */
    private double calculateTimeWeightedROI(List<PositionDto> openPositions) {
        if (openPositions == null || openPositions.isEmpty()) {
            return 0.0;
        }

        double totalWeightedROI = 0.0;
        double totalWeight = 0.0;

        for (PositionDto position : openPositions) {
            Integer roi = position.getAnnualizedRoiPercent();
            Double posValue = position.getPositionValue();
            Integer daysLeft = position.getDaysLeft();
            int qty = abs(position.getQuantity());

            if (roi != null && posValue != null && posValue > 0
                && daysLeft != null && daysLeft > 0) {

                double timeWeight = calculateNormalizedTimeWeight(daysLeft);
                double weight = posValue * qty * timeWeight;

                totalWeightedROI += (roi * weight);
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        double timeWeightedROI = totalWeightedROI / totalWeight;
        return round2Digits(timeWeightedROI);
    }

    /**
     * Calculate time-weighted probability for portfolio.
     * Weights each position by: positionValue × quantity × √(daysLeft / 45)
     *
     * Formula: sum(prob × posValue × qty × √(days/45)) / sum(posValue × qty × √(days/45))
     *
     * @param openPositions List of open positions
     * @return Time-weighted probability (0-100), or 0 if no valid positions
     */
    private double calculateTimeWeightedProbability(List<PositionDto> openPositions) {
        if (openPositions == null || openPositions.isEmpty()) {
            return 0.0;
        }

        double totalWeightedProbability = 0.0;
        double totalWeight = 0.0;

        for (PositionDto position : openPositions) {
            Integer probability = position.getProbability();
            Double posValue = position.getPositionValue();
            Integer daysLeft = position.getDaysLeft();
            int qty = abs(position.getQuantity());

            if (probability != null && posValue != null && posValue > 0
                && daysLeft != null && daysLeft > 0) {

                double timeWeight = calculateNormalizedTimeWeight(daysLeft);
                double weight = posValue * qty * timeWeight;

                totalWeightedProbability += (probability * weight);
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        double timeWeightedProbability = totalWeightedProbability / totalWeight;
        return round2Digits(timeWeightedProbability);
    }

    public void calculatePosition(PositionDto positionDto, List<PositionDto> openPositions, List<PositionDto> closedPositions) {

        double realizedProfitOrLoss = 0.0;
        double collectedPremium = 0.0;
        double openPositionsTradePrice = 0.0;
        double marketValue = 0.0;

        double unRealizedProfitOrLoss = 0.0;
        double put = 0.0;
        double call = 0.0;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();
        double positionValue = 0.0;

        double marketPrice = 0.0;
        double putMarketPrice = 0.0;
        double callMarketPrice = 0.0;
        double callObligationValue = 0.0;
        double callObligationMarketValue = 0.0;
        double allpop = 0.0;
        double allRoi = 0.0;
        int openPositionsSize = openPositions.size();

        for (PositionDto dto : closedPositions) {
            int qty = abs(dto.getQuantity());
//            if (dto.getTradeDate().isBefore(startDate)) {
//                startDate = dto.getTradeDate();
//            }
//            if (dto.getExpirationDate().isAfter(endDate)) {
//                endDate = dto.getExpirationDate();
//            }
            collectedPremium += dto.getTradePrice() * qty;
            realizedProfitOrLoss += dto.getTradePrice() * qty;
        }
        positionDto.setRealizedProfitOrLoss(round2Digits(realizedProfitOrLoss));

//        if(!openPositions.isEmpty())
//            endDate = LocalDate.now();

        for (PositionDto dto : openPositions) {
            if (dto.getTradeDate().isBefore(startDate)
                    && positionDto.getTradeDate()==null) {
                startDate = dto.getTradeDate();
            }

            if (dto.getExpirationDate().isAfter(endDate)
                    && positionDto.getExpirationDate()==null) {
                endDate = dto.getExpirationDate();
            }


            int qty = abs(dto.getQuantity());
            collectedPremium += dto.getTradePrice() * qty;
            openPositionsTradePrice += dto.getTradePrice() * qty;
            // ISSUE-033: Calculate unrealized P&L using both trade price and market price (TastyTrade methodology)
            // Unrealized P&L = (Entry Price - Current Market Price) * Quantity

            double unrealizedPnl = PositionCalculationHelper.calculateUnrealizedPnl(
                    dto.getTradePrice(),
                    dto.getMarketPrice(),
                    dto.getQuantity()
            );
            dto.setUnRealizedProfitOrLoss(unrealizedPnl);

            unRealizedProfitOrLoss += dto.getUnRealizedProfitOrLoss();

            if (OptionType.PUT.equals(dto.getType())) {
                put += dto.getTradePrice() * qty;
                putMarketPrice += dto.getMarketPrice();
                if (dto.getTradePrice() >= 0) { //PUT SELL
                    marketValue += dto.getMarketValue() * qty;
                    positionValue += dto.getPositionValue() * qty;
                }
            } else {
                call += dto.getTradePrice() * qty;
                callMarketPrice += dto.getMarketPrice();
                if (dto.getTradePrice() >= 0) { //CALL SELL
                    callObligationMarketValue += dto.getMarketValue() * qty;
                    callObligationValue += dto.getPositionValue() * qty;
                }
            }

            positionDto.setType(dto.getType());
            calculateAndSetAnnualizedRoi(dto);
            allpop += dto.getProbability();
            allRoi += dto.getAnnualizedRoiPercent();
            if(dto.getTradePrice() > 0) {
                marketPrice += (dto.getMarketPrice() * qty);
            } else {
                marketPrice += (-1 * dto.getMarketPrice() * qty);
            }
        }

        positionDto.setPut(round2Digits(put));
        positionDto.setCall(round2Digits(call));

        if (positionDto.getTradePrice() == null)
            positionDto.setTradePrice(round2Digits(openPositionsTradePrice));

        if (positionDto.getUnRealizedProfitOrLoss() == null)
            positionDto.setUnRealizedProfitOrLoss(round2Digits(unRealizedProfitOrLoss));

        if (positionDto.getExpirationDate() == null)
            positionDto.setExpirationDate(endDate);

        if (positionDto.getTradeDate() == null)
            positionDto.setTradeDate(startDate);

        if (positionDto.getMarketValue() == null)
            positionDto.setMarketValue(marketValue);

        positionDto.setCollectedPremium(round2Digits(collectedPremium));
        if(positionValue == 0 && openPositionsSize == 1)
            positionValue = openPositions.getFirst().getPositionValue();

        if (positionDto.getPositionValue() == null)
            positionDto.setPositionValue(round2Digits(positionValue));

        calculateAndSetAnnualizedRoi(positionDto);

        // Calculate position-weighted portfolio metrics
        if(positionDto.getPositionValue() == 0) {
            positionDto.setProbability(0);
            positionDto.setAnnualizedRoiPercent(0);
        } else if(openPositionsSize > 0) {
            // Use position-weighted calculations instead of simple averaging
            // Weight by capital at risk (positionValue * quantity) to properly reflect portfolio impact
            double weightedROI = calculateWeightedROI(openPositions);
            double weightedProbability = calculateWeightedProbability(openPositions);

            positionDto.setAnnualizedRoiPercent((int) weightedROI);
            positionDto.setProbability((int) weightedProbability);
        }



        positionDto.setMarketPrice(round2Digits(marketPrice));
        positionDto.setPutMarketPrice(round2Digits(putMarketPrice));
        positionDto.setCallMarketPrice(round2Digits(callMarketPrice));
        positionDto.setCallObligationValue(round2Digits(callObligationValue));
        positionDto.setCallObligationMarketValue(round2Digits(callObligationMarketValue));

        // Total P&L = Realized P&L + Unrealized P&L (TastyTrade methodology)
        // ISSUE-033: Fixed to follow standard accounting principle
        double totalProfitOrLoss = 0.0;
        if (positionDto.getRealizedProfitOrLoss() != null) {
            totalProfitOrLoss += positionDto.getRealizedProfitOrLoss();
        }
        if (positionDto.getUnRealizedProfitOrLoss() != null) {
            totalProfitOrLoss += positionDto.getUnRealizedProfitOrLoss();
        }
        positionDto.setProfitOrLoss(round2Digits(totalProfitOrLoss));

        double marketVsPositionsPercentage = positionValue > 0 ? ((marketValue / positionValue) * 100) - 100 : 0.0;
        positionDto.setMarketVsPositionsPercentage(round2Digits(marketVsPositionsPercentage));

        // Calculate CALL coverage percentage (similar to PUT)
        double callMarketVsObligationsPercentage = 0.0;
        if (callObligationValue > 0) {
            callMarketVsObligationsPercentage = ((callObligationMarketValue / callObligationValue) * 100) - 100;
        }
        positionDto.setCallMarketVsObligationsPercentage(round2Digits(callMarketVsObligationsPercentage));

        log.info("positionDto: {}", positionDto);
    }

    /**
     * Calculate metrics for a single position using ONLY form-input values.
     * Does NOT load or aggregate from database positions.
     *
     * This method enables what-if analysis: users can enter hypothetical position parameters
     * (trade date, expiration, trade price, position value) and see calculated metrics
     * without database position interference.
     *
     * Differs from calculatePosition():
     * - No database position loading (no openPositions, closedPositions)
     * - No aggregation logic
     * - No portfolio-weighted calculations
     * - Aggregated fields (Realized P&L, etc) set to zero
     * - Single-position focus only
     *
     * @param positionDto position with form input values (Trade Date, Expiration, Trade Price, Position Value, Market Value)
     * @return positionDto with calculated metrics (Days, ROI, Probability, P&L)
     */
    public PositionDto calculateSinglePosition(PositionDto positionDto) {
        // Validate required fields for calculation
        if (positionDto == null
            || positionDto.getTradeDate() == null
            || positionDto.getExpirationDate() == null
            || positionDto.getPositionValue() == null
            || positionDto.getPositionValue() == 0) {
            log.debug("Invalid position for calculation: missing required fields");
            return positionDto;
        }

        // Calculate individual position metrics using form inputs
        // This uses PositionMapper.calculateAndSetAnnualizedRoi which calculates:
        // - daysBetween (trade date to expiration)
        // - daysLeft (now to expiration)
        // - tradePrice (estimated if missing)
        // - breakEven (based on position value and trade price)
        // - annualizedRoiPercent (ROI calculation)
        // - probability (if market value available)
        calculateAndSetAnnualizedRoi(positionDto);

        // Clear aggregated fields that require database positions
        // These make sense only when aggregating from multiple database positions
        // Note: RealizedProfitOrLoss is preserved (mapped from form data)
        positionDto.setCallObligationValue(0.0);
        positionDto.setCallObligationMarketValue(0.0);
        positionDto.setMarketVsPositionsPercentage(0.0);
        positionDto.setCallMarketVsObligationsPercentage(0.0);

        // Set collected premium from form trade price
        if (positionDto.getTradePrice() != null) {
            int qty = abs(positionDto.getQuantity());
            positionDto.setCollectedPremium(round2Digits(positionDto.getTradePrice() * qty));
        }

        // Note: UnRealized P&L, Realized P&L, and Market Price are NOT modified here
        // These values are mapped from form data and should be preserved as-is
        // calculateSinglePosition() only recalculates:
        // - daysBetween, daysLeft
        // - annualizedRoiPercent, probability
        // - collectedPremium (from trade price)

        // For single position, PUT and CALL values come from form inputs
        if (OptionType.PUT.equals(positionDto.getType())) {
            positionDto.setPut(round2Digits(positionDto.getTradePrice() != null ? positionDto.getTradePrice() : 0.0));
            positionDto.setCall(0.0);
        } else if (OptionType.CALL.equals(positionDto.getType())) {
            positionDto.setCall(round2Digits(positionDto.getTradePrice() != null ? positionDto.getTradePrice() : 0.0));
            positionDto.setPut(0.0);
        }

        log.info("calculateSinglePosition result: {}", positionDto);
        return positionDto;
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
    public CsvImportResult saveCSV(String csv) {
        CsvImportResult result = new CsvImportResult();
        long start = System.currentTimeMillis();
        try (CSVParser csvRecords = parse(csv,
                CSVFormat.Builder.create()
                        .setHeader()                   // első sor fejléc
                        .setSkipHeaderRecord(true)    // ne olvassa be újra a fejlécet
                        .setIgnoreHeaderCase(true)    // fejlécmezők kis/nagybetű érzéketlenek
                        .setTrim(true)                // whitespace-ek levágása
                        .get())) {
            for (CSVRecord csvRecord : csvRecords) {
                result.setTotalRecords(result.getTotalRecords() + 1);

                try {
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
                    String conidStr = csvRecord.get("Conid");
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

                        Long conid;
                        try {
                            conid = Long.parseLong(conidStr);
                        } catch (NumberFormatException e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "Conid", conidStr,
                                    e.getMessage(), "NumberFormatException"));
                            log.error("CSV Record #{} - NumberFormatException parsing 'Conid' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), conidStr, e.getMessage());
                            continue;
                        }

                        double tradePrice;
                        try {
                            tradePrice = Math.round(Double.parseDouble(netCash) * 100.0) / 100.0;
                        } catch (NumberFormatException e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "NetCash", netCash,
                                    e.getMessage(), "NumberFormatException"));
                            log.error("CSV Record #{} - NumberFormatException parsing 'NetCash' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), netCash, e.getMessage());
                            continue;
                        }

                        log.debug("ticker: {}, optionStatus: {}, conid:{} qty:{}, tradePrice:{}", ticker, optionStatus, conid, quantity, tradePrice);
                        OptionEntity optionEntity = optionRepository.findByConidAndStatusAndTradePrice(conid, optionStatus,tradePrice);
                        if( optionEntity == null)
                            optionEntity = new OptionEntity();
                        else {
                            result.incrementSkipped();
                            continue;
                        }
                        optionEntity.setAccount(account);
                        optionEntity.setConid(conid);
                        optionEntity.setStatus(optionStatus);
                        optionEntity.setAssetClass(AssetClass.OPT);

                        if ("C".equals(putCall))
                            optionEntity.setType(OptionType.CALL);
                        else
                            optionEntity.setType(OptionType.PUT);

                        LocalDate tradeLocalDate;
                        try {
                            tradeLocalDate = LocalDate.parse(tradeDate);
                        } catch (Exception e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "TradeDate", tradeDate,
                                    e.getMessage(), e.getClass().getSimpleName()));
                            log.error("CSV Record #{} - {} parsing 'TradeDate' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), e.getClass().getSimpleName(), tradeDate, e.getMessage());
                            continue;
                        }

                        LocalDate expirationLocalDate;
                        try {
                            expirationLocalDate = LocalDate.parse(expirationDate);
                        } catch (Exception e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "Expiry", expirationDate,
                                    e.getMessage(), e.getClass().getSimpleName()));
                            log.error("CSV Record #{} - {} parsing 'Expiry' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), e.getClass().getSimpleName(), expirationDate, e.getMessage());
                            continue;
                        }

                        optionEntity.setTradeDate(tradeLocalDate);
                        optionEntity.setExpirationDate(expirationLocalDate);

                        double strikeValue;
                        try {
                            strikeValue = Double.parseDouble(strike) * 100;
                        } catch (NumberFormatException e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "Strike", strike,
                                    e.getMessage(), "NumberFormatException"));
                            log.error("CSV Record #{} - NumberFormatException parsing 'Strike' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), strike, e.getMessage());
                            continue;
                        }

                        optionEntity.setPositionValue(strikeValue);
                        optionEntity.setQuantity(quantity);

                        InstrumentEntity instrumentEntity = instrumentRepository.findByTicker(ticker);
                        if ((instrumentEntity == null || instrumentEntity.getId() == null)) {
                            instrumentEntity = new InstrumentEntity();
                            instrumentEntity.setTicker(ticker);
                            try {
                                instrumentEntity.setConid(Long.parseLong(underlyingConid));
                            } catch (NumberFormatException e) {
                                result.incrementFailed();
                                result.addError(new CsvImportResult.CsvRecordError(
                                        (int)csvRecord.getRecordNumber(), "UnderlyingConid", underlyingConid,
                                        e.getMessage(), "NumberFormatException"));
                                log.error("CSV Record #{} - NumberFormatException parsing 'UnderlyingConid' (value: '{}'): {}",
                                        csvRecord.getRecordNumber(), underlyingConid, e.getMessage());
                                continue;
                            }
                            instrumentRepository.save(instrumentEntity);
                        }

                        optionEntity.setTicker(ticker);
                        optionEntity.setCode(code);

                        double pnl;
                        try {
                            pnl = Math.round(Double.parseDouble(fifoPnlRealized) * 100.0) / 100.0;
                        } catch (NumberFormatException e) {
                            result.incrementFailed();
                            result.addError(new CsvImportResult.CsvRecordError(
                                    (int)csvRecord.getRecordNumber(), "FifoPnlRealized", fifoPnlRealized,
                                    e.getMessage(), "NumberFormatException"));
                            log.error("CSV Record #{} - NumberFormatException parsing 'FifoPnlRealized' (value: '{}'): {}",
                                    csvRecord.getRecordNumber(), fifoPnlRealized, e.getMessage());
                            continue;
                        }

                        optionEntity.setRealizedProfitOrLoss(pnl);
                        optionEntity.setTradePrice(tradePrice);
                        optionEntity.setInstrument(instrumentEntity);

                        double marketValue = Math.round((strikeValue + tradePrice) * 100.0) / 100.0;
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
                        log.debug("CSV Record #{} saved: {}", csvRecord.getRecordNumber(), csvRecord.toString());
                        result.incrementSuccessful();
                    } else {
                        result.incrementSkipped();
                    }
                } catch (Exception e) {
                    result.incrementFailed();
                    result.addError(new CsvImportResult.CsvRecordError(
                            (int)csvRecord.getRecordNumber(), "UNKNOWN", "",
                            e.getMessage(), e.getClass().getSimpleName()));
                    log.error("CSV Record #{} - Unexpected error: {}", csvRecord.getRecordNumber(), e.getMessage(), e);
                }
            }
            long end = System.currentTimeMillis();
            int elapsedSeconds = (int) ((end - start) / 1000.0);

            log.info(result.getSummary());
            log.info("CSV file parsed in {} sec, total records in file: {}", elapsedSeconds, csvRecords.getRecordNumber());
            return result;
        } catch (Exception e) {
            log.error("CSV parsing configuration error (missing columns or malformed CSV): {}", e.getMessage(), e);
            throw new RuntimeException("Fail to parse CSV " + e.getMessage(), e);
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