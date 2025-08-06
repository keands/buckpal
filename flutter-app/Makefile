# BuckPal Flutter App Makefile

.PHONY: help install clean generate build test analyze run

# Default target
help:
	@echo "Available commands:"
	@echo "  install    - Install Flutter dependencies"
	@echo "  clean      - Clean build artifacts"
	@echo "  generate   - Generate code (json_serializable, etc.)"
	@echo "  build      - Build for all platforms"
	@echo "  test       - Run all tests"
	@echo "  analyze    - Run static analysis"
	@echo "  run        - Run the app in development mode"
	@echo "  run-web    - Run the app in web browser"
	@echo "  build-web  - Build for web deployment"
	@echo "  build-android - Build Android APK"
	@echo "  build-ios  - Build iOS app"

# Install dependencies
install:
	flutter pub get

# Clean build artifacts
clean:
	flutter clean
	flutter pub get

# Generate code
generate:
	flutter packages pub run build_runner build --delete-conflicting-outputs

# Watch for changes and regenerate code
watch:
	flutter packages pub run build_runner watch --delete-conflicting-outputs

# Run tests
test:
	flutter test

# Run static analysis
analyze:
	flutter analyze

# Format code
format:
	dart format lib/ test/

# Check formatting
format-check:
	dart format --output=none --set-exit-if-changed lib/ test/

# Run the app
run:
	flutter run

# Run on web
run-web:
	flutter run -d web-server --web-hostname localhost --web-port 8081

# Build for all platforms
build: build-web build-android

# Build for web
build-web:
	flutter build web --release

# Build Android APK
build-android:
	flutter build apk --release

# Build Android App Bundle
build-appbundle:
	flutter build appbundle --release

# Build for iOS (macOS only)
build-ios:
	flutter build ios --release

# Run integration tests
test-integration:
	flutter test integration_test/

# Get dependencies and generate code
setup: install generate

# Full development setup
dev-setup: clean install generate

# Lint and fix code
lint:
	dart fix --apply

# Check for outdated dependencies
outdated:
	flutter pub outdated

# Upgrade dependencies
upgrade:
	flutter pub upgrade

# Create a new feature structure
create-feature:
	@read -p "Enter feature name: " feature; \
	mkdir -p lib/features/$$feature/{data/{models,repositories},domain/{entities,repositories,usecases},presentation/{bloc,pages,widgets}}; \
	echo "Created feature structure for $$feature"

# Bundle web assets for deployment
web-deploy: build-web
	@echo "Web build completed. Deploy contents of build/web/ to your web server."

# Run with specific flavor/environment
run-dev:
	flutter run --flavor development --dart-define=ENVIRONMENT=development

run-staging:
	flutter run --flavor staging --dart-define=ENVIRONMENT=staging

run-prod:
	flutter run --flavor production --dart-define=ENVIRONMENT=production

# Generate app icons (requires flutter_launcher_icons)
icons:
	flutter pub run flutter_launcher_icons:main

# Generate splash screens (requires flutter_native_splash)
splash:
	flutter pub run flutter_native_splash:create

# Security audit
security:
	flutter pub deps

# Performance analysis
profile:
	flutter run --profile

# Check app size
size-analysis:
	flutter build apk --analyze-size

# Doctor check
doctor:
	flutter doctor -v