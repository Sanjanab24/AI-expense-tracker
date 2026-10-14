"""
Budget Allocation Rules Engine
Step 3: Deterministic, rule-based budget suggestion logic and output validation.
No AI models are invoked here; this provides the baseline rule logic and acts
as the fallback mechanism when AI integration is added in subsequent steps.
"""

from typing import Dict, List
from schemas import (
    BudgetSuggestRequest,
    BudgetSuggestResponse,
    CategoryAllocation,
)


def validate_budget_output(
    response: BudgetSuggestResponse,
    request: BudgetSuggestRequest
) -> None:
    """
    Validates a budget suggestion response against strict business constraints.
    This validator is used for both rule-based outputs and future AI-generated outputs.

    Validation rules:
    1. Every allocated category_id must exist in the request categories.
    2. Every category in the request must be accounted for in allocations.
    3. Every allocated amount must be non-negative (>= 0).
    4. The sum of all allocated amounts must not exceed the monthly budget.

    Raises:
        ValueError: If any validation rule is violated.
    """
    valid_category_ids = {c.id for c in request.categories}
    allocated_category_ids = {a.category_id for a in response.allocations}

    # 1. Check for unknown category IDs
    for allocation in response.allocations:
        if allocation.category_id not in valid_category_ids:
            raise ValueError(
                f"Output validation failed: Category ID '{allocation.category_id}' "
                f"does not exist in requested categories."
            )

    # 2. Check for missing categories in allocations
    missing_ids = valid_category_ids - allocated_category_ids
    if missing_ids:
        raise ValueError(
            f"Output validation failed: The following categories are missing from allocations: {missing_ids}"
        )

    # 3. Check for negative amounts
    for allocation in response.allocations:
        if allocation.amount < 0.0:
            raise ValueError(
                f"Output validation failed: Negative amount {allocation.amount} "
                f"found for category '{allocation.category_id}'."
            )

    # 4. Check that total allocations do not exceed monthly budget (allowing 0.01 tolerance for floating point)
    total_allocated = round(sum(a.amount for a in response.allocations), 2)
    budget = round(request.monthly_budget, 2)
    if total_allocated > budget + 0.01:
        raise ValueError(
            f"Output validation failed: Total allocated amount (₹{total_allocated:.2f}) "
            f"exceeds the monthly budget (₹{budget:.2f})."
        )


