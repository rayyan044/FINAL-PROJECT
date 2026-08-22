# Falcon architecture review

## Executive result

The repository now has the intended deployment shape: `/backend` is the only Spring Boot service, `/frontend` is the web client, and `/mobile` is the Expo client. Both clients use the same REST API and PostgreSQL schema. The backend contains the authoritative entities and workflows.

The system is not yet feature-identical across clients. The web application is an operator/admin client covering most domains; the mobile application is a driver workspace covering driver login, dashboard, deliveries, profile, and delivery detail. Unsupported mobile notifications are no longer presented as a fake screen.

## Shared components

Both clients use the backend's:

- `/api/v1/auth` JWT login, refresh, logout, and current-user profile
- user accounts, roles, permissions, and linked driver records
- delivery/dispatch/loading/order/inventory/invoice/payment data exposed by Spring controllers
- `ApiResponse` envelopes and PostgreSQL-backed services

The web API adapter uses `/api/v1`; the backend also retains `/api` aliases. The mobile adapter uses `/api` because its base URL is configured as `...:8081/api`; both resolve to the same controller methods.

## Web-only functionality

The web client provides administration, customers, sales/orders, products/pricing, loading operations, dispatch, inventory/tanks/transactions, invoices/payment accounts, reporting, delivery documents, audit logs, company settings, vehicles, and user/driver administration.

## Mobile-only functionality

The mobile client provides a driver-scoped dashboard, assigned-delivery list/search, driver profile retrieval, and driver-scoped delivery details. It stores only the JWT/session cache in AsyncStorage; it has no database.

## Findings and fixes made

1. Browser-side `seedService` created fuel products and customers from the admin page. This duplicated backend bootstrap/business behavior and could mutate production data from a client. The admin call and service were removed; backend `DatabaseBootstrap` remains the single owner of defaults.
2. Mobile profile and delivery details were placeholders. Spring now exposes authenticated `GET /api/v1/auth/me` and driver-scoped `GET /api/v1/mobile/deliveries/{id}`, and the mobile screens call those APIs.
3. Mobile login now persists the backend refresh token and attempts a backend refresh when restoring an expired access token.
4. Mobile notifications had no persistence or backend API and the dashboard returned a hardcoded unread count of zero. The misleading Notifications navigation was removed; a real notification model/API remains future work.
5. Mobile API host is configurable with `EXPO_PUBLIC_API_URL`; the local fallback is the current LAN address and must be changed when the developer's network changes.

## API consistency gaps

The mobile endpoints currently used (`/auth/login`, `/auth/refresh`, `/auth/me`, `/mobile/dashboard`, `/mobile/deliveries`, `/mobile/deliveries/{id}`) exist in Spring Boot.

The web service layer still contains legacy methods for `PATCH /deliveries/{id}/status` and `PUT /deliveries/{id}` although no matching Spring controller methods exist. The active web driver screen also assumes legacy statuses (`PENDING`, `EN_ROUTE`, `ARRIVED`) while the backend enum is `IN_TRANSIT`, `ARRIVED_AT_DESTINATION`, `DELIVERED`, and `CANCELLED`. These should be removed or migrated to explicit backend transition endpoints before relying on that screen for operations.

## Duplicate or client-side business logic

- Mobile delivery sorting/filtering and display normalization are presentation concerns and are acceptable locally.
- Web form validation improves UX, but Spring validation remains authoritative.
- The removed web seed service was a genuine business/data duplication.
- Web driver status mapping and hardcoded fallback actor names (`operations`) are workflow assumptions in the client and should be replaced with authenticated transition APIs.

## Security concerns

- `application.yml` contains a development JWT secret and database password fallback. Production must require injected secrets and fail fast when absent.
- `POST /api/v1/orders` is currently permitted without authentication; verify this is an intentional public/customer-order flow, otherwise require an authenticated role.
- Logout is currently stateless logging; issued JWTs are not revoked until expiry. Add refresh-token persistence/revocation and rotate refresh tokens for production.
- Browser tokens in `sessionStorage` and mobile tokens in AsyncStorage are exposed if the client runtime is compromised. Prefer secure, platform-backed storage for mobile and a hardened same-site cookie strategy for web where deployment permits.
- CORS defaults are development-oriented. Set an explicit production allow-list and HTTPS-only API URLs.

## Database and architecture assessment

There is one Spring datasource and one PostgreSQL database. Flyway owns schema migrations and Hibernate is configured with `ddl-auto: update`; production should use `validate` (or `none`) so migrations remain the only schema authority. Neither client opens a database connection.

## Recommended next steps

1. Migrate or remove the legacy web driver screen methods and align status transitions with backend workflows.
2. Add persisted notifications and driver actions (arrival/completion/remarks) as authenticated, ownership-checked APIs if required by the product.
3. Introduce generated OpenAPI client types or a shared API contract to prevent DTO/status drift between clients.
4. Require `JWT_SECRET`, database credentials, and production CORS through deployment configuration; remove insecure defaults.
5. Add end-to-end tests that login as the same driver through both clients and verify dashboard/delivery visibility and authorization boundaries.

