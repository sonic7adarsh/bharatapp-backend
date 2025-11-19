# BharatShop – Design Patterns & Scaling Playbook (Implementation Prompts)

Yeh document pure project ka design-patterns audit + practical prompts deta hai. In prompts ko follow karke tum behaviour, creational aur structural patterns ko systematically implement kar sakte ho, aur system design ko scale-ready bana sakte ho.

## Quick Architecture Audit (Current)
- Layered monolith present: `Controller → Factory → Service → Domain → Repository`.
- Multi-tenant hook via `FactoryProvider`; `X-Tenant-Domain` se tenant pass hota hai.
- Patterns already visible: Factory (`DefaultStorefrontFactory`, `DefaultSellerFactory`), Repository (Spring Data), Adapter-ish for payments/notifications planned, basic state checks in checkout.
- Gaps: Central `TenantContext` missing, checkout orchestration scattered, validations mixed in controllers, payment/notification adapters incomplete, idempotency & outbox not implemented, specification queries absent.

## Creational Patterns — Prompts

### 1) Factory Specialization by Tenant
- Goal: Tenant-wise feature toggles and service wiring.
- Prompt:
  - Create `com.bharatshop.tenancy.TenantContext` (ThreadLocal) and `TenantInterceptor` that reads `X-Tenant-Domain` and sets context.
  - Update `FactoryProvider#getFactory(String tenantDomain)` to resolve specialized factories by tenant (map/cache), fallback to `DefaultStorefrontFactory`.
  - Same for `getSellerFactory`.
- Acceptance: Controllers no longer pass tenant strings around; services read `TenantContext.current()`.

### 2) Builder for Complex DTOs
- Goal: Readability + immutability for complex responses.
- Prompt:
  - Introduce builders for `AvailabilityResponse`, `PaymentOrder`, and `Order.Totals`.
  - Use fluent builder methods; avoid telescoping constructors.
- Acceptance: Controllers/services use `.builder()` to assemble DTOs without null juggling.

### 3) Object Mother (Tests)
- Goal: Stable, reusable test data creation.
- Prompt:
  - Add `testsupport/ObjectMothers` for `Store`, `Product`, `Order`, `BookingDetails` with sensible defaults per domain.
- Acceptance: Unit/integration tests construct rich objects via object mothers.

## Structural Patterns — Prompts

### 4) Facade for Checkout Orchestration
- Goal: Single orchestrator over validations, pricing, availability, payment, and order creation.
- Prompt:
  - Create `CheckoutService` (Facade) with methods: `placeRetailOrder(...)`, `placeBooking(...)`.
  - Move validation logic (items required, store availability, pricing recompute, idempotency) from `CheckoutController` into `CheckoutService`.
  - Controller becomes thin: parse, delegate, return.
- Acceptance: Checkout flow is testable via service unit tests; controller is slim.

### 5) Adapter + Ports for External Providers
- Goal: Clean boundary for payments/notifications/storage.
- Prompt:
  - Define `PaymentGateway` port (`createOrder`, `verify`, `refund?`) with `RazorpayAdapter` implementation; keep room for `StripeAdapter`, `PayUAdapter`.
  - Define `NotificationChannel` port (`sendSms`, `sendEmail`) with `Msg91Adapter`, `TwilioAdapter`, `SmtpAdapter`.
  - Define `MediaStorage` port with `S3Adapter`/`MinIOAdapter`.
- Acceptance: Services depend on interfaces; adapters handle SDK specifics.

### 6) Repository + Specification
- Goal: Powerful, composable querying without leaking details.
- Prompt:
  - Introduce Specification objects for catalog filtering: `StoreSpec.byCategory`, `StoreSpec.search`, `ProductSpec.byStore`, composable AND/OR.
  - Add simple query DSL or use Spring Data Specifications.
- Acceptance: Discovery endpoints use specifications; code stays expressive.

### 7) CQRS (Selective) Read Models
- Goal: Fast listings/dashboards under load.
- Prompt:
  - Create lightweight projections for `StoreListItem`, `OrderSummary`, `BookingSummary` (read models) separate from write models.
  - Services expose read ops returning projections, write ops return full aggregates.
- Acceptance: List endpoints return lean DTOs; complex aggregates not over-fetched.

## Behavioral Patterns — Prompts

### 8) Chain of Responsibility for Validations
- Goal: Pluggable validation steps.
- Prompt:
  - Implement `CheckoutValidationChain` with handlers: `ItemsValidator`, `StoreAvailabilityValidator`, `PricingValidator`, `PrescriptionPolicyValidator`, `IdempotencyValidator`.
  - `CheckoutService` runs the chain; each handler returns `ok` or throws domain error.
- Acceptance: Validation order and composition are testable and extendable.

### 9) Strategy for Pricing & Policies
- Goal: Different pricing/policy rules per tenant/category.
- Prompt:
  - Introduce `PricingStrategy` (retail/hospitality), `CodPolicyStrategy`, `DiscountStrategy` with tenant/category-based selection via factory.
  - Wire selection through `FactoryProvider` using `TenantContext`.
- Acceptance: Strategies switch cleanly; unit tests verify per-tenant behavior.

