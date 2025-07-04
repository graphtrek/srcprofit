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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
        Integer quantity = parseInt(formData.getFirst("quantity"), null);
        Double fee = parseDouble(formData.getFirst("fee"), null);
        Double marketValue = parseDouble(formData.getFirst("marketValue"), null);
        Double positionValue = parseDouble(formData.getFirst("positionValue"), null);
        Double tradePrice = parseDouble(formData.getFirst("tradePrice"), null);

        Double coveredPositionValue = parseDouble(formData.getFirst("coveredPositionValue"), null);
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
        positionDto.setCoveredPositionValue(coveredPositionValue);
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

    public static void calculateAndSetAnnualizedRoi(PositionDto dto) {
        if (dto == null ||
                dto.getTradeDate() == null || dto.getExpirationDate() == null) {
            return;
        }

        int daysBetween =
                (int) ChronoUnit.DAYS.between(dto.getTradeDate(), dto.getExpirationDate()
                        .plusDays(1)
                        .atStartOfDay());
        if (daysBetween <= 0) {
            dto.setAnnualizedRoiPercent(null);
            return;
        }

        dto.setDaysBetween(daysBetween);
        int daysLeft =
                (int) ChronoUnit.DAYS.between(LocalDateTime.now(), dto.getExpirationDate()
                        .plusDays(1)
                        .atStartOfDay());
        dto.setDaysLeft(daysLeft);

        if (dto.getPositionValue() == null) {
            return;
        }

        double positionValue = dto.getPositionValue();

        if (dto.getTradePrice() == null)
            dto.setTradePrice(Math.round(positionValue * 0.0014 * daysBetween * 100.0) / 100.0);

        double tradePrice = dto.getTradePrice();
        OptionType type = dto.getType();

        if (tradePrice > 0 && type == OptionType.PUT) {
            dto.setBreakEven(round2Digits(positionValue - tradePrice));
        } else if (tradePrice > 0 && type == OptionType.CALL) {
            dto.setBreakEven(round2Digits(positionValue + tradePrice));
        }

        float roiBasePrice = dto.getTradePrice().floatValue();
        if (dto.getFee() != null)
            roiBasePrice -= dto.getFee().floatValue();

        float roiPerDay = abs(roiBasePrice) / daysBetween;
        float annualizedRoi = roiPerDay * 365;
        double roiPercent = (annualizedRoi / positionValue) * 100;

        dto.setAnnualizedRoiPercent((int) Math.round(roiPercent));

        if (dto.getMarketValue() == null)
            return;
        BigDecimal tradeValue = BigDecimal.valueOf(positionValue);
        double marketMean = dto.getMarketValue(); // jelenlegi vagy várt érték
        double dailyStdDev = marketMean * 0.05;  // napi szórás

        int probability = probabilityMarketExceedsTradeValue(tradeValue, marketMean, dailyStdDev, daysBetween);
        dto.setProbability(probability);
    }

    public static Long parseLong(String s, Long defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Long.parseLong(s)).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Integer parseInt(String s, Integer defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Integer.parseInt(s)).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Double parseDouble(String s, Double defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Double.parseDouble(s)).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

    public static LocalDateTime toLocalDateTime(String dateTime) {
        Optional<LocalDateTime> safeStartDate = Optional.ofNullable(dateTime)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(LocalDateTime.parse(s));
                    } catch (DateTimeParseException e) {
                        return Optional.empty();
                    }
                });
        return safeStartDate.orElse(null);
    }

    public static LocalDate toLocalDate(String date) {
        Optional<LocalDate> safeStartDate = Optional.ofNullable(date)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(LocalDate.parse(s));
                    } catch (DateTimeParseException e) {
                        return Optional.empty();
                    }
                });
        return safeStartDate.orElse(null);
    }

    public static String generateOptionCode(String symbol, LocalDate expiryDate, BigDecimal strikePrice, OptionType type) {
        String formattedDate = expiryDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
        char typeChar = (type == OptionType.CALL) ? 'C' : 'P';

        // OPRA: strike 10.00 → 00010000
        int scaledStrike = strikePrice.multiply(new BigDecimal("1000")).intValue();
        String formattedStrike = String.format("%08d", scaledStrike);

        return String.format("%-6s%s%s%s", symbol, formattedDate, typeChar, formattedStrike);
    }

    public static double round2Digits(double value) {
        return Math.round(value * 100.0) / 100.0;
    }


}