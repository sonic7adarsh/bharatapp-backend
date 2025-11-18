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
        // For now, return default factory; hook for multi-tenant specialization later
        return defaultFactory;
    }

    public SellerFactory getSellerFactory(String tenantDomain) {
        // For now, return default seller factory; hook for multi-tenant specialization later
        return sellerFactory;
    }
}