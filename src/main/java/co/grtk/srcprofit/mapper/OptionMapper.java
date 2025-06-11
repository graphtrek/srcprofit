package co.grtk.srcprofit.mapper;

import co.grtk.srcprofit.dto.OptionDto;
import co.grtk.srcprofit.model.AssetClass;
import co.grtk.srcprofit.model.OptionType;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.lang.Math.abs;


public class OptionMapper {

    private OptionMapper() {}

    public static OptionDto mapFromData(MultiValueMap<String, String> formData) {
        String symbol = formData.getFirst("symbol");
        LocalDateTime startDate = toLocalDateTime(formData.getFirst("startDate"));

        String optionType = formData.getFirst("optionType");
        String description = formData.getFirst("description");
        Integer quantity = parseInt(formData.getFirst("quantity"), null);
        Double fee = parseDouble(formData.getFirst("fee"), null);
        Double marketValue = parseDouble(formData.getFirst("marketValue"), null);
        Double positionValue = parseDouble(formData.getFirst("positionValue"), null);
        Double tradePrice = parseDouble(formData.getFirst("tradePrice"), null);
        LocalDate expirationDate = toLocalDate(formData.getFirst("expirationDate"));

        OptionDto optionDto = new OptionDto();
        optionDto.setSymbol(symbol);
        optionDto.setStartDateTime(startDate);

        optionDto.setAssetClass(AssetClass.OPTION);
        optionDto.setOptionType(OptionType.fromCode(optionType));
        optionDto.setDescription(description);
        optionDto.setFee(fee);
        optionDto.setMarketValue(marketValue);
        optionDto.setPositionValue(positionValue);
        optionDto.setTradePrice(tradePrice);
        optionDto.setExpirationDate(expirationDate);
        optionDto.setQuantity(quantity);
        calculateAndSetAnnualizedRoi(optionDto);
        return optionDto;
    }

    public static void calculateAndSetAnnualizedRoi(OptionDto dto) {
        if (dto == null ||
                dto.getStartDateTime() == null || dto.getExpirationDate() == null) {
            return;
        }


        int daysBetween =
                (int) ChronoUnit.DAYS.between(dto.getStartDateTime(),dto.getExpirationDate()
                        .plusDays(1)
                        .atStartOfDay());
        if (daysBetween <= 0) {
            dto.setAnnualizedRoiPercent(null);
            return;
        }

        dto.setDaysBetween(daysBetween);
        int daysLeft =
                (int) ChronoUnit.DAYS.between(LocalDateTime.now(),dto.getExpirationDate()
                        .plusDays(1)
                        .atStartOfDay());
        dto.setDaysLeft(daysLeft);

        if (dto.getTradePrice() == null || dto.getMarketValue() == null || dto.getPositionValue() == null) {
            return;
        }

        float roiPerDay = abs(dto.getTradePrice().floatValue()) / daysBetween;
        float annualizedRoi = roiPerDay * 365;
        float roiPercent = (annualizedRoi / dto.getPositionValue().floatValue()) * 100;

        dto.setAnnualizedRoiPercent(Math.round(roiPercent));

        BigDecimal tradeValue = new BigDecimal(dto.getPositionValue());
        double marketMean =  dto.getMarketValue(); // jelenlegi vagy várt érték
        double dailyStdDev = marketMean * 0.05;  // napi szórás

        int probability = probabilityMarketExceedsTradeValue(tradeValue, marketMean, dailyStdDev, daysBetween);
        dto.setProbability(probability);
    }

    public static Integer parseInt(String s, Integer defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Integer.parseInt(s)).orElse(defaultValue);
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    public static Double parseDouble(String s, Double defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Double.parseDouble(s)).orElse(defaultValue);
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }


    public static int probabilityMarketExceedsTradeValue(BigDecimal tradeValue, double marketMean, double dailyStdDev, int days) {
        if (tradeValue == null || tradeValue.compareTo(BigDecimal.ZERO) <= 0 || days <= 0) {
            return 0;
        }

        double trade = tradeValue.doubleValue();

        // Szórás növelése idő szerint: σ × √t
        double timeAdjustedStdDev = dailyStdDev * Math.sqrt(days);

        NormalDistribution distribution = new NormalDistribution(marketMean, timeAdjustedStdDev);

        // P(marketValue > tradeValue) = 1 - CDF(tradeValue)
        double probability =  1.0 - distribution.cumulativeProbability(trade);

        // 0–1 közötti valószínűség → opcionálisan szorozhatod 100-zal ha százalék kell
        return (int) (probability * 100);
    }

    public static LocalDateTime toLocalDateTime(String dateTime) {
        Optional<LocalDateTime> safeStartDate = Optional.ofNullable(dateTime)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(LocalDateTime.parse(s));
                    } catch (DateTimeParseException _) {
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
                    } catch (DateTimeParseException _) {
                        return Optional.empty();
                    }
                });
        return safeStartDate.orElse(null);
    }
}