-- ISSUE-014: Add Alpaca Asset Metadata Columns to INSTRUMENT table
-- Date: 2025-11-09
-- Description: Extend InstrumentEntity with Alpaca-specific asset metadata fields
-- for pre-trade validation, position constraints, and margin calculations

-- Add Alpaca asset metadata fields to INSTRUMENT table
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_asset_id VARCHAR(36) UNIQUE;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_tradable BOOLEAN;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_marginable BOOLEAN;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_shortable BOOLEAN;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_easy_to_borrow BOOLEAN;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_fractionable BOOLEAN;
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_maintenance_margin_requirement NUMERIC(10, 4);
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_exchange VARCHAR(50);
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_status VARCHAR(50);
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_asset_class VARCHAR(50);
ALTER TABLE INSTRUMENT ADD COLUMN IF NOT EXISTS alpaca_metadata_updated_at TIMESTAMP;

-- Create index for efficient alpaca_asset_id lookups
CREATE INDEX IF NOT EXISTS instr_alpaca_asset_id_idx ON INSTRUMENT(alpaca_asset_id);

-- Notes:
-- - alpaca_asset_id: UUID from Alpaca API, unique identifier for the asset
-- - alpaca_tradable: Whether the asset can be traded (pre-trade validation)
-- - alpaca_marginable: Whether the asset can be bought on margin
-- - alpaca_shortable: Whether the asset can be shorted
-- - alpaca_easy_to_borrow: Whether the asset is easy to borrow (for short selling)
-- - alpaca_fractionable: Whether the asset supports fractional trading
-- - alpaca_maintenance_margin_requirement: Decimal value (e.g., 0.2 = 20%)
-- - alpaca_exchange: Exchange where the asset is traded (NYSE, NASDAQ, etc.)
-- - alpaca_status: Asset status (active, inactive, etc.)
-- - alpaca_asset_class: Asset class (us_equity, crypto, etc.)
-- - alpaca_metadata_updated_at: Timestamp of last metadata fetch from Alpaca API
