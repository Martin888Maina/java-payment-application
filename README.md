# java-payment-application

A small payments API built with Java and Spring Boot, with a plain React interface for trying it out.

It covers the four parts of a basic payment flow: creating a payment request, receiving the result from the payment provider through a callback, checking the status of a payment, and reconciling our records against the provider's records.

## Tech stack

- Java 21
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- H2 in-memory database
- JUnit 5, Mockito and Spring MockMvc for tests
- Maven, through the included Maven wrapper
- React 19, Vite and React Router for the web interface
- GitHub Actions for continuous integration

## Features

- **Payment requests.** A new payment starts as `PENDING` and gets a public reference such as `PAY-7F3K9Q2M8XWD`. Amounts are stored with `BigDecimal` and exactly two decimal places. The currency defaults to `KES`.
- **Idempotent requests.** Repeating a request with the same merchant reference returns the first payment instead of creating a second one. Reusing a merchant reference with different details is rejected. Two identical requests that arrive at the same moment are handled as well.
- **Signed callbacks.** The provider signs each callback with HMAC-SHA256 using a shared secret. A callback with a missing or wrong signature is rejected before its body is read.
- **Status lifecycle.** A payment moves from `PENDING` to `SUCCESSFUL` or `FAILED` once. A repeated callback with the same result changes nothing, and a finished payment cannot change. Callbacks for the same payment are processed one at a time using a database row lock.
- **Status lookup.** Get one payment by its reference, or list payments with an optional status filter.
- **Reconciliation.** Compare the provider's records with our payments. The result lists matched records, amount mismatches, status mismatches, records missing on our side and records missing on the provider side, with totals.
- **Consistent errors.** Every error uses the same JSON shape (RFC 9457 problem details), with a message per field for validation errors.
- **Callback simulator.** A small endpoint that only exists in the `dev` profile. It stands in for a real provider during local testing.
- **Web interface.** Pages to list, create and view payments, trigger the simulator and run a reconciliation.
