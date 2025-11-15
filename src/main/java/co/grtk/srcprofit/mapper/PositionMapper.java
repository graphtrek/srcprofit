package co.grtk.srcprofit.mapper;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static co.grtk.srcprofit.mapper.MapperUtils.parseDouble;
import static co.grtk.srcprofit.mapper.MapperUtils.parseInt;
import static co.grtk.srcprofit.mapper.MapperUtils.parseLong;
import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;
import static co.grtk.srcprofit.mapper.MapperUtils.toLocalDate;
import static java.lang.Math.abs;


public class PositionMapper {

    private PositionMapper() {
    }

    public static PositionDto mapFromData(MultiValueMap<String, String> formData) {

        Long id = parseLong(formData.getFirst("id"), null);
        Long parentId = parseLong(formData.getFirst("parentId"), null);
        String ticker = formData.getFirst("ticker");
        LocalDate startDate = toLocalDate(formData.getFirst("startDate"));
        LocalDate expirationDate = toLocalDate(formData.getFirst("expirationDate"));

        String optionType = formData.getFirst("type");
        String note = formData.getFirst("note");
        Integer quantity = parseInt(formData.getFirst("quantity"), 1);
        Double fee = parseDouble(formData.getFirst("fee"), null);
        Double marketValue = parseDouble(formData.getFirst("marketValue"), null);
        Double positionValue = parseDouble(formData.getFirst("positionValue"), null);
        Double tradePrice = parseDouble(formData.getFirst("tradePrice"), null);

        // Note: coveredPositionValue is NOT extracted from form data because it's a calculated
        // portfolio-level metric based on existing positions, not user input.
        // For virtual positions, the portfolio's actual coveredPositionValue should be preserved.
        String status = formData.getFirst("status");
        String color = formData.getFirst("color");

        PositionDto positionDto = new PositionDto();
        positionDto.setId(id);
        positionDto.setParentId(parentId);
        positionDto.setTicker(ticker);
        if (startDate == null)
            startDate = LocalDate.now();
        positionDto.setTradeDate(startDate);
        positionDto.setColor(color);
        positionDto.setAssetClass(AssetClass.OPT);
        // Note: coveredPositionValue is NOT set from form data - it's a calculated portfolio metric
        positionDto.setType(OptionType.fromCode(optionType));

        Optional.ofNullable(status)
                .ifPresentOrElse(
                        s -> positionDto.setStatus(OptionStatus.fromCode(s)),
                        () -> positionDto.setStatus(OptionStatus.PENDING)
                );

        positionDto.setNote(note);
        positionDto.setFee(fee);
        positionDto.setMarketValue(marketValue);
        positionDto.setPositionValue(positionValue);
        positionDto.setTradePrice(tradePrice);
        positionDto.setExpirationDate(expirationDate);
        positionDto.setQuantity(quantity);
        calculateAndSetAnnualizedRoi(positionDto);
        return positionDto;
    }

    /**
     * Calculates ROI, break-even, and probability metrics for a position and sets them on the DTO.
     *
     * This method orchestrates the calculation of financial metrics for an options position:
     * - daysBetween: Days from trade to expiration
     * - daysLeft: Days from now to expiration
     * - tradePrice: Estimated if not provided
     * - breakEven: Calculated based on option type
     * - annualizedRoiPercent: Annualized return on investment
     * - probability: Probability of profit at expiration
     *
     * @param dto the position DTO to calculate and set metrics on
     */
    public static void calculateAndSetAnnualizedRoi(PositionDto dto) {
        if (!isValidForCalculation(dto)) {
            return;
        }

        int daysBetween = PositionCalculationHelper.calculateDaysBetween(dto.getTradeDate(), dto.getExpirationDate());
        if (daysBetween <= 0) {
            dto.setAnnualizedRoiPercent(null);
            return;
        }

        // Set time-based metrics
        dto.setDaysBetween(daysBetween);
        int daysLeft = PositionCalculationHelper.calculateDaysLeft(dto.getExpirationDate());
        dto.setDaysLeft(daysLeft);

        if (dto.getPositionValue() == null) {
            return;
        }

        double positionValue = dto.getPositionValue();

        // Estimate trade price if not provided
        if (dto.getTradePrice() == null) {
            double estimatedPrice = PositionCalculationHelper.estimateTradePrice(positionValue, daysBetween);
            dto.setTradePrice(estimatedPrice);
        }

        double tradePrice = dto.getTradePrice();

        // Calculate break-even based on option type
        Double breakEven = PositionCalculationHelper.calculateBreakEven(positionValue, tradePrice, dto.getType());
        if (breakEven != null) {
            dto.setBreakEven(breakEven);
        }

        // Calculate annualized ROI percentage
        int roiPercent = PositionCalculationHelper.calculateAnnualizedRoiPercent(positionValue, tradePrice, dto.getFee(), daysBetween);
        dto.setAnnualizedRoiPercent(roiPercent);

        // Calculate probability of profit (only if market value is available)
        if (dto.getMarketValue() != null) {
            int probability = PositionCalculationHelper.calculateProbability(positionValue, dto.getMarketValue(), daysBetween);
            dto.setProbability(probability);
        }
    }

    /**
     * Validates that the DTO has the minimum required fields for ROI calculation.
     *
     * @param dto the position DTO to validate
     * @return true if DTO is valid for calculation, false otherwise
     */
    private static boolean isValidForCalculation(PositionDto dto) {
        return dto != null && dto.getTradeDate() != null && dto.getExpirationDate() != null;
    }


    public static int probabilityMarketExceedsTradeValue(BigDecimal tradeValue, double marketMean, double dailyStdDev, int days) {
        if (tradeValue == null || tradeValue.compareTo(BigDecimal.ZERO) <= 0 || days <= 0 || marketMean <= 0 || dailyStdDev <= 0) {
            return 0;
        }

        double trade = tradeValue.doubleValue();

        // Szórás növelése idő szerint: σ × √t
        double timeAdjustedStdDev = dailyStdDev * Math.sqrt(days);

        NormalDistribution distribution = new NormalDistribution(marketMean, timeAdjustedStdDev);

        // P(marketValue > tradeValue) = 1 - CDF(tradeValue)
        double probability = 1.0 - distribution.cumulativeProbability(trade);

        // 0–1 közötti valószínűség → opcionálisan szorozhatod 100-zal ha százalék kell
        return (int) (probability * 100);
    }


    public static String generateOptionCode(String symbol, LocalDate expiryDate, BigDecimal strikePrice, OptionType type) {
        String formattedDate = expiryDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
        char typeChar = (type == OptionType.CALL) ? 'C' : 'P';

        // OPRA: strike 10.00 → 00010000
        int scaledStrike = strikePrice.multiply(new BigDecimal("1000")).intValue();
        String formattedStrike = String.format("%08d", scaledStrike);

        return String.format("%-6s%s%s%s", symbol, formattedDate, typeChar, formattedStrike);
    }

}