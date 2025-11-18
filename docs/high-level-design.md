# BharatShop Backend – High Level Design (HLD)

This HLD summarizes current architecture and proposes patterns and a scalability roadmap to evolve the monolith into a modular, resilient, multi-tenant platform. It reflects existing modules (auth, catalog, cart, checkout, orders/bookings, payments, availability, events) and their interactions.

## Overview
- Architecture: Layered Spring Boot monolith — Controller → Service → Domain → Repository.
- Multi-tenancy: Tenant propagated via `X-Tenant-Domain` header; recommended central TenantContext.
- Domains: Stores, Products, Categories, Cart, Orders/Bookings, Payments, Availability (hospitality), Auth (email/phone+OTP), Events/Notifications.
- External integrations: Razorpay (payments), SMS/Email providers (Msg91/Twilio/SMTP).

## Architecture Layers
- Controller: Request validation, header parsing (tenant, auth), DTO mapping.
- Service: Business orchestration, domain invariants, state transitions, integration boundaries.
- Domain: Entities/aggregates (`Order`, `Product`, `Store`, `BookingDetails`, `PaymentOrder`, `User`).
- Repository: Persistence via JPA/DAO; include tenant-aware filters.
- Infrastructure: Gateway clients, notification senders, payment adapters, cache providers.

## Core Modules
- Auth: Register, Email login, Phone login + OTP, Profile, OTP send/verify/resend.
- Catalog: Stores, Products, Categories — discovery and storefront endpoints.
- Cart: Guest/User cart CRUD; totals calculation.
- Checkout: Unified order/booking placement; validations and store operational checks.
- Orders & Bookings: Retrieval and status tracking; seller response deadlines.
- Payments: Create payment order (gateway), verify payment signature, update order/payment status.
- Availability (Hospitality): Capacity/pricing checks for bookings, operational status enforcement.
- Events: Lightweight ingestion for analytics/telemetry; future async export.

## Domain Model (High-Level)
- Product: `id`, `name`, `price`, `currency`, `sku`, `stock`, `active`, `storeId`, `category`.
- Store: `id`, `name`, `area`, `category`, `status`, `orderingDisabled`, `closedReason`, `closedUntil`.
- CartItem: `id`, `name`, `price`, `quantity`, `requiresPrescription`.
- Order: `id`, `reference`, `type` (`order`|`room_booking`), `status`, `items`, `totals`, `paymentMethod`, `paymentInfo`, `address`, `deliverySlot`, `booking`, `createdAt`, `sellerResponseDeadline`, `storeId`.
- BookingDetails: `checkIn`, `checkOut`, `guests`, `nights`, `rooms`, `perRoomMax`, `extraMattressAllowed`, `extraMattressCount`, `mattressFeePerNight`.
- PaymentOrder: `id`, `amount`; gateway identifiers (`orderId`, `paymentId`, `signature`).
- AvailabilityResponse: `available`, `reason`, pricing breakdown (`base`, `surchargeRate`, `subtotal`, `taxes`, `fees`, `total`) + capacity fields.
- User: `id`, `name`, `email`, `phone`, `role`.
- Event: `name`, `payload`, `timestamp`.

## Tenancy Strategy
- Tenant resolution: Extract from `X-Tenant-Domain` → set `TenantContext` (ThreadLocal or request-scoped bean).
- Persistence isolation options:
  - Row-level: `tenant_id` column + Hibernate filters (short-term, simplest).
  - Schema-per-tenant: migrations per schema (Flyway/Liquibase), moderate isolation.
  - DB-per-tenant: strong isolation for premium tenants (long-term).
- Ensure tenant propagation in async tasks, schedulers, and outbound integrations.

## Authentication & Authorization
- JWT/session tokens issued on register/login; attach `Authorization: Bearer <token>`.
- OTP flows: send/verify/resend; dev returns OTP in response (limit in prod).
- Recommendation: Externalize auth using Keycloak/Spring Authorization Server for scalability and centralized policies.
- Roles: `USER`, `SELLER`, `ADMIN` (future). Use method-level `@PreAuthorize` for fine-grained authz.

