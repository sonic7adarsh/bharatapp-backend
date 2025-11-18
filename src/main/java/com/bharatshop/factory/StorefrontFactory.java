package com.bharatshop.factory;

import com.bharatshop.factory.ops.*;

public interface StorefrontFactory {
    ProductOps products();
    StoreOps stores();
    OrderOps orders();
    CartOps carts();
    PaymentOps payments();
    AvailabilityOps availability();
}