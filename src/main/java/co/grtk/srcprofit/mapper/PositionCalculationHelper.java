package co.grtk.srcprofit.mapper;

import co.grtk.srcprofit.entity.OptionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static co.grtk.srcprofit.mapper.MapperUtils.round2Digits;
import static co.grtk.srcprofit.mapper.PositionCalculationConstants.*;
import static java.lang.Math.abs;

/**
 * Helper class for calculating position-related financial metrics.
 * Contains pure calculation methods with no side effects on DTOs.
 *
 * All methods are static utilities that perform financial calculations
 * for options trading positions based on TastyTrade methodology.
 */
public class PositionCalculationHelper {

    private PositionCalculationHelper() {
    }

    /**
     * Calculates the number of days between trade date and expiration date.
     *
     * @param tradeDate the date the position was opened
     * @param expirationDate the date the option expires
     * @return days between dates plus 1 (to include expiration day), or -1 if dates are invalid
     */
    public static int calculateDaysBetween(LocalDate tradeDate, LocalDate expirationDate) {
        if (tradeDate == null || expirationDate == null) {
            return -1;
        }

        int days = (int) ChronoUnit.DAYS.between(tradeDate, expirationDate.plusDays(1).atStartOfDay());
        return days;
    }

    /**
     * Calculates the number of days remaining until option expiration from now.
     *
     * @param expirationDate the date the option expires
     * @return days remaining until expiration (including expiration day)
     */
    public static int calculateDaysLeft(LocalDate expirationDate) {
        if (expirationDate == null) {
            return -1;
        }

        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(), expirationDate.plusDays(1).atStartOfDay());
    }

    /**
     * Estimates the trade price (premium collected) for an option position.
     *
     * Formula: positionValue × DAILY_PREMIUM_RATE × daysBetween
     *
     * @param positionValue the underlying position value (strike price or current market value)
     * @param daysBetween the number of days in the position (DTE)
     * @return estimated trade price (premium), rounded to 2 decimal places
     */
    public static double estimateTradePrice(double positionValue, int daysBetween) {
        if (daysBetween <= 0) {
            return 0.0;
        }

        double estimate = positionValue * DAILY_PREMIUM_RATE * daysBetween;
        return Math.round(estimate * 100.0) / 100.0; // Round to 2 decimals
    }

    /**
     * Calculates the break-even price for an option position.
     *
     * For PUT options: break-even = positionValue - tradePrice
     * For CALL options: break-even = positionValue + tradePrice
     *
     * @param positionValue the underlying position value (strike price)
     * @param tradePrice the premium collected or paid
     * @param type the option type (PUT or CALL)
     * @return break-even price rounded to 2 decimals, or null if inputs are invalid
     */
    public static Double calculateBreakEven(double positionValue, double tradePrice, OptionType type) {
        if (tradePrice <= 0 || type == null) {
            return null;
        }

        double breakEven;
        if (type == OptionType.PUT) {
            breakEven = positionValue - tradePrice;
        } else if (type == OptionType.CALL) {
            breakEven = positionValue + tradePrice;
        } else {
            return null;
        }

        return round2Digits(breakEven);
    }

    /**
     * Calculates the annualized ROI percentage for a position.
     *
     * Process:
     * 1. Adjust trade price by subtracting fees
     * 2. Calculate daily ROI: abs(adjustedTradePrice) / daysBetween
     * 3. Annualize: dailyROI × DAYS_PER_YEAR
     * 4. Convert to percentage: (annualizedROI / positionValue) × PERCENT_MULTIPLIER
     *
     * @param positionValue the underlying position value (strike price or current market value)
     * @param tradePrice the premium collected or paid
     * @param fee optional trading fees to deduct from the premium
     * @param daysBetween the number of days in the position (DTE)
     * @return annualized ROI as a percentage (rounded to nearest integer)
     */
    public static int calculateAnnualizedRoiPercent(double positionValue, double tradePrice, Double fee, int daysBetween) {
        if (daysBetween <= 0 || positionValue <= 0) {
            return 0;
        }

        // Adjust trade price for fees
        float roiBasePrice = (float) tradePrice;
        if (fee != null) {
            roiBasePrice -= fee.floatValue();
        }

        // Calculate daily and annualized ROI
        float roiPerDay = abs(roiBasePrice) / daysBetween;
        float annualizedRoi = roiPerDay * DAYS_PER_YEAR;

        // Convert to percentage
        double roiPercent = (annualizedRoi / positionValue) * PERCENT_MULTIPLIER;

        return (int) Math.round(roiPercent);
    }

    /**
     * Calculates the annualized ROI percentage for a position using daysBetween (trade to expiration).
     *
     * Process:
     * 1. Calculate daily ROI: abs(costBasisPrice) / daysBetween
     * 2. Annualize: dailyROI × DAYS_PER_YEAR
     * 3. Convert to percentage: (annualizedROI / strikePrice) × PERCENT_MULTIPLIER
     *
     * This variant is used for open positions where daysBetween (original trade duration from
     * trade date to expiration) provides the actual time horizon of the trade.
     *
     * @param strikePrice the option strike price (position value)
     * @param costBasisPrice the premium collected or paid (acquisition cost per unit)
     * @param daysBetween the number of days from trade date to expiration
     * @return annualized ROI as a percentage (rounded to nearest integer)
     */
    public static int calculateAnnualizedRoiPercent(double strikePrice, double costBasisPrice, int daysBetween) {
        if (daysBetween <= 0 || strikePrice <= 0) {
            return 0;
        }

        // Calculate daily and annualized ROI based on original trade duration
        float roiPerDay = abs((float) costBasisPrice) / daysBetween;
        float annualizedRoi = roiPerDay * DAYS_PER_YEAR;

        // Convert to percentage
        double roiPercent = (annualizedRoi / strikePrice) * PERCENT_MULTIPLIER;

        return (int) Math.round(roiPercent);
    }

    /**
     * Calculates the probability that the market value will exceed the trade value at expiration.
     *
     * Uses normal distribution with volatility adjusted by square root of time.
     * Formula: volatilityByTime = dailyVolatility × √(daysBetween)
     *
     * @param positionValue the underlying position value (strike price)
     * @param marketValue the current or expected market value
     * @param daysBetween the number of days until expiration
     * @return probability as a percentage (0-100), or 0 if inputs are invalid
     */
    public static int calculateProbability(double positionValue, double marketValue, int daysBetween) {
        if (marketValue <= 0 || daysBetween <= 0) {
            return 0;
        }

        BigDecimal tradeValue = BigDecimal.valueOf(positionValue);
        double dailyStdDev = marketValue * DAILY_VOLATILITY_ESTIMATE;

        return PositionMapper.probabilityMarketExceedsTradeValue(tradeValue, marketValue, dailyStdDev, daysBetween);
    }

    /**
     * Calculates unrealized profit/loss for an option position.
     *
     * Formula: Market Value - Cost Basis
     *          = (quantity × markPrice × multiplier) - (quantity × costBasisPrice × multiplier)
     *
     * Note: For short positions (negative quantity), a negative P&L indicates profit,
     *       while positive P&L indicates loss (sold premium vs current buyback cost).
     *
     * @param quantity position size (positive for long, negative for short)
     * @param markPrice current market price per contract
     * @param costBasisPrice average acquisition cost per contract
     * @param multiplier contract multiplier (typically 100 for equity options)
     * @return calculated P&L rounded to 2 decimals, or null if required data is missing
     */
    public static Double calculateUnrealizedPnl(
            Integer quantity,
            Double markPrice,
            Double costBasisPrice,
            Double multiplier) {

        // Validate required parameters
        if (quantity == null || markPrice == null || costBasisPrice == null || multiplier == null) {
            return null;
        }

        // Calculate market value and cost basis
        double marketValue = quantity * markPrice * multiplier;
        double costBasis = quantity * costBasisPrice * multiplier;

        // P&L = Market Value - Cost Basis
        double pnl = marketValue - costBasis;

        return round2Digits(pnl);
    }

}
