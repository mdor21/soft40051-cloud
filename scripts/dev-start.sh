#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

echo ">> Building Aggregator and Load Balancer images + starting Docker stack..."
docker compose up -d --build

echo ">> Building Host Manager..."
cd hostmanager
mvn -q clean package
cd ..

echo ">> Starting Host Manager..."
./scripts/hostmanager-start.sh

echo ">> All backend services are up."
echo "Next step: open NTU VM via RDP (localhost:3390), then run the GUI as documented."