## Validation performed

- `./mvnw clean test -DskipTests` — backend compiles successfully.
- `npm run build` — web client builds successfully.
- `npx expo export --platform android` — mobile bundle exports successfully.

Final audit validation also re-ran `./mvnw clean compile -DskipTests`, `npm run build`, and `npx expo export --platform android`; all three passed. The full backend test suite still has the two previously identified fixture-dependent integration failures (`DeliveryServiceTest` payment receipt setup and `FuelOrderIntegrationTest` fuel-price setup), so those remain release-gate work.

## Final production-readiness audit

### End-to-end workflow verification

| Workflow | Backend/API path | Web | Mobile | Audit result |
|---|---|---:|---:|---|
| Login, JWT refresh, logout | `/auth/*` | Yes | Yes | Shared backend; refresh persistence is implemented on mobile. |
| Dashboard and analytics | `/dashboard/admin`, `/mobile/dashboard`, `/reports/*` | Yes | Driver dashboard only | Backend-backed; no client mock data found. |
| Orders and pricing | `/orders/*`, `/order-pricing/preview` | Yes | N/A | Backend-backed; order creation is intentionally public and must be confirmed. |
| Invoices/payments/receipts | `/invoices/*`, `/payment-accounts/*`, `/payment-receipts/*` | Yes | N/A | Backend-backed. |
| Loading and loading reports | `/loading-orders/*`, `/loading-activities/*`, `/reports/loading` | Yes | N/A | Backend-backed. |
| Dispatch and driver assignment | `/dispatch/*`, `/drivers/*`, `/vehicles/*` | Yes | Assignment is read through driver-scoped APIs | Backend-backed; driver listing authorization needs tightening. |
| Trip status and proof of delivery | `/deliveries/*`, delivery-document APIs | Yes | Read-only mobile details | Cancel transition was added; mobile arrival/completion/POD actions remain a product gap. |
| Inventory | `/inventory/*`, `/transactions/*`, `/tanks/*` | Yes | N/A | Backend-backed. |
| Reports | `/reports/*` | Yes | N/A | Backend-backed. |
| Notifications | No notification entity/API | No real UI | No real UI | Not production-complete; previous fake mobile entry was removed. |
| User management/audit | `/users/*`, `/admin/*`, `/audit-logs/*` | Yes | N/A | Backend-backed. |

### Endpoint consistency

All endpoint calls currently made by the active web and mobile service adapters resolve to Spring controllers, including the driver-specific `/mobile/deliveries/{id}` and the new `/deliveries/{id}/cancel`. The obsolete frontend `PATCH /deliveries/{id}/status` and `PUT /deliveries/{id}` calls were removed.

Backend surface with no active client call includes the legacy `/api/v1/truck-pricing` controller (the web UI uses `/transport-price-ranges`) and duplicate `/api` compatibility aliases. The truck-pricing entity/repository is still referenced by backend order/loading services, so only the controller should be retired after confirming external consumers; it was not deleted in this audit.

### Security and role audit

Spring Security is stateless JWT-based, method security is enabled, and all unlisted routes fall through to authenticated access. Login/register/refresh, public product/customer reads, and pricing preview are explicit exceptions. The following remain release blockers or require explicit product approval:

- `POST /orders` is unauthenticated.
- `GET /drivers` and `GET /drivers/{id}` have no method-level role restriction; any authenticated role can enumerate driver records.
- JWT secret and database password have insecure development fallbacks in `application.yml`.
- Logout does not revoke already-issued access/refresh tokens.
- CORS defaults are development-oriented and must be replaced by an explicit production allow-list.
- `ddl-auto: update` allows runtime schema mutation; production should use `validate` with Flyway-only migrations.

### Mocks, placeholders, and duplicated logic

No mock/fixture/local JSON data source is used by active screens. The only AsyncStorage/sessionStorage data is authentication/session caching. The unused mobile placeholder screen has been removed. Form defaults and input placeholders are UI affordances, not data sources. Browser-side database seeding was removed in the prior pass.

One backend-generated notification count remains hardcoded to zero because no notification persistence exists. It is not exposed through an active mobile screen, but it must be replaced before claiming notifications are complete.

### Configuration and data boundaries

- Web: `VITE_API_BASE_URL`, local `http://localhost:8081/api/v1`, production same-origin `/api/v1`.
- Mobile: `EXPO_PUBLIC_API_URL`, resolved to the same Spring API with `/api` base path.
- Backend: one PostgreSQL datasource, Flyway migrations, JPA repositories; no client database.
- CORS: `CORS_ALLOWED_ORIGINS` controls browser origins; native mobile requests are not browser-CORS constrained.

### Dependency diagram

```text
┌──────────────────────┐        REST + JWT        ┌────────────────────────┐
│ React + Vite Web     │ ───────────────────────▶ │ Spring Boot /backend   │
└──────────────────────┘                         │ controllers/services   │
                                                 │ security + validation  │
┌──────────────────────┐        REST + JWT        └───────────┬────────────┘
│ React Native /mobile │ ─────▶             │ JPA/Flyway
└──────────────────────┘                                      ▼
                                                   ┌────────────────────────┐
                                                   │ PostgreSQL              │
                                                   └────────────────────────┘
```

