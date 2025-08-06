#!/bin/bash

# BuckPal Flutter Build Script for All Platforms
# This script builds the Flutter app for Web, Android, and iOS

set -e

echo "🚀 Building BuckPal Flutter App for all platforms..."

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

# Create builds directory
mkdir -p builds

print_step "Preparing build environment..."

# Clean and get dependencies
flutter clean
flutter pub get

# Generate code
print_step "Generating code..."
flutter packages pub run build_runner build --delete-conflicting-outputs

# Build for Web
print_step "Building for Web..."
flutter build web --release

if [ $? -eq 0 ]; then
    print_success "Web build completed"
    # Copy web build to builds directory
    cp -r build/web builds/web
    print_success "Web build copied to builds/web"
else
    print_error "Web build failed"
fi

# Build for Android APK
print_step "Building Android APK..."
flutter build apk --release

if [ $? -eq 0 ]; then
    print_success "Android APK build completed"
    # Copy APK to builds directory
    cp build/app/outputs/flutter-apk/app-release.apk builds/buckpal-android.apk
    print_success "APK copied to builds/buckpal-android.apk"
else
    print_error "Android APK build failed"
fi

# Build for Android App Bundle
print_step "Building Android App Bundle..."
flutter build appbundle --release

if [ $? -eq 0 ]; then
    print_success "Android App Bundle build completed"
    # Copy AAB to builds directory
    cp build/app/outputs/bundle/release/app-release.aab builds/buckpal-android.aab
    print_success "App Bundle copied to builds/buckpal-android.aab"
else
    print_error "Android App Bundle build failed"
fi

# Build for iOS (only on macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    print_step "Building for iOS..."
    flutter build ios --release --no-codesign
    
    if [ $? -eq 0 ]; then
        print_success "iOS build completed"
        print_warning "iOS build requires Xcode for final packaging and signing"
    else
        print_error "iOS build failed"
    fi
else
    print_warning "Skipping iOS build (requires macOS)"
fi

# Calculate build sizes
print_step "Calculating build sizes..."

echo ""
echo "📊 Build Summary:"
echo "=================="

if [ -f "builds/buckpal-android.apk" ]; then
    APK_SIZE=$(du -sh builds/buckpal-android.apk | cut -f1)
    echo "Android APK:      $APK_SIZE"
fi

if [ -f "builds/buckpal-android.aab" ]; then
    AAB_SIZE=$(du -sh builds/buckpal-android.aab | cut -f1)
    echo "Android Bundle:   $AAB_SIZE"
fi

if [ -d "builds/web" ]; then
    WEB_SIZE=$(du -sh builds/web | cut -f1)
    echo "Web Build:        $WEB_SIZE"
fi

if [ -d "build/ios" ]; then
    IOS_SIZE=$(du -sh build/ios | cut -f1)
    echo "iOS Build:        $IOS_SIZE"
fi

echo ""
print_success "🎉 Build process completed!"
echo ""
echo "Build artifacts are available in the 'builds/' directory:"
echo "  - Web: builds/web/"
echo "  - Android APK: builds/buckpal-android.apk"
echo "  - Android Bundle: builds/buckpal-android.aab"
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "  - iOS: build/ios/iphoneos/Runner.app (requires Xcode for packaging)"
fi
echo ""
echo "Deployment instructions:"
echo "========================"
echo "🌐 Web: Upload contents of builds/web/ to your web server"
echo "📱 Android: Upload APK or AAB to Google Play Console"
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "🍎 iOS: Open build/ios/iphoneos/Runner.app in Xcode for App Store submission"
fi

# Generate deployment info
cat > builds/deployment-info.txt << EOF
BuckPal Flutter App - Build Information
=======================================
Build Date: $(date)
Flutter Version: $(flutter --version | head -n1)
Build Environment: $(uname -s) $(uname -r)

Artifacts:
- Web build: builds/web/
- Android APK: builds/buckpal-android.apk
- Android Bundle: builds/buckpal-android.aab

Deployment Notes:
- Web: Deploy contents of web/ to your HTTP server
- Android: Upload to Google Play Console
- iOS: Package and sign with Xcode before App Store submission

For production deployment:
1. Update API endpoints in app_constants.dart
2. Configure proper certificates and signing
3. Test on target platforms before release
4. Set up CI/CD pipeline for automated builds
EOF

print_success "Build information saved to builds/deployment-info.txt"