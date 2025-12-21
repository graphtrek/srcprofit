package co.grtk.srcprofit.dto;

import java.time.LocalDate;

public record StockPositionViewDto(
    Long id,
    String symbol,
    LocalDate tradeDate,
    Integer quantity,
    Double costBasisMoney,
    Double markPrice,
    Double positionValue,
    Double pnl,
    Double percentOfNAV
) {}
