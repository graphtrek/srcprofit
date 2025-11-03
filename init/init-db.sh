#!/bin/bash
set -e

: ${POSTGRES_USER:="postgres"}
: ${SRCPROFIT_DB_USER:="srcprofit"}
: ${SRCPROFIT_DB_PWD:="srcprofit"}

# Create srcprofit user if it doesn't exist
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE USER ${SRCPROFIT_DB_USER} WITH PASSWORD ''${SRCPROFIT_DB_PWD}'''
    WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = '${SRCPROFIT_DB_USER}')\gexec
EOSQL

# Create databases and set owner to srcprofit user
for db in srcprofit srcprofit1 srcprofit2 moneypenny stableips; do
    if ! psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -lqt | cut -d \| -f 1 | grep -qw "$db"; then
        echo "Creating database: $db"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            CREATE DATABASE $db OWNER ${SRCPROFIT_DB_USER};
            GRANT ALL PRIVILEGES ON DATABASE $db TO ${SRCPROFIT_DB_USER};
EOSQL
    else
        echo "Database $db already exists, ensuring proper ownership"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            ALTER DATABASE $db OWNER TO ${SRCPROFIT_DB_USER};
            GRANT ALL PRIVILEGES ON DATABASE $db TO ${SRCPROFIT_DB_USER};
EOSQL
    fi
done

# Create schema for each database
create_schema() {
    local db_name=$1
    echo "Creating schema for database: $db_name"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d "$db_name" <<-EOSQL
-- Create INSTRUMENT table
CREATE TABLE IF NOT EXISTS instrument (
    id BIGSERIAL PRIMARY KEY,
    conid BIGINT UNIQUE,
    name VARCHAR(255),
    ticker VARCHAR(255) NOT NULL UNIQUE,
    price DOUBLE PRECISION,
    updated TIMESTAMP,
    change DOUBLE PRECISION,
    change_percent DOUBLE PRECISION,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    earning_date DATE
);
CREATE INDEX IF NOT EXISTS instr_ticker_idx ON instrument(ticker);
CREATE INDEX IF NOT EXISTS instr_conid_idx ON instrument(conid);
CREATE INDEX IF NOT EXISTS instr_name_idx ON instrument(name);

-- Create OPTION table
CREATE TABLE IF NOT EXISTS option (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL REFERENCES instrument(id),
    asset_class VARCHAR(50),
    trade_date DATE NOT NULL,
    quantity INTEGER NOT NULL,
    position_value DOUBLE PRECISION NOT NULL,
    trade_price DOUBLE PRECISION NOT NULL,
    market_value DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    account VARCHAR(255) NOT NULL,
    expiration_date DATE NOT NULL,
    fee DOUBLE PRECISION,
    realized_profit_or_loss DOUBLE PRECISION,
    annualized_roi_percent INTEGER,
    probability INTEGER,
    days_between INTEGER,
    days_left INTEGER,
    color VARCHAR(255),
    note VARCHAR(255),
    conid BIGINT,
    ticker VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    type VARCHAR(50),
    market_price DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS opt_conid_idx ON option(conid);
CREATE INDEX IF NOT EXISTS opt_conid_status_idx ON option(conid, status);
CREATE INDEX IF NOT EXISTS opt_conid_status_price_idx ON option(conid, status, trade_price);
CREATE INDEX IF NOT EXISTS opt_code_idx ON option(code);
CREATE INDEX IF NOT EXISTS opt_ticker_idx ON option(ticker);
CREATE INDEX IF NOT EXISTS opt_instrument_id_idx ON option(instrument_id);
CREATE INDEX IF NOT EXISTS opt_status_idx ON option(status);

-- Create NET_ASSET_VALUE table
CREATE TABLE IF NOT EXISTS net_asset_value (
    id BIGSERIAL PRIMARY KEY,
    account VARCHAR(255) NOT NULL,
    report_date DATE NOT NULL UNIQUE,
    cash DOUBLE PRECISION,
    stock DOUBLE PRECISION,
    options DOUBLE PRECISION,
    dividend_accruals DOUBLE PRECISION,
    interest_accruals DOUBLE PRECISION,
    total DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS nav_report_date_idx ON net_asset_value(report_date);

-- Create EARNING table
CREATE TABLE IF NOT EXISTS earning (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    fiscal_date_ending DATE,
    estimate VARCHAR(255),
    currency VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS earning_report_date_idx ON earning(report_date);
CREATE INDEX IF NOT EXISTS earning_symbol_idx ON earning(symbol);
CREATE INDEX IF NOT EXISTS earning_idx ON earning(symbol, report_date, fiscal_date_ending);

-- Create FLEX_STATEMENT_RESPONSE table
CREATE TABLE IF NOT EXISTS flex_statement_response (
    id BIGSERIAL PRIMARY KEY,
    reference_code VARCHAR(100) NOT NULL UNIQUE,
    request_date VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    report_type VARCHAR(20) NOT NULL,
    original_timestamp VARCHAR(50),
    db_url VARCHAR(255),
    csv_file_path VARCHAR(255),
    csv_records_count INTEGER,
    data_fix_records_count INTEGER
);
CREATE INDEX IF NOT EXISTS fsr_reference_code_idx ON flex_statement_response(reference_code);
CREATE INDEX IF NOT EXISTS fsr_request_date_idx ON flex_statement_response(request_date);
CREATE INDEX IF NOT EXISTS fsr_report_type_idx ON flex_statement_response(report_type);

-- Grant all permissions to srcprofit user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${SRCPROFIT_DB_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${SRCPROFIT_DB_USER};
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO ${SRCPROFIT_DB_USER};
EOSQL

    # Grant default permissions for future tables
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d "$db" <<-EOSQL
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${SRCPROFIT_DB_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${SRCPROFIT_DB_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO ${SRCPROFIT_DB_USER};
EOSQL
}

# Create schema for srcprofit databases
for db in srcprofit srcprofit1 srcprofit2; do
    if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -lqt | cut -d \| -f 1 | grep -qw "$db"; then
        create_schema "$db"
    fi
done

echo "Database initialization complete!"