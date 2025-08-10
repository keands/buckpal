# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BuckPal is a multi-platform budget management application consisting of:
- **Spring Boot Backend** (Java 17) with PostgreSQL database
- **React Web Frontend** (TypeScript, Vite, Tailwind CSS) for web interface
- **Flutter Mobile Apps** (planned for future development after web stabilization)

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
├── react-app/                     # React web frontend
│   ├── src/
│   │   ├── components/            # Reusable UI components
│   │   ├── contexts/              # React contexts (auth, etc.)
│   │   ├── lib/                   # Utilities and API client
│   │   ├── pages/                 # Page components
│   │   └── types/                 # TypeScript type definitions
│   ├── public/                    # Static assets
│   └── package.json               # Node.js dependencies
├── flutter-app/                   # Flutter apps (future development)
│   └── lib/                       # Flutter source (when implemented)
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

## Frontend Development (React)

### Commands
Navigation to react-app directory required for all React commands:
```bash
cd react-app
```

**Essential development commands:**
- **Install dependencies**: `npm install`
- **Start development server**: `npm run dev`
- **Build for production**: `npm run build`
- **Run linting**: `npm run lint`
- **Preview production build**: `npm run preview`

### Technology Stack
- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **React Router** for client-side routing
- **Axios** for API communication
- **React Hook Form** with Zod validation
- **Lucide React** for icons
- **date-fns** for date manipulation

### Architecture
- **Component-based architecture** with reusable UI components
- **Context API** for state management (auth, etc.)
- **Custom hooks** for business logic
- **TypeScript** for type safety
- **Responsive design** with mobile-first approach

### Key Files
- **Entry point**: `src/main.tsx`
- **App component**: `src/App.tsx`
- **API client**: `src/lib/api.ts`
- **Type definitions**: `src/types/api.ts`
- **Utilities**: `src/lib/utils.ts`
- **Auth context**: `src/contexts/auth-context.tsx`

### UI Components
- Located in `src/components/ui/`
- **Shadcn/ui** inspired components
- **Tailwind CSS** for styling
- **Responsive** and **accessible** design patterns

### Navigation
- Uses **React Router DOM** for routing
- Routes defined in `src/App.tsx`
- Protected routes with authentication guards
- Current routes: `/login`, `/register`, `/dashboard`, `/accounts`, `/calendar`, `/csv-import`, `/settings`

## Development Workflow

### Backend Setup
1. Ensure Java 17+ and Maven installed
2. Set up PostgreSQL database: `createdb buckpal_db`
3. Configure environment variables or update `application.yml`
4. Run: `mvn spring-boot:run`

### Frontend Setup (React)
1. Navigate to `react-app/` directory: `cd react-app`
2. Install dependencies: `npm install`
3. Update API endpoint in Vite config if needed (proxy configured for `/api`)
4. Start development server: `npm run dev`

### Full Development Setup
Use the provided commands for comprehensive setup:
- **Backend**: `mvn spring-boot:run` (runs on port 8080)
- **Frontend**: `cd react-app && npm install && npm run dev` (runs on port 5173)

### Flutter Apps (Future Development)
When ready to implement mobile apps:
1. Navigate to `flutter-app/` directory
2. Install dependencies: `flutter pub get`
3. Generate code: `flutter packages pub run build_runner build --delete-conflicting-outputs`
4. Run: `flutter run` for mobile or `flutter run -d web`

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

### Frontend Testing (React)
- **Unit tests** for all business logic (API client, utilities, custom hooks)
- **Component tests** for all UI components using React Testing Library
- **Context tests** for React contexts and state management
- **Integration tests** for complete user flows and API interactions
- **TDD approach**: Write test files (`.test.tsx` or `.test.ts`) before implementation
- **Mock external dependencies** using Mock Service Worker (MSW) for API calls
- Run with: `npm test` (from react-app directory)
- **Coverage requirement**: Run `npm run test:coverage` and maintain >85% coverage

### Future Flutter Testing
When implementing mobile apps:
- **Unit tests** for all business logic (use cases, repositories, models)
- **Widget tests** for all custom UI components
- **BLoC tests** using `bloc_test` package for all state management
- **Integration tests** for complete user flows
- Run with: `flutter test` (from flutter-app directory)

### TDD Best Practices
- **Backend**: Create test classes in `src/test/java/com/buckpal/` mirroring production structure
- **React Frontend**: Create test files alongside components or in `src/__tests__/` directory
- **Naming convention**: `ClassNameTest.java` (backend) or `ComponentName.test.tsx` (React frontend)
- **Test isolation**: Each test should be independent and repeatable
- **Arrange-Act-Assert** pattern for test structure
- **Edge cases**: Test null values, empty collections, error conditions, network failures
- **Integration points**: Mock external services (Plaid API, database, API calls)

## Environment Configuration

### Backend Environment Variables
```bash
export JWT_SECRET=yourJwtSecretKey
export PLAID_CLIENT_ID=your_plaid_client_id
export PLAID_SECRET=your_plaid_secret
export DB_USERNAME=database_username
export DB_PASSWORD=database_password
```

### React Configuration
- API endpoint configured via Vite proxy in `vite.config.ts`
- Development: API calls to `/api/*` are proxied to `http://localhost:8080/api`
- Production: Configure base URL in `src/lib/api.ts` or environment variables
- Environment variables using `.env` files (`.env.local`, `.env.development`, `.env.production`)

### Future Flutter Configuration
When implementing mobile apps:
- API endpoint configured in `lib/core/constants/app_constants.dart`
- Default backend URL: `http://localhost:8080/api`
- Supports multiple environments via build flavors

## Development Guidelines

### Specification Clarification Process
**MANDATORY**: Before implementing any feature or making changes:
1. **Ask clarifying questions** about requirements and specifications until everything is clear
2. **Confirm understanding** of the expected behavior, edge cases, and constraints
3. **Validate assumptions** about data structures, user flows, and business logic
4. **Only proceed with implementation** after all specifications are well-defined
5. **Don't make assumptions** - when in doubt, ask the user for clarification

## Important Notes

- **Database migrations** handled automatically by Hibernate DDL (update mode)
- **JWT tokens** stored securely in localStorage (React) - consider upgrading to httpOnly cookies for production
- **CORS configuration** in Spring Boot allows React web clients
- **Linting rules** enforced via ESLint and Prettier for React (see `.eslintrc.json`)
- **Type safety** ensured through TypeScript across the React frontend
- **Responsive design** implemented with Tailwind CSS mobile-first approach

### Current Focus: React Web Frontend
- Primary development focus on React web application
- Backend API fully supports React frontend
- Calendar feature implemented and functional
- Mobile apps (Flutter) planned for future development once web frontend is stable

### Future Mobile Development
When ready for mobile apps:
- **Code generation** will be required for Flutter after modifying models or API clients
- **JWT tokens** will be stored securely using FlutterSecureStorage
- **Multi-platform support**: Android, iOS with Flutter
- **Design system** to be consistent between React web and Flutter mobile