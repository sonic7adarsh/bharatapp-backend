API Prompt Examples (cURL)
==========================

Use these examples to quickly test and integrate the backend with the current UI. Replace placeholders like `{{BASE}}`, `{{TOKEN}}`, and IDs as needed.

Common headers:
- `-H "Content-Type: application/json"`
- `-H "Authorization: Bearer {{TOKEN}}"` (when logged in)
- `-H "X-Tenant-Domain: {{TENANT}}"` for storefront APIs

Storefront Auth
- Register:
  curl -X POST "{{BASE}}/api/storefront/auth/register" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"name":"Alice","email":"alice@example.com","password":"Pass123!"}'

- Login (email):
  curl -X POST "{{BASE}}/api/storefront/auth/login/email" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"email":"alice@example.com","password":"Pass123!"}'

- Login (phone):
  curl -X POST "{{BASE}}/api/storefront/auth/login/phone" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"phone":"+919999999999","password":"Pass123!"}'

 - Send OTP (phone):
   curl -X POST "{{BASE}}/api/storefront/auth/otp/send" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"phone":"+919999999999","channel":"sms"}'

 - Verify OTP:
   curl -X POST "{{BASE}}/api/storefront/auth/otp/verify" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"phone":"+919999999999","otp":"123456"}'

 - Resend OTP:
   curl -X POST "{{BASE}}/api/storefront/auth/otp/resend" -H "Content-Type: application/json" -H "X-Tenant-Domain: {{TENANT}}" -d '{"phone":"+919999999999","otpId":"OTP_abc123"}'

- Profile:
  curl -X GET "{{BASE}}/api/storefront/auth/profile" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

Products & Categories
- List products:
  curl -X GET "{{BASE}}/api/storefront/products?search=&category=&storeId=" -H "X-Tenant-Domain: {{TENANT}}"

- Product detail:
  curl -X GET "{{BASE}}/api/storefront/products/{{PRODUCT_ID}}" -H "X-Tenant-Domain: {{TENANT}}"

- Categories (primary):
  curl -X GET "{{BASE}}/api/categories" -H "X-Tenant-Domain: {{TENANT}}"

- Categories (legacy storefront):
  curl -X GET "{{BASE}}/api/storefront/categories" -H "X-Tenant-Domain: {{TENANT}}"

Stores & Store Products
- List stores:
  curl -X GET "{{BASE}}/api/stores?search=&category=" -H "X-Tenant-Domain: {{TENANT}}"

- Store detail:
  curl -X GET "{{BASE}}/api/stores/{{STORE_ID}}" -H "X-Tenant-Domain: {{TENANT}}"

- Store products (query route):
  curl -X GET "{{BASE}}/api/products?storeId={{STORE_ID}}" -H "X-Tenant-Domain: {{TENANT}}"

- Store products (nested route):
  curl -X GET "{{BASE}}/api/stores/{{STORE_ID}}/products" -H "X-Tenant-Domain: {{TENANT}}"

- Store onboarding:
  curl -X POST "{{BASE}}/api/stores" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"My Store","area":"MG Road","city":"Bengaluru","category":"Grocery"}'

Cart
- Get cart:
  curl -X GET "{{BASE}}/store/cart" -H "Authorization: Bearer {{TOKEN}}"

- Add to cart:
  curl -X POST "{{BASE}}/store/cart/add" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"id":"prod_1","name":"Product","price":99.99,"quantity":1}'

- Remove from cart:
  curl -X DELETE "{{BASE}}/store/cart/{{ITEM_ID}}" -H "Authorization: Bearer {{TOKEN}}"

Promo & Coupons
- Apply coupon (storefront route):
  curl -X POST "{{BASE}}/api/storefront/coupons/apply" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"code":"WELCOME50"}'

- Remove coupon (storefront route):
  curl -X DELETE "{{BASE}}/api/storefront/coupons/remove" -H "Authorization: Bearer {{TOKEN}}"

- Apply coupon (legacy store route):
  curl -X POST "{{BASE}}/store/cart/coupon" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" -d '{"code":"SAVE10"}'

- Remove coupon (legacy store route):
  curl -X DELETE "{{BASE}}/store/cart/coupon" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

Payments (Storefront)
- Create order:
  curl -X POST "{{BASE}}/api/storefront/payments/create-order" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"amount":1500,"currency":"INR"}'

- Verify payment:
  curl -X POST "{{BASE}}/api/storefront/payments/verify" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"razorpay_order_id":"order_abc","razorpay_payment_id":"pay_def","razorpay_signature":"sig_xyz"}'

Checkout, Orders, Bookings
- Checkout order:
  curl -X POST "{{BASE}}/api/storefront/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"items":[{"id":"p1","name":"Prod","price":100,"quantity":1}],"totals":{"payable":100},"paymentMethod":"cod","address":{"line1":"123","city":"BLR"}}'

