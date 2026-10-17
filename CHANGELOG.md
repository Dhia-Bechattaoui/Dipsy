# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2026-10-17

### Added
- Custom 4x3 POS numpad with cent-shift amount input (QAR 0.00 format).
- Maximum 7-digit input constraint (QAR 99,999.99 limit).
- Cancel button to reset amount and generate fresh idempotency keys.
- Automatic disabling of the OK button when amount is zero.
- Asynchronous payment dispatch via `POST /pay` with loading spinner.
- Idempotency key (UUID) support with double-tap request protection.
- In-place API error messaging for HTTP 400, 401, 500, and network failures.
- Retry button on HTTP 500 and network timeouts reusing the same idempotency key.
- Periodic status polling on `GET /transactions/{id}` every 1 second (capped at 15 attempts).
- Dedicated result states: Success (approved) and Failed (with reason, e.g., fraud detected).
- "New payment" action returning cleanly to a reset POS screen.
- Transaction History screen displaying the latest 20 transactions from `GET /transactions`.
- Release APK build and signing configuration.

[0.0.1]: https://github.com/Dhia-Bechattaoui/Dipsy/releases/tag/v0.0.1
