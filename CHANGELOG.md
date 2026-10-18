# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-10-18

### Added
- Dynamic multi-currency selector (`QAR`, `USD`, `EUR`) on the POS keypad screen using segmented pill buttons.
- `formatAmount(cents, currency)` helper function supporting any ISO currency string while retaining backward-compatible `formatQar` delegation.
- Multi-currency unit test suite in `AmountInputTest.kt` verifying formatting across USD, EUR, and QAR.
- Dynamic currency display in `ResultActivity` reflecting the selected currency on submission and real-time transaction polling updates.
- Dynamic currency formatting in `HistoryActivity` rendering each transaction with its respective server currency.

### Changed
- Active payment submission in `MainActivity.kt` now passes the selected currency in `PayRequest`.
- Switching currency dynamically refreshes the display and resets the idempotency key to avoid server-side conflict errors.

## [0.0.2] - 2026-10-17

### Added
- Pure helper functions (`appendDigit`, `formatQar`) in `MainActivity.kt` for testable amount logic without Android framework dependencies.
- Unit test suite (`AmountInputTest.kt`) verifying cent-shift entry, 7-digit ceiling constraints, zero-value guards, and standard QAR currency formatting.

### Fixed
- Automatic idempotency key invalidation when editing amounts after an API error, eliminating HTTP 422 (`idempotency_mismatch`) errors on retry.
- Coroutine cooperative cancellation in `ResultActivity.kt` status polling by catching `IOException` instead of swallowing `CancellationException`.

### Removed
- Default `ExampleUnitTest.kt` boilerplate.

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

[0.1.0]: https://github.com/Dhia-Bechattaoui/Dipsy/compare/v0.0.2...v0.1.0
[0.0.2]: https://github.com/Dhia-Bechattaoui/Dipsy/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/Dhia-Bechattaoui/Dipsy/releases/tag/v0.0.1