## Payments Architecture
- Pattern: Strategy + Adapter for gateways (Razorpay now; Stripe/PayU later).
- Flow: Create gateway order → client completes payment → backend verifies signature → order/payment status updated.
- Reliability: Outbox pattern + message queue to ensure consistency from verification to order state; idempotent handlers.
- Security: Signature verification, amount currency checks, replay protection.

## Cart & Checkout
- Cart: Scoped by user or `X-Guest-Id`; service-level safeguards for quantity, stock, prescription items.
- Checkout validations:
  - `type = room_booking` requires `booking` payload.
  - Non-booking requires non-empty `items`.
  - Store must be available (not disabled/closed/closedUntil).
- Pricing: Server-side recomputation/verification to avoid client tampering.
- Idempotency: Use client-provided `Idempotency-Key` to prevent duplicate orders.

## Order State Machine
- Suggested states (retail): `draft → placed → seller_accepted|seller_rejected → payment_pending|paid → preparing → dispatched → delivered → cancelled`.
- Suggested states (hospitality): `placed → confirmed → checked_in → completed → cancelled`.
- Implement transitions in service layer; enforce invariants (e.g., cannot deliver before paid).
- SLAs: `sellerResponseDeadline`, auto-cancel on timeout, notifications to buyer/seller.

## Availability (Hospitality)
- Services:
  - Availability Service: date/capacity checks, operational status enforcement.
  - Pricing Service: base + surcharges + taxes + fees computation.
  - Reservation Service: soft hold (TTL) during checkout to reduce contention.
- Edge cases: overlapping bookings, extra mattresses, per-room capacity, seasonal surcharges.

## Events & Notifications
- Events: accept ingestion; enqueue to Kafka/RabbitMQ (future) for analytics pipelines.
- Notifications: Adapter pattern for SMS/Email providers; retries, backoff, DLQ.
- Audit: log significant actions with tenant and user context.

## Data & Infra
- DB: MySQL/PostgreSQL; schema management via Flyway/Liquibase.
- Cache: Redis for catalog, store status, availability holds, rate limiting.
- Search: (Optional) OpenSearch/Elasticsearch for product/store search.
- Storage: S3/MinIO for media/documents.
- Config: Centralized via Spring Cloud Config (future) for multi-env.

## Scalability Roadmap
- Phase 1 — Modular Monolith
  - Package boundaries per domain: `auth`, `catalog`, `cart`, `checkout`, `orders`, `payments`, `availability`, `stores`, `events`.
  - Internal service interfaces; explicit boundaries to limit coupling.
- Phase 2 — Service Extraction
  - Auth Service
  - Catalog Service (stores, products, categories)
  - Cart Service
  - Checkout Orchestrator
  - Order Service (state machine, SLAs)
  - Payment Service (gateway adapters, webhooks)
  - Availability/Booking Service
  - Notification Service
  - Events/Analytics Service
- Platform: API Gateway (rate limit, auth, tenant propagation), Service Discovery, centralized config, per-service DB.
- Communication: REST + async messaging (events/commands); Outbox/Saga for cross-service transactions.

## Cross-Cutting Concerns
- Observability: structured logs, trace IDs, OpenTelemetry tracing/metrics, dashboards.
- Resilience: Resilience4j (circuit breakers, retries, timeouts, bulkheads), fallbacks.
- Security: input validation, authz policies, PII masking, GDPR/IT Act compliance, payment signatures.
- Performance: pagination/default limits, cache hit ratios, N+1 avoidance, batch operations.
- Compliance: prescription workflows, audit trails, consent logs.

## Design Patterns
- Strategy: Payment gateways; notification channels.
- Adapter: External providers (SMS, Email, Payment).
- Factory: Storefront composition by tenant (`DefaultStorefrontFactory`).
- Repository: Per aggregate (Order, Product, Store).
- Aggregate Root: `Order` controls `items`, `paymentInfo`, `status` transitions.
- State Machine: Orders/bookings lifecycle.
- Builder: Complex DTOs like `CheckoutRequest` assembly.
- Facade: `CheckoutService` orchestrating cart, availability, payment.
- Outbox/Saga: Reliable cross-service workflows.
- Specification: Catalog filtering/search criteria.
- Tenant Resolver: Middleware to populate/use `TenantContext`.

