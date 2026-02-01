# Backend Prompt: Order Acceptance Window & Seller Actions

Objective
- Implement server-side order acceptance window, seller accept/reject/cancel actions, and auto-cancellation, fully controlled by backend configuration.
- Ensure all FE events flow to backend via existing API paths; FE should not own any acceptance window config.

Key Requirements
- Backend sets and returns `sellerResponseDeadline` for each order at creation (storefront checkout).
- Status transitions supported: `placed` → `processing` → `shipped` → `delivered`; cancellation allowed from `placed` and post-acceptance states.
- Auto-cancel orders past `sellerResponseDeadline` if `sellerAcceptedAt` is not set; return final status to FE and persist server-side.
- Accept/Reject/Cancel actions record timestamps and reasons; FE sends payloads, backend validates and persists.

Security & Headers
- Expect `Authorization: Bearer <token>` for authenticated requests.
- Support multi-tenant header: `X-Tenant-Domain: <domain>` (present in FE axios wrapper).
- Role-based access: Storefront endpoints require a customer; Seller endpoints require a seller belonging to the store.

Data Model (Order)
- id: string (UUID) and/or `reference: string` returned to FE
- status: `placed | processing | shipped | delivered | cancelled`
- totals: object with `payable` etc.
- total: number (shortcut for display)
- items: array of `{ id, name, price, quantity }`
- createdAt: ISO string
- sellerResponseDeadline: ISO string (set by backend at creation)
- sellerAcceptedAt: ISO string | null
- cancelledAt: ISO string | null
- cancellationReason: string | null
- paymentMethod: `cod | online`
- paymentInfo: optional (transaction fields)
- type: `order | room_booking` (FE filters bookings out of My Orders)
- store/address/notes: optional metadata

Endpoints (align to FE paths)

Storefront (Customer)
- POST `/api/storefront/checkout`
  - Purpose: Create a new order and compute `sellerResponseDeadline` server-side.
  - Request body: `{ items, totals, paymentMethod, paymentInfo?, address?, deliverySlot?, deliveryInstructions?, promo?, type?, booking?, guest?, room?, store?, notes? }`
  - Response: `{ order }` or full order object
    - Must include: `id | reference`, `status='placed'`, `sellerResponseDeadline` (ISO), other fields above.

- GET `/api/storefront/orders`
  - Purpose: List customer orders.
  - Response: `Order[] | { orders: Order[] }`
  - Business rule: Already-auto-cancel any `placed` orders that have crossed `sellerResponseDeadline` without acceptance; do not return stale `placed` beyond deadline.

- GET `/api/storefront/bookings` (if hospitality is in scope)
  - Response: `Booking[]`

Seller (Seller Dashboard)
- GET `/api/seller/orders`
  - Query params supported: `status?`, `from?` (ISO date), `to?` (ISO date), `limit?`
  - Response: `Order[] | { orders: Order[] }`
  - Business rule: May proactively auto-cancel expired `placed` orders (server-side) before responding.

- GET `/api/seller/orders/:id`
  - Response: `Order`

- PATCH `/api/seller/orders/:id/status`
  - Purpose: Update status or record accept/reject/cancel.
  - Request body (examples):
    - Accept: `{ status: 'processing', sellerAcceptedAt: <ISO> }`
    - Reject before deadline: `{ status: 'cancelled', cancellationReason: 'seller_rejected', cancelledAt: <ISO> }`
    - Auto-cancel (server or FE-initiated timer): `{ status: 'cancelled', cancellationReason: 'auto_cancelled_no_response', cancelledAt: <ISO> }`
    - Cancel after acceptance: `{ status: 'cancelled', cancellationReason: 'seller_cancelled_after_acceptance', cancelledAt: <ISO> }`
  - Response: updated `Order`
  - Validation:
    - If `status='processing'`, require `sellerAcceptedAt` to be present.
    - If `status='cancelled'`, require `cancelledAt` and allow `cancellationReason` (string).
    - Enforce allowed transitions; reject invalid combos with 400.