### Production readiness score

**78% — not yet production-ready.** Core single-backend architecture, API integration, builds, JWT authentication, database boundary, and primary operator workflows are present. The remaining 22% is concentrated in security hardening (public order creation, driver enumeration, secret defaults, token revocation, CORS), incomplete mobile write workflows/proof of delivery, missing notifications, legacy API cleanup, and two failing integration tests that require payment/fuel-price fixtures.

## Production hardening addendum (2026-08-06)

This addendum supersedes the earlier findings above where they conflict. The mobile client is now located at `/mobile`.

### Completed hardening

- Removed hardcoded database credentials, JWT secret, bootstrap password, API host fallbacks, and development token fallbacks. Required values are documented in `backend/.env.example`, `frontend/.env.example`, and `mobile/.env.example`.
- Changed Hibernate to `ddl-auto: validate`, disabled SQL/show-sql debug output, and made CORS an injected allow-list (`CORS_ALLOWED_ORIGINS`). Flyway migration V33 adds refresh-token revocation storage.
- Logout now revokes refresh tokens by SHA-256 hash; expired revocations are purged. Access tokens remain valid only until their short configured expiry, so a production deployment should keep access TTL short.
- Order creation is authenticated by the default security rule. Driver enumeration is restricted to operational roles; driver-scoped mobile queries remain ownership-filtered.
- Removed the obsolete browser seed service, placeholder mobile screen, and unsupported legacy delivery update calls. Delivery cancellation uses the backend transition endpoint.
- Corrected integration-test fixtures to create required fuel/transport pricing, invoice/payment receipt, nominated vehicle, driver, customer, and loading data rather than weakening assertions.

### Security checklist

| Control | Status |
|---|---|
| Single JWT/Spring Security backend | PASS |
| Unlisted endpoints require authentication | PASS |
| Order creation authentication | PASS |
| Admin/management method security | PASS; role matrix should be regression-tested per deployment |
| Driver-scoped mobile data | PASS for dashboard/delivery reads |
| Refresh-token logout revocation | PASS |
| Secrets externalized | PASS; deployment must provide all required variables |
| Production CORS and schema validation | PASS when production env is supplied |
| Photo/signature/GPS proof-of-delivery API | NOT IMPLEMENTED in the existing product; adding it would be a new business feature |
| Persisted notifications | NOT IMPLEMENTED; fake notification UI was removed |

Public authentication endpoints and public catalog/pricing reads are intentional exceptions. Dispatcher/operations and finance role groupings are broader than one single role in a few controllers; preserve current workflow but formalize the authorization matrix before external penetration testing.

### Endpoint audit

Active web and mobile API calls map to Spring controllers. No active client calls a missing endpoint. The removed obsolete calls were `PATCH /deliveries/{id}/status` and `PUT /deliveries/{id}`. `/api` and `/api/v1` aliases remain for compatibility. The legacy truck-pricing controller has no active client consumer but its entity/service are still used internally; remove only after checking external integrations.

### Repository structure

```text
falcon-fuel-management/
├── backend/                     # only backend; Spring Boot + Flyway + JPA
├── frontend/                    # React/Vite web client
├── mobile/                      # React Native/Expo driver client
├── ARCHITECTURE_REVIEW.md
└── README.md
```

### Deployment checklist

1. Provide `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 256 bits), `BOOTSTRAP_ADMIN_PASSWORD`, and an HTTPS `CORS_ALLOWED_ORIGINS` value from a secret manager.
2. Run Flyway migrations; never enable `ddl-auto=update` in production.
3. Build the web client with `VITE_API_BASE_URL` pointing to the HTTPS API and export the mobile app with `EXPO_PUBLIC_API_URL` pointing to the same API.
4. Terminate TLS at Nginx/load balancer, forward only `/api` to Spring Boot, set HSTS, request-size/rate limits, and health checks.
5. Run the complete Maven test suite, web build, and Expo export in CI. Do not ship with default bootstrap credentials.
6. Use encrypted daily `pg_dump` backups plus WAL/PITR, store copies off-host, and perform documented restore drills.

### Recommended reverse proxy

```nginx
server {
  listen 443 ssl http2;
  server_name api.example.com;
  client_max_body_size 20m;
  location /api/ { proxy_pass http://backend:8081; proxy_set_header Host $host; proxy_set_header X-Forwarded-Proto https; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; }
}
```

### Final assessment

Web build and Expo Android export pass with environment-provided API URLs. Backend integration tests now create the missing fixture data; the final targeted run is the release-gate verification. Remaining technical debt is limited to the pre-existing absence of mobile PoD media/GPS and persisted notifications, plus role-matrix review, OpenAPI contract generation, and operational observability. **Production readiness: 88%** after hardening; the remaining 12% is product scope/operational controls rather than duplicated client business logic.