## Testing Strategy
- Unit: Services, validators, pricing calculators, availability checks.
- Contract: Payment verification, checkout payloads/responses.
- Integration: Tenant context propagation, repository filters, controller flows.
- End-to-end: Add-to-cart → checkout → payment → order status transitions.
- Load/Stress: Availability and checkout under peak; rate limiting correctness.

## Recommended Next Enhancements
- Add OpenAPI via `springdoc-openapi` for auto docs and client stubs.
- Implement idempotency keys and server-side pricing verification in checkout.
- Introduce Redis-backed caches for catalog and store status.
- Add order state machine with seller workflows and SLAs.
- Move events to Kafka/RabbitMQ; add analytics processors.
- Extract payment/auth/catalog into separate services as traffic grows.
- Feature flags per tenant; remote config.
- Per-tenant rate limiting and quotas.

---

## Sequence Flows (Textual)

### Auth Login via Phone OTP
1. Client: `POST /api/storefront/auth/otp/send { phone }` (tenant header).
2. Auth Service → Notification Service: send OTP via provider (Msg91/Twilio).
3. Client receives `otpId` (dev also receives `otp`).
4. Client: `POST /api/storefront/auth/otp/verify { phone, otp }`.
5. Auth Service verifies OTP → issues token → returns `{ token, user }`.

### Checkout with Online Payment (Retail)
1. Client loads cart (guest/user) → computes preliminary totals.
2. Client: `POST /api/storefront/payments/create-order { amount, currency }` (authorized).
3. Payment Service → Razorpay: creates order → returns `paymentOrderId`.
4. Client completes payment in UI → receives `paymentId/signature`.
5. Client: `POST /api/storefront/payments/verify { orderId, paymentId, signature }`.
6. Payment Service verifies signature → marks payment verified.
7. Client: `POST /api/storefront/checkout { items/totals/... paymentInfo }`.
8. Checkout Service validates store availability → recomputes totals → places order → returns `{ order, reference }`.

### Payment Webhook → Order Update (Future Hardening)
1. Razorpay sends webhook → `/store/payments/webhook`.
2. Payment Service validates event → writes Outbox event (payment_verified).
3. Async processor publishes message → Order Service consumes → transitions order to `paid`.
4. Idempotency ensures duplicate webhooks do not cause duplicate transitions.

### Availability Check and Booking (Hospitality)
1. Client: `GET /api/availability` with store/room/dates/guests.
2. Availability Service validates operational status → capacity → Pricing Service computes totals.
3. On checkout: Reservation Service soft-holds availability; after payment, confirm booking.

---

## Non-Functional Requirements (NFRs)
- Availability: 99.9% target for core APIs; graceful degradation on provider failures.
- Latency: P95 under 300ms for critical read APIs; write flows under 800ms.
- Scalability: Horizontal scalability with stateless services; cache and DB tuning.
- Security: OWASP best practices; secure credential storage; audit trails.
- Observability: End-to-end tracing; actionable dashboards; alerting on SLAs.

## Service Decomposition Mapping (Future)
- User/Auth → Auth Service, shared directory for tokens.
- Catalog → Product/Store Service, search indexer.
- Cart → Cart Service (Redis-backed), TTL for guest carts.
- Checkout → Orchestrator; integrates payment, pricing, availability.
- Orders → Order Service, state machine and SLAs.
- Payments → Payment Service, gateway adapters and webhooks.
- Availability/Booking → Booking Service, reservation holds.
- Notifications → Notification Service, templates and delivery.
- Events/Analytics → Event Bus + processors (ETL to warehouse).

# Backend Requirements for Frontend Integration

Yeh document backend ko woh sari endpoints, headers, payloads, aur response shapes batata hai jo current frontend expect karta hai. Iske basis par backend implementation aur testing karo.

## Common Headers & Tenancy
- `X-Tenant-Domain`: tenant scoping header (frontend axios attach karta hai).
- `Authorization`: `Bearer <token>` jab user authenticated ho.
- `X-Guest-Id`: unauthenticated carts ke liye stable guest id (frontend set karta hai).
- `Accept-Language`: i18n pages/translations ke liye recommended; query param `?locale=xx-XX` fallback supported.
- `Idempotency-Key`: checkout ke liye recommend; duplicate requests ko safely handle karo.

