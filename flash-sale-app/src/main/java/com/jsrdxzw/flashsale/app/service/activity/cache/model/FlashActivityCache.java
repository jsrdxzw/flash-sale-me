package com.jsrdxzw.flashsale.app.service.activity.cache.model;

import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/3 11:16 AM
 */
@Data
@Accessors(chain = true)
public class FlashActivityCache {
    /**
     * 防止缓存穿透
     * exist表示当前数据是否存在，而不是缓存是否存在。
     * 比如ID为1的秒杀品不存在，但是缓存会存在，并且exist的值为false，
     * 应用层拿到缓存结果并且exist=false时，就知道这个数据不存在，可以返回NOT FOUND之类的错误
     */
    protected boolean exist;
    private FlashActivity flashActivity;
    private Long version;
    private boolean later;

    public FlashActivityCache with(FlashActivity flashActivity) {
        this.exist = true;
        this.flashActivity = flashActivity;
        return this;
    }

    public FlashActivityCache withVersion(Long version) {
        this.version = version;
        return this;
    }

    public FlashActivityCache tryLater() {
        this.later = true;
        return this;
    }

    public FlashActivityCache notExist() {
        this.exist = false;
        return this;
    }
}
