package com.oms.constant;

import java.util.List;

/**
 * Cache and sort field constants.
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCTS_LIST = "products_list";
    public static final String CACHE_KEY_ALL = "all";

    public static final String SORT_NAME = "name";
    public static final String SORT_PRICE = "price";
    public static final String SORT_CREATED_AT = "createdAt";
    public static final List<String> ALLOWED_SORT_FIELDS = List.of(SORT_NAME, SORT_PRICE, SORT_CREATED_AT);
}
