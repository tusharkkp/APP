# Contributing to Vision AI

Thank you for your interest in contributing to Vision AI! This document outlines the process and guidelines for contributing to this Android AI camera application.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)

## Code of Conduct

This project follows a [Code of Conduct](./CODE_OF_CONDUCT.md). By participating, you agree to uphold these standards.

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/APP.git
   cd APP
   ```
3. **Set upstream** remote:
   ```bash
   git remote add upstream https://github.com/tusharkkp/APP.git
   ```

## Development Setup

See the [Installation & Setup](./README.md#installation--setup) section in the README for full setup instructions.

**Required tools:**
- Android Studio (Hedgehog or later)
- JDK 17+
- Android SDK API 26+
- Google Gemini API Key

## How to Contribute

### Bug Fixes
1. Check existing [Issues](https://github.com/tusharkkp/APP/issues) to avoid duplicates
2. Create a new issue if your bug isn't listed
3. Fork and create a branch: `git checkout -b fix/issue-description`
4. Apply your fix with tests
5. Submit a pull request referencing the issue

### New Features
1. Open a [Feature Request issue](https://github.com/tusharkkp/APP/issues/new) first to discuss the idea
2. Wait for maintainer feedback before starting significant work
3. Create a branch: `git checkout -b feature/feature-name`
4. Implement the feature with documentation
5. Submit a pull request

### Documentation
- Fix typos, improve clarity, or expand existing docs
- No issue required for minor documentation fixes
- Branch: `git checkout -b docs/description`

## Pull Request Process

1. Ensure your branch is up to date with `main`:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. Run all tests before submitting:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```
3. Ensure the app builds without errors:
   ```bash
   ./gradlew assembleDebug
   ```
4. Fill out the PR template completely
5. Request a review from the maintainer
6. Address any review feedback promptly

## Coding Standards

- **Language:** Kotlin (no Java files in new code)
- **UI:** Jetpack Compose only (no XML layouts)
- **Architecture:** Follow existing MVVM + Repository pattern
- **Naming:** Use descriptive, camelCase names for functions/variables
- **Comments:** Add KDoc comments to all public functions
- **Tests:** Write unit tests for ViewModel and Repository logic
- **Lint:** Run `./gradlew lint` and fix all warnings before PR

## Commit Message Guidelines

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

[optional body]

[optional footer]
```

**Types:**
- `feat` — New feature
- `fix` — Bug fix
- `docs` — Documentation changes
- `style` — Formatting, missing semicolons (no logic change)
- `refactor` — Code refactoring
- `test` — Adding or updating tests
- `chore` — Build process or tooling changes

**Examples:**
```
feat(camera): add multi-frame burst scanning mode
fix(history): resolve crash when deleting last scan entry
docs(readme): update installation steps for Android Studio Hedgehog
```

## Reporting Bugs

When filing a bug report, include:
- Device model and Android version
- App version / commit hash
- Steps to reproduce
- Expected vs actual behavior
- Logcat output if available
- Screenshots or screen recordings

## Requesting Features

When requesting a feature, include:
- Clear description of the feature
- Use case / problem it solves
- Proposed implementation approach (optional)
- Any relevant references or examples

---

Thank you for helping make Vision AI better!
