# Backend Integration Prompt

Goal: Make every user-facing feature fully backend-driven. The frontend no longer contains business logic, mocks, or local fallbacks for critical data. This document specifies the endpoints, request/response shapes, and behavior the backend must implement for smooth integration.

## Storefront
- GET `/api/stores`
  - Query params: `search` (string), `category` (string), `city` (string)
  - Returns: array of stores
  - Store fields: `id`, `name`, `category`, `type`, `rating`, `area`, `city`, `address`, `phone`, `pincode`, `hours`, `image`

- POST `/api/stores`
  - Purpose: Public store onboarding request (seller/partner submits details)
  - Payload: `{ name, city, category, address, phone, pincode, ... }`
  - Returns: created store/onboarding request object

- GET `/api/stores/:id`
  - Returns: store object with above fields

- GET `/api/stores/:id/products` OR GET `/api/products?storeId=:id`
  - Returns: array of products for a store
  - Product fields: `id`, `name`, `description`, `price`, `image`, `category`, `inventory`

- GET `/api/storefront/products`
  - Query params: `search`, `category`, `storeId`, pagination as applicable
  - Returns: array of products

- GET `/api/storefront/products/:id`
  - Returns: single product object

- GET `/api/categories` and GET `/api/storefront/categories`
  - Returns: array of category names (strings)

## Availability & Checkout (Hospitality + Orders)
- GET `/api/availability`
  - Query params: `storeId`, `roomId`, `checkIn` (YYYY-MM-DD), `checkOut` (YYYY-MM-DD), `guests` (number), `roomsGuests` (array of numbers)
  - Returns: availability and pricing object
    - Fields: `available` (bool), `nights` (int), `rooms` (int), `perRoomMax` (int), `extraMattressAllowed` (bool), `extraMattressCount` (int), `subtotal` (int), `taxes` (int), `fees` (int), `total` (int), `surchargeRate` (number), `mattressFeePerNight` (int)
  - Note: Capacity rules, pricing calculations, surcharges, taxes, fees MUST be computed on backend.

- POST `/api/storefront/checkout`
  - Payload for orders:
    - `type`: `order`
    - `items`: array of `{ id, name, price, quantity }`
    - `paymentMethod`: `cod` | `online`
    - `paymentInfo`: `{ transactionId | paymentId | reference | orderId }` when `online`
    - `address`, `deliverySlot`, `deliveryInstructions`, `promo`
  - Payload for room bookings:
    - `type`: `room_booking`
    - `booking`: `{ checkIn, checkOut }`
    - `guest`: `{ name, phone }`
    - `room`: `{ id, name, price, image }`
    - `store`: `{ id, name, area }`
    - `notes`
  - Returns: canonical order/booking object
    - Common fields: `id`, `reference`, `status`, `total`, `totals`, `createdAt`
    - For orders: include `sellerResponseDeadline` if applicable (but any auto-cancel logic must be backend-side)
    - For bookings: include booking metadata and normalized `store` and `room` objects to display without local enrichment

## Storefront History
- GET `/api/storefront/orders`
  - Returns: array of order objects

- GET `/api/storefront/bookings`
  - Returns: array of booking objects

## Authentication
- Storefront Auth
  - POST `/api/storefront/auth/register` → create customer
  - POST `/api/storefront/auth/login` → password login
  - POST `/api/storefront/auth/login/email` → email login
  - POST `/api/storefront/auth/login/phone` → phone login
  - POST `/api/storefront/auth/otp/send` → send OTP
  - POST `/api/storefront/auth/otp/verify` → verify OTP
  - POST `/api/storefront/auth/otp/resend` → resend OTP
  - GET `/api/storefront/auth/profile` → current user profile
  - Headers: `Authorization: Bearer <token>`, `X-Tenant-Domain`

- Seller Auth
  - POST `/api/auth/seller/register`
  - POST `/api/auth/seller/login`
  - May expose `/api/auth/me` for current seller (optional for FE)

## Seller Portal
- Stores
  - GET `/api/seller/stores` (query params for filtering)
  - POST `/api/seller/stores` (supports JSON or `multipart/form-data` for documents)
  - PATCH `/api/seller/stores/:id`

- Products
  - GET `/api/seller/stores/:id/products`
  - POST `/api/seller/stores/:id/products` (supports JSON or `multipart/form-data` for images)
  - PATCH `/api/seller/products/:productId`
  - DELETE `/api/seller/products/:productId`
  - PATCH `/api/seller/products/:productId/inventory`

