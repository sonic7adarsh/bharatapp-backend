# BharatShop – Design Patterns & Scaling Playbook

This document maps concrete design patterns to current modules, proposes bounded contexts (Retail vs Hospitality), and outlines best practices to scale to millions of users. It is implementation-ready guidance to follow before coding changes.

## Bounded Contexts
- Retail (Store Catalog & Orders): Products, Categories, Cart, Retail Checkout, Retail Orders.
- Hospitality (Hotel/Rooms & Bookings): Rooms, Availability, Booking Checkout, Booking Orders.
- Shared: Auth, Payments, Media (images/docs), Notifications, Events/Analytics, Tenancy.
- Rationale: Different lifecycles (inventory vs capacity), pricing logic, and state machines deserve distinct modules/services.

## Module Split (Monolith → Modular Monolith)
- `catalog-retail`: Store, Product, Category, Search.
- `hospitality`: Room, Availability, Booking, Pricing.
- `cart`: Guest/user carts; retail-only in phase 1.
- `checkout`: Orchestrator for retail/hospitality flows; validates store operational state.
- `orders`: Retail orders & hospitality bookings; common envelope, separate state machines.
- `payments`: Gateway adapters, signature verification, webhooks.
- `media`: Presign uploads, media metadata, S3 keys/public URLs.
- `auth`: Register/Login/OTP/Profile; centralized token issuing.
- `events`: Ingestion and export.

## Design Patterns (Where/How)
- Strategy
  - Payments: `PaymentGateway` interface; implementations `RazorpayGateway`, `PayUGateway`, `StripeGateway`.
  - Notifications: `NotificationChannel` for SMS/Email providers.
- Adapter
  - External providers: wrap SDKs (Razorpay, Msg91, Twilio, AWS S3) behind adapters.
- Factory
  - Storefront composition per tenant: `StorefrontFactory` (`DefaultStorefrontFactory`) to toggle features by tenant.
- Repository
  - Domain persistence per aggregate: `OrderRepository`, `ProductRepository`, `StoreRepository`, `BookingRepository`.
- Aggregate Root
  - `Order`/`Booking`: gateway for items/booking details/totals/status changes; enforce invariants.
- State Machine
  - Retail order: `draft → placed → accepted/rejected → paid → preparing → dispatched → delivered → cancelled`.
  - Booking: `placed → confirmed → checked_in → completed → cancelled`.
- Facade
  - `CheckoutService`: orchestrates pricing validation, availability checks, payment, and order creation.
- Builder
  - DTO assembly: `CheckoutRequest`, `AvailabilityResponse` without leaking internals.
- Specification
  - Catalog search: compose filters (category, price range, text search).
- Outbox + Saga (Reliability)
  - Payment verification → order state transitions; cross-service workflows (future).
- CQRS (Selective)
  - Read-optimized projections for product listings/orders dashboards; write model stays normalized.
- Circuit Breaker/Resilience
  - External calls (payment/notifications) protected via Resilience4j.

## Tenancy Best Practices
- TenantContext: resolve from `X-Tenant-Domain`; inject via interceptor.
- Persistence isolation: start with `tenant_id` + JPA filters; migrate to schema-per-tenant or DB-per-tenant for premium tenants.
- Rate limits & quotas per tenant; config flags via remote config.

## Data & Storage
- Primary DB: MySQL/PostgreSQL; Flyway/Liquibase for migrations.
- Cache: Redis (catalog caching, store status, availability holds, rate limiting).
- Search: OpenSearch/Elasticsearch for text search and facets (optional).
- Media: S3/MinIO via presigned uploads; store `publicUrl` on Product/Store.
- Files/Docs: S3/MinIO with lifecycle rules; CDN (CloudFront) for delivery.

## API Contracts (Key Improvements)
- Pagination everywhere: default limits; cursors or page/limit.
- Idempotency: `Idempotency-Key` on checkout & payment verify.
- Server-side pricing verification: compute totals on backend to avoid client tampering.
- Consistent errors: `{ message, code?, reason? }`.
- Validation: strong schema constraints and descriptive messages.

## Performance & Scale
- Indexing: DB indexes on `tenant_id`, `store_id`, `product.category`, `order.status`, `created_at`.
- N+1 avoidance: review repositories; batch loads; projections for listings.
- Caching: Redis for hot endpoints; cache invalidation on write.
- Async workflows: notifications, analytics, webhooks via queues.
- Backpressure: rate limits, bulkheads, circuit breakers.
- Observability: OpenTelemetry traces, metrics (latency, error rate, saturation), structured logs.

