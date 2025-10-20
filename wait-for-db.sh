#!/bin/bash
set -e
until nc -z db 5432; do
  echo "Postgres is unavailable - sleeping"
  sleep 1
done
echo "Postgres is up - executing command"
./gradlew assembleDebug
tail -f /dev/null