---

# Frontend Integration: OTP → Seller Upgrade (Extended Prompts)

Use these prompts to wire OTP login → seller registration → role upgrade, plus filters, headers, token refresh, and Postman chain.

## API Contracts Recap
- Send OTP: `POST /api/storefront/auth/otp/send` { `phone` }
- Verify OTP: `POST /api/storefront/auth/otp/verify` { `phone`, `otp` } → returns `token`
- Create Store: `POST /api/seller/stores` → upgrades user to `seller`
- Profile: `GET /api/storefront/auth/profile` → includes `role`
- Seller Stores: `GET /api/seller/stores?status=open&ownerPhone=...`

## UI Copy (EN | HI)
- Login via OTP: "Enter OTP sent to your phone" | "अपने फ़ोन पर भेजा गया OTP दर्ज करें"
- Role upgraded: "You are now a seller" | "अब आप सेलर हैं"
- Refresh session: "Please re-login to refresh access" | "एक्सेस रीफ्रेश के लिए दोबारा लॉगिन करें"
- Create store CTA: "Create your first store" | "अपना पहला स्टोर बनाएं"
- Filter status: "Show only open stores" | "सिर्फ खुले स्टोर दिखाएं"
- Filter owner: "Filter by owner phone" | "ओनर फोन से फ़िल्टर करें"

## Token Refresh (JWT Enabled)
If JWT is enabled, refresh token after role upgrade:
```
async function refreshTokenIfNeeded() {
  const profile = await getProfile();
  if (profile?.role !== 'seller') {
    const phone = localStorage.getItem('PHONE');
    const otp = await promptUserForOtp();
    await verifyOtp(phone, otp); // stores new JWT token
  }
}
```

## Seller Listing Filters & Headers
- Status filter: `GET /api/seller/stores?status=open` (values: `open|closed|maintenance`)
- Owner phone filter: `GET /api/seller/stores?ownerPhone=+91XXXXXXXXXX`
  - Backend normalizes `+91/91/0` prefixes and non-digits; match on 10-digit number.
- Tenancy header (storefront): `X-Tenant-Domain: {{TENANT}}` (safe to include on seller APIs).

## Postman Chain (Quick Start)
1) Send OTP
```
POST http://localhost:8081/api/storefront/auth/otp/send
Body: { "phone": "+919876543210" }
```

2) Verify OTP
```
POST http://localhost:8081/api/storefront/auth/otp/verify
Body: { "phone": "+919876543210", "otp": "{{OTP}}" }
```

3) Create Store (role upgrade)
```
POST http://localhost:8081/api/seller/stores
Headers: Authorization: Bearer {{TOKEN}}
Body: { "name": "My First Store", "city": "Delhi", "area": "CP", "category": "Grocery" }
```

4) Confirm Role
```
GET http://localhost:8081/api/storefront/auth/profile
Headers: Authorization: Bearer {{TOKEN}}
Expect: role === "seller"
```

5) List Seller Stores
```
GET http://localhost:8081/api/seller/stores?status=open
Headers: Authorization: Bearer {{TOKEN}}
```

## Capability & Status Rendering
- Capability: show `booking` toggle when `capabilities.bookings === true` (e.g., `category: hospitality`).
- Status badge mapping: `open → green`, `closed → gray`, `maintenance → yellow`.

## Error Prompts
- `401`: "Please sign in to continue" | "कृपया जारी रखने के लिए साइन इन करें"
- `403`: "Access denied. Seller role required" | "एक्सेस नहीं मिला। सेलर भूमिका आवश्यक"
- `422`: "Invalid data. Please check the fields" | "गलत डेटा। कृपया फील्ड्स जांचें"
- Generic: "Something went wrong. Please try again" | "कुछ गलत हो गया। कृपया पुनः प्रयास करें"

---

Notes
- Creating a store is the supported trigger to upgrade to `seller`.
- Avoid `POST /api/auth/seller/register` after OTP login; it creates a separate identity.
- Use `ownerPhone` only for internal tooling or support flows.
 - After upgrade, all subsequent OTP logins issue `seller` role tokens.
 - JWT disabled: seller features unlock immediately post-store-creation (no re-login).

## Frontend CTA Flow (Register as Seller)
```
async function registerAsSeller(minPayload) {
  // minPayload: { name, city, area, category }
  const token = getToken();
  await fetch('/api/seller/stores', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(minPayload)
  });
  // Optional: refresh if JWT enabled and profile still not seller
  await refreshTokenIfNeeded();
  // Navigate to seller dashboard
  router.push('/seller/dashboard');
}
```