Rider Module — DB Schemas, API Contracts, Status Logic

DB Schemas
- `riders`
  - `id` (string, PK; equals `user_id` of RIDER)
  - `tenant_id` (string)
  - `name` (string)
  - `phone` (string)
  - `status` (string; `OFFLINE|ONLINE`)
  - `current_zone_id` (string, nullable)
  - `created_at` (timestamp)
  - `updated_at` (timestamp)

- `rider_status`
  - `id` (string, PK)
  - `tenant_id` (string)
  - `rider_id` (string)
  - `status` (string; `ONLINE|OFFLINE`)
  - `ts` (timestamp)

- `rider_earnings`
  - `id` (string, PK)
  - `tenant_id` (string)
  - `rider_id` (string)
  - `order_id` (string)
  - `delivery_id` (string)
  - `amount` (decimal; basic per-order)
  - `created_at` (timestamp)

API Contracts (Rider-only; JWT with `RIDER` role)
- `POST /api/rider/onboard`
  - Request: `{ "name": string, "phone": string }`
  - Response: `{ "id": string, "name": string, "phone": string, "status": string }`
  - Notes: Upserts rider record using current `user_id` as `riders.id`.

- `POST /api/rider/status`
  - Request: `{ "status": "ONLINE" | "OFFLINE" }`
  - Response: `{ "status": string }`
  - Side-effects: Appends entry to `rider_status` for history.

- `GET /api/rider/orders/assigned`
  - Response: `{ "deliveries": [ { "deliveryId": string, "orderId": string, "storeId": string, "status": string } ] }`
  - Logic: Lists `order_deliveries` for current `rider_id` and tenant.

- `POST /api/rider/orders/{orderId}/pickup`
  - Response: `{ "status": string }`
  - Logic: Locates delivery by `(tenant_id, order_id, rider_id)` and enforces forward-only transition.

- `POST /api/rider/orders/{orderId}/deliver`
  - Request: `{ "otp": string }`
  - Response: `{ "status": string, "reason": string | null }`
  - Side-effects: Records basic earning into `rider_earnings` when status becomes `DELIVERED`.
    - Idempotency: skip insert if earning exists for `(tenant_id, delivery_id)`.

Status Validation Logic (maps spec to existing delivery states)
- Allowed transitions (spec): `READY -> PICKED`, `PICKED -> DELIVERED`
- Existing delivery states: `RIDER_ASSIGNED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED`
- Mapping applied in implementation:
  - Pickup by order: requires `order_deliveries.status == "RIDER_ASSIGNED"`, sets `PICKED_UP`
  - Deliver by order: if `PICKED_UP`, first advances to `OUT_FOR_DELIVERY`, then validates OTP and sets `DELIVERED`
- Pseudo-code
  - `pickup(orderId)`
    - `d = findDelivery(tenantId, orderId, riderId)`
    - `if d.status != "RIDER_ASSIGNED": 400 INVALID_TRANSITION`
    - `markPickedUp(d.id)` → `d.status = "PICKED_UP"`
  - `deliver(orderId, otp)`
    - `d = findDelivery(tenantId, orderId, riderId)`
    - `if d.status == "PICKED_UP": d = markOutForDelivery(d.id)`
    - `if d.status != "OUT_FOR_DELIVERY": 400 INVALID_TRANSITION`
    - `d = completeWithOtp(d.id, otp)` → `d.status = "DELIVERED"`
    - `insert rider_earnings(tenant, riderId, d.orderId, d.id, amount)`

Integration Constraints
- Do not change `order_deliveries` schema or flow.
- Use `order_deliveries.rider_id` (present) to determine assignment; no new mapping table required.
- All endpoints are tenant-aware and require `RIDER` role.
 - OTP validation stays in existing service; rider endpoint only passes through.