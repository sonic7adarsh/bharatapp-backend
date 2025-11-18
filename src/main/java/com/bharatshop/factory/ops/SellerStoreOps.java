package com.bharatshop.factory.ops;

import com.bharatshop.domain.Store;
import java.util.List;
import java.util.Map;

public interface SellerStoreOps {
    List<Store> list(String search, Integer page, Integer limit);
    Store get(String id);
    Store create(Store store);
    Store updatePartial(String id, Map<String, Object> changes);
}