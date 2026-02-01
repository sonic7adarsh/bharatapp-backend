# Frozen API: Auth and Profile (MVP v1)

## Endpoints

- POST `/api/auth/send-otp`
  - Request: `{ phone: string }`
  - Response 200: `{ success: boolean, otpId: string, ttlSeconds: number, otp: string }`
  - Notes: `otp` included for dev/test; remove in production.

- POST `/api/auth/verify-otp`
  - Request: `{ phone: string, otp: string }`
  - Response 200: `{ success: true, token: string, user: User }`
  - Errors: `400 Invalid OTP` when verification fails.

- POST `/api/storefront/auth/register`
  - Request: `{ name?: string, email: string, password: string }`
  - Response 200: `{ token: string, user: User }`

- POST `/api/storefront/auth/login`
  - Request: `{ email: string, password: string }`
  - Response 200: `{ token: string, user: User }`

- GET `/api/user/me`
  - Headers: `Authorization: Bearer <token>`
  - Response 200:
    ```json
    {
      "id": string,
      "name": string,
      "email": string,
      "phone": string,
      "active_role": string,        // e.g., CUSTOMER | SELLER | RIDER
      "allowed_roles": string[],    // assigned roles
      "tenant_id": string,
      "created_at": string          // ISO-8601
    }
    ```
  - Errors: `401 { error: "Invalid token" }` when token missing/invalid.

## Schemas

- User
  - Fields: `id`, `name`, `email`, `phone`, `role`, `tenantId`
  - Source: `com.bharatshop.domain.User`

- LoginRequest
  - Fields: `email`, `password`
  - Source: `com.bharatshop.dto.LoginRequest`

- OTP Send Response
  - Fields: `success`, `otpId`, `ttlSeconds`, `otp`
  - Source: `AuthService.sendOtp`

- OTP Verify Response
  - Fields: `success`, `token`, `user`
  - Source: `AuthController.verifyOtp`, `AuthService.verifyOtp`

- Profile (`/api/user/me`) Response
  - Fields: `id`, `name`, `email`, `phone`, `active_role`, `allowed_roles`, `tenant_id`, `created_at`
  - Source: `com.bharatshop.controller.UserController#getCurrentUser`

## Tables (Frozen)

- `zones`
  - Fields: `id`, `tenantId`, `name`, `type`, `centerLat`, `centerLng`, `radiusMeters`, `polygonJson`, `createdAt`, `updatedAt`
  - Source: `com.bharatshop.entity.ZoneEntity`

- `store_zones`
  - Fields: `id`, `tenantId`, `storeId`, `zoneId`
  - Source: `com.bharatshop.entity.StoreZoneEntity`

- `order_deliveries`
  - Fields: `id`, `tenantId`, `orderId`, `storeId`, `riderId`, `status`, `otp`, `assignedAt`, `pickedUpAt`, `completedAt`, `failureReason`
  - Source: `com.bharatshop.entity.OrderDeliveryEntity`

- `delivery_attempts`
  - Fields: `id`, `tenantId`, `deliveryId`, `status`, `note`, `ts`
  - Source: `com.bharatshop.entity.DeliveryAttemptEntity`

## Notes

- Tokens are JWT-backed when `JwtService` is enabled; include roles and tenant.
- Storefront auth is tenant-aware; ensure `X-Tenant-Domain` header is set by clients.
- Role assignment and active role are managed via `user_roles` and surfaced in `/api/user/me`.