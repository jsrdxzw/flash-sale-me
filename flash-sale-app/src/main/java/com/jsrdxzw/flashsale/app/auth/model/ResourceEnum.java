package com.jsrdxzw.flashsale.app.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/2 10:34 AM
 */
@AllArgsConstructor
@Getter
public enum ResourceEnum {
    FLASH_ITEM_CREATE("FLASH_ITEM_CREATE", "创建秒杀品"),
    FLASH_ITEM_OFFLINE("FLASH_ITEM_OFFLINE", "下线秒杀品"),
    FLASH_ITEMS_GET("FLASH_ITEMS_GET", "秒杀品集合获取"),
    FLASH_ITEM_GET("FLASH_ITEM_GET", "获取指定秒杀品"),
    STOCK_BUCKETS_ARRANGEMENT("STOCK_BUCKETS_ARRANGEMENT", "编排库存分桶"),
    STOCK_BUCKETS_SUMMERY_QUERY("STOCK_BUCKETS_SUMMERY_QUERY", "获取库存分桶");

    private final String code;
    private final String desc;
}
