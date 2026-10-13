# AI Expense Tracker - Backend Service

Standalone Python FastAPI backend for the AI Expense Tracker application.
This service provides an API layer designed to run independently from the Android client.

---

## Prerequisites

- **Python 3.10+** (tested on Python 3.14)
- **Windows PowerShell**

---

## Getting Started (Windows PowerShell)

Follow these steps from the project root (`AIExpenseTracker`) or directly inside the `backend` directory.

### 1. Open PowerShell and Navigate to Backend

```powershell
cd backend
```

### 2. Create a Python Virtual Environment

```powershell
python -m venv .venv
```

### 3. Activate the Virtual Environment

```powershell
.\.venv\Scripts\Activate.ps1
```

> **PowerShell Execution Policy Tip**:
> If you encounter an error like `cannot be loaded because running scripts is disabled on this system`, PowerShell's execution policy is restricting script execution.
> You can allow scripts for your current user session by running:
> ```powershell
> Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
> ```
> Alternatively, for just the current PowerShell process:
> ```powershell
> Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
> ```
> Then run `.\.venv\Scripts\Activate.ps1` again. When activated, your prompt will show `(.venv)`.

### 4. Install Dependencies

Install the pinned packages from `requirements.txt`:

```powershell
pip install -r requirements.txt
```

### 5. Environment Configuration (Optional / Prepared for Future Steps)

Copy the `.env.example` file to `.env` if you want a local configuration file:

```powershell
Copy-Item .env.example .env
```

*(Note: No real API key is required for Step 1. The service runs without any AI API key).*

### 6. Start the FastAPI Server

Run using Uvicorn:

```powershell
uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

Alternatively, you can run directly with Python:

```powershell
python main.py
```

The server will start at: `http://127.0.0.1:8000`

---

## Testing the Service

### 1. In a Web Browser

Open your browser and navigate to:
- Health check: [http://127.0.0.1:8000/health](http://127.0.0.1:8000/health)
- Interactive API Docs (Swagger UI): [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
- Alternative API Docs (ReDoc): [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)

### 2. In PowerShell using `Invoke-RestMethod`

Open another PowerShell terminal and run:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8000/health" -Method Get
```

**Expected JSON Response:**
```json
{
  "status": "ok",
  "service": "ai-expense-tracker-backend",
  "version": "0.1.0"
}
```

You can also test the root endpoint:
```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8000/" -Method Get
```

---

## Stopping the Server

Press `Ctrl + C` in the terminal where Uvicorn is running.
To deactivate the virtual environment when finished:
```powershell
deactivate
```
