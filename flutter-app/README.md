# BuckPal Flutter App

BuckPal is a comprehensive budget management application built with Flutter, supporting iOS, Android, and Web platforms. It provides users with powerful tools to track their finances, manage accounts, import transactions, and visualize spending patterns.

## Features

### 🔐 Authentication
- User registration and login
- JWT token-based authentication
- Secure token storage with FlutterSecureStorage
- Form validation with comprehensive error handling

### 💰 Financial Management
- Multi-account support (Checking, Savings, Credit Cards, etc.)
- Transaction tracking with automatic categorization
- CSV import functionality for bulk transaction imports
- Real-time balance calculations
- Income vs expense tracking

### 📊 Dashboard & Analytics
- Overview of total balance, income, and expenses
- Recent transactions display
- Quick action buttons for common tasks
- Visual indicators for different transaction types

### 🏗️ Architecture
- **Clean Architecture** with clear separation of concerns
- **Domain Layer**: Business logic and entities
- **Data Layer**: API calls, models, and repositories
- **Presentation Layer**: UI components, BLoC state management
- **Dependency Injection** using Provider pattern

### 🛠️ Technical Stack
- **Flutter**: Cross-platform mobile framework
- **Dart**: Programming language
- **BLoC**: State management pattern
- **Dio**: HTTP client for API calls
- **GoRouter**: Navigation and routing
- **json_annotation**: JSON serialization
- **flutter_secure_storage**: Secure local storage

## Project Structure

```
lib/
├── core/
│   ├── constants/          # App-wide constants
│   ├── network/           # HTTP client configuration
│   ├── storage/           # Local storage utilities
│   └── utils/             # Common utilities
├── features/
│   ├── auth/              # Authentication feature
│   │   ├── data/          # Models, repositories implementation
│   │   ├── domain/        # Entities, repositories interface, use cases
│   │   └── presentation/  # UI pages, widgets, BLoC
│   ├── dashboard/         # Dashboard feature
│   ├── transactions/      # Transaction management
│   └── accounts/          # Account management
└── main.dart              # App entry point
```

## Getting Started

### Prerequisites
- Flutter SDK (>=3.1.0)
- Dart SDK
- Android Studio / VS Code
- Backend API running (Spring Boot)

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd buckpal/flutter-app
```

2. Install dependencies:
```bash
flutter pub get
```

3. Generate code:
```bash
flutter packages pub run build_runner build
```

4. Configure API endpoint in `lib/core/constants/app_constants.dart`:
```dart
static const String baseUrl = 'http://your-backend-url:8080/api';
```

5. Run the app:
```bash
# For development
flutter run

# For web
flutter run -d web

# For specific platform
flutter run -d android
flutter run -d ios
```

## Code Generation

This project uses code generation for JSON serialization and other boilerplate code:

```bash
# Generate code once
flutter packages pub run build_runner build

# Watch for changes and auto-generate
flutter packages pub run build_runner watch

# Clean and rebuild
flutter packages pub run build_runner build --delete-conflicting-outputs
```

## API Integration

The app integrates with the BuckPal Spring Boot backend:

- **Authentication**: POST `/api/auth/signin`, `/api/auth/signup`
- **Accounts**: GET/POST `/api/accounts`
- **Transactions**: GET/POST `/api/transactions`
- **CSV Import**: POST `/api/transactions/import-csv/{accountId}`

## State Management

Using BLoC pattern for predictable state management:

```dart
// Event
context.read<AuthBloc>().add(AuthLoginRequested(email: email, password: password));

// State listening
BlocListener<AuthBloc, AuthState>(
  listener: (context, state) {
    if (state is AuthAuthenticated) {
      // Handle success
    }
  },
  child: Widget(),
)
```

## Testing

Run tests:
```bash
# Unit tests
flutter test

# Integration tests
flutter test integration_test/
```

## Building for Production

### Android
```bash
flutter build apk --release
flutter build appbundle --release
```

### iOS
```bash
flutter build ios --release
```

### Web
```bash
flutter build web --release
```

## Configuration

### Environment Variables
Create `.env` files for different environments:
- `.env.development`
- `.env.staging` 
- `.env.production`

### Theme Customization
Modify themes in `lib/core/constants/app_constants.dart`:
```dart
static const Color primaryColor = Color(0xFF2E7D32);
static const Color secondaryColor = Color(0xFF4CAF50);
```

## Contributing

1. Follow the established architecture patterns
2. Write tests for new features
3. Use proper commit message conventions
4. Ensure code passes linting rules
5. Update documentation as needed

## Security Considerations

- JWT tokens stored securely using FlutterSecureStorage
- API calls use HTTPS in production
- Input validation on all forms
- Error handling without exposing sensitive information

## Performance Optimization

- Image caching and optimization
- Lazy loading of data
- Efficient state management
- Code splitting for web platform

## Deployment

### Web Deployment
```bash
flutter build web --release
# Deploy contents of build/web/ to your web server
```

### Mobile App Stores
- Follow platform-specific deployment guides
- Configure app signing and certificates
- Set up CI/CD pipelines for automated builds

## Support

For issues and feature requests, please use the GitHub issues tracker.

## License

This project is licensed under the MIT License - see the LICENSE file for details.