"""
Pydantic schemas for the AI Expense Tracker backend API.
Step 3: Budget suggestion request and response data contract.
"""

from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, model_validator


class CategoryItem(BaseModel):
    """Represents an expense category defined by the client."""
    model_config = ConfigDict(extra="forbid")

    id: str = Field(..., min_length=1, max_length=50, description="Unique identifier for the category")
    name: str = Field(..., min_length=1, max_length=50, description="Human-readable category display name")


class CategorySpending(BaseModel):
    """Spending within a specific category for a historical month."""
    model_config = ConfigDict(extra="forbid")

    category_id: str = Field(..., min_length=1, description="Category identifier")
    amount: float = Field(..., ge=0.0, description="Amount spent in this category (non-negative)")


class HistoricalMonth(BaseModel):
    """Historical spending summary for a past month."""
    model_config = ConfigDict(extra="forbid")

    year: int = Field(..., ge=2000, le=2100, description="4-digit calendar year")
    month: int = Field(..., ge=1, le=12, description="1-based calendar month (1=Jan, 12=Dec)")
    total_spent: float = Field(..., ge=0.0, description="Total amount spent across all categories (>= 0)")
    by_category: list[CategorySpending] = Field(
        default_factory=list,
        description="Category spending breakdown for this month"
    )


class RecurringCommitment(BaseModel):
    """Known recurring expense commitment for the target month (e.g. rent, subscriptions)."""
    model_config = ConfigDict(extra="forbid")

    category_id: str = Field(..., min_length=1, description="Category identifier")
    amount: float = Field(..., ge=0.0, description="Recurring expense amount (non-negative)")


class UpcomingExpense(BaseModel):
    """Planned upcoming expense expected in the target month."""
    model_config = ConfigDict(extra="forbid")

    category_id: str = Field(..., min_length=1, description="Category identifier")
    amount: float = Field(..., ge=0.0, description="Expected expense amount (non-negative)")
    expected_date: str = Field(..., min_length=1, max_length=20, description="Expected date (e.g. YYYY-MM-DD)")
    label: str = Field(..., min_length=1, max_length=60, description="Brief label (max 60 chars, no merchant names)")


class BudgetSuggestRequest(BaseModel):
    """
    Request payload for POST /v1/budget/suggest.
    Strict schema: extra fields are forbidden to avoid receiving unexpected or sensitive data.
    """
    model_config = ConfigDict(extra="forbid")

    month: int = Field(..., ge=1, le=12, description="Target 1-based month (1-12)")
    year: int = Field(..., ge=2000, le=2100, description="Target 4-digit year")
    currency: str = Field(default="INR", min_length=3, max_length=5, description="ISO currency code (e.g. INR)")
    monthly_budget: float = Field(..., gt=0.0, description="Total monthly budget to allocate (> 0)")
    categories: list[CategoryItem] = Field(
        ...,
        min_length=1,
        max_length=30,
        description="Available categories (1 to 30 items with unique IDs)"
    )
    history: list[HistoricalMonth] = Field(
        default_factory=list,
        max_length=12,
        description="Historical spending data for up to 12 past months"
    )
    recurring: list[RecurringCommitment] = Field(
        default_factory=list,
        description="Known recurring commitments for the target month"
    )
    upcoming: list[UpcomingExpense] = Field(
        default_factory=list,
        max_length=20,
        description="Planned upcoming expenses (max 20 items)"
    )

    @model_validator(mode="after")
    def validate_category_consistency(self) -> "BudgetSuggestRequest":
        # 1. Ensure category IDs are unique
        category_ids = [c.id for c in self.categories]
        if len(category_ids) != len(set(category_ids)):
            raise ValueError("Category IDs in 'categories' must be unique.")

        valid_cat_ids = set(category_ids)

        # 2. Validate recurring category IDs
        for idx, item in enumerate(self.recurring):
            if item.category_id not in valid_cat_ids:
                raise ValueError(
                    f"recurring[{idx}].category_id '{item.category_id}' is not defined in 'categories'."
                )

        # 3. Validate upcoming category IDs
        for idx, item in enumerate(self.upcoming):
            if item.category_id not in valid_cat_ids:
                raise ValueError(
                    f"upcoming[{idx}].category_id '{item.category_id}' is not defined in 'categories'."
                )

        # 4. Validate historical category IDs
        for m_idx, month in enumerate(self.history):
            for c_idx, item in enumerate(month.by_category):
                if item.category_id not in valid_cat_ids:
                    raise ValueError(
                        f"history[{m_idx}].by_category[{c_idx}].category_id '{item.category_id}' "
                        f"is not defined in 'categories'."
                    )

        return self


class CategoryAllocation(BaseModel):
    """Allocated budget and explanatory rationale for a single category."""
    model_config = ConfigDict(extra="forbid")

    category_id: str = Field(..., description="Category identifier matching the request")
    amount: float = Field(..., ge=0.0, description="Allocated amount in request currency")
    reason: str = Field(..., description="Human-readable reason for this allocation")


class BudgetSuggestResponse(BaseModel):
    """
    Response payload for POST /v1/budget/suggest.
    Contains category allocations, summary, any warnings, and data source.
    """
    model_config = ConfigDict(extra="forbid")

    allocations: list[CategoryAllocation] = Field(..., description="List of category allocations")
    summary: str = Field(..., description="Summary explanation of the budget suggestion")
    warnings: list[str] = Field(default_factory=list, description="Warnings or notes about constraints encountered")
    source: Literal["rules", "ai", "ai_fallback"] = Field(
        default="rules",
        description="Source of this recommendation ('rules' for Step 3)"
    )