- Checkout with prescription files (multipart):
  curl -X POST "{{BASE}}/api/storefront/checkout" -H "Authorization: Bearer {{TOKEN}}" \
    -F 'payload={"items":[{"id":"p1","name":"Medicine","price":100,"quantity":1}],"totals":{"payable":100},"paymentMethod":"cod","address":{"line1":"123","city":"BLR"},"promo":"WELCOME50"};type=application/json' \
    -F 'prescriptionFiles[]=@/path/to/prescription1.jpg' \
    -F 'prescriptionFiles[]=@/path/to/prescription2.pdf'

- List orders:
  curl -X GET "{{BASE}}/api/storefront/orders" -H "Authorization: Bearer {{TOKEN}}"

- Order detail (storefront):
  curl -X GET "{{BASE}}/api/storefront/orders/{{ORDER_ID}}" -H "Authorization: Bearer {{TOKEN}}"

- List bookings:
  curl -X GET "{{BASE}}/api/storefront/bookings" -H "Authorization: Bearer {{TOKEN}}"

- Booking detail (storefront):
  curl -X GET "{{BASE}}/api/storefront/bookings/{{BOOKING_ID}}" -H "Authorization: Bearer {{TOKEN}}"

- Check hospitality availability:
  curl -X GET "{{BASE}}/api/availability?storeId={{STORE_ID}}&roomId={{ROOM_ID}}&checkIn=2025-01-01&checkOut=2025-01-03&guests=2" -H "X-Tenant-Domain: {{TENANT}}"

External Location Service
- Reverse geocode with Nominatim:
  curl -X GET "https://nominatim.openstreetmap.org/reverse?format=json&lat={{LAT}}&lon={{LON}}&zoom=10&addressdetails=1" -H "Accept: application/json"

Seller Auth
- Seller register:
  curl -X POST "{{BASE}}/api/auth/seller/register" -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@example.com","phone":"+919999999999","password":"Pass123!","businessType":"Grocery"}'

- Seller login:
  curl -X POST "{{BASE}}/api/auth/seller/login" -H "Content-Type: application/json" -d '{"email":"alice@example.com","password":"Pass123!"}'

Seller Stores & Products
- Seller stores:
  curl -X GET "{{BASE}}/api/seller/stores" -H "Authorization: Bearer {{TOKEN}}"

- Create seller store:
  curl -X POST "{{BASE}}/api/seller/stores" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"My Store","city":"BLR"}'

 - Create seller store (FormData with image):
   curl -X POST "{{BASE}}/api/seller/stores" -H "Authorization: Bearer {{TOKEN}}" -F "name=My Store" -F "city=BLR" -F "category=Grocery" -F "imageFile=@/path/to/logo.jpg"

- Update seller store:
  curl -X PATCH "{{BASE}}/api/seller/stores/{{STORE_ID}}" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"Updated Store"}'

- Seller store products:
  curl -X GET "{{BASE}}/api/seller/stores/{{STORE_ID}}/products" -H "Authorization: Bearer {{TOKEN}}"

- Create product (JSON):
  curl -X POST "{{BASE}}/api/seller/stores/{{STORE_ID}}/products" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"Item","price":199,"description":"Desc","category":"General"}'

 - Create product (FormData with image):
   curl -X POST "{{BASE}}/api/seller/stores/{{STORE_ID}}/products" -H "Authorization: Bearer {{TOKEN}}" -F "name=Item" -F "price=199" -F "description=Desc" -F "category=General" -F "imageFile=@/path/to/item.jpg"

- Update product:
  curl -X PATCH "{{BASE}}/api/seller/products/{{PRODUCT_ID}}" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"price":249}'

- Delete product:
  curl -X DELETE "{{BASE}}/api/seller/products/{{PRODUCT_ID}}" -H "Authorization: Bearer {{TOKEN}}"

- Update inventory:
  curl -X PATCH "{{BASE}}/api/seller/products/{{PRODUCT_ID}}/inventory" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"stock":50}'

Seller Orders & Bookings
- List orders:
  curl -X GET "{{BASE}}/api/seller/orders?from=&to=&status=&page=&pageSize=" -H "Authorization: Bearer {{TOKEN}}"

- Order detail:
  curl -X GET "{{BASE}}/api/seller/orders/{{ORDER_ID}}" -H "Authorization: Bearer {{TOKEN}}"

- Update order status:
  curl -X PATCH "{{BASE}}/api/seller/orders/{{ORDER_ID}}/status" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"status":"shipped"}'

- Request refund:
  curl -X POST "{{BASE}}/api/seller/orders/{{ORDER_ID}}/refunds" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"amount":100,"reason":"Customer canceled"}'

- List bookings:
  curl -X GET "{{BASE}}/api/seller/bookings?from=&to=&status=" -H "Authorization: Bearer {{TOKEN}}"

- Update booking status:
  curl -X PATCH "{{BASE}}/api/seller/bookings/{{BOOKING_ID}}/status" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"status":"confirmed"}'

Seller Payouts & Config
- Get payouts:
  curl -X GET "{{BASE}}/api/seller/payouts" -H "Authorization: Bearer {{TOKEN}}"