## Discovery & Stores
- `GET /api/stores`
  - Response: array `{ id, name, area?, image?, rating?, closed? }`.
- `GET /api/stores/{id}`
  - Response: `{ id, name, area, description?, image?, hours?, closed? }`.
- `GET /api/stores/{storeId}/products` OR `GET /api/products?storeId=...`
  - Response: array `{ id, name, price, image?, category?, stock?, requiresPrescription? }`.

## Products & Categories
- `GET /api/storefront/products` (optional alternative path)
  - Response: array of products.
- `GET /api/storefront/categories` preferred; fallback `GET /api/categories`
  - Response: array `{ id, name }`.

## Availability (Hospitality)
- `GET /api/availability`
  - Query params: `storeId`, `checkIn`, `checkOut`, `guests`.
  - Response: `{ available: boolean, rooms?: [ { id, name, price, image? } ] }`.

## Cart: Storefront API
- Sabhi endpoints `X-Guest-Id` accept karte hain (unauthenticated).
- `GET /api/storefront/cart`
  - Response: `{ items: [ { id, name, price, quantity, requiresPrescription?, storeId? } ], totals?: { subtotal, tax?, deliveryFee?, discount?, tip?, payable? } }`.
  - Frontend array-only `items` bhi handle kar leta hai.
- `POST /api/storefront/cart/items`
  - Request: `{ productId, quantity, storeId? }`.
  - Response: updated cart.
- `PATCH /api/storefront/cart/items/{id}`
  - Request: `{ quantity }` (>0).
  - Response: updated cart.
- `DELETE /api/storefront/cart/items/{id}` → updated cart.
- `DELETE /api/storefront/cart` → `{ items: [] }`.

## Checkout & Orders
- `POST /api/storefront/checkout`
  - Headers: `Idempotency-Key` (recommended) to avoid duplicate orders.
  - Request:
    ```json
    {
      "items": [ { "id": "p1", "name": "...", "price": 100, "quantity": 2, "requiresPrescription": false, "storeId": "s1" } ],
      "totals": { "subtotal": 200, "tax": 18, "deliveryFee": 20, "discount": 0, "tip": 0, "payable": 238 },
      "address": { "name": "", "phone": "", "line1": "", "line2": "", "city": "", "pincode": "" },
      "deliverySlot": "2025-11-14T16:00:00+05:30",
      "deliveryInstructions": "",
      "prescriptions": [ { "url": "https://..." } ],
      "payment": { "method": "cod|online", "gateway": "razorpay?", "orderId": "", "paymentId": "", "signature": "", "status": "captured|authorized" }
    }
    ```
  - Response: order `{ id, reference, status, total, createdAt, items, address?, deliverySlot?, deliveryInstructions?, paymentMethod, paymentInfo?, totals? }`.
  - Idempotency behavior: same `Idempotency-Key` par duplicate request par existing order return karo (200) ya 409 dekar error shape return karo.
- `GET /api/storefront/orders`
  - Response: array of orders (desc by `createdAt`), first item FE me `latest` liya ja sakta hai.
- `GET /api/storefront/orders/{idOrRef}` → single order detail.

## Bookings (Hospitality)
- `GET /api/storefront/bookings`
  - Response: array `{ id, reference, status, total, createdAt, booking: { checkIn, checkOut, guests, nights?, roomsGuests? }, guest?: { name, phone? }, store?: { id, name, area? }, room?: { id, name, price, image? }, totals?: { perNight?, nights?, subtotal?, taxes?, fees?, payable } }` (desc by `createdAt`).
- `GET /api/storefront/bookings/{idOrRef}` → single booking detail.

## Payments
- `GET /api/storefront/payments/config`
  - Response: `{ gateways: ["razorpay"], methodsAllowed: { online: true, cod: true, upi: true, card: true, wallet: false }, codPolicy?: { enabled: true, minTotal?: number, maxTotal?: number, disallowedCategories?: [string] } }`.
- `POST /api/storefront/payments/create-order`
  - Request: `{ amount, currency: "INR", notes?: { cartId?, tenant?, userId? } }`.
  - Response: `{ orderId, amount, currency, gateway: "razorpay" }`.
