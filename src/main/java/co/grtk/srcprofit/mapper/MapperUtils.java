package co.grtk.srcprofit.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

public class MapperUtils {

    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("E, MMM dd yyyy HH:mm:ss");

    private MapperUtils() {
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
}