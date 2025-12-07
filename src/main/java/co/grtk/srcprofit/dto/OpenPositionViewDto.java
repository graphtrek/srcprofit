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
    Double pnl,
    Integer roi,
    Integer pop,
    String type
) {}
