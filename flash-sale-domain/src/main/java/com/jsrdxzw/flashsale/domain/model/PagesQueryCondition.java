package com.jsrdxzw.flashsale.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/1 5:25 下午
 */
@Data
@Accessors(chain = true)
public class PagesQueryCondition {
    public static final int MAX_PAGE_SIZE_LIMIT = 100;
    private String keyword;
    private Integer pageSize = 10;
    private Integer pageNumber = 1;
    private Integer offset;
    private Integer status;
    private Long activityId;
    // 是否已经预热的标识
    private Integer stockWarmUp;

    public PagesQueryCondition buildParams() {
        if (this.pageSize == null) {
            this.pageSize = 10;
        }
        if (this.pageSize > MAX_PAGE_SIZE_LIMIT) {
            this.pageSize = 100;
        }
        if (this.pageNumber == null || this.pageNumber == 0) {
            this.pageNumber = 1;
        }
        this.offset = (pageNumber - 1) * pageSize;
        return this;
    }
}
