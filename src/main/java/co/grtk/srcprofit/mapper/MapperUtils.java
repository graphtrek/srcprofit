package co.grtk.srcprofit.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class MapperUtils {

    static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MapperUtils() {
    }

    public static Long parseLong(String s, Long defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of(Long.parseLong(s)).orElse(defaultValue);
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    public static Integer parseInt(String s, Integer defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return Optional.of((int)Double.parseDouble(s)).orElse(defaultValue);
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

    public static double round2Digits(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                TimeZone.getDefault().toZoneId());
    }

    public static String getLocalDateAsString(LocalDate date) {
        return Objects.isNull(date) ? "" : date.toString();
    }

    public static String getlocalTimeAsString(LocalDateTime date) {
        return Objects.isNull(date) ? "" : date.format(timeFormatter);
    }

    public static String getLocalDateTimeAsString(LocalDateTime date) {
        return Objects.isNull(date) ? "" : date.format(dateFormatter);
    }

    public static String getDatesCsv(Map<LocalDate, BigDecimal> map) {
        return map.keySet().stream()
                .sorted()
                .map(date -> "\"" + date.toString() + "\"")
                .reduce("", (a, b) -> a + "," + b);
    }

    public static String getValuesCsv(Map<LocalDate, BigDecimal> map) {
       return  map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String value = entry.getValue().toString();
                    // Filter out NaN and Infinity values to prevent chart rendering issues
                    if (value.contains("NaN") || value.contains("Infinity")) {
                        return "0";
                    }
                    return value;
                })
               .collect(Collectors.joining(","));
    }
}