package com.bharatshop.factory;

import com.bharatshop.factory.ops.SellerOrderOps;
import com.bharatshop.factory.ops.SellerProductOps;
import com.bharatshop.factory.ops.SellerStoreOps;

public interface SellerFactory {
    SellerProductOps products();
    SellerOrderOps orders();
    SellerStoreOps stores();
}