package co.grtk.srcprofit.mapper;

/**
 * Constants used in position ROI and probability calculations.
 *
 * These values are based on TastyTrade methodology for options trading analysis.
 */
public class PositionCalculationConstants {

    private PositionCalculationConstants() {
    }

    /**
     * Daily premium rate used in trade price estimation.
     * Represents the estimated daily return rate for the premium collected.
     */
    public static final double DAILY_PREMIUM_RATE = 0.0014;

    /**
     * Daily volatility estimate as a percentage of market mean.
     * Used to calculate daily standard deviation for probability calculations.
     * Represents 5% of the market mean value.
     */
    public static final double DAILY_VOLATILITY_ESTIMATE = 0.05;

    /**
     * Number of trading days in a year.
     * Used to annualize daily ROI calculations.
     */
    public static final int DAYS_PER_YEAR = 365;

    /**
     * Multiplier to convert decimal ROI to percentage.
     * Example: 0.55 (55%) × 100 = 55
     */
    public static final double PERCENT_MULTIPLIER = 100.0;

}
