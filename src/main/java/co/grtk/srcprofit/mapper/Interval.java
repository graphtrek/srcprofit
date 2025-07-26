package co.grtk.srcprofit.mapper;

public enum Interval {
    WEEK, MONTH, YEAR, ALL;

    public static Interval fromString(String intervalStr) {
        if (intervalStr == null) {
           return ALL;
        }
        return switch (intervalStr.trim().toLowerCase()) {
            case "week" -> WEEK;
            case "month" -> MONTH;
            case "year" -> YEAR;
            default -> ALL;
        };
    }
}