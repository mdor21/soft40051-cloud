#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

# Kill existing Host Manager if running
HM_PID=$(ps aux | grep 'hostmanager-1.0-SNAPSHOT.jar' | grep -v grep | awk '{print $2}' || true)
if [ -n "$HM_PID" ]; then
  kill "$HM_PID"
  sleep 1
fi

# Start new Host Manager
cd hostmanager
nohup java -jar target/hostmanager-1.0-SNAPSHOT.jar > hostmanager.log 2>&1 &

echo "Host Manager started (logs: hostmanager/hostmanager.log)"