### 10) State Machine for Orders/Bookings
- Goal: Predictable lifecycle transitions.
- Prompt:
  - Define explicit states and transitions; implement in `OrderService`/`BookingService`.
  - Enforce invariants (e.g., cannot dispatch before paid; auto-cancel on SLA breach).
  - Add `sellerResponseDeadline` timers.
- Acceptance: Transition APIs reject invalid moves; state diagrams reflected in code.

### 11) Observer/Event for Notifications & Analytics
- Goal: Decouple side effects.
- Prompt:
  - Publish domain events (`order_placed`, `payment_verified`, `store_closed`) via `EventBus`.
  - Handlers trigger notifications/analytics; prepare for Kafka/RabbitMQ later.
- Acceptance: Core flows remain synchronous; side effects move to handlers.

## Tenancy & Cross-Cutting — Prompts

### 12) TenantContext & Propagation
- Goal: Central, consistent tenant scoping.
- Prompt:
  - Implement `TenantContext`, interceptor, and filter to set/clear per request.
  - Ensure async tasks propagate context (decorated `Executor` or context snapshot).
- Acceptance: Services read tenant implicitly; controllers stop threading headers.

### 13) Resilience (Circuit Breakers/Timeouts/Retry)
- Goal: Stable under provider issues.
- Prompt:
  - Add Resilience4j annotations/config around payment/notification/storage adapters.
  - Define sensible timeouts and fallbacks; capture metrics.
- Acceptance: External failures don’t cascade; observability shows circuit activity.

### 14) Idempotency & Outbox
- Goal: No duplicate side effects; reliable async.
- Prompt:
  - Add `IdempotencyService` using request hash keyed by `Idempotency-Key` header (checkout/payments).
  - Implement Outbox table + processor for `payment_verified → order_paid` updates.
- Acceptance: Replays safe; webhooks processed exactly-once.

## API & Error Best Practices — Prompts
- Pagination defaults everywhere; enforce `page`/`limit` or cursor.
- Standardized errors: `{ code, message, details? }` with domain codes like `STORE_CLOSED`, `INVALID_ITEMS`, `INVALID_STATUS`.
- Strong validation messages; avoid silent failures.
- Server-side pricing recompute and mismatch rejection.

## Concrete Implementation Steps (File-Level)
- `src/main/java/com/bharatshop/factory/FactoryProvider.java`
  - Add tenant specialization registry; use `TenantContext`.
- `src/main/java/com/bharatshop/web/CheckoutController.java`
  - Delegate to new `CheckoutService` facade; remove heavy validation from controller.
- `src/main/java/com/bharatshop/service/CheckoutService.java` (new)
  - Implement validation chain, pricing recompute, store availability policy, idempotency, order creation.
- `src/main/java/com/bharatshop/policy/StoreAvailabilityPolicy.java` (new)
  - Encapsulate closed/disabled/closedUntil checks; return `{ available, code, reason, closedUntil }`.
- `src/main/java/com/bharatshop/payment/PaymentGateway.java` + `adapter/RazorpayAdapter.java`
  - Define gateway port and adapter with Resilience4j.
- `src/main/java/com/bharatshop/notification/NotificationChannel.java` + adapters
  - Implement SMS/Email adapters.
- `src/main/java/com/bharatshop/tenancy/TenantContext.java` + `TenantInterceptor.java`
  - Centralize tenant scoping.
- `src/main/java/com/bharatshop/spec/*`
  - Add Specifications for Store/Product discovery and apply in services.
- `src/main/java/com/bharatshop/events/*`
  - Event bus + handlers for notifications/analytics (seed now, queue later).

## Acceptance Matrix (High-Level)
- Controllers slim; services hold orchestration.
- Tenant propagation centralized.
- Payment/notifications wrapped behind ports/adapters.
- Checkout validations pluggable and tested.
- Domain events published for key transitions.
- Read models/projections used for listings.
- Standardized error shapes with domain codes.

## Rollout Plan
- Phase 1: TenantContext, CheckoutService facade, StoreAvailabilityPolicy, error standardization.
- Phase 2: Payment/Notification adapters + resilience, IdempotencyService, Outbox skeleton.
- Phase 3: Specifications/CQRS projections, event bus handlers, rate limiting.
- Phase 4: Service extraction (Auth/Payments/Catalog) when traffic demands.

---

## Quick Prompts (Copy-Paste for Tasks)
- Implement `TenantContext` and wire interceptor; remove direct tenant passing in controllers.
- Create `CheckoutService` and move validations there; add `CheckoutValidationChain`.
- Add `StoreAvailabilityPolicy` and reuse in checkout and availability.
- Introduce `PaymentGateway` interface + `RazorpayAdapter` with Resilience4j.
- Add `NotificationChannel` interface + SMS/Email adapters.
- Add `IdempotencyService` with `Idempotency-Key` support for checkout/payments.
- Seed Outbox table and processor for payment → order state transitions.
- Add `Specification` classes for discovery queries; refactor `StoreService`/`ProductService` to use them.
- Add read projections for listings; controllers return lean DTOs.
- Standardize error responses with `code` + `message` + optional `details`.