## Security & Compliance
- Auth: JWT; short-lived access tokens; refresh mechanism.
- Authorization: method-level policies; roles (`USER`, `SELLER`, `ADMIN`).
- Input hardening: validation; prescription-specific rules.
- Payments: signature verification, amount checks, replay protection.
- PII: encryption at rest; redaction in logs; consent/audit trails.

## Hospitality Separation (Example)
- Entities: `Room`, `RatePlan`, `Booking`, `AvailabilityCalendar`.
- Pricing: `PricingService` computes `base + surcharge + taxes + fees`.
- Availability: `AvailabilityService` checks capacity/operational status.
- Reservation holds: Redis-backed soft-hold with TTL during checkout.
- Service boundary: separate `hospitality` module; later extract to microservice with own DB.

## Media Service (S3) — Implementation Plan
- Endpoints:
  - `POST /api/media/presign-upload` → returns `{ method, uploadUrl, headers, bucket, region, key, expiresInSec, publicUrl }`.
  - `POST /api/media` → finalize and persist metadata `{ key, bucket, region, size, contentType }` → returns `{ id, publicUrl }`.
  - `DELETE /api/media/:id` → deletes S3 object and invalidates CDN (optional).
- AWS SDK v2 S3: use `S3Presigner` for presign; `S3Client` for deletes.
- Config: `aws.region`, `aws.s3.bucket`, optional `aws.cloudfront.domain`, creds via IAM role/keys.
- Frontend flow: direct `PUT` to S3 using presigned URL; store `publicUrl` in Product/Store payloads.
- Security: enforce `contentType`, `maxSize`, allowed folders per tenant; short expiry; CORS on bucket.

## Migration Path (Monolith → Services)
- Extract first: Payments, Auth, Media (stateless, clear boundaries).
- Then: Catalog, Orders, Hospitality.
- Introduce API Gateway; service discovery; per-service schemas; async messaging.

## Testing Strategy
- Unit tests: services, pricing, availability, verification logic.
- Contract tests: payment adapters, presign endpoints.
- Integration tests: tenant filters, checkout validations, order transitions.
- E2E: Cart → Payment → Checkout → Status updates.
- Load tests: availability (peak dates), checkout (flash sales), media uploads.

## Action Items (Prioritized)
- Implement Media Service with S3 presign/finalize; switch image fields to `publicUrl`.
- Add idempotency and server-side pricing verification in checkout.
- Introduce Redis caching and rate limiting per tenant.
- Add order/booking state machines with SLAs and notifications.
- Enable OpenAPI docs via `springdoc-openapi` to align FE/BE.
- Add observability stack (OTel + dashboards) and resilience policies.

## Requirements Collection (Frontend + Product)
- Tenants & locales: supported locales per tenant, default locale, `Accept-Language` header policy and fallback order.
- Pages & copy keys: list each FE page slug and provide copy keys with language variants and variable placeholders.
- Domain translations: which entity fields need localization (product/store name/description) and which locales to serve.
- Notifications: user/tenant channel preferences (`sms`, `email`, `whatsapp`), template keys per event, opt-in/opt-out requirements.
- Media policy: allowed formats (`jpg/png/webp/pdf`), max size, target dimensions, cropping rules, CDN domain, caching headers.
- Checkout flow specifics: delivery slot rules, prescription flow, idempotency-key usage, pricing confirmation UX.
- Payment gateways: which providers to enable (Razorpay/Stripe/PayU), capture/authorize mode, refund flows.
- Hospitality needs: availability search params, rate plans, hold TTL, overbooking policy.
- Security/compliance: PII handling, consent texts, analytics identifiers, audit requirements.

## Entities & Normalization
- Product: move `category` to master `categories` table; support multi-category via `product_category_map` if needed.
- Store: add `StoreStatus` enum and `store_capabilities` table for flags (accepts_orders, accepts_bookings, prescription_required, delivery_slots config).
- Order: keep `Order` as aggregate; normalize payments into `payment_transactions` with gateway, amounts, currency, ids, signature, status, timestamps.
- Address: separate `addresses` table for reusable addresses; store immutable snapshot in orders.
- Tenancy: ensure `tenant_id` column for major tables; apply JPA filter for queries.
- Audit: add `created_at`, `updated_at`, `created_by` consistently.

