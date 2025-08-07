# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BuckPal is a multi-platform budget management application consisting of:
- **Spring Boot Backend** (Java 17) with PostgreSQL database
- **Flutter Frontend** supporting iOS, Android, and Web platforms

## Project Structure

```
buckpal/
├── src/main/java/com/buckpal/     # Spring Boot backend
│   ├── controller/                # REST API endpoints
│   ├── service/                   # Business logic
│   ├── entity/                    # JPA entities
│   ├── repository/                # Data access layer
│   ├── dto/                       # Data transfer objects
│   ├── config/                    # Configuration classes
│   └── security/                  # JWT authentication
├── src/test/                      # Backend unit/integration tests
├── flutter-app/                   # Flutter multiplatform app
│   └── lib/
│       ├── core/                  # Utilities, constants, network
│       └── features/              # Feature-based clean architecture
│           ├── auth/              # Authentication
│           ├── dashboard/         # Dashboard views
│           └── transactions/      # Transaction management
└── pom.xml                        # Maven configuration
```

## Backend Development (Spring Boot)

### Commands
- **Start backend**: `mvn spring-boot:run`
- **Run tests**: `mvn test`
- **Build JAR**: `mvn clean package`
- **Run with profile**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

### Architecture
- **JWT Authentication** with Spring Security
- **Clean layered architecture**: Controller → Service → Repository → Entity
- **PostgreSQL database** with JPA/Hibernate
- **Plaid API integration** for bank synchronization
- **CSV import service** for bulk transaction uploads

### Key Configuration
- Database config in `src/main/resources/application.yml`
- Environment variables: `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, `PLAID_CLIENT_ID`, `PLAID_SECRET`
- API runs on port 8080 by default

### API Endpoints
- Authentication: `/api/auth/signin`, `/api/auth/signup`
- Accounts: `/api/accounts` (CRUD operations)
- Transactions: `/api/transactions` with pagination and CSV import
- Health checks: `/actuator/health`

## Frontend Development (Flutter)

### Commands
Navigation to flutter-app directory required for all Flutter commands:
```bash
cd flutter-app
```

**Essential development commands:**
- **Install dependencies**: `flutter pub get`
- **Generate code**: `flutter packages pub run build_runner build --delete-conflicting-outputs`
- **Watch for changes**: `flutter packages pub run build_runner watch --delete-conflicting-outputs`
- **Run app**: `flutter run` (mobile/desktop) or `flutter run -d web` (web)
- **Run tests**: `flutter test`
- **Static analysis**: `flutter analyze`
- **Format code**: `dart format lib/ test/ --line-length 80`

**Build commands:**
- **Web**: `flutter build web --release`
- **Android APK**: `flutter build apk --release`
- **Android Bundle**: `flutter build appbundle --release`
- **iOS**: `flutter build ios --release` (macOS only)

**Makefile shortcuts** (in flutter-app/):
- `make help` - Show all available commands
- `make setup` - Install dependencies and generate code
- `make dev-setup` - Full development setup (clean + install + generate)
- `make generate` - Generate code only
- `make test` - Run tests
- `make build` - Build for all platforms
- `make run-web` - Run on web with custom port (8081)

### Architecture
- **Clean Architecture** with feature-based organization
- **BLoC pattern** for state management
- **Repository pattern** for data access
- **Dependency injection** using Provider pattern

### Key Files
- **Entry point**: `lib/main.dart`
- **App constants**: `lib/core/constants/app_constants.dart` (API endpoints, colors, etc.)
- **Network client**: `lib/core/network/dio_client.dart`
- **Secure storage**: `lib/core/storage/secure_storage.dart`

### Code Generation Requirements
- **JSON serialization**: Uses `json_annotation` and `build_runner`
- **Generated files** have `.g.dart` extension
- **Must run code generation** after modifying models or API clients
- Generated files are excluded from linting in `analysis_options.yaml`

### Navigation
- Uses **GoRouter** for navigation
- Routes defined in `main.dart`: `/`, `/register`, `/dashboard`

## Development Workflow

### Backend Setup
1. Ensure Java 17+ and Maven installed
2. Set up PostgreSQL database: `createdb buckpal_db`
3. Configure environment variables or update `application.yml`
4. Run: `mvn spring-boot:run`

### Frontend Setup
1. Navigate to `flutter-app/` directory
2. Install dependencies: `flutter pub get`
3. Generate code: `flutter packages pub run build_runner build --delete-conflicting-outputs`
4. Update API endpoint in `lib/core/constants/app_constants.dart` if needed
5. Run: `flutter run` or `flutter run -d web`

### Full Development Setup
Use the provided scripts for comprehensive setup:
- **Backend**: `mvn spring-boot:run`
- **Frontend**: `cd flutter-app && make dev-setup && flutter run`

## Testing

### Test-Driven Development (TDD) Requirements
**MANDATORY**: All new components, services, and features MUST follow TDD principles:
1. **Write tests FIRST** before implementing functionality
2. **Red-Green-Refactor cycle**: Write failing test → Make it pass → Refactor
3. **100% test coverage** for business logic (services, use cases, repositories)
4. **Unit tests required** for every new class, method, and feature

### Backend Testing
- **Unit tests** with JUnit 5 and Mockito for all services, repositories, and controllers
- **Integration tests** with TestContainers for database operations
- **Security tests** for authentication and authorization flows
- **TDD approach**: Write `@Test` methods before implementing business logic
- **Test structure**: Given-When-Then pattern using AssertJ
- Run with: `mvn test`
- **Coverage requirement**: Minimum 90% code coverage for service layer

### Frontend Testing
- **Unit tests** for all business logic (use cases, repositories, models)
- **Widget tests** for all custom UI components
- **BLoC tests** using `bloc_test` package for all state management
- **Integration tests** for complete user flows
- **TDD approach**: Write test files (`.test.dart`) before implementation
- **Mock external dependencies** using Mockito
- Run with: `flutter test` (from flutter-app directory)
- **Coverage requirement**: Run `flutter test --coverage` and maintain >85% coverage

### TDD Best Practices
- **Backend**: Create test classes in `src/test/java/com/buckpal/` mirroring production structure
- **Frontend**: Create test files in `test/` directory mirroring `lib/` structure
- **Naming convention**: `ClassNameTest.java` (backend) or `class_name_test.dart` (frontend)
- **Test isolation**: Each test should be independent and repeatable
- **Arrange-Act-Assert** pattern for test structure
- **Edge cases**: Test null values, empty collections, error conditions
- **Integration points**: Mock external services (Plaid API, database)

## Environment Configuration

### Backend Environment Variables
```bash
export JWT_SECRET=yourJwtSecretKey
export PLAID_CLIENT_ID=your_plaid_client_id
export PLAID_SECRET=your_plaid_secret
export DB_USERNAME=database_username
export DB_PASSWORD=database_password
```

### Flutter Configuration
- API endpoint configured in `lib/core/constants/app_constants.dart`
- Default backend URL: `http://localhost:8080/api`
- Supports multiple environments via build flavors

## Important Notes

- **Code generation is required** for Flutter after modifying models or API clients
- **Database migrations** handled automatically by Hibernate DDL (update mode)
- **JWT tokens** stored securely using FlutterSecureStorage
- **CORS configuration** in Spring Boot allows Flutter web clients
- **Linting rules** strictly enforced for Flutter (see `analysis_options.yaml`)
- **Multi-platform support**: Web, Android, iOS with responsive UI