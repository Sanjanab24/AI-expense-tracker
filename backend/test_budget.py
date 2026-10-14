"""
Test suite for the AI Expense Tracker budget suggestion API and rules engine.
Step 3: Verification of data contract, rule-based allocations, and input validations.
"""

import pytest
from fastapi.testclient import TestClient

from main import app
from schemas import (
    BudgetSuggestRequest,
    BudgetSuggestResponse,
    CategoryAllocation,
    CategoryItem,
    HistoricalMonth,
    CategorySpending,
    RecurringCommitment,
    UpcomingExpense,
)
from budget_rules import generate_budget_suggestions, validate_budget_output

client = TestClient(app)


# -----------------------------------------------------------------------------
# Base Test Fixtures
# -----------------------------------------------------------------------------
@pytest.fixture
def default_categories():
    return [
        {"id": "food", "name": "Food & Dining"},
        {"id": "rent", "name": "Rent & Housing"},
        {"id": "transport", "name": "Transport"},
    ]


def test_health_check_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "ai-expense-tracker-backend"



# -----------------------------------------------------------------------------
# 1. Test: No History Available
# -----------------------------------------------------------------------------
def test_no_history_divides_budget_equally(default_categories):
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 3000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [],
        "upcoming": [],
    }

    response = client.post("/v1/budget/suggest", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    assert data["source"] == "rules"
    assert any("No historical spending data available" in w for w in data["warnings"])
    assert len(data["allocations"]) == 3

    # 3000 split equally among 3 categories = 1000 each
    for alloc in data["allocations"]:
        assert alloc["amount"] == 1000.0
        assert "equal discretionary share" in alloc["reason"]

    total = sum(a["amount"] for a in data["allocations"])
    assert round(total, 2) == 3000.0


# -----------------------------------------------------------------------------
# 2. Test: Normal History Proportional Allocation
# -----------------------------------------------------------------------------
def test_normal_history_allocates_proportionally(default_categories):
    # Food spent 6000 (60%), Transport spent 4000 (40%), Rent spent 0 in history
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 10000.0,
        "categories": default_categories,
        "history": [
            {
                "year": 2026,
                "month": 10,
                "total_spent": 10000.0,
                "by_category": [
                    {"category_id": "food", "amount": 6000.0},
                    {"category_id": "transport", "amount": 4000.0},
                ],
            }
        ],
        "recurring": [
            {"category_id": "rent", "amount": 5000.0}
        ],
        "upcoming": [],
    }

    response = client.post("/v1/budget/suggest", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    allocations = {a["category_id"]: a["amount"] for a in data["allocations"]}
    # Rent reserved = 5000. Remaining discretionary = 5000.
    # Food gets 60% of 5000 = 3000.
    # Transport gets 40% of 5000 = 2000.
    assert allocations["rent"] == 5000.0
    assert allocations["food"] == 3000.0
    assert allocations["transport"] == 2000.0

    total = sum(allocations.values())
    assert round(total, 2) == 10000.0


# -----------------------------------------------------------------------------
# 3. Test: Recurring Commitments Larger than Monthly Budget
# -----------------------------------------------------------------------------
def test_recurring_larger_than_budget(default_categories):
    # Budget is 5000, but recurring is 8000
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 5000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [
            {"category_id": "rent", "amount": 6000.0},
            {"category_id": "food", "amount": 2000.0},
        ],
        "upcoming": [],
    }

    response = client.post("/v1/budget/suggest", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    assert any("exceed the monthly budget" in w for w in data["warnings"])
    total = sum(a["amount"] for a in data["allocations"])
    assert round(total, 2) <= 5000.0
    assert round(total, 2) == 5000.0


# -----------------------------------------------------------------------------
# 4. Test: One Category Missing from History
# -----------------------------------------------------------------------------
def test_one_category_missing_from_history(default_categories):
    # History exists for 'food' and 'rent', but not for 'transport'
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 6000.0,
        "categories": default_categories,
        "history": [
            {
                "year": 2026,
                "month": 9,
                "total_spent": 5000.0,
                "by_category": [
                    {"category_id": "food", "amount": 3000.0},
                    {"category_id": "rent", "amount": 2000.0},
                    # 'transport' is absent from history
                ],
            }
        ],
        "recurring": [],
        "upcoming": [],
    }

    response = client.post("/v1/budget/suggest", json=payload)
    assert response.status_code == 200, response.text
    data = response.json()

    allocations = {a["category_id"]: a["amount"] for a in data["allocations"]}
    # Food: 3000/5000 = 60% of 6000 = 3600.
    # Rent: 2000/5000 = 40% of 6000 = 2400.
    # Transport has 0 in history -> receives 0.0 discretionary.
    assert allocations["food"] == 3600.0
    assert allocations["rent"] == 2400.0
    assert allocations["transport"] == 0.0

    total = sum(allocations.values())
    assert round(total, 2) == 6000.0


# -----------------------------------------------------------------------------
# 5. Test: Negative Amounts are Rejected (422)
# -----------------------------------------------------------------------------
def test_negative_amounts_rejected(default_categories):
    # 5A: Negative monthly_budget
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": -1000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [],
        "upcoming": [],
    }
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422

    # 5B: Negative recurring amount
    payload["monthly_budget"] = 5000.0
    payload["recurring"] = [{"category_id": "food", "amount": -200.0}]
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422

    # 5C: Negative upcoming amount
    payload["recurring"] = []
    payload["upcoming"] = [{"category_id": "food", "amount": -50.0, "expected_date": "2026-11-15", "label": "Dinner"}]
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422


