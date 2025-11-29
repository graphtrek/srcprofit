-- Migration: Add calculated fields to OPTION_SNAPSHOT table
-- Purpose: Store ROI, POP, and daysLeft calculations
-- Date: 2025-11-28

ALTER TABLE OPTION_SNAPSHOT
ADD COLUMN days_left INTEGER COMMENT 'Days remaining until expiration (can be negative)',
ADD COLUMN roi_on_collateral INTEGER COMMENT 'Annualized ROI on capital at risk (strike price)',
ADD COLUMN roi_on_premium INTEGER COMMENT 'Annualized ROI on premium (midPrice)',
ADD COLUMN pop INTEGER COMMENT 'Probability of Profit using delta approximation (0-100)';