- `POST /api/storefront/payments/verify`
  - Request: `{ orderId, paymentId, signature }`.
  - Response: `{ success: true, reference?: string }`.
  - HMAC verification mandatory for Razorpay; reference ko checkout me use kar sakte ho.

## Events (Analytics/Ingest)
- `POST /api/events`
  - Request: `{ type, payload, userId?, tenant?, ts? }`.
  - Response: `{ ok: true }` (prefer `202 Accepted`).
  - Lightweight, non-blocking; rate-limit optional.

## Authentication & Profiles (Summary)
- OTP/email/phone login, seller flows; `Authorization` propagate ho across storefront endpoints.

## i18n (Preferences, Locales, Pages)
- Preferences:
  - `GET /api/storefront/i18n/preferences` → `{ locale }`.
  - `POST /api/storefront/i18n/preferences` → body `{ locale }`, response `{ ok: true }`.
- Locales & translations:
  - `GET /api/i18n/locales` → `{ locales: [ { code, name } ], defaultLocale, fallbackLocale }`.
  - `GET /api/i18n/translations?locale=xx-XX` → `{ key: text, ... }`.
- Page bundle (per FE prompt):
  - `GET /api/i18n/pages/{page}?locale=xx-XX` → `{ "key": "text", ... }` flattened map.
  - Caching: Redis `{tenant}:{page}:{locale}`; support `ETag` + `If-None-Match` (304) or `If-Modified-Since`.
  - Keys may carry metadata in authoring system (placeholders/conditions/category); API returns resolved strings per locale.

## Notifications (Templates)
- Config:
  - `GET /api/storefront/notifications/config` → `{ channelsEnabled, whatsappProvider?, templates? }`.
- Admin authoring (optional):
  - `POST /api/admin/notifications/templates` → upsert per-event templates with locales.

## Media Uploads
- Direct S3 recommended:
  - `POST /api/media/sign-upload` → request `{ contentType, folder }`, response `{ url, fields, maxSizeMB, allowedTypes }`.
  - `POST /api/media/complete` → verify & persist metadata `{ tenant, key, size, contentType }`.
- Constraints honor karo: `allowedTypes`, `maxSizeMB`, `folderConvention`.

## Checkout Config
- `GET /api/storefront/checkout/config` → `{ deliverySlots: { enabled, slotLengthMin?, start?, end?, blackoutDays? }, tips?: { enabled, ranges: [10,20,50] }, prescriptionFlow?: { enabled, categories: ["pharmacy"] }, addressFields?: { required: ["name","phone","line1","pincode"] } }`.

## Hospitality Config
- `GET /api/storefront/hospitality/config` → `{ holdTTL: 300, overbookingPolicy: "strict|lenient", ratePlans?: { dynamic: true } }`.

## Versioning & Caching
- i18n pages/translations par `ETag`/`If-None-Match` support.
- Stable sorting: orders/bookings list desc by `createdAt`.

## Error Handling & Status Codes
- Status codes: `200/201`, `202` (events), `304` (ETag), `401`, `403`, `404`, `409` (idempotency/duplicate), `422` (validation), `429` (rate limit).
- Error shape: `{ error: { code: "string", message: "string" } }` across endpoints.
- Cart unauthorized par FE local fallback karega; orders/bookings list failure par FE empty array handle karta hai; detail par `404`/`null` expected.

## Validation Checklist
- Discovery endpoints return consistent shapes.
- Storefront cart supports CRUD + `X-Guest-Id`.
- Checkout idempotent; totals backend source of truth.
- Orders/Bookings detail endpoints present and sorted lists provided.
- Payments config + create-order + verify working.
- Events ingest non-blocking.
 - i18n preferences + locales + page bundle served with caching.
   - `GET /api/i18n/locales` → `{ locales: [{ code, name }], defaultLocale, fallbackLocale }`
   - `GET /api/i18n/translations?locale=xx-XX` → `{ key: text }` (global)
   - `GET /api/i18n/pages/{page}?locale=xx-XX` → `{ key: text }` (preferred)
   - `GET /api/storefront/i18n/preferences` → `{ locale }`
   - `POST /api/storefront/i18n/preferences` → `{ ok: true }`