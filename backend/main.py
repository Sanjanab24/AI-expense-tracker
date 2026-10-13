"""
AI Expense Tracker - FastAPI Backend
Step 1: Minimal standalone backend foundation with health check endpoint.
"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import uvicorn

# Application metadata
APP_TITLE = "AI Expense Tracker Backend"
APP_DESCRIPTION = "Backend service for the AI Expense Tracker application"
APP_VERSION = "0.1.0"
SERVICE_NAME = "ai-expense-tracker-backend"

# Initialize FastAPI application
app = FastAPI(
    title=APP_TITLE,
    description=APP_DESCRIPTION,
    version=APP_VERSION,
)


# Global generic exception handler to return clean JSON responses on unexpected errors
@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    """
    Catches any unhandled exceptions and returns a clean JSON response
    instead of exposing internal stack traces.
    """
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


if __name__ == "__main__":
    # Allows running directly via `python main.py`
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