- POST `/api/seller/orders/:id/refunds`
  - Optional: Refund flow for seller, used by FE.
  - Request body: `{ amount: number, reason?: string }`
  - Response: `{ success: true } | { refund: ... }`

Backend-Driven Configuration
- Acceptance window MUST be backend-controlled. Recommended approaches:
  1) Compute per-order deadline at checkout: `sellerResponseDeadline = now + W minutes` where `W` is configurable per tenant/store/category.
  2) Or expose config endpoint `/api/config` returning `{ sellerResponseWindowMinutes: number }` and internal services use it to set deadlines. FE will not consume this directly for timing; FE only uses `sellerResponseDeadline` sent with the order.
- Auto-cancel policy:
  - Prefer server-side job/trigger to auto-cancel overdue orders and stamp `cancelledAt` + `cancellationReason`.
  - FE may send PATCH auto-cancel when countdown hits 0; backend must idempotently accept if already cancelled.

Business Rules
- Acceptance Window: Seller has `W` minutes to accept (`processing`) or reject (`cancelled`). On expiry without acceptance, auto-cancel.
- Post-acceptance cancellation allowed any time before `delivered`. Must record `cancellationReason`.
- Reasons are free-form strings. Common values used by FE: `seller_rejected`, `auto_cancelled_no_response`, `seller_cancelled_after_acceptance`.
- `delivered` is terminal; no further status changes allowed.
- Return consistent status casing; FE displays using lowercased comparisons.

Validation & Edge Cases
- Prevent accept after `sellerResponseDeadline` unless policy allows; otherwise require cancellation.
- If PATCH tries to accept when already cancelled or delivered, return 409.
- Ensure `sellerAcceptedAt` stays null unless status is `processing` or advanced.
- Ensure `cancelledAt` is set exactly once; subsequent patches should be idempotent.

Response Examples
- Checkout response (success):
```json
{
  "order": {
    "id": "ord_123",
    "reference": "BA-2024-0001",
    "status": "placed",
    "createdAt": "2024-11-10T10:00:00.000Z",
    "sellerResponseDeadline": "2024-11-10T10:05:00.000Z",
    "sellerAcceptedAt": null,
    "cancellationReason": null,
    "items": [{ "id": "p1", "name": "Item A", "price": 99, "quantity": 1 }],
    "total": 99,
    "totals": { "payable": 99 }
  }
}
```

- Seller accept PATCH:
```json
{
  "status": "processing",
  "sellerAcceptedAt": "2024-11-10T10:02:30.000Z"
}
```

- Seller reject PATCH:
```json
{
  "status": "cancelled",
  "cancellationReason": "seller_rejected",
  "cancelledAt": "2024-11-10T10:03:00.000Z"
}
```

- Auto-cancel PATCH (FE or server job):
```json
{
  "status": "cancelled",
  "cancellationReason": "auto_cancelled_no_response",
  "cancelledAt": "2024-11-10T10:05:01.000Z"
}
```

Filtering & Query
- `/api/seller/orders?status=placed|processing|shipped|delivered|cancelled&from=ISO&to=ISO&limit=number`
- Return array of orders; backend may paginate with `limit` and include cursors if needed.

Testing Checklist
- Checkout sets `sellerResponseDeadline` based on backend config.
- Seller list shows `placed` orders with deadline; accepting before expiry updates `sellerAcceptedAt` and `status=processing`.
- Reject before expiry sets `status=cancelled` with reason.
- Auto-cancel triggers correctly at or after deadline, ensures idempotency if FE also sends PATCH.
- Buyer views reflect `cancellationReason` and correct status.
- Role and tenant headers enforced; unauthorized requests rejected.

Notes
- FE will not maintain or read any acceptance window config; it relies solely on `sellerResponseDeadline` included in order payloads from backend.
- FE already integrates to these endpoints via axios; adhere to paths and response shapes above for seamless behavior.

---

# Store Closure & Disable New Orders (Backend Specification)

Objective
- Add a backend-controlled mechanism for sellers to close their store and disable new orders. The frontend must respect server flags; backend enforces at checkout.

