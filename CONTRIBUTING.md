# Contributing to Weather Service

Thank you for considering contributing to the Weather Service project! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)

## Code of Conduct

This project follows standard open source community guidelines. Please be respectful and constructive in all interactions.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/weather-service.git`
3. Add upstream remote: `git remote add upstream https://github.com/ORIGINAL_OWNER/weather-service.git`
4. Create a branch for your changes

## Development Setup

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose
- Maven 3.6.3+

### Local Environment

1. Copy `.env.example` to `.env` and configure:
   ```bash
   cp .env.example .env
   ```

2. Set up an external authentication service (required for full functionality)
   - The weather service requires a JWT-based authentication service
   - Configure `jwt.public-key-url` in `application.yaml`
   - See [SECURITY.md](SECURITY.md) for details

3. Start dependencies:
   ```bash
   docker-compose up -d weather-postgres weather-redis
   ```

4. Run the backend:
   ```bash
   cd weather-service
   ../mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

5. Run the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Making Changes

1. Create a feature branch from `develop`:
   ```bash
   git checkout develop
   git pull upstream develop
   git checkout -b feature/your-feature-name
   ```

2. Make your changes
3. Write or update tests as needed
4. Ensure all tests pass
5. Update documentation if needed

## Commit Messages

This project uses semantic versioning with automatic release generation. Follow these commit message conventions:

### Format

```
<type>: <description>

[optional body]
```

### Types

- `feat:` - New feature (triggers minor version bump)
- `fix:` - Bug fix (triggers patch version bump)
- `chore:` - Maintenance tasks (triggers patch version bump)
- `docs:` - Documentation changes
- `style:` - Code style/formatting changes
- `refactor:` - Code refactoring without feature changes
- `test:` - Adding or updating tests
- `perf:` - Performance improvements

### Examples

```
feat: add hourly weather graph modal

fix: correct token refresh logic for expired sessions

chore(deps): bump spring-boot to 3.5.7
```

### Breaking Changes

For breaking changes that require a major version bump, add `BREAKING CHANGE:` in the commit body:

```
feat: redesign authentication flow

BREAKING CHANGE: authentication now requires external JWT service
```

See [VERSIONING.md](VERSIONING.md) for detailed versioning information.

## Pull Request Process

1. **Update Documentation**: Update README.md and other docs if needed
2. **Add Tests**: Ensure your changes are covered by tests
3. **Run Tests**: All tests must pass
4. **Update CHANGELOG**: Not required - automatically generated on release
5. **Create PR**: 
   - Target the `develop` branch (or `main` for hotfixes)
   - Use a clear, descriptive title
   - Reference any related issues
   - Describe what changed and why

### PR Title Format

Use the same format as commit messages:

- `feat: add weather alerts feature`
- `fix: resolve location search race condition`
- `chore(deps): update dependencies`

### Branch Naming

- `feature/feature-name` - New features (triggers minor version)
- `fix/bug-name` or `bugfix/bug-name` - Bug fixes (triggers patch version)
- `major/breaking-change` - Breaking changes (triggers major version)

The automated release system uses branch names to determine version bumps.

## Coding Standards

### Java (Backend)

- Follow Java naming conventions
- Use Lombok annotations where appropriate
- Document public APIs with JavaDoc
- Maximum line length: 120 characters
- Use MapStruct for DTO mapping
- Format code with Google Java Style (automatically formatted with fmt-maven-plugin)

### TypeScript/React (Frontend)

- Use TypeScript for type safety
- Follow React hooks best practices
- Use functional components
- Keep components focused and single-purpose
- Use next-intl for all user-facing text
- Format code consistently (ESLint configured)

### General

- Write clear, self-documenting code
- Add comments for complex logic
- Follow DRY (Don't Repeat Yourself) principle
- Keep methods/functions focused and small
- Use meaningful variable and function names

## Testing

### Backend Tests

```bash
cd weather-service
../mvnw test
```

Run integration tests:
```bash
../mvnw verify
```

### Frontend Tests

```bash
cd frontend
npm test
```

Run tests with coverage:
```bash
npm test -- --coverage
```

### Test Guidelines

- Write unit tests for new functionality
- Maintain or improve code coverage
- Test edge cases and error conditions
- Use meaningful test names that describe what's being tested
- Mock external dependencies appropriately

## Code Review

All submissions require review. We use GitHub pull requests for this purpose. Reviewers will check:

- Code quality and style
- Test coverage
- Documentation updates
- Potential security issues
- Performance implications

## Questions?

Feel free to open an issue for:
- Questions about contributing
- Clarification on requirements
- Discussion of proposed changes

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

