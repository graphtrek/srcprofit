package co.grtk.srcprofit.dto;

import java.time.LocalDate;

public record OpenPositionViewDto(
    Long id,
    String symbol,
    LocalDate tradeDate,
    LocalDate expirationDate,
    Integer daysLeft,
    Integer qty,
    Double strikePrice,
    Double underlyingPrice,
    Double pnl,              // Static P&L from IBKR
    Double calculatedPnl,    // Dynamic calculated P&L (ISSUE-049)
    Integer roi,
    Integer pop,
    String type
) {}