Key Requirements
- Store entity includes operational state fields, owned by backend:
  - `status: 'open' | 'closed'` — high-level operational state.
  - `orderingDisabled: boolean` — explicit flag to block new orders even if store is visible.
  - `closedReason?: string` — optional seller-provided reason for closure.
  - `closedUntil?: string (ISO)` — optional end time; backend can auto-reopen after this.
  - `updatedAt: string (ISO)` — last operational change time.
- When a store is closed or `orderingDisabled=true`, backend must reject checkout attempts for that store with a 403/409 and a clear error message.
- Frontend should display a banner and disable quick actions when closed; no FE config — use fields returned by `/api/stores/:id` and other endpoints.

Endpoints (align to FE paths)
- GET `/api/stores/:id`
  - Response must include the fields above so FE can render closed state and disable order actions.

- PATCH `/api/seller/stores/:id`
  - Purpose: Seller toggles operational state.
  - Request body examples:
    - Close immediately: `{ status: 'closed', orderingDisabled: true, closedReason: 'Inventory audit', closedUntil: null }`
    - Schedule reopen: `{ status: 'closed', orderingDisabled: true, closedReason: 'Holiday', closedUntil: '2024-12-01T09:00:00.000Z' }`
    - Reopen: `{ status: 'open', orderingDisabled: false, closedReason: null, closedUntil: null }`
  - Response: updated store object including `status`, `orderingDisabled`, `closedReason`, `closedUntil`.
  - Validation: Only the store owner (seller) or admin can toggle. If `closedUntil` is in the past, backend may auto-clear closure.

- POST `/api/storefront/checkout`
  - Enforcement: If the order payload maps to a closed store, respond with 403/409 and `{ code: 'STORE_CLOSED', message: 'Store is closed. Please try later.' }`.
  - For multi-store carts, either hard-block entire checkout or split by store according to platform policy. Recommended: enforce single-store carts.

Backend Behavior
- Auto Reopen: If `closedUntil` is set, the backend may automatically set `status='open'` and `orderingDisabled=false` at or after that time.
- Visibility vs Ordering: `status='closed'` implies ordering disabled; a backend may still list the store (discoverable) but with closed banner.
- Search/Discovery: `/api/stores` responses should include closed flags per store so FE can indicate closed state on cards.

Business Rules & Edge Cases
- While closed, do not create new orders; allow customer to browse products.
- Existing open orders are unaffected; sellers can continue processing them.
- Hospitality: Bookings must be blocked or scheduled depending on policy; use `orderingDisabled` as the universal gate.
- Admin override: Admin may force reopen/close for compliance.

Response Examples
- Store closed state:
```json
{
  "id": "s_123",
  "name": "Sharma Grocery",
  "status": "closed",
  "orderingDisabled": true,
  "closedReason": "Holiday",
  "closedUntil": "2024-12-01T09:00:00.000Z",
  "updatedAt": "2024-11-10T11:00:00.000Z"
}
```

- Error on checkout when closed:
```json
{
  "code": "STORE_CLOSED",
  "message": "Store is closed. Please try later.",
  "closedUntil": "2024-12-01T09:00:00.000Z"
}
```

Testing Checklist (Closure)
- Seller toggles close/open via PATCH; response reflects updated flags.
- Storefront detail (`/api/stores/:id`) returns `status` and `orderingDisabled`; FE shows a closed banner.
- Quick actions (add to cart, quick buy) are disabled when closed.
- Checkout rejects with `STORE_CLOSED` when attempting to order from a closed store.
- `closedUntil` auto-reopen observed by backend or requires manual reopen as per policy.

---

# Seller Dashboard Visibility Rules

Objective
- Align dashboard links with store capabilities. FE should show bookings management only for hospitality stores and remove generic “View Orders” tab per your requirement.

Data Model Additions (Store)
- `category: string` (e.g., `Grocery`, `Pharmacy`, `Hotel`, `Hospitality`, `Residency`).
- `type?: string` (optional specific type; e.g., `Hotel`).
- `capabilities?: { orders?: boolean, bookings?: boolean }` — backend-driven capability flags.