- Orders
  - GET `/api/seller/orders`
  - GET `/api/seller/orders/:id`
  - PATCH `/api/seller/orders/:id/status`
  - POST `/api/seller/orders/:id/refunds`
  - Note: Any automatic status transitions (e.g., auto-cancel if seller misses deadline) MUST happen on backend. Frontend will not mutate orders or enforce business rules.

- Bookings
  - GET `/api/seller/bookings`
  - GET `/api/seller/bookings/:id`
  - PATCH `/api/seller/bookings/:id/status`

- Payouts
  - GET `/api/seller/payouts`
  - POST `/api/seller/payouts/request`
  - GET `/api/seller/payouts/config`
  - PATCH `/api/seller/payouts/config`

- Analytics
  - GET `/api/seller/analytics/overview` → returns `{ totals, series }`
    - `totals`: `{ revenue, orders, ... }`
    - `series`: array of points `{ x: ISOString, y: number }`

- Announcements
  - POST `/api/seller/announcements`

## Payments
- POST `/api/storefront/payments/create-order`
  - Payload: `{ amount, currency }`
  - Returns: gateway order/session info

- POST `/api/storefront/payments/verify`
  - Payload: gateway verification fields (e.g., Razorpay IDs and signature)
  - Returns: `{ success: boolean, paymentId?, orderId?, reference? }`

## Internationalization (i18n)
- GET `/api/i18n/locales` → list available locales
- GET `/api/i18n/translations?locale=xx` → key-value translations
- POST `/api/storefront/i18n/preferences` (optional) → persist user locale

## Frontend Changes Summary
- Removed local mocks and sample data: no `STORES`, no `lib/mock`, no localStorage persistence for stores/products/orders/bookings.
- Services now return backend data or empty arrays/null on failure; no business logic runs on the frontend.
- Pages updated to fetch data from backend only (Home, Stores, Hotels, StoreDetail, RoomBooking, BookingDetail).
- Availability, pricing, capacity constraints, auto-cancellation, refunds, payouts, analytics — all are backend responsibilities.

## Events & Observability
- POST `/api/events`
  - Body: `{ name, payload?, timestamp? }`
  - Headers: `Authorization` (optional), `X-Tenant-Domain`
  - Backend enriches with user/tenant/UA/IP and persists.
- Optional admin reporting: GET `/api/admin/events?name=&from=&to=`

## Integration Expectations
- Stable, documented response shapes across endpoints.
- Consistent IDs and references for cross-linking (orders/bookings → store/room).
- Proper CORS and authentication headers as required.
- Errors should be returned with meaningful messages; frontend will display them without fallback logic.

If additional endpoints are needed, please add them with clear request/response formats. The frontend will integrate strictly via these APIs.

---

# Store Operational State & Capabilities

Backend-owned fields returned by store endpoints to control visibility and ordering:
- `status: 'open' | 'closed'`
- `orderingDisabled: boolean`
- `closedReason?: string`
- `closedUntil?: string (ISO)`
- `capabilities?: { orders?: boolean, bookings?: boolean }`

Rules
- When `status='closed'` or `orderingDisabled=true`, reject checkout with `403/409` and `{ code: 'STORE_CLOSED', message }`.
- `/api/stores/:id` and `/api/stores` responses include the above fields so FE can render closed banners and disable CTAs.
- Hospitality `capabilities.bookings=true` enables seller bookings flows; otherwise hidden.
- Auto-reopen behavior for `closedUntil` is backend-controlled.

Endpoints
- GET `/api/stores/:id` → include operational state and capabilities
- PATCH `/api/seller/stores/:id` → toggle close/open and flags
- POST `/api/storefront/checkout` → enforce closed/disabled rejection

---

# Platform Products (Optional)
- GET `/api/platform/products` → admin/vendor catalog
- POST `/api/platform/products` → create platform product

---

# Media (Optional, used by multipart uploads)
- POST `/api/media/presign-upload` → returns presigned URL and metadata for direct S3 upload
  - Request: `{ fileName, contentType, folder?, checksum? }`
  - Response (PUT flow):
    ```json
    {
      "method": "PUT",
      "uploadUrl": "https://s3.ap-south-1.amazonaws.com/<bucket>/<key>?X-Amz-...",
      "headers": { "Content-Type": "image/jpeg" },
      "bucket": "<bucket>",
      "region": "ap-south-1",
      "key": "uploads/2025/11/10/store-logos/logo.jpg",
      "expiresInSec": 900,
      "publicUrl": "https://cdn.example.com/uploads/2025/11/10/store-logos/logo.jpg"
    }
    ```
  - Response (POST multipart flow, if used):
    - Include form `fields` for HTML form-data to S3; frontend will post file accordingly.