def generate_budget_suggestions(request: BudgetSuggestRequest) -> BudgetSuggestResponse:
    """
    Computes deterministic category allocations based on budget rules:

    Rule A: Reserved commitments (recurring expenses + upcoming planned expenses)
            are prioritized and reserved first per category.
    Rule B: The remaining discretionary budget is distributed across categories
            proportionally to their historical average spending.
    Rule C: If there is no spending history (or 0 historical spending),
            the remaining budget is divided equally across all categories with a warning.
    Rule D: The sum of allocations must never exceed the monthly budget.
            If reserved commitments exceed the budget, recurring commitments are
            prioritized, and an explanatory warning is attached.
    Rule E: All amounts are rounded to 2 decimal places, exact sums are maintained,
            and no category receives a negative allocation.
    """
    budget = round(request.monthly_budget, 2)
    categories = request.categories
    category_ids = [c.id for c in categories]
    category_name_map = {c.id: c.name for c in categories}
    warnings: List[str] = []

    curr = request.currency
    recurring_by_cat: Dict[str, float] = {cid: 0.0 for cid in category_ids}
    for item in request.recurring:
        recurring_by_cat[item.category_id] = round(
            recurring_by_cat[item.category_id] + item.amount, 2
        )

    upcoming_by_cat: Dict[str, float] = {cid: 0.0 for cid in category_ids}
    for item in request.upcoming:
        upcoming_by_cat[item.category_id] = round(
            upcoming_by_cat[item.category_id] + item.amount, 2
        )

    total_recurring = round(sum(recurring_by_cat.values()), 2)
    total_upcoming = round(sum(upcoming_by_cat.values()), 2)
    total_reserved = round(total_recurring + total_upcoming, 2)

    allocations_map: Dict[str, float] = {cid: 0.0 for cid in category_ids}
    reasons_map: Dict[str, str] = {cid: "" for cid in category_ids}

    # -------------------------------------------------------------------------
    # Step 2: Handle cases where reserved commitments exceed or equal the budget
    # -------------------------------------------------------------------------
    if total_recurring > budget:
        # Case 2A: Even recurring commitments alone exceed the total monthly budget
        warnings.append(
            f"Recurring commitments ({curr} {total_recurring:.2f}) exceed the monthly budget "
            f"({curr} {budget:.2f}). Recurring expenses were scaled down; upcoming and "
            f"discretionary expenses could not be funded."
        )

        for cid in category_ids:
            if recurring_by_cat[cid] > 0.0 and total_recurring > 0.0:
                share = round((recurring_by_cat[cid] / total_recurring) * budget, 2)
                allocations_map[cid] = share
                reasons_map[cid] = (
                    f"Partially funded recurring commitment ({curr} {share:.2f} of "
                    f"{curr} {recurring_by_cat[cid]:.2f}) due to budget ceiling."
                )
            else:
                allocations_map[cid] = 0.0
                reasons_map[cid] = "No allocation (budget exhausted by recurring commitments)."

        # Fix minor rounding discrepancy to ensure exact match with budget
        _adjust_rounding(allocations_map, budget, category_ids)

        summary = (
            f"Monthly budget of {curr} {budget:.2f} is insufficient to cover total recurring "
            f"commitments of {curr} {total_recurring:.2f}. Allocations were scaled down."
        )

    elif total_reserved > budget:
        # Case 2B: Recurring fits fully, but recurring + upcoming exceeds the budget
        warnings.append(
            f"Total reserved expenses (recurring {curr} {total_recurring:.2f} + upcoming "
            f"{curr} {total_upcoming:.2f} = {curr} {total_reserved:.2f}) exceed the monthly budget "
            f"({curr} {budget:.2f}). Recurring commitments were fully reserved; upcoming "
            f"expenses were partially funded."
        )

        # Fully reserve recurring
        for cid in category_ids:
            allocations_map[cid] = recurring_by_cat[cid]

        # Allocate remaining budget to upcoming expenses proportionally
        avail_for_upcoming = round(budget - total_recurring, 2)
        for cid in category_ids:
            if upcoming_by_cat[cid] > 0.0 and total_upcoming > 0.0:
                upcoming_share = round((upcoming_by_cat[cid] / total_upcoming) * avail_for_upcoming, 2)
                allocations_map[cid] = round(allocations_map[cid] + upcoming_share, 2)
                reasons_map[cid] = (
                    f"Fully reserved {curr} {recurring_by_cat[cid]:.2f} recurring + "
                    f"partially funded {curr} {upcoming_share:.2f} of {curr} {upcoming_by_cat[cid]:.2f} upcoming."
                    if recurring_by_cat[cid] > 0.0
                    else f"Partially funded {curr} {upcoming_share:.2f} of {curr} {upcoming_by_cat[cid]:.2f} upcoming."
                )
            elif recurring_by_cat[cid] > 0.0:
                reasons_map[cid] = f"Fully reserved {curr} {recurring_by_cat[cid]:.2f} for recurring commitments."
            else:
                reasons_map[cid] = "No allocation (budget exhausted by reserved commitments)."

        _adjust_rounding(allocations_map, budget, category_ids)

        summary = (
            f"Recurring commitments ({curr} {total_recurring:.2f}) were fully covered, and upcoming "
            f"expenses were partially funded up to the {curr} {budget:.2f} budget limit."
        )

    else:
        # Case 2C: Both recurring and upcoming commitments fit within the budget!
        for cid in category_ids:
            allocations_map[cid] = round(recurring_by_cat[cid] + upcoming_by_cat[cid], 2)

        discretionary_budget = round(budget - total_reserved, 2)

        if discretionary_budget == 0.0:
            for cid in category_ids:
                if allocations_map[cid] > 0.0:
                    reasons_map[cid] = f"Fully reserved {curr} {allocations_map[cid]:.2f} for commitments."
                else:
                    reasons_map[cid] = "No allocation (no commitments and zero discretionary budget)."
            summary = "Monthly budget exactly matches total reserved commitments."

        else:
            # -----------------------------------------------------------------
            # Step 3: Distribute discretionary budget using history or equal split
            # -----------------------------------------------------------------
            history_by_cat: Dict[str, float] = {cid: 0.0 for cid in category_ids}
            for month_data in request.history:
                for item in month_data.by_category:
                    if item.category_id in history_by_cat:
                        history_by_cat[item.category_id] = round(
                            history_by_cat[item.category_id] + item.amount, 2
                        )

            total_history_spent = round(sum(history_by_cat.values()), 2)

            if len(request.history) == 0 or total_history_spent == 0.0:
                # Rule C: No history at all - split discretionary budget equally
                warnings.append(
                    "No historical spending data available; remaining budget was divided "
                    "equally among all categories."
                )

                num_categories = len(categories)
                equal_share = round(discretionary_budget / num_categories, 2)

                discretionary_shares: Dict[str, float] = {
                    cid: equal_share for cid in category_ids
                }
                _adjust_discretionary_rounding(discretionary_shares, discretionary_budget, category_ids)

                for cid in category_ids:
                    allocations_map[cid] = round(allocations_map[cid] + discretionary_shares[cid], 2)
                    reserved_amt = round(recurring_by_cat[cid] + upcoming_by_cat[cid], 2)
                    if reserved_amt > 0.0:
                        reasons_map[cid] = (
                            f"{curr} {reserved_amt:.2f} reserved + {curr} {discretionary_shares[cid]:.2f} "
                            f"equal discretionary share (no history available)."
                        )
                    else:
                        reasons_map[cid] = (
                            f"{curr} {discretionary_shares[cid]:.2f} equal discretionary share "
                            f"(no history available)."
                        )

                summary = (
                    f"Reserved {curr} {total_reserved:.2f} for commitments and divided the remaining "
                    f"{curr} {discretionary_budget:.2f} equally across all {num_categories} categories."
                )

            else:
                # Rule B: History available - split proportionally based on average spending
                discretionary_shares = {}
                for cid in category_ids:
                    if history_by_cat[cid] > 0.0:
                        share = round(
                            (history_by_cat[cid] / total_history_spent) * discretionary_budget, 2
                        )
                        discretionary_shares[cid] = share
                    else:
                        discretionary_shares[cid] = 0.0

                _adjust_discretionary_rounding(discretionary_shares, discretionary_budget, category_ids)

                for cid in category_ids:
                    allocations_map[cid] = round(allocations_map[cid] + discretionary_shares[cid], 2)
                    reserved_amt = round(recurring_by_cat[cid] + upcoming_by_cat[cid], 2)
                    history_pct = (
                        (history_by_cat[cid] / total_history_spent) * 100
                        if total_history_spent > 0.0
                        else 0.0
                    )

                    if reserved_amt > 0.0 and discretionary_shares[cid] > 0.0:
                        reasons_map[cid] = (
                            f"{curr} {reserved_amt:.2f} reserved + {curr} {discretionary_shares[cid]:.2f} "
                            f"based on spending history ({history_pct:.1f}%)."
                        )
                    elif reserved_amt > 0.0:
                        reasons_map[cid] = (
                            f"{curr} {reserved_amt:.2f} reserved for commitments; "
                            f"no historical discretionary spending."
                        )
                    elif discretionary_shares[cid] > 0.0:
                        reasons_map[cid] = (
                            f"{curr} {discretionary_shares[cid]:.2f} allocated based on "
                            f"spending history ({history_pct:.1f}%)."
                        )
                    else:
                        reasons_map[cid] = (
                            "No allocation (no historical spending and no upcoming commitments)."
                        )

                summary = (
                    f"Reserved {curr} {total_reserved:.2f} for commitments and allocated the remaining "
                    f"{curr} {discretionary_budget:.2f} based on {len(request.history)} month(s) of "
                    f"spending history."
                )

        _adjust_rounding(allocations_map, budget, category_ids)

    # -------------------------------------------------------------------------
    # Step 4: Build final response object and validate output
    # -------------------------------------------------------------------------
    allocations = [
        CategoryAllocation(
            category_id=cid,
            amount=allocations_map[cid],
            reason=reasons_map[cid] or f"Allocation for {category_name_map.get(cid, cid)}.",
        )
        for cid in category_ids
    ]

    response = BudgetSuggestResponse(
        allocations=allocations,
        summary=summary,
        warnings=warnings,
        source="rules",
    )

    # Reusable output validation step
    validate_budget_output(response, request)

    return response


