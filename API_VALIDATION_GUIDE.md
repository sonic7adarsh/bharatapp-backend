# Customer Experience Guards - API Validation Guide

This guide documents the new pre-checkout validation endpoints and enhanced error handling implemented to improve customer experience.

## Overview

The system now provides early validation capabilities that allow customers to check store availability and cart validity before attempting checkout. This prevents frustrating checkout failures and provides clear, actionable error messages.

## New Validation Endpoints

### 1. Store Validation Endpoint

**Endpoint:** `POST /api/stores/{storeId}/validate`

**Purpose:** Validates if a store is available for ordering and delivers to the customer's location.

**Request Parameters:**
- `storeId` (path, required): The store ID to validate
- `lat` (query, optional): Customer's latitude for zone serviceability check
- `lng` (query, optional): Customer's longitude for zone serviceability check

**Response Examples:**

**Success Response (Store Available):**
```json
{
  "available": true,
  "message": "Store is available for ordering"
}
```

**Store Not Found:**
```json
{
  "code": "STORE_NOT_FOUND",
  "message": "Store not found",
  "storeId": "store123"
}
```

**Store Closed:**
```json
{
  "available": false,
  "reason": {
    "code": "STORE_CLOSED",
    "message": "Store is currently closed",
    "storeId": "store123"
  }
}
```

**Store Temporarily Closed:**
```json
{
  "available": false,
  "reason": {
    "code": "STORE_TEMPORARILY_CLOSED",
    "message": "Store is temporarily closed until 18:30",
    "storeId": "store123",
    "closedUntil": "2024-01-15T18:30:00Z"
  }
}
```

**Store Ordering Disabled:**
```json
{
  "available": false,
  "reason": {
    "code": "STORE_ORDERING_DISABLED",
    "message": "Store is not accepting orders at the moment",
    "storeId": "store123"
  }
}
```

**Out of Service Area:**
```json
{
  "available": false,
  "reason": {
    "code": "STORE_OUT_OF_SERVICE_AREA",
    "message": "Store does not deliver to your location",
    "storeId": "store123",
    "lat": 12.9716,
    "lng": 77.5946
  }
}
```

### 2. Cart Validation Endpoint

**Endpoint:** `POST /api/cart/validate`

**Purpose:** Validates if all items in the cart are available for purchase.

**Request Parameters:**
- `storeId` (query, required): The store ID for inventory validation
- `guestId` (header, optional): Guest user ID (for unauthenticated users)
- `X-User-ID` (header, optional): Authenticated user ID

**Response Examples:**

**Success Response (Cart Valid):**
```json
{
  "valid": true,
  "message": "Cart is valid for checkout",
  "items": 3
}
```

**Empty Cart:**
```json
{
  "valid": false,
  "reason": {
    "code": "EMPTY_CART",
    "message": "Your cart is empty"
  }
}
```

**Insufficient Inventory:**
```json
{
  "valid": false,
  "reason": {
    "code": "INSUFFICIENT_INVENTORY",
    "message": "Some items are no longer available",
    "items": [
      {
        "productId": "prod123",
        "name": "Organic Tomatoes",
        "requested": 5,
        "available": 2
      },
      {
        "productId": "prod456",
        "name": "Fresh Milk",
        "requested": 3,
        "available": 0
      }
    ]
  }
}
```

## Enhanced Checkout Error Handling

The checkout endpoint now provides more detailed error information:

### Store Availability Errors

**Store Not Found:**
```json
{
  "status": 400,
  "code": "STORE_NOT_FOUND",
  "message": "Store not found"
}
```

**Store Closed:**
```json
{
  "status": 400,
  "code": "STORE_CLOSED",
  "message": "Store is currently closed"
}
```

**Store Temporarily Closed:**
```json
{
  "status": 400,
  "code": "STORE_TEMPORARILY_CLOSED",
  "message": "Store is temporarily closed until 18:30"
}
```

**Store Ordering Disabled:**
```json
{
  "status": 400,
  "code": "STORE_ORDERING_DISABLED",
  "message": "Store is not accepting orders at the moment"
}
```

**Out of Service Area:**
```json
{
  "status": 400,
  "code": "STORE_OUT_OF_SERVICE_AREA",
  "message": "Store does not deliver to your location"
}
```

### Inventory Errors

**Insufficient Inventory:**
```json
{
  "status": 400,
  "code": "INSUFFICIENT_INVENTORY",
  "message": "Some items are no longer available",
  "details": {
    "unavailableProducts": [
      {
        "productId": "prod123",
        "productName": "Organic Tomatoes",
        "requested": 5,
        "available": 2
      }
    ],
    "suggestion": "Please update your cart and try again"
  }
}
```

## Error Code Reference

| Error Code | Description | User-Friendly Message |
|------------|-------------|----------------------|
| `STORE_NOT_FOUND` | Store does not exist | Store not found |
| `STORE_CLOSED` | Store status is "closed" | Store is currently closed |
| `STORE_TEMPORARILY_CLOSED` | Store has closedUntil timestamp in future | Store is temporarily closed until {time} |
| `STORE_ORDERING_DISABLED` | Store orderingDisabled is true | Store is not accepting orders at the moment |
| `STORE_OUT_OF_SERVICE_AREA` | Customer location outside store zones | Store does not deliver to your location |
| `INSUFFICIENT_INVENTORY` | Product available quantity < requested | Some items are no longer available |
| `EMPTY_CART` | Cart has no items | Your cart is empty |

## Implementation Details

### StoreAvailabilityPolicy

The `StoreAvailabilityPolicy` class now provides comprehensive validation:

1. **Basic Store Availability**: Checks `orderingDisabled`, `status`, and `closedUntil` fields
2. **Inventory Availability**: Validates product availability without reserving inventory
3. **Zone Serviceability**: Checks if customer location is within store delivery zones

### Enhanced Services

- **InventoryService**: Added `canReserve()` and `getAvailable()` methods for non-blocking inventory checks
- **StoreDiscoveryController**: New `/stores/{storeId}/validate` endpoint
- **StorefrontCartController**: New `/cart/validate` endpoint
- **CheckoutService**: Enhanced with comprehensive validation before order placement

## Usage Recommendations

1. **Pre-Checkout Validation Flow**:
   - Call store validation when customer selects a store
   - Call cart validation when customer views cart or before checkout
   - Display clear error messages with suggested actions

2. **Error Handling**:
   - Use error codes to determine appropriate UI responses
   - Provide specific guidance for each error type
   - Consider automatic cart updates for inventory issues

3. **Performance**:
   - Validation endpoints are lightweight and cacheable
   - Inventory checks use non-locking queries
   - Zone checks are optimized for multiple zones per store

## Testing

Use the provided Postman collections to test the new endpoints:
- Store validation: `POST /api/stores/{storeId}/validate?lat=12.9716&lng=77.5946`
- Cart validation: `POST /api/cart/validate?storeId=store123`

The enhanced error handling ensures customers receive clear, actionable feedback before attempting checkout, significantly improving the user experience.