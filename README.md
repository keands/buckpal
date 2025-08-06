# BuckPal - Multi-Platform Budget Management Application

BuckPal is a comprehensive budget management solution consisting of a Spring Boot backend API and a Flutter multiplatform frontend (iOS, Android, Web).

## 🏗️ Architecture Overview

```
buckpal/
├── src/                    # Spring Boot Backend
│   ├── main/java/com/buckpal/
│   │   ├── controller/     # REST API Controllers
│   │   ├── service/        # Business Logic Services
│   │   ├── entity/         # JPA Entities
│   │   ├── repository/     # Data Access Layer
│   │   ├── dto/           # Data Transfer Objects
│   │   ├── config/        # Configuration Classes
│   │   └── security/      # Security & JWT Configuration
│   └── test/              # Unit Tests
├── flutter-app/           # Flutter Multiplatform Frontend
│   └── lib/
│       ├── core/          # Core utilities and constants
│       └── features/      # Feature-based modules
└── pom.xml               # Maven configuration
```

## 🚀 Features

### Backend (Spring Boot)
- ✅ **JWT Authentication** with Spring Security
- ✅ **RESTful API** with comprehensive endpoints
- ✅ **PostgreSQL Database** with JPA/Hibernate
- ✅ **CSV Import** for bulk transaction uploads
- ✅ **Automatic Transaction Categorization** 
- ✅ **Plaid API Integration** for bank synchronization
- ✅ **Comprehensive Unit Tests** with Mockito
- ✅ **Docker Ready** configuration

### Frontend (Flutter)
- ✅ **Cross-Platform** support (iOS, Android, Web)
- ✅ **Clean Architecture** with BLoC state management
- ✅ **Material Design 3** with custom theming
- ✅ **Secure JWT Storage** with FlutterSecureStorage
- ✅ **Responsive UI** that works across devices
- ✅ **Form Validation** and error handling
- ✅ **File Upload** for CSV import

### Financial Features
- ✅ **Multi-Account Management** (Checking, Savings, Credit Cards)
- ✅ **Transaction Tracking** with categorization
- ✅ **Income vs Expense Analysis**
- ✅ **Real-time Balance Calculations**
- ✅ **CSV Import/Export** functionality
- ✅ **Bank Account Synchronization** via Plaid

## 🛠️ Tech Stack

### Backend
- **Java 17** with Spring Boot 3.1.0
- **Spring Security** for authentication
- **Spring Data JPA** for data persistence
- **PostgreSQL** database
- **JWT** for stateless authentication
- **Plaid API** for banking integration
- **Maven** for dependency management
- **JUnit 5 + Mockito** for testing

### Frontend
- **Flutter 3.1+** for cross-platform development
- **Dart** programming language
- **BLoC Pattern** for state management
- **Dio** for HTTP client
- **GoRouter** for navigation
- **FlutterSecureStorage** for secure data storage
- **JSON Annotation** for serialization

## 📋 Prerequisites

### Backend
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- IDE (IntelliJ IDEA, VS Code, etc.)

### Frontend
- Flutter SDK 3.1.0+
- Dart SDK
- Android Studio / Xcode (for mobile development)
- Web browser (for web development)

## 🚀 Getting Started

### 1. Backend Setup

1. **Clone the repository:**
```bash
git clone <repository-url>
cd buckpal
```

2. **Configure PostgreSQL database:**
```bash
# Create database
createdb buckpal_db

# Update credentials in src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/buckpal_db
    username: your_username
    password: your_password
```

3. **Configure environment variables:**
```bash
export JWT_SECRET=yourJwtSecretKey
export PLAID_CLIENT_ID=your_plaid_client_id
export PLAID_SECRET=your_plaid_secret
```

4. **Run the backend:**
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 2. Frontend Setup

1. **Navigate to Flutter app:**
```bash
cd flutter-app
```

2. **Install dependencies:**
```bash
flutter pub get
```

3. **Generate code:**
```bash
flutter packages pub run build_runner build
```

4. **Update API endpoint** in `lib/core/constants/app_constants.dart`:
```dart
static const String baseUrl = 'http://localhost:8080/api';
```

5. **Run the app:**
```bash
# For mobile
flutter run

# For web
flutter run -d web

# For specific platform
flutter run -d android
flutter run -d ios
```

## 🔧 Development

### Backend Development

**Run tests:**
```bash
mvn test
```

**Build JAR:**
```bash
mvn clean package
```

**Run with profile:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend Development

**Generate code (watch mode):**
```bash
flutter packages pub run build_runner watch
```