def _adjust_rounding(
    allocations_map: Dict[str, float],
    target_total: float,
    category_ids: List[str]
) -> None:
    """
    Ensures the sum of all allocations exactly matches target_total to the cent (0.01).
    Adjusts the category with the highest allocation to prevent negative values.
    """
    current_sum = round(sum(allocations_map.values()), 2)
    diff = round(target_total - current_sum, 2)
    if diff != 0.0 and category_ids:
        # Find category with largest allocation to absorb the 1-2 cent rounding diff
        best_cid = max(category_ids, key=lambda cid: allocations_map[cid])
        new_amount = round(allocations_map[best_cid] + diff, 2)
        if new_amount >= 0.0:
            allocations_map[best_cid] = new_amount


def _adjust_discretionary_rounding(
    shares_map: Dict[str, float],
    target_discretionary: float,
    category_ids: List[str]
) -> None:
    """
    Ensures the sum of discretionary shares exactly equals target_discretionary.
    """
    current_sum = round(sum(shares_map.values()), 2)
    diff = round(target_discretionary - current_sum, 2)
    if diff != 0.0 and category_ids:
        best_cid = max(category_ids, key=lambda cid: shares_map[cid])
        new_amount = round(shares_map[best_cid] + diff, 2)
        if new_amount >= 0.0:
            shares_map[best_cid] = new_amount
