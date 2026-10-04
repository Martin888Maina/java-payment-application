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

## API endpoints

The base URL is `http://localhost:8080/api/v1`. Requests and responses use JSON.

| Area | Method and path | Purpose |
|---|---|---|
| Payment request | `POST /payments` | Create a payment |
| Status | `GET /payments/{reference}` | Get one payment |
| Status | `GET /payments?status=PENDING` | List payments, newest first, with an optional status filter |
| Callback | `POST /payments/callback` | Receive the result of a payment from the provider |
| Reconciliation | `POST /reconciliation` | Compare provider records with our payments |

### Payment request

`POST /api/v1/payments`

```json
{
  "merchantReference": "INV-1001",
  "amount": 1500.00,
  "currency": "KES",
  "payerPhone": "254712345678"
}
```

- `merchantReference` is required, up to 64 characters, and is used to detect repeated requests.
- `amount` is required, greater than 0, with at most two decimal places.
- `currency` is optional, three capital letters, and defaults to `KES`.
- `payerPhone` must be in the format `2547XXXXXXXX` or `2541XXXXXXXX`.

A new payment returns `201 Created` with a `Location` header such as `http://localhost:8080/api/v1/payments/PAY-7F3K9Q2M8XWD`:

```json
{
  "reference": "PAY-7F3K9Q2M8XWD",
  "merchantReference": "INV-1001",
  "amount": 1500.00,
  "currency": "KES",
  "payerPhone": "254712345678",
  "status": "PENDING",
  "providerReference": null,
  "createdAt": "2026-10-04T11:58:17.908Z",
  "updatedAt": "2026-10-04T11:58:17.908Z"
}
```

Sending the same request again returns `200 OK` with the same payment. Sending the same merchant reference with a different amount, currency or phone number returns `409 Conflict`.

### Status

`GET /api/v1/payments/PAY-7F3K9Q2M8XWD` returns the payment in the same shape as above, or `404 Not Found` if the reference is unknown.

`GET /api/v1/payments` returns an array of payments, newest first. Add `?status=PENDING`, `?status=SUCCESSFUL` or `?status=FAILED` to filter the list.

### Callback

`POST /api/v1/payments/callback`

The provider sends the result of a payment:

```json
{
  "reference": "PAY-7F3K9Q2M8XWD",
  "status": "SUCCESSFUL",
  "providerReference": "QJK3H2L9P0"
}
```

`status` must be `SUCCESSFUL` or `FAILED`. `providerReference` is optional.

Every callback must carry an `X-Signature` header: the HMAC-SHA256 of the exact request body, written as hexadecimal, using the shared secret from `PAYMENT_CALLBACK_SECRET`. The signature is checked against the raw bytes before the JSON is read. A signed callback can be sent like this:

```bash
BODY='{"reference":"PAY-7F3K9Q2M8XWD","status":"SUCCESSFUL","providerReference":"QJK3H2L9P0"}'
SIGNATURE=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$PAYMENT_CALLBACK_SECRET" | sed 's/^.* //')
curl -X POST http://localhost:8080/api/v1/payments/callback \
  -H "Content-Type: application/json" \
  -H "X-Signature: $SIGNATURE" \
  -d "$BODY"
```

The response is the updated payment, now `SUCCESSFUL` with its provider reference.

| Response | When |
|---|---|
| `200 OK` | The result was applied, or the same result was already applied |
| `400 Bad Request` | The body is not valid, for example a status of `PENDING` |
| `401 Unauthorized` | The signature is missing or does not match |
| `404 Not Found` | No payment has that reference |
| `409 Conflict` | The payment is already finished with a different result |

### Reconciliation

`POST /api/v1/reconciliation`

```json
{
  "records": [
    { "reference": "PAY-7F3K9Q2M8XWD", "amount": 1500.00, "status": "SUCCESSFUL" },
    { "reference": "PAY-ZZZ999ZZZ999", "amount": 400.00, "status": "SUCCESSFUL" }
  ]
}
```

Each provider record is compared with our payment that has the same reference, in this order:

1. No payment with that reference: missing on our side.
2. The amounts differ: amount mismatch.
3. The statuses differ: status mismatch.
4. Otherwise: matched.

Our `SUCCESSFUL` and `FAILED` payments that are not in the provider records are reported as missing on the provider side. `PENDING` payments are left out because they are still in progress. Requests with an empty list or a repeated reference are rejected with `400 Bad Request`.

Response:

```json
{
  "summary": {
    "providerRecords": 2,
    "matched": 1,
    "amountMismatches": 0,
    "statusMismatches": 0,
    "missingOnOurSide": 1,
    "missingOnProviderSide": 0,
    "matchedAmount": 1500.00
  },
  "matched": [
    {
      "reference": "PAY-7F3K9Q2M8XWD",
      "ourAmount": 1500.00,
      "providerAmount": 1500.00,
      "ourStatus": "SUCCESSFUL",
      "providerStatus": "SUCCESSFUL"
    }
  ],
  "amountMismatches": [],
  "statusMismatches": [],
  "missingOnOurSide": [
    {
      "reference": "PAY-ZZZ999ZZZ999",
      "ourAmount": null,
      "providerAmount": 400.00,
      "ourStatus": null,
      "providerStatus": "SUCCESSFUL"
    }
  ],
  "missingOnProviderSide": []
}
```

### Errors

Every error uses the same problem details format, with the content type `application/problem+json`. Validation errors include a message for each field:

```json
{
  "detail": "Request validation failed",
  "instance": "/api/v1/payments",
  "status": 400,
  "title": "Bad Request",
  "errors": [
    { "field": "amount", "message": "must be greater than 0" },
    { "field": "payerPhone", "message": "must be in the format 2547XXXXXXXX or 2541XXXXXXXX" }
  ]
}
```

Unexpected errors return `500` with the message `An unexpected error occurred`. The details are written to the server log only.

### Callback simulator (dev profile only)

`POST /api/v1/dev/payments/{reference}/simulate-callback` with `{ "status": "SUCCESSFUL" }` or `{ "status": "FAILED" }`.

There is no real payment provider in this project. When the back end runs with the `dev` profile, this endpoint plays the provider's part: it builds a callback, signs it with the configured secret and sends it to the callback endpoint above, so the signature check, validation and status rules all run as they would for a real provider. Without the `dev` profile the endpoint does not exist and returns `404`.
