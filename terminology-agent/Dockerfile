# ========== Build stage ==========
FROM python:3.11-slim AS builder

WORKDIR /app

RUN pip install --no-cache-dir poetry

COPY pyproject.toml ./
RUN poetry config virtualenvs.create false \
    && poetry install --no-root --only main

# ========== Runtime stage ==========
FROM python:3.11-slim

WORKDIR /app

# Copy installed packages from builder
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application code
COPY app/ ./app/
COPY config/ ./config/
COPY .env.example ./.env

EXPOSE 18002

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "18002"]
