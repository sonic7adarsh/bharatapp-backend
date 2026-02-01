# Seller API Contract (v1)

Base URL: `/api`

## Authentication

### 1. Register or Upgrade to Seller
Registers a new seller or upgrades an existing user to SELLER role.

- **Endpoint**: `POST /auth/seller/register`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "name": "Seller Name",
    "email": "seller@example.com", // Optional if phone provided
    "phone": "9876543210",         // Optional if email provided
    "password": "securepassword"   // Optional
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "jwt_token_here",
    "user": {
      "id": "user_uuid",
      "name": "Seller Name",
      "email": "seller@example.com",
      "role": "SELLER"
    }
  }
  ```

### 2. Login Seller
Login with email and password.

- **Endpoint**: `POST /auth/seller/login`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "email": "seller@example.com",
    "password": "securepassword"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "jwt_token_here",
    "user": {
      "id": "user_uuid",
      "email": "seller@example.com",
      "role": "SELLER"
    }
  }
  ```

### 3. Send OTP (Generic)
Send OTP to a phone number.

- **Endpoint**: `POST /auth/send-otp`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "phone": "9876543210"
  }
  ```
- **Response**: `200 OK`

### 4. Verify OTP (Generic)
Verify OTP and login/register user (default role).

- **Endpoint**: `POST /auth/verify-otp`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "phone": "9876543210",
    "otp": "123456"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "token": "jwt_token_here",
    "user": { ... }
  }
  ```

---

## Store Management
**Requires Header**: `Authorization: Bearer <token>`

### 1. List Stores
List all stores owned by the authenticated seller.

- **Endpoint**: `GET /seller/stores`
- **Query Params**:
  - `search` (optional): Filter by name
  - `page` (optional): Page number (default 1)
  - `limit` (optional): Items per page
  - `status` (optional): Filter by status (open/closed)
- **Response**: `200 OK`
  ```json
  {
    "stores": [
      {
        "id": "store_uuid",
        "name": "My Store",
        "ownerId": "user_uuid",
        "category": "Grocery",
        "status": "open",
        "orderingDisabled": false,
        "closedReason": null,
        "closedUntil": null
      }
    ]
  }
  ```

### 2. Create Store
Create a new store. Can be JSON or Multipart (for image).

- **Endpoint**: `POST /seller/stores`
- **Content-Type**: `application/json` OR `multipart/form-data`
- **Body (JSON)**:
  ```json
  {
    "name": "My New Store",
    "city": "Mumbai",
    "area": "Andheri West",
    "category": "Grocery"
  }
  ```
- **Body (Multipart)**:
  - `name`: String
  - `city`: String
  - `area`: String
  - `category`: String
  - `image`: File (optional)
- **Response**: `200 OK`
  ```json
  {
    "id": "store_uuid",
    "name": "My New Store",
    "status": "open"
  }
  ```

### 3. Get Store Details
- **Endpoint**: `GET /seller/stores/{storeId}`
- **Response**: `200 OK`
  ```json
  {
    "id": "store_uuid",
    "name": "My Store",
    "area": "Andheri West",
    "category": "Grocery",
    "status": "open",
    "orderingDisabled": false,
    "logo": "filename.jpg",
    "updatedAt": "2023-10-27T10:00:00Z"
  }
  ```

### 4. Update Store
Update store details.

- **Endpoint**: `PATCH /seller/stores/{storeId}`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "name": "Updated Name",
    "status": "closed", // open|closed
    "orderingDisabled": true,
    "closedReason": "Renovation",
    "closedUntil": "2023-11-01T00:00:00Z"
  }
  ```
- **Response**: `200 OK` (Returns updated Store object)

---

## Product Management
**Requires Header**: `Authorization: Bearer <token>`

### 1. List Products
- **Endpoint**: `GET /seller/stores/{storeId}/products`
- **Query Params**:
  - `search` (optional)
  - `category` (optional)
  - `active` (optional): true/false
  - `page`, `limit` (optional)
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "product_uuid",
      "name": "Apple",
      "price": 100.0,
      "stock": 50,
      "active": true,
      "category": "Fruits",
      "image": "apple.jpg"
    }
  ]
  ```

