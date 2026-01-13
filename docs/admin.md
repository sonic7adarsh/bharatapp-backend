# Admin Optional MVP Features

## Admin Zone Management

- Endpoints under `/api/admin/zones` (ADMIN only).
  - `POST /api/admin/zones` — Create zone for current tenant.
    - Body: `name`, `type` (`radius`|`polygon`).
      - For `radius`: `centerLat`, `centerLng`, `radiusMeters` (50–5000).
      - For `polygon`: `polygonJson` with at least 3 points.
  - `GET /api/admin/zones` — List zones scoped to current tenant.
  - `PUT /api/admin/zones/{zoneId}` — Update zone (validates type-specific fields).
    - Update payload must include all required fields for the chosen `type`.
    - Validations are performed on the request payload and do not fall back to stored values.
  - `DELETE /api/admin/zones/{zoneId}` — Delete zone (blocked if attached to stores/riders).

- Tenant-scoped: Uses `TenantContext` for tenant ID, prevents cross-tenant mutations.
- RBAC: `@PreAuthorize("hasRole('ADMIN')")` at controller level.
- Validations:
  - Enforces micro-zone rules (radius bounds; polygon minimum points).
  - Prevents deletion when linked to store/rider zone mappings.

## Admin Logistics Overrides

- Endpoints under `/api/admin/logistics` (ADMIN only).
  - `POST /api/admin/logistics/assign` — Assign rider to READY order.
    - Body: `orderId` (required), `riderId` (optional explicit assignment).
    - Respects state machine: only READY orders; tenant-scoped rider.
    - Status codes: `404` when order not found; `409` when no riders available.
  - `POST /api/admin/logistics/unassign` — Unassign rider from delivery.
    - Body: `deliveryId` (required).
    - Respects state machine: only when `RIDER_ASSIGNED` and order is READY.
    - Status codes: `404` when delivery not found.

- Tenant-scoped and RBAC enforced. Does not change seller/customer/rider flows.

## Notes

- Existing SLA deadlines, OTP delivery, inventory reserve/consume, and cancellation rules remain unchanged.
- Smoke tests included for zone validations and admin logistics overrides.