Backend Behavior
- Return `capabilities.bookings=true` for hospitality stores; else false.
- Return `capabilities.orders=true` for all regular commerce stores; hospitality may have both based on product mix.
- If `capabilities` is absent, FE will infer hospitality via `category/type` containing `hotel|hospitality|residency` (case-insensitive).

---

# Event Tracking & Observability (Backend Specification)

Objective
- Provide a backend-driven event ingestion endpoint for UI navigation and business actions. Frontend sends events via `/api/events`; backend enriches and persists.

Endpoint
- POST `/api/events`
  - Headers: `Authorization: Bearer <token>` (optional for public events), `X-Tenant-Domain` (multi-tenant)
  - Request body: `{ name: string, payload?: object, timestamp?: number }`
    - `name` examples: `nav_click`, `product_create`, `booking_update`, `checkout_start`, `checkout_complete`
    - `payload` contains contextual fields (e.g., `{ target: '/rooms/add', context: 'account_dropdown' }`)
    - `timestamp` optional; backend should set server time if absent.
  - Response: `{ ok: true, id?: string }` on success; `{ ok: false, code, message }` on failure.

Backend Behavior
- Enrich events server-side with: user id/role (if authenticated), tenant/domain, user agent, IP, request id.
- Persist to an events table/stream; support batching/queueing and retry/backoff.
- Idempotency: accept duplicate events safely using a hash of `{userId, name, payload, timestamp}` when practical.
- Rate limiting: per-user and per-tenant guards (e.g., 60 events/min).
- Privacy: avoid sensitive payload fields; validate/sanitize inputs.
- Observability: expose admin reporting endpoints (optional) e.g., `/api/admin/events?name=nav_click&from=&to=`.

Testing Checklist (Events)
- Authenticated events are accepted and enriched; unauthenticated public events gated by policy.
- Rate limits applied; proper error responses returned.
- Events visible in analytics/BI pipeline or logs.

---

# Bookings Data Model & Status (Seller + Storefront)

Objective
- Define bookings model and state transitions to support hospitality flows and ensure backend-driven behavior.

Data Model (Booking)
- id: string (UUID)
- reference: string (human-readable)
- status: `pending | confirmed | checked_in | checked_out | cancelled`
- storeId: string, roomId?: string
- guest: `{ name, phone, email? }`
- dates: `{ checkIn: ISO, checkOut: ISO }`
- totals: `{ payable: number }`
- createdAt: ISO, updatedAt: ISO
- cancellationReason?: string

Endpoints
- Storefront:
  - GET `/api/storefront/bookings` → `Booking[] | { bookings: Booking[] }`
  - GET `/api/storefront/bookings/:id` → `Booking`
  - POST `/api/storefront/bookings` (optional) → create booking; backend validates availability.

- Seller:
  - GET `/api/seller/bookings?from=&to=&status=&storeId=` → list with pagination (`limit`, `page`)
  - GET `/api/seller/bookings/:id` → detail
  - PATCH `/api/seller/bookings/:id/status` → update status
    - Examples: `{ status: 'confirmed' }`, `{ status: 'cancelled', cancellationReason: 'no_show' }`

Backend Behavior
- Availability: implement server-side checks `/api/availability` to ensure room/slot availability.
- Capability gating: only sellers with `capabilities.bookings=true` can access seller bookings endpoints for their stores.
- Consistent casing and shapes with orders (for FE normalization).

---

# Capabilities & Visibility Enforcement

Objective
- Backend owns feature flags and capabilities; FE consumes returned flags and infers only when absent.

Requirements
- Store entity may include `capabilities: { orders?: boolean, bookings?: boolean }`.
- Auth profile may include `capabilities` for user-level flags; store-level flags take precedence for UI gating.
- Ensure `GET /api/seller/stores` returns `category | type` and `capabilities` for each store.
- Optional config endpoint `/api/config` can return defaults per tenant/category; FE does not directly toggle features without backend flags.

Error Response Schema (Standardized)
- Use consistent error shape for all endpoints:
```json
{ "code": "ERROR_CODE", "message": "Human-readable message", "details": { /* optional context */ } }
```
- Common codes: `STORE_CLOSED`, `UNAUTHORIZED`, `FORBIDDEN`, `RATE_LIMITED`, `VALIDATION_ERROR`, `NOT_FOUND`.
- Axios interceptors in FE already surface messages; keep messages concise and actionable.

