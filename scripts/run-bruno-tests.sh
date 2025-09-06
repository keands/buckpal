#!/bin/bash

# Bruno API Tests Runner Script for BuckPal
# Usage: ./scripts/run-bruno-tests.sh [environment]
# Environment: local (default) | ci

set -e

ENVIRONMENT=${1:-local}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BRUNO_DIR="$PROJECT_ROOT/bruno-tests/BuckPal-API"

echo "🚀 Starting BuckPal API Integration Tests with Bruno"
echo "Environment: $ENVIRONMENT"
echo "Project Root: $PROJECT_ROOT"
echo "Bruno Directory: $BRUNO_DIR"

# Check if Bruno CLI is installed
if ! command -v bru &> /dev/null; then
    echo "❌ Bruno CLI is not installed. Installing..."
    npm install -g @usebruno/cli
fi

# Check if Spring Boot app is running
echo "🔍 Checking if Spring Boot application is running..."
if curl -f http://localhost:8080/actuator/health &> /dev/null; then
    echo "✅ Spring Boot application is running"
else
    echo "❌ Spring Boot application is not running"
    echo "Please start the application first:"
    echo "  mvn spring-boot:run"
    exit 1
fi

# Navigate to Bruno directory
cd "$BRUNO_DIR"

echo "🧪 Running Bruno API tests..."

# Run Bruno tests based on environment
case $ENVIRONMENT in
    "local")
        echo "Running tests with Local environment..."
        bru run --env Local --reporter-html-template
        ;;
    "ci")
        echo "Running tests with CI environment..."
        bru run --env CI --output junit.xml --format junit
        ;;
    *)
        echo "Unknown environment: $ENVIRONMENT"
        echo "Available environments: local, ci"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo "✅ All Bruno API tests passed!"
else
    echo "❌ Some Bruno API tests failed!"
    exit 1
fi