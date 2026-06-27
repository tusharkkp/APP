# Changelog

All notable changes to Vision AI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Multi-language voice output (Hindi, Spanish, French)
- Offline identification mode with TensorFlow Lite
- Cloud sync with Firebase Firestore
- Social sharing of scan results
- Batch scanning mode
- AR overlay for identification labels
- Google Play Store release
- Android home screen widget

---

## [1.0.0] - 2026-05-01

### Added
- **Core Vision AI features** powered by Google Gemini API (server-side)
  - Real-time object detection via camera
  - Plant species identification
  - Animal and insect recognition
  - Landmark and monument detection
  - Text recognition (OCR)
  - QR code scanning with AI context
- **Voice narration** for all scan results using Android TextToSpeech
- **Scan history** stored locally with Room Database
- **Analytics dashboard** with scan category breakdown
- **Bottom navigation** with Scan, History, and Metrics tabs
- **Jetpack Compose UI** with Material 3 and edge-to-edge design
- **MVVM architecture** with ViewModel and Repository pattern
- **Environment configuration** via `.env` file with Gemini API key
- **Google AI Studio integration** for app deployment
- `.env.example` for local setup guidance
- `metadata.json` for AI Studio capability declaration
- Full project infrastructure setup with Gradle KTS

### Technical
- Kotlin 100% codebase
- CameraX for lifecycle-aware camera preview
- Room Database for offline scan persistence
- Coroutines for async Gemini API calls
- AndroidX Test infrastructure for unit and instrumented tests
- ProGuard rules configured for release builds

---

## Notes

- This project was bootstrapped from the [Google AI Studio Android template](https://github.com/google-gemini/aistudio-repository-template)
- Gemini API key is required for all identification features
- See [README.md](./README.md) for full setup instructions
