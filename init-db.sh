#!/bin/bash
set -e
until pg_isready -h db -p 5432 -U postgres -d notesdb; do
  echo "Postgres is unavailable - sleeping"
  sleep 1
done
echo "Postgres is up - executing database setup"
psql -h db -U postgres -d notesdb -f /docker-entrypoint-initdb.d/database-setup.sql