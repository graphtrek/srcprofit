package co.grtk.srcprofit.repository;

import co.grtk.srcprofit.entity.OpenPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for OpenPositionEntity - IBKR Flex Report open positions snapshots.
 *
 * Provides data access methods for querying, inserting, and updating open positions.
 * The findByConid() method is critical for upsert logic (check if position exists before insert/update).
 *
 * @see OpenPositionEntity for entity structure
 * @see OpenPositionService for CSV parsing and persistence logic
 */
@Repository
public interface OpenPositionRepository extends JpaRepository<OpenPositionEntity, Long> {

    /**
     * Upsert lookup: Find position by conid (contract ID).
     *
     * Returns the existing position if found, allowing saveCSV() to update it.
     * Returns null if position doesn't exist, triggering a new insert.
     *
     * Natural key: conid is unique per contract (enforced by database constraint).
     *
     * @param conid IBKR contract ID
     * @return OpenPositionEntity if found, null otherwise
     */
    OpenPositionEntity findByConid(Long conid);

    /**
     * Find all positions of a specific asset class.
     *
     * Useful for filtering: findByAssetClass("OPT") returns only options positions.
     * Supports: OPT, STK, CASH, BOND, FOP, etc.
     *
     * @param assetClass the asset class to filter by (case-sensitive)
     * @return list of positions matching the asset class
     */
    List<OpenPositionEntity> findByAssetClass(String assetClass);

    /**
     * Find all positions with a specific symbol/ticker.
     *
     * Useful for analyzing positions in a single underlying: findBySymbol("SPY")
     *
     * @param symbol the ticker symbol to filter by (case-sensitive)
     * @return list of positions with matching symbol
     */
    List<OpenPositionEntity> findBySymbol(String symbol);

    /**
     * Find all positions from a specific report date.
     *
     * Useful for analyzing snapshots from a particular day.
     *
     * @param reportDate the report date to filter by
     * @return list of positions from that date
     */
    @Query("SELECT o FROM OpenPositionEntity o WHERE o.reportDate = :reportDate")
    List<OpenPositionEntity> findByReportDate(@Param("reportDate") LocalDate reportDate);

    /**
     * Find all option positions (asset class = 'OPT').
     *
     * Convenience method for filtering to only options.
     *
     * @return list of all option positions
     */
    @Query("SELECT o FROM OpenPositionEntity o WHERE o.assetClass = 'OPT' ORDER BY o.symbol ASC")
    List<OpenPositionEntity> findAllOptions();

    /**
     * Find all option positions with reportDate on or after the specified start date.
     *
     * Useful for filtering options by snapshot date: getAllOpenOptionDtos(startDate)
     * supports date-filtered queries in controllers.
     *
     * @param startDate the earliest report date to include (inclusive)
     * @return list of option positions with reportDate >= startDate, ordered by reportDate DESC
     */
    @Query("SELECT o FROM OpenPositionEntity o WHERE o.assetClass = 'OPT' " +
           "AND o.reportDate >= :startDate ORDER BY o.reportDate DESC")
    List<OpenPositionEntity> findAllOptionsByDate(@Param("startDate") LocalDate startDate);

    /**
     * Count positions by asset class.
     *
     * Useful for portfolio summary: How many option vs stock positions?
     *
     * @param assetClass the asset class to count
     * @return count of positions in that asset class
     */
    @Query("SELECT COUNT(o) FROM OpenPositionEntity o WHERE o.assetClass = :assetClass")
    long countByAssetClass(@Param("assetClass") String assetClass);

    /**
     * Find all option positions with their underlying instruments eagerly loaded.
     * Uses regular LEFT JOIN (not FETCH) to handle missing referenced entities gracefully.
     * Fetches are done automatically by accessing the relationship.
     *
     * @return List of option positions with underlying instruments
     */
    @Query("SELECT DISTINCT op FROM OpenPositionEntity op " +
           "LEFT JOIN op.underlyingInstrument " +
           "WHERE op.assetClass = 'OPT' " +
           "ORDER BY op.symbol ASC, op.expirationDate ASC")
    List<OpenPositionEntity> findAllOptionsWithUnderlying();

    /**
     * Find all stock positions with their instruments eagerly loaded.
     * Uses regular LEFT JOIN (not FETCH) to handle missing referenced entities gracefully.
     *
     * @return List of stock positions with instruments
     */
    @Query("SELECT DISTINCT op FROM OpenPositionEntity op " +
           "LEFT JOIN op.instrument " +
           "WHERE op.assetClass = 'STK' " +
           "ORDER BY op.symbol ASC")
    List<OpenPositionEntity> findAllStocksWithInstrument();

    /**
     * Find option positions by underlying ticker symbol.
     * Example: Find all SPY option positions.
     *
     * @param ticker Underlying instrument ticker
     * @return List of option positions for that underlying
     */
    @Query("SELECT DISTINCT op FROM OpenPositionEntity op " +
           "LEFT JOIN op.underlyingInstrument i " +
           "WHERE op.assetClass = 'OPT' AND i.ticker = :ticker " +
           "ORDER BY op.expirationDate ASC, op.strike ASC, op.putCall ASC")
    List<OpenPositionEntity> findOptionsByUnderlyingTicker(@Param("ticker") String ticker);

    /**
     * Find all positions (any asset class) with their related instruments eagerly loaded.
     * Uses regular LEFT JOIN (not FETCH) to handle missing referenced entities gracefully.
     *
     * @return List of all positions with instruments
     */
    @Query("SELECT DISTINCT op FROM OpenPositionEntity op " +
           "LEFT JOIN op.instrument " +
           "LEFT JOIN op.underlyingInstrument " +
           "ORDER BY op.assetClass ASC, op.symbol ASC")
    List<OpenPositionEntity> findAllWithInstruments();

    /**
     * Find all positions for a specific account.
     *
     * Used by saveCSV() deletion logic to scope cleanup to positions from specific accounts.
     * Enables account-scoped deletion: only positions from accounts present in CSV are deleted.
     *
     * @param account the client account ID (e.g., "DU12345")
     * @return list of all positions for that account
     */
    List<OpenPositionEntity> findByAccount(String account);
}
