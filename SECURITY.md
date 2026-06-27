# Security Policy

## Supported Versions

| Version | Supported |
|---------|----------|
| Latest (main branch) | Yes |
| Older commits | No |

## Reporting a Vulnerability

The Vision AI project takes security seriously. If you discover a security vulnerability, please follow responsible disclosure practices.

**Please do NOT report security vulnerabilities through public GitHub Issues.**

### How to Report

1. **Contact the maintainer directly** via GitHub: [@tusharkkp](https://github.com/tusharkkp)
2. **Provide a detailed description** of the vulnerability including:
   - Type of issue (e.g., data exposure, API key leakage, insecure storage)
   - Full path of the source file(s) related to the issue
   - Location of the affected source code (tag/branch/commit or direct URL)
   - Steps to reproduce the issue
   - Proof-of-concept or exploit code (if possible)
   - Impact assessment

### Response Timeline

- **Acknowledgment:** Within 48 hours of receiving your report
- **Investigation:** Within 7 days
- **Fix or Mitigation:** Within 30 days (depending on severity)
- **Public Disclosure:** After the fix is released

## Security Best Practices for Users

### API Key Security

- **Never commit your `.env` file** to version control (it is `.gitignore`d by default)
- Store your `GEMINI_API_KEY` only in the local `.env` file
- Rotate your API key immediately if you suspect it has been exposed
- Use [Google AI Studio](https://aistudio.google.com/app/apikey) to manage and rotate keys

### Android Permissions

This app requests the following permissions:
- `CAMERA` — Required for real-time camera scanning
- `INTERNET` — Required for Gemini API communication

No other sensitive permissions are requested. The app does not collect or transmit personal data beyond what is necessary for AI identification.

### Data Storage

- Scan history is stored locally in Room Database on-device only
- No scan data is uploaded to external servers
- Gemini API receives only the captured image frame for inference

## Scope

Vulnerabilities in scope include:
- API key exposure via code or build artifacts
- Insecure local data storage
- Man-in-the-middle attack vectors on API communication
- Unauthorized camera access

Out of scope:
- Issues in third-party libraries (report to respective maintainers)
- Issues in Google Gemini API itself (report to Google)
