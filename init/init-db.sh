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

echo "Database initialization complete!"