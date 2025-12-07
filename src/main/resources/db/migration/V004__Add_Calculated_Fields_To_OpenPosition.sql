-- ISSUE-048: Add persisted calculated fields to OPEN_POSITION
-- This optimization moves tradeDate, daysBetween, and roi calculations from
-- convertToOpenPositionViewDto() to saveCSV() to eliminate N+1 query problem.

ALTER TABLE OPEN_POSITION ADD COLUMN trade_date DATE;
ALTER TABLE OPEN_POSITION ADD COLUMN days_between INTEGER;
ALTER TABLE OPEN_POSITION ADD COLUMN roi INTEGER;