# -----------------------------------------------------------------------------
# 6. Test: Unknown category_id Rejected (422)
# -----------------------------------------------------------------------------
def test_unknown_category_id_rejected(default_categories):
    # Recurring references 'investments' which is NOT in categories
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 5000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [{"category_id": "unknown_cat", "amount": 1000.0}],
        "upcoming": [],
    }
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422
    assert "not defined in 'categories'" in res.text

    # Upcoming references unknown category
    payload["recurring"] = []
    payload["upcoming"] = [{"category_id": "another_ghost", "amount": 500.0, "expected_date": "2026-11-10", "label": "Trip"}]
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422
    assert "not defined in 'categories'" in res.text


# -----------------------------------------------------------------------------
# 7. Test: Rounding Sums Exactly
# -----------------------------------------------------------------------------
def test_rounding_sums_exactly(default_categories):
    # 1000 divided among 3 categories without history yields 333.33333333...
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 1000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [],
        "upcoming": [],
    }
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 200
    data = res.json()

    total = round(sum(a["amount"] for a in data["allocations"]), 2)
    assert total == 1000.0

    # Also test prime split ratio: 7 : 13 : 23 = 43 total
    payload["history"] = [
        {
            "year": 2026,
            "month": 10,
            "total_spent": 43.0,
            "by_category": [
                {"category_id": "food", "amount": 7.0},
                {"category_id": "rent", "amount": 13.0},
                {"category_id": "transport", "amount": 23.0},
            ],
        }
    ]
    payload["monthly_budget"] = 100.0
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 200
    data = res.json()

    total = round(sum(a["amount"] for a in data["allocations"]), 2)
    assert total == 100.0


# -----------------------------------------------------------------------------
# 8. Test: Empty Categories Rejected (422)
# -----------------------------------------------------------------------------
def test_empty_categories_rejected():
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 5000.0,
        "categories": [],
        "history": [],
        "recurring": [],
        "upcoming": [],
    }
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422


# -----------------------------------------------------------------------------
# 9. Test: Extra Forbidden Fields Rejected (422)
# -----------------------------------------------------------------------------
def test_extra_fields_forbidden(default_categories):
    payload = {
        "month": 11,
        "year": 2026,
        "currency": "INR",
        "monthly_budget": 5000.0,
        "categories": default_categories,
        "history": [],
        "recurring": [],
        "upcoming": [],
        "merchant": "Amazon",  # Forbidden extra field!
    }
    res = client.post("/v1/budget/suggest", json=payload)
    assert res.status_code == 422


# -----------------------------------------------------------------------------
# 10. Test: Output Validation Function
# -----------------------------------------------------------------------------
def test_output_validator_detects_violations(default_categories):
    req = BudgetSuggestRequest(
        month=11,
        year=2026,
        monthly_budget=1000.0,
        categories=[CategoryItem(**c) for c in default_categories],
    )

    # Valid response
    valid_resp = BudgetSuggestResponse(
        allocations=[
            CategoryAllocation(category_id="food", amount=400.0, reason="test"),
            CategoryAllocation(category_id="rent", amount=400.0, reason="test"),
            CategoryAllocation(category_id="transport", amount=200.0, reason="test"),
        ],
        summary="OK",
    )
    validate_budget_output(valid_resp, req)  # Should not raise

    # Violation 1: Exceeds budget
    over_resp = BudgetSuggestResponse(
        allocations=[
            CategoryAllocation(category_id="food", amount=600.0, reason="test"),
            CategoryAllocation(category_id="rent", amount=600.0, reason="test"),
            CategoryAllocation(category_id="transport", amount=200.0, reason="test"),
        ],
        summary="Over",
    )
    with pytest.raises(ValueError, match="exceeds the monthly budget"):
        validate_budget_output(over_resp, req)

    # Violation 2: Unknown category ID
    unknown_resp = BudgetSuggestResponse(
        allocations=[
            CategoryAllocation(category_id="crypto", amount=1000.0, reason="test"),
            CategoryAllocation(category_id="food", amount=0.0, reason="test"),
            CategoryAllocation(category_id="rent", amount=0.0, reason="test"),
            CategoryAllocation(category_id="transport", amount=0.0, reason="test"),
        ],
        summary="Unknown",
    )
    with pytest.raises(ValueError, match="does not exist in requested categories"):
        validate_budget_output(unknown_resp, req)

    # Violation 3: Missing categories in allocations
    missing_resp = BudgetSuggestResponse(
        allocations=[
            CategoryAllocation(category_id="food", amount=500.0, reason="test"),
            CategoryAllocation(category_id="rent", amount=500.0, reason="test"),
        ],
        summary="Missing transport",
    )
    with pytest.raises(ValueError, match="missing from allocations"):
        validate_budget_output(missing_resp, req)

    # Violation 4: Negative amount (testing fallback if unvalidated object passed)
    negative_resp = BudgetSuggestResponse(
        allocations=[
            CategoryAllocation.model_construct(category_id="food", amount=-100.0, reason="test"),
            CategoryAllocation(category_id="rent", amount=600.0, reason="test"),
            CategoryAllocation(category_id="transport", amount=500.0, reason="test"),
        ],
        summary="Negative",
    )
    with pytest.raises(ValueError, match="Negative amount"):
        validate_budget_output(negative_resp, req)