Endpoints
- GET `/api/seller/stores` and GET `/api/stores/:id` must include `category|type` and preferably `capabilities`.

Frontend Expectations
- “Manage Bookings” link is shown only when `capabilities.bookings=true` OR inferred hospitality.
- “View Orders” tab removed from the dashboard as per product requirement; order management remains accessible via the seller orders page (`/seller/orders`).

Testing Checklist (Visibility)
- Seller with hospitality store(s) sees “Manage Bookings” on dashboard; non-hospitality sellers do not.
- “View Orders” tab is absent; seller orders remain accessible elsewhere.
- Capability flags override inference when present.

---

# Backend Integration Addendum: Capabilities, RBAC, Taxonomy, Cart Linkage, Metrics, Audit

Goal
- Backend supplies authoritative data so frontend does not infer behavior. This includes store capabilities, normalized taxonomy, RBAC, cart→store linkage, dashboard metrics, and audit trails.

Store Schema (Authoritative)
- Minimal fields the frontend needs returned by seller/store endpoints:
```
Store {
  id: string,
  name: string,
  category: 'Grocery' | 'Pharmacy' | 'Hotel' | 'Hospitality' | 'Residency' | 'Services' | 'Other',
  type?: 'Hotel' | 'Resort' | 'PG' | 'Homestay' | 'Clinic' | 'Salon' | string,
  status: 'open' | 'closed',
  orderingDisabled: boolean,
  capabilities: { orders: boolean, bookings: boolean },
  closedReason?: string,
  closedUntil?: string, // ISO 8601
  statusHistory?: Array<{ status: 'open'|'closed', orderingDisabled: boolean, reason?: string, at: string, by: string }>
}
```

Rules
- Backend determines `capabilities` based on store category/type and offerings; do not rely on frontend inference.
- If `closedUntil` is set and policy is auto-reopen, server flips `status='open'`, `orderingDisabled=false` at or after that timestamp and appends to `statusHistory`.

Endpoints (augmentations)
- GET `/api/stores/:id` → include full store schema fields above.
- GET `/api/seller/stores` → return array of Store for the authenticated seller.
- Optional: GET `/api/storefront/stores?city=...&category=...` → include `status`, `orderingDisabled`, `closedUntil` for listing badges.

RBAC & Auth Payload
- Auth response (login or `/api/auth/me`) should include:
```
AuthContext {
  userId: string,
  role: 'buyer' | 'seller' | 'admin',
  storeIds?: string[], // stores owned by seller
  permissions?: { manageOrders?: boolean, manageBookings?: boolean }
}
```
- Enforcement:
  - Seller bookings APIs only accessible if user owns the store AND `capabilities.bookings=true`.
  - Seller orders APIs accessible if user owns the store AND `capabilities.orders=true`.
  - Deny with `403 FORBIDDEN` when capability or ownership fails.

Cart→Store Linkage and Checkout Contract
- Frontend will include `storeId` per line item so backend can enforce store-state policies.
- Request shape:
```
POST /api/storefront/checkout
{
  buyerId: string,
  paymentMethod: 'COD' | 'ONLINE',
  items: Array<{
    productId: string,
    quantity: number,
    requiresPrescription?: boolean,
    storeId: string
  }>,
  notes?: string
}
```
- Backend validation:
  - Reject if any item’s `storeId` resolves to a store where `status='closed'` OR `orderingDisabled=true`.
  - Mixed-store cart allowed if business policy permits; otherwise enforce single-store carts by returning `422 UNPROCESSABLE_ENTITY`.
- Error example:
```
409 STORE_CLOSED
{
  code: 'STORE_CLOSED',
  message: 'Store is closed for new orders',
  storeId: 'st_123',
  closedReason: 'Maintenance',
  closedUntil: '2025-11-12T09:30:00Z'
}
```

Listing & Badge Data
- Ensure listing endpoints include `status`, `orderingDisabled`, and `closedUntil` so FE can show “Closed” chips and disable CTAs.

