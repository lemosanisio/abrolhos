#!/bin/bash

# Function to kill background processes on exit
cleanup() {
    echo "Stopping services..."
    kill $(jobs -p)
    docker compose stop
}
trap cleanup EXIT

echo "Starting PostgreSQL..."
cd backend/container
docker compose up -d
cd ../..

echo "Waiting for DB to be ready..."
sleep 5

echo "Starting Backend..."
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/abrolhos
export DB_USERNAME=abrolhos
export DB_PASSWORD=abrolhos
export CORS_ALLOWED_ORIGINS=http://localhost:5173
./gradlew bootRun &
BACKEND_PID=$!
cd ..

echo "Starting React Frontend..."
cd frontend-react
bun run dev &
FRONTEND_PID=$!
cd ..

echo "Application is starting..."
echo "Backend (HTMX): http://localhost:8080"
echo "Frontend (React): http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop."

wait $BACKEND_PID $FRONTEND_PID
