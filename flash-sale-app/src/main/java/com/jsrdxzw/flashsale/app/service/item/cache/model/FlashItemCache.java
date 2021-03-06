package com.jsrdxzw.flashsale.app.service.item.cache.model;

import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlashItemCache {
    protected boolean exist;
    private FlashItem flashItem;
    private Long version;
    private boolean later;

    public FlashItemCache with(FlashItem flashActivity) {
        this.exist = true;
        this.flashItem = flashActivity;
        return this;
    }

    public FlashItemCache withVersion(Long version) {
        this.version = version;
        return this;
    }

    public FlashItemCache tryLater() {
        this.later = true;
        return this;
    }

    public FlashItemCache notExist() {
        this.exist = false;
        return this;
    }
}
