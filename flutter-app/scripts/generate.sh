#!/bin/bash

# BuckPal Flutter Code Generation Script
# This script handles code generation for the Flutter app

set -e

echo "🚀 Starting BuckPal Flutter code generation..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Flutter is installed
if ! command -v flutter &> /dev/null; then
    print_error "Flutter is not installed or not in PATH"
    exit 1
fi

print_success "Flutter found: $(flutter --version | head -n1)"

# Get dependencies first
print_step "Getting Flutter dependencies..."
flutter pub get

if [ $? -eq 0 ]; then
    print_success "Dependencies installed successfully"
else
    print_error "Failed to install dependencies"
    exit 1
fi

# Clean previous build
print_step "Cleaning previous build artifacts..."
flutter clean
flutter pub get

# Generate code
print_step "Running code generation..."
flutter packages pub run build_runner build --delete-conflicting-outputs

if [ $? -eq 0 ]; then
    print_success "Code generation completed successfully"
else
    print_error "Code generation failed"
    exit 1
fi

# Run analysis
print_step "Running static analysis..."
flutter analyze

if [ $? -eq 0 ]; then
    print_success "Analysis passed"
else
    print_warning "Analysis found issues (check output above)"
fi

# Format code
print_step "Formatting code..."
dart format lib/ test/ --line-length 80

print_success "Code formatting completed"

# Run tests
print_step "Running tests..."
flutter test

if [ $? -eq 0 ]; then
    print_success "All tests passed"
else
    print_warning "Some tests failed (check output above)"
fi

echo ""
print_success "🎉 Code generation and validation completed!"
echo ""
echo "You can now run the app with:"
echo "  flutter run              # For mobile/desktop"
echo "  flutter run -d web       # For web browser"
echo ""
echo "To rebuild generated files in the future:"
echo "  flutter packages pub run build_runner build --delete-conflicting-outputs"
echo ""
echo "To watch for changes and auto-generate:"
echo "  flutter packages pub run build_runner watch --delete-conflicting-outputs"