Dashboard Metrics by Capability
- Provide a metrics endpoint that returns orders vs bookings keys based on capabilities:
```
GET /api/seller/dashboard/metrics?periodDays=7
{
  periodDays: 7,
  stores: [
    {
      storeId: 'st_123',
      capabilities: { orders: true, bookings: false },
      metrics: {
        orders: { totalOrders: 120, revenue: 54000, avgOrderValue: 450, itemsSold: 300 },
        bookings: { totalBookings: 0, revenue: 0 }
      }
    },
    {
      storeId: 'st_456',
      capabilities: { orders: false, bookings: true },
      metrics: {
        orders: { totalOrders: 0, revenue: 0 },
        bookings: { totalBookings: 32, revenue: 96000, avgBookingValue: 3000 }
      }
    }
  ]
}
```
- If capability is false, return zeros or omit the key; FE will adapt.

Scheduled Reopen & Audit
- PATCH `/api/seller/stores/:id`
```
{
  status: 'closed' | 'open',
  orderingDisabled?: boolean,
  closedReason?: string,
  closedUntil?: string // optional schedule
}
```
- On change, append to `statusHistory` with `{ status, orderingDisabled, reason, at, by }`.

Testing Checklist (Addendum)
- Capabilities present and correctly set for hospitality vs retail.
- RBAC denies access when capability/ownership missing; allows when valid.
- Checkout fails with `STORE_CLOSED` when attempting from closed/disabled store.
- Listing responses include fields needed for FE badges and CTA disabling.
- Metrics endpoint returns orders/bookings keys aligned to capabilities.
- `closedUntil` schedules honored; `statusHistory` updated on each change.

---

# Contracts & Examples (Backend → Frontend)

Headers
- `Authorization: Bearer <token>` for all protected endpoints
- Optional `X-Request-Id` for traceability
- Optional `X-Idempotency-Key` for idempotent checkout

GET /api/seller/stores
Response
```
200 OK
[
  {
    "id": "st_123",
    "name": "Hotel Sunrise",
    "category": "Hospitality",
    "type": "Hotel",
    "status": "open",
    "orderingDisabled": false,
    "capabilities": { "orders": false, "bookings": true },
    "closedReason": null,
    "closedUntil": null
  },
  {
    "id": "st_456",
    "name": "Daily Needs",
    "category": "Grocery",
    "status": "open",
    "orderingDisabled": false,
    "capabilities": { "orders": true, "bookings": false }
  }
]
```

PATCH /api/seller/stores/:id
Request
```
{
  "status": "closed",
  "orderingDisabled": true,
  "closedReason": "Maintenance",
  "closedUntil": "2025-11-12T09:30:00Z"
}
```
Response
```
200 OK
{
  "id": "st_456",
  "status": "closed",
  "orderingDisabled": true,
  "closedReason": "Maintenance",
  "closedUntil": "2025-11-12T09:30:00Z",
  "statusHistory": [
    { "status": "closed", "orderingDisabled": true, "reason": "Maintenance", "at": "2025-11-10T10:00:00Z", "by": "seller_789" }
  ]
}
```

GET /api/storefront/stores?city=Jaipur&category=Grocery
Response (listing fields for badges)
```
200 OK
[
  { "id": "st_456", "name": "Daily Needs", "status": "closed", "orderingDisabled": true, "closedUntil": "2025-11-12T09:30:00Z" },
  { "id": "st_789", "name": "Fresh Mart", "status": "open", "orderingDisabled": false }
]
```

