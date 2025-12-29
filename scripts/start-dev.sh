#!/bin/bash
echo "Building all services and starting the system..."
sudo docker compose up --build -d
echo "System started. Tailing logs (Press Ctrl+C to stop)..."
sudo docker compose logs -f
