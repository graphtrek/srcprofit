-- Migration: Add OPTION_SNAPSHOT table for storing option chain snapshot data from Alpaca Data API
-- Purpose: Store latest trading data, quotes, and Greeks for option contracts
-- Date: 2025-11-28

CREATE TABLE IF NOT EXISTS OPTION_SNAPSHOT (
    id BIGSERIAL PRIMARY KEY,

    -- Contract Identification
    symbol VARCHAR(50) UNIQUE NOT NULL COMMENT 'OCC contract symbol, e.g., AAPL230120C00150000',
    instrument_id BIGINT NOT NULL COMMENT 'Foreign key to INSTRUMENT table',
    option_type VARCHAR(10) NOT NULL COMMENT 'call or put',
    strike_price NUMERIC(10, 2) NOT NULL COMMENT 'Strike price, e.g., 150.00',
    expiration_date DATE NOT NULL COMMENT 'Expiration date extracted from OCC symbol',

    -- Latest Trade
    last_trade_time TIMESTAMP WITH TIME ZONE COMMENT 'When the last trade occurred',
    last_trade_exchange VARCHAR(10) COMMENT 'Exchange code for last trade',
    last_trade_price NUMERIC(10, 4) COMMENT 'Price of last trade',
    last_trade_size INTEGER COMMENT 'Size of last trade',

    -- Latest Quote (Bid/Ask)
    last_quote_time TIMESTAMP WITH TIME ZONE COMMENT 'When the last quote was updated',
    ask_exchange VARCHAR(10) COMMENT 'Exchange code for ask price',
    ask_price NUMERIC(10, 4) COMMENT 'Current ask (offer) price',
    ask_size INTEGER COMMENT 'Size available at ask price',
    bid_exchange VARCHAR(10) COMMENT 'Exchange code for bid price',
    bid_price NUMERIC(10, 4) COMMENT 'Current bid price',
    bid_size INTEGER COMMENT 'Size available at bid price',

    -- Greeks (Option Risk Sensitivities)
    delta NUMERIC(8, 6) COMMENT 'Delta: rate of change vs stock price',
    gamma NUMERIC(8, 6) COMMENT 'Gamma: rate of change of delta',
    theta NUMERIC(8, 6) COMMENT 'Theta: time decay (usually negative)',
    vega NUMERIC(8, 6) COMMENT 'Vega: sensitivity to volatility',
    rho NUMERIC(8, 6) COMMENT 'Rho: sensitivity to interest rates',
    implied_volatility NUMERIC(6, 4) COMMENT 'Implied volatility (0-1)',

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_updated_at TIMESTAMP COMMENT 'When snapshot data was last refreshed from API',

    -- Foreign Key
    CONSTRAINT fk_option_snapshot_instrument
        FOREIGN KEY (instrument_id)
        REFERENCES INSTRUMENT(id)
        ON DELETE CASCADE
);

-- Create indexes for common queries
CREATE INDEX opt_snap_symbol_idx ON OPTION_SNAPSHOT(symbol);
CREATE INDEX opt_snap_instrument_idx ON OPTION_SNAPSHOT(instrument_id);
CREATE INDEX opt_snap_expiration_idx ON OPTION_SNAPSHOT(expiration_date);
CREATE INDEX opt_snap_type_idx ON OPTION_SNAPSHOT(option_type);

-- Composite indexes for common filtering patterns
CREATE INDEX opt_snap_instrument_expiration_idx ON OPTION_SNAPSHOT(instrument_id, expiration_date);
CREATE INDEX opt_snap_instrument_type_idx ON OPTION_SNAPSHOT(instrument_id, option_type);
