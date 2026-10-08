# Contributing to ZTE MC7500 ODU Monitor

Thank you for your interest in contributing to this project. We welcome bug reports, documentation improvements, and pull requests.

## Development Setup

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK with API 34 platform installed
- Android Studio Iguana (2023.2.1) or newer, or the command-line Gradle wrapper
- Physical ZTE MC7500 Outdoor Unit (ODU) or use the built-in Demo Simulator mode

### Building from Source

Clone the repository and build the debug APK:

```bash
git clone https://github.com/OurKode/ZTE-MC7500.git
cd ZTE-MC7500

# Linux / macOS
./gradlew assembleDebug

# Windows PowerShell
.\gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew test
```

## Pull Request Guidelines

1. **Create a Topic Branch**:
   Create a descriptive branch for your work:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. **Commit Message Conventions**:
   Follow Conventional Commits:
   - `feat(scope): ...` for new features
   - `fix(scope): ...` for bug fixes
   - `docs(scope): ...` for documentation changes
   - `refactor(scope): ...` for code refactoring

3. **Versioning Discipline**:
   - Do not manually edit `versionCode` in Gradle scripts. It is generated dynamically from Git commit history.
   - For version increments, use `./gradlew bumpPatch`, `./gradlew bumpMinor`, or `./gradlew bumpMajor`.

4. **Verify Locally Before Submitting**:
   Ensure your code compiles cleanly and passes all tests:
   ```bash
   ./gradlew test assembleDebug
   ```

5. **Open a Pull Request**:
   Submit your pull request against the `main` branch with a clear summary of changes and screenshots for UI modifications.