- Optional finalize: POST `/api/media`
  - Purpose: Persist media record after successful S3 upload
  - Request: `{ key, bucket, region, size, contentType, checksum? }`
  - Response: `{ id, publicUrl }`

- GET `/api/media/:id` → fetch media metadata or file (if proxied by backend)
  - If serving directly from S3/CloudFront, return metadata including `publicUrl`

Implementation Notes (S3)
- Uploads: Frontend performs direct `PUT` to `uploadUrl` with `Content-Type` header provided by backend. No file data passes through backend for this step.
- Public access: Backend should provide a stable `publicUrl` (prefer CloudFront CDN) to be stored in product/store images. FE will reference this URL directly in `image` fields.
- Security: Validate `contentType` and `maxSize` server-side; limit folders per tenant; optionally run malware scan via backend workflows.
- Expiry & CORS: Presigned URLs should have short expiry; S3 bucket must have CORS allowing `PUT` from the app origin.
- Deletion: If needed, expose DELETE `/api/media/:id` which deletes S3 object and invalidates CDN.
- Recommended schema: Media records include `{ id, key, bucket, region, contentType, size, publicUrl, createdAt, createdBy }`.

Usage in Store/Product payloads
- Store `image` and Product `image` fields should carry the `publicUrl` (CDN/S3) returned by backend. Frontend will not transform keys.

Examples: Seller/Admin media workflows
- Store logo upload (seller)
  1) Seller requests presign:
     - POST `/api/media/presign-upload` with `{ fileName: 'logo.jpg', contentType: 'image/jpeg', folder: 'store-logos' }`
  2) FE uploads file via `PUT` to `uploadUrl` with `Content-Type: image/jpeg`.
  3) FE finalizes:
     - POST `/api/media` with `{ key, bucket, region, size, contentType }` → returns `{ id, publicUrl }`.
  4) Seller sets logo on store:
     - PATCH `/api/seller/stores/:id` with `{ image: publicUrl }` or `{ imageId: id }` (backend should support either, but prefer `image` as URL for FE simplicity).

- Product gallery upload (seller)
  1) Presign each image with `/api/media/presign-upload`.
  2) Upload each file to S3 (PUT).
  3) Finalize each image via POST `/api/media` to get `{ id, publicUrl }`.
  4) Create product with images:
     - POST `/api/seller/products` with:
       ```json
       {
         "name": "Organic Atta",
         "price": 299,
         "images": [
           { "url": "https://cdn.example.com/uploads/products/atta-front.jpg", "alt": "Front" },
           { "url": "https://cdn.example.com/uploads/products/atta-back.jpg", "alt": "Back" }
         ]
       }
       ```
     - Backend may also accept `{ images: [{ mediaId, alt }] }` if using media IDs.

- Replace/delete media
  - To replace a logo or image, upload new media and update the store/product with the new `publicUrl`.
  - To delete old media, expose DELETE `/api/media/:id` and optionally `DELETE /api/media/by-key?key=...`.
  - If using CDN, backend should handle cache invalidation as needed.

---

# Seller Bulk Product Upload (CSV)

Objective
- Allow sellers to add/update products in bulk via a CSV file with backend-owned validation, mapping, and async processing. Frontend provides a simple upload UI and shows processing results; no FE-side parsing or business logic.

Endpoints
- POST `/api/seller/products/bulk-upload/presign` (optional)
  - Purpose: Presign an S3 upload for the CSV file.
  - Request: `{ fileName: 'products.csv', contentType: 'text/csv', folder?: 'bulk-products' }`
  - Response: `{ method: 'PUT', uploadUrl, headers: { 'Content-Type': 'text/csv' }, bucket, region, key, expiresInSec }`

- POST `/api/seller/products/bulk-upload`
  - Purpose: Submit a CSV for processing (either direct multipart upload or by S3 key returned from presign).
  - Headers: `Authorization: Bearer <seller-token>`, `X-Tenant-Domain`.
  - Content-Types supported:
    - `multipart/form-data` with file field `file`.
    - `application/json` with `{ key: '<s3-key>', bucket?: '<bucket>', region?: '<region>' }`.
  - Query/body options:
    - `mode` — `'create' | 'upsert' | 'update'` (default: `'upsert'`).
    - `dryRun` — boolean; when true, validate only and return a detailed report without persisting.
    - `defaultCurrency` — string (ISO 4217) fallback if column absent.
    - `defaultTaxRate` — number fallback if column absent.
  - Response:
    - For async processing: `{ jobId: 'job_123', status: 'queued', acceptedRows: number, totalRows?: number }`
    - For `dryRun=true`: `{ dryRun: true, totalRows, validRows, invalidRows, errors: Array<{ row: number, column?: string, code, message }> }`