POST /api/storefront/checkout
Request (retail order)
```
{
  "buyerId": "user_001",
  "paymentMethod": "COD",
  "items": [
    { "productId": "p_101", "id": "p_101__v:standard__a:", "name": "Biscuit", "price": 40, "quantity": 2, "storeId": "st_456" }
  ],
  "notes": "Leave at door"
}
```
Request (hospitality booking via checkout)
```
{
  "buyerId": "user_001",
  "paymentMethod": "ONLINE",
  "type": "room_booking",
  "items": [
    { "productId": "room_301", "id": "room_301__v:deluxe__a:", "name": "Deluxe Room", "price": 3000, "quantity": 1, "storeId": "st_123" }
  ],
  "booking": { "checkIn": "2025-11-20", "checkOut": "2025-11-22", "guests": 2 },
  "paymentInfo": { "gateway": "razorpay", "orderId": "rp_order_123", "paymentId": "pay_abc", "status": "success" }
}
```
Responses
```
200 OK
{ "success": true, "order": { "id": "order_123", "status": "placed", "sellerResponseDeadline": "2025-11-10T12:10:00Z" } }

409 STORE_CLOSED
{ "code": "STORE_CLOSED", "message": "Store is closed for new orders", "storeId": "st_456", "closedReason": "Maintenance", "closedUntil": "2025-11-12T09:30:00Z" }

422 MIXED_STORE_NOT_ALLOWED
{ "code": "MIXED_STORE_NOT_ALLOWED", "message": "Cart must contain items from a single store", "stores": ["st_456", "st_999"] }

400 PRESCRIPTION_REQUIRED
{ "code": "PRESCRIPTION_REQUIRED", "message": "Prescription needed for pharmacy items", "productIds": ["p_rx_12"] }
```

GET /api/seller/dashboard/metrics?periodDays=7
Response (per store with capability keys)
```
200 OK
{
  "periodDays": 7,
  "stores": [
    {
      "storeId": "st_456",
      "capabilities": { "orders": true, "bookings": false },
      "metrics": { "orders": { "totalOrders": 120, "revenue": 54000, "avgOrderValue": 450, "itemsSold": 300 } }
    },
    {
      "storeId": "st_123",
      "capabilities": { "orders": false, "bookings": true },
      "metrics": { "bookings": { "totalBookings": 32, "revenue": 96000, "avgBookingValue": 3000 } }
    }
  ]
}
```

GET /api/storefront/bookings
Response (authenticated buyer)
```
200 OK
[
  { "id": "bk_001", "storeId": "st_123", "roomId": "room_301", "checkIn": "2025-11-20", "checkOut": "2025-11-22", "guests": 2, "status": "confirmed" }
]
```
RBAC
- Sellers can access booking management only if they own the store and `capabilities.bookings=true`.
- Buyers can access their own bookings only.

Error Codes Summary
- `STORE_CLOSED` (409) — Attempt to order/book from closed or ordering-disabled store
- `MIXED_STORE_NOT_ALLOWED` (422) — Multi-store cart rejected if policy disallows
- `PRESCRIPTION_REQUIRED` (400) — Pharmacy item lacks prescription
- `FORBIDDEN` (403) — Capability or ownership violation

---

# Bookings Management API (Seller)

Entity Schema
```
Booking {
  id: string,
  storeId: string,
  roomId: string,
  buyerId: string,
  checkIn: string, // yyyy-mm-dd
  checkOut: string, // yyyy-mm-dd
  guests: number,
  status: 'pending' | 'confirmed' | 'rejected' | 'cancelled' | 'checked_in' | 'checked_out',
  createdAt: string,
  confirmedAt?: string,
  rejectedAt?: string,
  cancelledAt?: string,
  checkedInAt?: string,
  checkedOutAt?: string,
  cancellationReason?: string,
  notes?: string,
  paymentMethod?: 'COD' | 'ONLINE',
  paymentInfo?: { gateway?: string, orderId?: string, paymentId?: string, status?: string }
}
```

Endpoints
- GET `/api/seller/bookings?storeId=st_123&status=pending&from=2025-11-01&to=2025-11-30&limit=20&page=1`
- POST `/api/seller/bookings/:id/confirm` → 200 `{ status: 'confirmed', confirmedAt: '...' }`
- POST `/api/seller/bookings/:id/reject` → 200 `{ status: 'rejected', rejectedAt: '...', reason: '...' }`
- POST `/api/seller/bookings/:id/checkin` → 200 `{ status: 'checked_in', checkedInAt: '...' }`
- POST `/api/seller/bookings/:id/checkout` → 200 `{ status: 'checked_out', checkedOutAt: '...' }`
- PATCH `/api/seller/bookings/:id` (reschedule or note update)