- Request payout:
  curl -X POST "{{BASE}}/api/seller/payouts/request" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"amount":5000,"upiId":"alice@upi"}'

- Get payout config:
  curl -X GET "{{BASE}}/api/seller/payouts/config" -H "Authorization: Bearer {{TOKEN}}"

- Update payout config:
  curl -X PATCH "{{BASE}}/api/seller/payouts/config" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"method":"upi","upiId":"alice@upi"}'

Seller Analytics & Announcements
- Overview analytics:
  curl -X GET "{{BASE}}/api/seller/analytics/overview?from=2025-01-01&to=2025-01-31" -H "Authorization: Bearer {{TOKEN}}"

- Post announcement:
  curl -X POST "{{BASE}}/api/seller/announcements" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"title":"Holiday","message":"Store closed on Jan 26"}'

Platform Products
- List platform products:
  curl -X GET "{{BASE}}/api/platform/products?search=&storeId=" -H "Authorization: Bearer {{TOKEN}}"

- Create platform product:
  curl -X POST "{{BASE}}/api/platform/products" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"Platform Item","price":299,"description":"Desc","category":"General"}'

Store (Legacy Paths)
- List products:
  curl -X GET "{{BASE}}/store/products?search=&category=&page=0&size=20&sort=name,asc" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

- Product detail:
  curl -X GET "{{BASE}}/store/products/{{PRODUCT_ID}}" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

- Categories:
  curl -X GET "{{BASE}}/store/categories" -H "X-Tenant-Domain: {{TENANT}}"

- Checkout order:
  curl -X POST "{{BASE}}/store/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" -d '{"items":[{"id":"p1","name":"Prod","price":100,"quantity":1}],"totals":{"payable":100},"paymentMethod":"cod","address":{"line1":"123","city":"BLR"}}'

- Checkout with prescription files (multipart):
  curl -X POST "{{BASE}}/store/checkout" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" \
    -F 'payload={"items":[{"id":"p1","name":"Medicine","price":100,"quantity":1}],"totals":{"payable":100},"paymentMethod":"cod","address":{"line1":"123","city":"BLR"},"promo":"SAVE10"};type=application/json' \
    -F 'prescriptionFiles[]=@/path/to/prescription1.jpg' \
    -F 'prescriptionFiles[]=@/path/to/prescription2.pdf'

- List orders:
  curl -X GET "{{BASE}}/store/orders?page=0&size=10" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

- Order detail:
  curl -X GET "{{BASE}}/store/orders/{{ORDER_ID}}" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

- Booking detail:
  curl -X GET "{{BASE}}/store/bookings/{{BOOKING_ID}}" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}"

Prescriptions (Pharmacy)
- Upload prescriptions (storefront route):
  curl -X POST "{{BASE}}/api/storefront/prescriptions/upload" -H "Authorization: Bearer {{TOKEN}}" \
    -F 'files[]=@/path/to/prescription1.jpg' \
    -F 'files[]=@/path/to/prescription2.pdf'

- Upload prescriptions (legacy store route):
  curl -X POST "{{BASE}}/store/prescriptions/upload" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" \
    -F 'files[]=@/path/to/prescription1.jpg' \
    -F 'files[]=@/path/to/prescription2.pdf'

- Initiate payment:
  curl -X POST "{{BASE}}/store/payments/initiate" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" -d '{"amount":1500,"currency":"INR"}'

- Verify payment:
  curl -X POST "{{BASE}}/store/payments/verify" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" -d '{"razorpay_order_id":"order_abc","razorpay_payment_id":"pay_def","razorpay_signature":"sig_xyz"}'

- Payment webhook (server-to-server):

Events & Observability
- Track UI/navigation event:
  curl -X POST "{{BASE}}/api/events" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -H "X-Tenant-Domain: {{TENANT}}" -d '{"name":"nav_click","payload":{"target":"/rooms/add","context":"account_dropdown"},"timestamp":1731234567890}'

- Track business action event:
  curl -X POST "{{BASE}}/api/events" -H "Content-Type: application/json" -H "Authorization: Bearer {{TOKEN}}" -d '{"name":"booking_update","payload":{"bookingId":"b_123","status":"confirmed"}}'

- Suggested response (success):
  { "ok": true, "id": "evt_abc123" }

- Suggested response (failure):
  { "ok": false, "code": "RATE_LIMITED", "message": "Too many events" }

Capabilities & Config
- Get seller stores (with capabilities):
  curl -X GET "{{BASE}}/api/seller/stores" -H "Authorization: Bearer {{TOKEN}}"

- Optional: Get platform config:
  curl -X GET "{{BASE}}/api/config" -H "Authorization: Bearer {{TOKEN}}"
  # Response may include capability defaults and acceptance window values used server-side.
  curl -X POST "{{BASE}}/store/payments/webhook" -H "Content-Type: application/json" -d '{"event":"payment.captured","payload":{}}'