- GET `/api/seller/products/bulk-upload/:jobId`
  - Purpose: Poll job status.
  - Response:
    ```json
    {
      "jobId": "job_123",
      "status": "queued|processing|completed|failed",
      "startedAt": "2025-11-10T10:00:00Z",
      "completedAt": "2025-11-10T10:02:30Z",
      "stats": {
        "total": 532,
        "created": 480,
        "updated": 22,
        "unchanged": 10,
        "invalid": 20
      }
    }
    ```

- GET `/api/seller/products/bulk-upload/:jobId/errors`
  - Purpose: Fetch detailed per-row errors for the job.
  - Response: `Array<{ row: number, column?: string, code: string, message: string }>` or CSV.

CSV Schema
- Required columns: `name`, `sku`, `price`, `stockQuantity`.
- Recommended optional columns: `description`, `mrp`, `currency`, `taxRate`, `categoryId`, `categoryName`, `imageUrl`, `imageUrls` (semicolon-separated), `requiresPrescription`, `active`, `barcode`, `brand`, `unit`, `weight`, `attributes` (JSON).
- Optional `storeId` column; if absent, backend resolves seller’s current store context.

Example CSV
```
name,sku,price,stockQuantity,categoryName,imageUrls,active,currency,taxRate
Organic Atta 5kg,SKU-ATTA-5,299,120,Grocery,https://cdn.example.com/img/atta-front.jpg;https://cdn.example.com/img/atta-back.jpg,true,INR,5
Paracetamol 500mg,SKU-PARA-500,49,500,Pharmacy,https://cdn.example.com/img/para.jpg,true,INR,12
```

Processing Rules
- Mode semantics:
  - `create`: fail if `sku` exists.
  - `update`: require existing `sku`; only update provided fields.
  - `upsert`: create if new, update if exists.
- SKU uniqueness enforced per store/tenant; duplicates in the same file should be consolidated or rejected with clear errors.
- Category resolution: prefer `categoryId`; if missing, resolve by `categoryName` (case-insensitive). Unknown categories should yield `VALIDATION_ERROR` unless a backend auto-create policy exists.
- Price/tax validation: `price>0`, `mrp>=price` if present, `taxRate` in 0–100.
- Stock validation: integer `>=0`.
- Images:
  - If `imageUrls` provided, backend may store URLs as-is or download to S3 and persist `publicUrl`.
  - For large galleries, recommend pre-upload media and include media IDs instead of external URLs.
- Attributes column may contain JSON key-values; backend should validate and store as structured attributes.

Limits & Controls
- Max file size: 5–10 MB (configurable).
- Max rows per upload: 5,000 (configurable).
- Rate limiting: e.g., 3 bulk uploads per seller per hour.
- Idempotency: support `Idempotency-Key` header; retry should not create duplicates.

Error Handling
- Standard error shape: `{ code, message, details? }`.
- Common codes: `VALIDATION_ERROR`, `DUPLICATE_SKU`, `UNKNOWN_CATEGORY`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `RATE_LIMITED`.
- Partial success: overall job can be `completed` with `invalid>0`; FE will surface errors via the `/errors` endpoint.

Results & FE Expectations
- Frontend will:
  - Offer upload UI (CSV file picker) and optional `dryRun` toggle.
  - Call presign (optional), upload to S3, then submit the bulk job via `POST /bulk-upload` with `{ key }`.
  - Poll job status until `completed|failed` and fetch errors.
  - Display summary: created/updated/unchanged/invalid counts and allow downloading an error report.
- Backend must return consistent casing and numeric types; FE will not guess data types.

Security & RBAC
- Only authenticated sellers can access bulk upload endpoints for their stores.
- Multi-tenant header `X-Tenant-Domain` required when applicable.
- Backend must validate ownership of `storeId` if provided.

Testing Checklist (Bulk Upload)
- Small CSV uploads succeed with `upsert` default, correct stats returned.
- Dry run yields validation errors without persisting.
- Duplicate SKU rows produce clear errors; idempotent retries do not double-create.
- Category resolution works with `categoryId` or `categoryName`.
- Images are preserved or moved to S3 per backend policy.
- Polling and error report endpoints function with realistic payload sizes.