**Run tests:**
```bash
flutter test
```

**Build for production:**
```bash
# Web
flutter build web --release

# Android
flutter build apk --release

# iOS
flutter build ios --release
```

**Using Makefile (Flutter):**
```bash
make help          # Show available commands
make setup         # Full development setup
make generate      # Generate code
make test          # Run tests
make build         # Build for all platforms
```

## 📱 Platform Support

| Platform | Status | Notes |
|----------|--------|-------|
| **Web** | ✅ | Full PWA support |
| **Android** | ✅ | API Level 21+ |
| **iOS** | ✅ | iOS 11.0+ |
| **macOS** | 🚧 | Desktop support (planned) |
| **Windows** | 🚧 | Desktop support (planned) |
| **Linux** | 🚧 | Desktop support (planned) |

## 🔒 Security

- **JWT Authentication** with secure token storage
- **Password Encryption** using BCrypt
- **Input Validation** on all API endpoints
- **CORS Configuration** for web security
- **SQL Injection Protection** via JPA/Hibernate
- **XSS Protection** through proper data sanitization

## 📊 API Documentation

### Authentication Endpoints
- `POST /api/auth/signin` - User login
- `POST /api/auth/signup` - User registration

### Account Management
- `GET /api/accounts` - Get user accounts
- `POST /api/accounts` - Create new account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Deactivate account

### Transaction Management
- `GET /api/transactions` - Get transactions (paginated)
- `GET /api/transactions/account/{accountId}` - Get transactions by account
- `POST /api/transactions/import-csv/{accountId}` - Import CSV transactions
- `PUT /api/transactions/{id}/category` - Update transaction category

### Utilities
- `GET /api/transactions/csv-template` - Download CSV template

## 🧪 Testing

### Backend Testing
- **Unit Tests**: Service layer testing with Mockito
- **Integration Tests**: Full API endpoint testing
- **Security Tests**: Authentication and authorization
- **Repository Tests**: Database interaction testing

### Frontend Testing
- **Unit Tests**: Business logic and utilities
- **Widget Tests**: UI component testing
- **Integration Tests**: End-to-end user flows
- **Golden Tests**: Visual regression testing

**Test Coverage:**
```bash
# Backend
mvn jacoco:report

# Frontend
flutter test --coverage
```

## 🚀 Deployment

### Backend Deployment

**Docker:**
```bash
# Build image
docker build -t buckpal-backend .

# Run container
docker run -p 8080:8080 buckpal-backend
```

**Heroku:**
```bash
heroku create buckpal-api
git push heroku main
```

### Frontend Deployment

**Web (Netlify/Vercel):**
```bash
flutter build web --release
# Deploy contents of build/web/
```

**Mobile (App Stores):**
```bash
# Android Play Store
flutter build appbundle --release

# iOS App Store
flutter build ios --release
```

## 🔧 Configuration

### Environment Variables

**Backend:**
```env
JWT_SECRET=your_jwt_secret_key
DB_USERNAME=database_username  
DB_PASSWORD=database_password
PLAID_CLIENT_ID=plaid_client_id
PLAID_SECRET=plaid_secret_key
PLAID_ENVIRONMENT=sandbox|development|production
```

**Frontend:**
```env
API_BASE_URL=http://localhost:8080/api
ENVIRONMENT=development|staging|production
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards
- **Backend**: Follow Google Java Style Guide
- **Frontend**: Follow Dart/Flutter style guide
- **Commit Messages**: Use conventional commit format
- **Testing**: Maintain >80% test coverage

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check the README files in each module
- **Issues**: Use GitHub Issues for bug reports
- **Discussions**: Use GitHub Discussions for questions

## 🗺️ Roadmap

### Q1 2024
- [ ] Enhanced analytics dashboard
- [ ] Bill reminders and notifications
- [ ] Budget goal setting

### Q2 2024
- [ ] Multi-currency support
- [ ] Export to Excel/PDF
- [ ] Advanced categorization with ML

### Q3 2024
- [ ] Family account sharing
- [ ] Investment tracking
- [ ] Subscription management

### Q4 2024
- [ ] Desktop applications (Windows/macOS/Linux)
- [ ] Advanced reporting
- [ ] API for third-party integrations

## 👥 Team

- **Backend Development**: Spring Boot + PostgreSQL
- **Frontend Development**: Flutter multiplatform
- **DevOps**: Docker, CI/CD, Cloud deployment
- **Testing**: Comprehensive test suites

---

**Built with ❤️ using Spring Boot and Flutter**