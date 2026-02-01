package com.bharatshop.factory.ops;

import com.bharatshop.domain.Store;
import java.util.List;

public interface StoreOps {
    List<Store> list(String search, String category);
    Store get(String id);
    Store add(Store s);
}