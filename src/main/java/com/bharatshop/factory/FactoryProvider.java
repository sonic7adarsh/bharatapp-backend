package com.bharatshop.factory;

import org.springframework.stereotype.Component;

@Component
public class FactoryProvider {
    private final StorefrontFactory defaultFactory;
    private final SellerFactory sellerFactory;

    public FactoryProvider(StorefrontFactory defaultFactory, SellerFactory sellerFactory) {
        this.defaultFactory = defaultFactory;
        this.sellerFactory = sellerFactory;
    }

    public StorefrontFactory getFactory(String tenantDomain) {
        // tenantDomain ignored in local-first platform
        return defaultFactory;
    }

    public SellerFactory getSellerFactory(String tenantDomain) {
        // tenantDomain ignored in local-first platform
        return sellerFactory;
    }

    public StorefrontFactory getFactory() {
        return getFactory(null);
    }

    public SellerFactory getSellerFactory() {
        return getSellerFactory(null);
    }
}