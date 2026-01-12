# BharatShop API Documentation

Complete API reference for the BharatShop hyperlocal marketplace platform.

## 🏪 Storefront APIs

### Authentication

#### Register User
```http
POST /api/storefront/auth/register
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "Pass123!"
}
```

#### Login (Email)
```http
POST /api/storefront/auth/login/email
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "email": "alice@example.com",
  "password": "Pass123!"
}
```

#### Login (Phone + OTP)
```http
POST /api/storefront/auth/login/phone
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "phone": "+919999999999",
  "otp": "123456"
}
```

#### Send OTP
```http
POST /api/storefront/auth/otp/send
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "phone": "+919999999999"
}
```

#### Verify OTP
```http
POST /api/storefront/auth/otp/verify
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "phone": "+919999999999",
  "otp": "123456"
}
```

#### Get Profile
```http
GET /api/storefront/auth/profile
Authorization: Bearer {token}
X-Tenant-Domain: {tenant}
```

### Products & Discovery

#### List Products
```http
GET /api/storefront/products?search=&category=
X-Tenant-Domain: {tenant}
```

#### Get Product Details
```http
GET /api/storefront/products/{productId}
X-Tenant-Domain: {tenant}
```

### Cart

#### Get Cart
```http
GET /api/storefront/cart
Authorization: Bearer {token}
X-Tenant-Domain: {tenant}
```

#### Add to Cart
```http
POST /api/storefront/cart/add
Authorization: Bearer {token}
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "id": "prod_1",
  "name": "Product Name",
  "price": 99.99,
  "quantity": 1
}
```

### Orders & Checkout

#### Create Order
```http
POST /api/storefront/checkout
Authorization: Bearer {token}
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "items": [
    {
      "id": "p1",
      "name": "Prod",
      "price": 100,
      "quantity": 1
    }
  ],
  "totals": {
    "payable": 100
  },
  "paymentMethod": "cod",
  "address": {
    "line1": "123",
    "city": "BLR"
  }
}
```

#### Get Orders
```http
GET /api/storefront/orders
Authorization: Bearer {token}
X-Tenant-Domain: {tenant}
```

### Payments

#### Create Payment Order
```http
POST /api/storefront/payments/create-order
Authorization: Bearer {token}
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "amount": 1500,
  "currency": "INR"
}
```

#### Verify Payment
```http
POST /api/storefront/payments/verify
Authorization: Bearer {token}
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "razorpay_order_id": "order_abc",
  "razorpay_payment_id": "pay_def",
  "razorpay_signature": "sig_xyz"
}
```

## 🚚 Logistics APIs

### Rider Management

#### Rider Login
```http
POST /api/riders/login
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "phone": "+919999999998",
  "riderId": "rider_123"
}
```

#### Update Rider Status
```http
POST /api/riders/status
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "riderId": "rider_123",
  "status": "ONLINE"
}
```

Status Options: `OFFLINE`, `ONLINE`, `ASSIGNED`, `PICKED_UP`

#### Update Rider Location
```http
POST /api/riders/location
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "riderId": "rider_123",
  "lat": 12.9716,
  "lng": 77.5946
}
```

### Delivery Management

#### Assign Rider to Order
```http
POST /api/logistics/assign
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "orderId": "order_123",
  "storeId": "store_123"
}
```

#### Mark Order Picked Up
```http
POST /api/logistics/pickup
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "deliveryId": "delivery_123"
}
```

#### Mark Out for Delivery
```http
POST /api/logistics/out-for-delivery
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "deliveryId": "delivery_123"
}
```

#### Complete Delivery
```http
POST /api/logistics/complete
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "deliveryId": "delivery_123",
  "otp": "1234"
}
```

#### Record Delivery Attempt
```http
POST /api/logistics/attempt
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "deliveryId": "delivery_123",
  "status": "failed",
  "note": "Customer not available"
}
```

#### Get Delivery Attempts
```http
GET /api/logistics/attempts?deliveryId=delivery_123
X-Tenant-Domain: {tenant}
```

#### Get Delivery Details
```http
GET /api/logistics/delivery/{deliveryId}
X-Tenant-Domain: {tenant}
```

## 🌍 Zone Management APIs

### Zone Operations

#### Create Zone
```http
POST /api/zones
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "name": "MG Road Zone",
  "type": "radius",
  "centerLat": 12.9716,
  "centerLng": 77.5946,
  "radiusMeters": 3000
}
```

#### List Zones
```http
GET /api/zones?tenantId={tenant}
X-Tenant-Domain: {tenant}
```

#### Attach Store to Zone
```http
POST /api/zones/attach-store
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "storeId": "store_123",
  "zoneId": "zone_123"
}
```

#### Attach Rider to Zone
```http
POST /api/zones/attach-rider
Content-Type: application/json
X-Tenant-Domain: {tenant}

{
  "riderId": "rider_123",
  "zoneId": "zone_123"
}
```

## 🏪 Seller APIs

### Authentication

#### Seller Register
```http
POST /api/auth/seller/register
Content-Type: application/json

{
  "name": "Alice Seller",
  "email": "alice@example.com",
  "phone": "+919999999999",
  "password": "Pass123!",
  "businessType": "Grocery"
}
```

#### Seller Login
```http
POST /api/auth/seller/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "Pass123!"
}
```

### Store Management

#### Create Store
```http
POST /api/seller/stores
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "My Store",
  "city": "BLR"
}
```

#### List Stores
```http
GET /api/seller/stores
Authorization: Bearer {token}
```

### Product Management

#### Add Product
```http
POST /api/seller/stores/{storeId}/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Item",
  "price": 199,
  "description": "Desc",
  "category": "General"
}
```

### Order Management

#### Get Store Orders
```http
GET /api/seller/orders
Authorization: Bearer {token}
```

#### Update Order Status
```http
POST /api/seller/orders/{orderId}/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "READY"
}
```

### Inventory Management

#### Update Inventory
```http
POST /api/seller/inventory/update
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": "prod_123",
  "quantity": 50
}
```

## 📊 Platform APIs

### Platform Products

#### Create Platform Product
```http
POST /api/platform/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Platform Item",
  "price": 299,
  "description": "Desc",
  "category": "General"
}
```

## 🔧 System Status

### Health Check
```http
GET /actuator/health
```

### API Documentation
```http
GET /swagger-ui.html
```

## 📋 Common Response Formats

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error message"
  }
}
```

## 🔑 Authentication Headers

- **Storefront APIs**: `Authorization: Bearer {token}` + `X-Tenant-Domain: {tenant}`
- **Seller APIs**: `Authorization: Bearer {token}`
- **Rider APIs**: `X-Tenant-Domain: {tenant}` (some endpoints)
- **Platform APIs**: `Authorization: Bearer {token}`

## 🎯 Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `404` - Not Found
- `500` - Internal Server Error

---

**Note**: All APIs support multi-tenancy via `X-Tenant-Domain` header. Replace `{tenant}`, `{token}`, and IDs with actual values.