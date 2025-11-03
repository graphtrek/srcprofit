-- Seed data for instrument table
-- Source: Local srcprofit2 database (2025-11-03)
-- Purpose: Provide initial instrument data needed for FLEX imports to work

-- Insert instruments (10 rows)
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (1, 'RIVN', 525768800, NULL, 13.195, -0.39, -3.04, '2025-11-05');
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (2, 'RKLB', 510685704, NULL, 61.29, -1.69, -2.74, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (3, 'TSLA', 76792991, NULL, 468.23, 11.72, 2.62, '2026-01-28');
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (4, 'QQQ', NULL, NULL, 632.06, 2.8, 0.44, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (5, 'GDX', NULL, NULL, 71.315, -0.71, -0.97, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (6, 'IBIT', NULL, NULL, 60.53, -1.77, -2.83, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (11, 'TMF', 665380902, NULL, 41.08, -0.36, -0.86, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (12, 'TBIL', NULL, NULL, 49.865, -0.16, -0.31, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (13, 'SLV', NULL, NULL, 43.755, -0.27, -0.6, NULL);
INSERT INTO instrument (id, ticker, conid, name, price, change, change_percent, earning_date) VALUES (14, 'URA', NULL, NULL, 52.55, -2.56, -4.53, NULL);

-- Update sequence to prevent ID conflicts
SELECT setval('instrument_id_seq', (SELECT MAX(id) FROM instrument));
