"""
AI Expense Tracker - FastAPI Backend
Step 3: Standalone backend with /health and /v1/budget/suggest endpoints.
"""

import time
import logging
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException
import uvicorn

from schemas import BudgetSuggestRequest, BudgetSuggestResponse
from budget_rules import generate_budget_suggestions

# Configure basic application logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger("backend")

# Application metadata
APP_TITLE = "AI Expense Tracker Backend"
APP_DESCRIPTION = "Backend service for the AI Expense Tracker application"
APP_VERSION = "0.2.0"
SERVICE_NAME = "ai-expense-tracker-backend"

# Initialize FastAPI application
app = FastAPI(
    title=APP_TITLE,
    description=APP_DESCRIPTION,
    version=APP_VERSION,
)


# Middleware: Log request method, path, status, and duration ONLY.
# Strictly NEVER log request or response bodies to prevent exposing financial or user data.
@app.middleware("http")
async def log_requests_middleware(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    duration_ms = round((time.time() - start_time) * 1000, 2)
    logger.info(f"{request.method} {request.url.path} -> {response.status_code} ({duration_ms}ms)")
    return response


from fastapi.exception_handlers import (
    request_validation_exception_handler as default_validation_handler,
    http_exception_handler as default_http_exception_handler,
)

# Validation error handler to preserve FastAPI's standard 422 Unprocessable Entity
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Returns clean 422 validation error responses when input schemas are violated.
    """
    return await default_validation_handler(request, exc)


# HTTP error handler (e.g. 404 Not Found)
@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """
    Preserves standard HTTP error codes like 404 without masking them as 500.
    """
    return await default_http_exception_handler(request, exc)


# Global generic exception handler to return clean JSON responses on unexpected errors
@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    """
    Catches any unexpected server errors and returns a clean 500 JSON response
    instead of exposing internal python tracebacks to clients.
    """
    logger.error(f"Unexpected server error on {request.url.path}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "status": "error",
            "message": "An unexpected server error occurred.",
            "detail": str(exc),
        },
    )


# Root endpoint providing service information
@app.get("/")
def read_root():
    """
    Root endpoint returning basic information about the API service.
    """
    return {
        "service": SERVICE_NAME,
        "title": APP_TITLE,
        "version": APP_VERSION,
        "docs_url": "/docs",
        "health_url": "/health",
        "suggest_url": "/v1/budget/suggest",
    }


# Health check endpoint
@app.get("/health")
def health_check():
    """
    Health check endpoint to verify that the service is running and responsive.
    Returns status, service identifier, and current version.
    """
    return {
        "status": "ok",
        "service": SERVICE_NAME,
        "version": APP_VERSION,
    }


# Budget suggestion endpoint (Step 3: Rule-based contract)
@app.post(
    "/v1/budget/suggest",
    response_model=BudgetSuggestResponse,
    summary="Generate budget suggestions",
    description="Calculates category budget allocations based on reserved commitments and spending history.",
)
def suggest_budget(request: BudgetSuggestRequest) -> BudgetSuggestResponse:
    """
    Calculates deterministic budget allocations according to established business rules.
    Does not invoke AI models or store financial records.
    """
    return generate_budget_suggestions(request)


if __name__ == "__main__":
    # Allows running directly via `python main.py`
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