### 2. Create Product
- **Endpoint**: `POST /seller/stores/{storeId}/products`
- **Content-Type**: `application/json` OR `multipart/form-data`
- **Body (JSON)**:
  ```json
  {
    "name": "Apple",
    "price": 120,
    "description": "Fresh apples",
    "category": "Fruits",
    "sku": "APP-001",
    "stock": 100,
    "active": true,
    "image": "url_or_filename"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "product": { ... }
  }
  ```

### 3. Update Product
- **Endpoint**: `PATCH /seller/products/{productId}`
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "price": 130,
    "active": false
  }
  ```
- **Response**: `200 OK` (Returns updated Product)

### 4. Upload Product Image
- **Endpoint**: `POST /seller/products/{productId}/images`
- **Content-Type**: `multipart/form-data`
- **Body**:
  - `file`: File
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "image": "filename.jpg"
  }
  ```

### 5. Adjust Inventory
- **Endpoint**: `PATCH /seller/products/{productId}/inventory`
- **Body**:
  ```json
  {
    "stockDelta": -5, // Decrease by 5
    "stockSet": 100,  // OR set to 100 (optional)
    "price": 150.0    // Optional price update
  }
  ```
- **Response**: `200 OK` (Returns updated Product)

### 6. Bulk Upload
- **Endpoint**: `POST /seller/products/bulk-upload`
- **Content-Type**: `multipart/form-data`
- **Body**:
  - `file`: CSV/Excel file
  - `mode`: "upsert" (default)
  - `dryRun`: true/false (optional)
- **Response**: `200 OK`
  ```json
  {
    "jobId": "job_uuid",
    "status": "PENDING"
  }
  ```
- **Check Status**: `GET /seller/products/bulk-upload/{jobId}`
- **Get Errors**: `GET /seller/products/bulk-upload/{jobId}/errors`

---

## Order Management
**Requires Header**: `Authorization: Bearer <token>`

### 1. List Orders
- **Endpoint**: `GET /seller/orders`
- **Query Params**:
  - `storeId` (optional)
  - `status` (optional): placed, accepted, preparing, ready, shipped, delivered, cancelled
  - `from`, `to` (optional): Date range
  - `page`, `limit` (optional)
- **Response**: `200 OK` (List of Order objects)

### 2. Get Order
- **Endpoint**: `GET /seller/orders/{orderId}`
- **Response**: `200 OK` (Order object)

### 3. Order Actions
All actions are `POST` requests to `/seller/orders/{orderId}/{action}` unless specified.

| Action | Endpoint | Method | Body (JSON) | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Update Status** | `/status` | `PATCH` | `{"status": "...", "notes": "..."}` | Manually update status |
| **Accept** | `/accept` | `POST` | - | Accept a placed order |
| **Reject** | `/reject` | `POST` | `{"reason": "..."}` | Reject a placed order |
| **Prepare** | `/prepare` | `POST` | - | Mark order as preparing |
| **Ready** | `/ready` | `POST` | - | Mark order as ready for pickup |
| **Ship** | `/ship` | `POST` | - | Mark order as shipped |
| **Deliver** | `/deliver` | `POST` | - | Mark order as delivered |
| **Cancel** | `/cancel` | `POST` | `{"reason": "..."}` | Cancel an order |
| **Refund** | `/refunds` | `POST` | `{"amount": 100, "reason": "..."}` | Process refund |

### 4. Update Item Status
- **Endpoint**: `PATCH /seller/orders/{orderId}/items/{itemId}/status`
- **Body**:
  ```json
  {
    "status": "unavailable"
  }
  ```
- **Response**: `200 OK`