## Enums Catalogue
- OrderType: `ORDER`, `ROOM_BOOKING`.
- OrderStatus: `DRAFT`, `PLACED`, `SELLER_ACCEPTED`, `SELLER_REJECTED`, `PAYMENT_PENDING`, `PAID`, `PREPARING`, `DISPATCHED`, `DELIVERED`, `CANCELLED`.
- PaymentMethod: `COD`, `ONLINE`, `UPI`, `CARD`, `WALLET`.
- PaymentStatus: `INITIATED`, `VERIFIED`, `FAILED`, `REFUNDED`.
- StoreStatus: `OPEN`, `CLOSED`, `DISABLED`.
- UserRole: `USER`, `SELLER`, `ADMIN`.
- AvailabilityReason: `STORE_UNAVAILABLE`, `CAPACITY_FULL`, `INVALID_INPUT`, `CLOSED_UNTIL`.

## Notifications – WhatsApp Integration
- Providers: Twilio WhatsApp (quick if Twilio is used) or WhatsApp Cloud API (Meta official).
- Configuration: `notifications.provider.whatsapp = twilio|meta` with credentials (`TWILIO_*` or `WHATSAPP_CLOUD_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`).
- Design: add `WhatsAppChannel` implementing `NotificationChannel`; register via `NotificationConfig` in composite service.
- Templates: store message templates `{ template_key, locale, text, variables }`; render per locale with placeholders.
- Events: trigger notifications on `ORDER_PLACED`, `SELLER_ACCEPTED`, `DELIVERY_UPDATE`, `BOOKING_CONFIRMED` based on tenant/user preferences.
- Compliance: user opt-in storage, rate limiting, retries, WhatsApp policy adherence.

## Internationalization (Backend-Driven)
- Pages API: `GET /api/i18n/pages/{page}?locale=hi-IN` returns `{ key: text }` for the requested page.
- Storage: `translations` table with `tenant_id, page, key, locale, text, version` and uniqueness constraints; editing via admin tools.
- Caching: Redis cache key `{tenant}:{page}:{locale}` with TTL; bust on updates; ETag/If-None-Match support.
- Domain i18n:
  - Option A: per-entity JSON fields (`name_i18n`, `description_i18n`) like `{ "en-IN": "...", "hi-IN": "..." }`.
  - Option B: `entity_translations` table (`entity_type, entity_id, field, locale, text`) for normalized storage and better indexing.
- Frontend usage: send `Accept-Language` or `?locale=...` consistently; rely on backend fallbacks to default tenant locale.

### Frontend Prompt – Pages & Keys
Provide page-wise JSON keys with language variants and placeholders.

Example (Checkout page):

```
page: "checkout"
keys: {
  "title": { "en-IN": "Checkout", "hi-IN": "चेकआउट" },
  "placeOrder": { "en-IN": "Place Order", "hi-IN": "ऑर्डर करें" },
  "deliverySlot": { "en-IN": "Delivery Slot", "hi-IN": "डिलीवरी स्लॉट" },
  "welcomeUser": { "en-IN": "Welcome, {{name}}", "hi-IN": "स्वागत है, {{name}}" }
}
```

Also provide:
- Supported locales list per tenant and default locale.
- Which domain fields should be localized (product/store name/description) and required locales.
- Notification channel preferences (`sms`, `email`, `whatsapp`) and template keys per event.
- Versioning policy for page bundles (for cache control).

## S3 Image Storage – Status & Plan
- Current status: no direct AWS S3 SDK usage found in code; adopt Media Service with presign and finalize endpoints.
- Endpoints:
  - `POST /api/media/presign-upload` → `{ method, uploadUrl, headers, bucket, region, key, expiresInSec, publicUrl }`.
  - `POST /api/media` → persist `{ key, bucket, region, size, contentType }` → returns `{ id, publicUrl }`.
  - `DELETE /api/media/:id` → delete S3 object; optional CDN invalidation.
- Frontend flow: direct `PUT` to S3 using presigned URL; backend stores `publicUrl` on Product/Store.
- Requirements to collect: `aws.region`, `aws.s3.bucket`, IAM/cross-account setup, CORS policy, allowed content types, max size, folder conventions per tenant.

## Additional Scale Practices (Operational)
- Idempotency keys: require `Idempotency-Key` in checkout and payment verification endpoints; store request hash to prevent duplicate side effects.
- Pricing verification: recompute totals server-side; compare with client-provided values; reject mismatches and log anomalies.
- Observability: include correlation IDs, per-tenant metrics dashboards, error budgets; OpenTelemetry tracing across checkout/payment paths.
- Rate limiting: per tenant/user/IP on critical endpoints; bursts controlled with Redis tokens.
- Indexes & projections: ensure indexes on high-cardinality columns and use lightweight projections for listings.