Rules
- RBAC: seller must own `storeId` and `capabilities.bookings=true`.
- Availability validation on confirm/reschedule to avoid double booking.
- If store is `closed` or `orderingDisabled=true`, new bookings must be blocked unless policy allows scheduling-only with deferred confirmation.

Errors
- `409 STORE_CLOSED` — Store closed; bookings not accepted
- `409 ROOM_UNAVAILABLE` — Conflict with existing booking
- `403 FORBIDDEN` — No capability/ownership

---

# Orders Management API (Seller)

Status Flow
- `placed` → `accepted` | `rejected` → `in_transit` → `delivered` | `cancelled`
- Auto-cancel from `placed` if `sellerResponseDeadline` expires and not accepted.

Endpoints
- GET `/api/seller/orders?storeId=st_456&status=placed&limit=20&page=1`
- POST `/api/seller/orders/:id/accept` → 200 `{ status: 'accepted', sellerAcceptedAt: '...' }`
- POST `/api/seller/orders/:id/reject` → 200 `{ status: 'rejected', cancellationReason: 'seller_rejected', cancelledAt: '...' }`
- POST `/api/seller/orders/:id/ship` → 200 `{ status: 'in_transit', shippedAt: '...' }`
- POST `/api/seller/orders/:id/deliver` → 200 `{ status: 'delivered', deliveredAt: '...' }`
- POST `/api/seller/orders/:id/cancel` → 200 `{ status: 'cancelled', cancelledAt: '...', cancellationReason: '...' }`

Rules
- RBAC: seller must own the store; `capabilities.orders=true`.
- Pharmacy: enforce prescription presence on accept; else `400 PRESCRIPTION_REQUIRED`.

---

# Product & Room Schema (Storefront)

Retail Product
```
Product {
  id: string,
  storeId: string,
  name: string,
  price: number,
  requiresPrescription?: boolean,
  variants?: Array<{ key: string, label: string, priceDelta?: number }> ,
  addons?: Array<{ key: string, label: string, price: number }>
}
```

Hospitality Room
```
Room {
  id: string,
  storeId: string,
  name: string,
  basePrice: number,
  maxGuests: number,
  amenities?: string[]
}
```

Availability
- POST `/api/storefront/rooms/availability` → `{ available: boolean, reason?: string }`

---

# Store Hours & Closures

- Store entity may include `hours: { mon?: { open: '09:00', close: '21:00' }, ... }`.
- Backend should compute open/closed state server-side when feasible; FE relies on `status/orderingDisabled` primarily.
- Special closures: return `closedReason` and `closedUntil` to drive FE banners and badges.

---

# Idempotency & Rate Limits

- Idempotent checkout using `X-Idempotency-Key`; server must return same order for the same key within a TTL.
- Suggested limits: 60 req/min per user for storefront; 120 req/min for seller dashboards.
- Return `429 TOO_MANY_REQUESTS` with `Retry-After`.

---

# Webhooks & Events

Outbound Webhooks (optional)
- `payment.captured` → `{ orderId, paymentId, amount }`
- `order.created` → `{ orderId, storeId, buyerId }`
- `order.cancelled` → `{ orderId, reason }`
- `booking.created` → `{ bookingId, storeId, buyerId }`
- `booking.confirmed` → `{ bookingId }`
- `store.status_changed` → `{ storeId, status, orderingDisabled, closedUntil }`

Security
- Sign webhooks with shared secret; provide `X-Signature` header.

---

# Pagination & Filtering

- Support `limit`, `page`, and optional cursor style: `cursor`, `nextCursor`.
- Standard filters: `status`, `storeId`, `from`, `to`, category/type where applicable.

---

# Error Catalogue (Extended)

- `ROOM_UNAVAILABLE` (409) — Room conflict
- `INVALID_SLOT` (400) — Delivery slot invalid/outside service hours
- `UNPROCESSABLE_ENTITY` (422) — Payload semantically invalid
- `UNAUTHORIZED` (401) — Auth missing/invalid
- `NOT_FOUND` (404) — Resource not found