# AI Expense Tracker - API Contract

This document specifies the REST API contract for the **AI Expense Tracker** backend service.
Client applications (such as the Android app) communicate with these endpoints to check service availability and request rule-based / AI budget allocation recommendations.

---

## 1. Endpoints Overview

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Service health status check | None |
| `POST` | `/v1/budget/suggest` | Computes monthly category budget allocations | None |

---

## 2. GET `/health`

Verifies that the backend server is running and responsive.

### Response `200 OK`
```json
{
  "status": "ok",
  "service": "ai-expense-tracker-backend",
  "version": "0.2.0"
}
```

---

## 3. POST `/v1/budget/suggest`

Generates deterministic category budget allocations for a given target month based on:
1. Reserved recurring commitments (e.g. rent, bills, subscriptions).
2. Planned upcoming expenses.
3. Proportional distribution of remaining discretionary budget based on spending history.

### Security & Privacy Rules
- **Extra Fields Forbidden**: Extra/unknown fields are strictly rejected with HTTP `422`.
- **Zero Merchant / Description Data**: Merchant names, receipt text, and specific transaction notes are **never** accepted in this payload. Only high-level category aggregates and brief labels are accepted.
- **Amounts**: Stored as `float` (representing Kotlin's `Double` in Indian Rupees `₹`), must be `>= 0.0`.

---

### Request Schema Specification

| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `month` | `integer` | Yes | `1 <= month <= 12` | 1-based calendar month (1 = Jan, 12 = Dec). Matches Android `DateUtils`. |
| `year` | `integer` | Yes | `2000 <= year <= 2100` | 4-digit calendar year (e.g. `2026`). |
| `currency` | `string` | No | Default: `"INR"`, length 3-5 | ISO currency code. |
| `monthly_budget` | `number` | Yes | `> 0.0` | Total budget to distribute for the month in currency units. |
| `categories` | `array` | Yes | 1 to 30 items, unique `id`s | Available categories defined by the client. |
| `categories[].id` | `string` | Yes | Length 1-50 | Unique identifier for category (e.g. `"food"`, `"rent"`, or `"Food"`). |
| `categories[].name`| `string` | Yes | Length 1-50 | Human-readable category display name (e.g. `"Food & Dining"`). |
| `history` | `array` | No | Max 12 items | Spending data for up to 12 preceding months. |
| `history[].year` | `integer` | Yes | `2000 <= year <= 2100` | Historical month's year. |
| `history[].month` | `integer` | Yes | `1 <= month <= 12` | Historical month (1-12). |
| `history[].total_spent`| `number` | Yes | `>= 0.0` | Total spending in that historical month. |
| `history[].by_category`| `array` | No | Category breakdown | List of `{ category_id, amount }` for that month. |
| `recurring` | `array` | No | Optional list | Known recurring expenses for target month (rent, EMI, subscriptions). |
| `recurring[].category_id` | `string` | Yes | Must exist in `categories` | Category identifier. |
| `recurring[].amount` | `number` | Yes | `>= 0.0` | Recurring monthly amount. |
| `upcoming` | `array` | No | Max 20 items | Planned irregular expenses expected in target month. |
| `upcoming[].category_id` | `string` | Yes | Must exist in `categories` | Category identifier. |
| `upcoming[].amount` | `number` | Yes | `>= 0.0` | Expected amount. |
| `upcoming[].expected_date` | `string` | Yes | Length 1-20 | Date string (e.g. `"2026-11-15"`). |
| `upcoming[].label` | `string` | Yes | Max 60 characters | Short label (e.g. `"Festival shopping"`). |

---

### Response Schema Specification

| Field | Type | Description |
| :--- | :--- | :--- |
| `allocations` | `array` | Complete list of budget allocations. Exactly one entry per category in request. |
| `allocations[].category_id` | `string` | Category identifier matching the request. |
| `allocations[].amount` | `number` | Allocated budget amount (rounded to 2 decimal places). Non-negative. |
| `allocations[].reason` | `string` | Plain-English explanation of how this allocation was computed. |
| `summary` | `string` | Executive summary of the allocation methodology. |
| `warnings` | `array[string]`| Non-fatal warnings (e.g. no history available, commitments exceeded budget). |
| `source` | `string` | `"rules"` (indicates deterministic rules engine; later `"ai"` or `"ai_fallback"`). |

---

## 4. Request & Response Examples

### Example A: Normal Flow (Commitments + Historical Spending)

#### Request:
```json
{
  "month": 11,
  "year": 2026,
  "currency": "INR",
  "monthly_budget": 25000.0,
  "categories": [
    { "id": "rent", "name": "Rent" },
    { "id": "groceries", "name": "Groceries" },
    { "id": "utilities", "name": "Utilities" },
    { "id": "dining", "name": "Dining & Entertainment" },
    { "id": "transport", "name": "Transport" }
  ],
  "history": [
    {
      "year": 2026,
      "month": 10,
      "total_spent": 15000.0,
      "by_category": [
        { "category_id": "groceries", "amount": 6000.0 },
        { "category_id": "dining", "amount": 5000.0 },
        { "category_id": "transport", "amount": 4000.0 }
      ]
    }
  ],
  "recurring": [
    { "category_id": "rent", "amount": 10000.0 },
    { "category_id": "utilities", "amount": 2000.0 }
  ],
  "upcoming": [
    {
      "category_id": "groceries",
      "amount": 1000.0,
      "expected_date": "2026-11-05",
      "label": "Festival groceries"
    }
  ]
}
```

#### Response (`200 OK`):
```json
{
  "allocations": [
    {
      "category_id": "rent",
      "amount": 10000.0,
      "reason": "INR 10000.00 reserved for commitments; no historical discretionary spending."
    },
    {
      "category_id": "groceries",
      "amount": 5800.0,
      "reason": "INR 1000.00 reserved + INR 4800.00 based on spending history (40.0%)."
    },
    {
      "category_id": "utilities",
      "amount": 2000.0,
      "reason": "INR 2000.00 reserved for commitments; no historical discretionary spending."
    },
    {
      "category_id": "dining",
      "amount": 4000.0,
      "reason": "INR 4000.00 allocated based on spending history (33.3%)."
    },
    {
      "category_id": "transport",
      "amount": 3200.0,
      "reason": "INR 3200.00 allocated based on spending history (26.7%)."
    }
  ],
  "summary": "Reserved INR 13000.00 for commitments and allocated the remaining INR 12000.00 based on 1 month(s) of spending history.",
  "warnings": [],
  "source": "rules"
}
```

---

### Example B: No History (Equal Discretionary Split)

When no history is provided, recurring/upcoming commitments are reserved first, and the remaining budget is divided equally across all categories with a warning.

#### Response (`200 OK`):
```json
{
  "allocations": [
    {
      "category_id": "food",
      "amount": 1000.0,
      "reason": "INR 1000.00 equal discretionary share (no history available)."
    },
    {
      "category_id": "rent",
      "amount": 1000.0,
      "reason": "INR 1000.00 equal discretionary share (no history available)."
    },
    {
      "category_id": "transport",
      "amount": 1000.0,
      "reason": "INR 1000.00 equal discretionary share (no history available)."
    }
  ],
  "summary": "Reserved INR 0.00 for commitments and divided the remaining INR 3000.00 equally across all 3 categories.",
  "warnings": [
    "No historical spending data available; remaining budget was divided equally among all categories."
  ],
  "source": "rules"
}
```

---

### Example C: Over-Committed (Recurring Exceeds Budget)

When recurring commitments exceed the monthly budget, recurring amounts are scaled down proportionally to prevent the sum from exceeding the budget ceiling.

#### Response (`200 OK`):
```json
{
  "allocations": [
    {
      "category_id": "rent",
      "amount": 3750.0,
      "reason": "Partially funded recurring commitment (INR 3750.00 of INR 6000.00) due to budget ceiling."
    },
    {
      "category_id": "food",
      "amount": 1250.0,
      "reason": "Partially funded recurring commitment (INR 1250.00 of INR 2000.00) due to budget ceiling."
    },
    {
      "category_id": "transport",
      "amount": 0.0,
      "reason": "No allocation (budget exhausted by recurring commitments)."
    }
  ],
  "summary": "Monthly budget of INR 5000.00 is insufficient to cover total recurring commitments of INR 8000.00. Allocations were scaled down.",
  "warnings": [
    "Recurring commitments (INR 8000.00) exceed the monthly budget (INR 5000.00). Recurring expenses were scaled down; upcoming and discretionary expenses could not be funded."
  ],
  "source": "rules"
}
```

---

## 5. Error Responses

### `422 Unprocessable Entity` (Schema Validation Failed)
Occurs when request constraints are violated (e.g. negative amount, unknown category ID, or missing required fields):
```json
{
  "detail": [
    {
      "type": "value_error",
      "loc": ["body"],
      "msg": "Value error, recurring[0].category_id 'crypto' is not defined in 'categories'.",
      "input": { ... }
    }
  ]
}
```

### `500 Internal Server Error` (Unexpected Server Failure)
```json
{
  "status": "error",
  "message": "An unexpected server error occurred.",
  "detail": "..."
}
```
