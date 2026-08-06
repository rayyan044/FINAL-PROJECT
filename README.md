# Falcon Fuel Distribution Management System

Falcon is one system with one Spring Boot backend, one PostgreSQL database, and two clients:

```text
React/Vite web client ─────┐
                           ├── Spring Boot REST API ─── PostgreSQL
React Native/Expo client ──┘
```

## Run locally

Start PostgreSQL (or use `backend/docker-compose.yml`), then start the API:

```bash
cd backend
# Create an untracked .env from .env.example and replace every value first.
cp .env.example .env
set -a; source .env; set +a
./mvnw spring-boot:run
```

The backend intentionally fails fast when `DB_USERNAME`, `DB_PASSWORD`,
`JWT_SECRET`, `BOOTSTRAP_ADMIN_PASSWORD`, or `CORS_ALLOWED_ORIGINS` is absent.
Do not commit `backend/.env`; use a secret manager in production. Generate a
strong JWT key, for example: `openssl rand -base64 48`.

The API listens on `http://localhost:8081`.

Start the web client:

```bash
cd frontend
npm install
npm run dev
```

The web client reads `VITE_API_BASE_URL` from `frontend/.env*` and defaults to `/api/v1` when served behind the API proxy. Local development uses `http://localhost:8081/api/v1`.

Start the driver client:

```bash
cd falcon-driver-mobile-app
npm install
EXPO_PUBLIC_API_URL=http://YOUR_COMPUTER_LAN_IP:8081/api npx expo start
```

For Expo Go on a physical device, the phone and computer must share a network. Never use `localhost` as the mobile API host: on the phone it means the phone itself.

## Ownership boundary

All authentication, JWT issuance/refresh, authorization, validation, workflows, reporting, persistence, and business rules belong in `/backend`. The clients contain presentation, local form state, session caching, and REST API adapters only. See [ARCHITECTURE_REVIEW.md](ARCHITECTURE_REVIEW.md) for the audit and remaining recommendations.
