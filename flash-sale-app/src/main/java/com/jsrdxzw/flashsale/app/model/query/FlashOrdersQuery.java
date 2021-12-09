package com.jsrdxzw.flashsale.app.model.query;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:01 AM
 */
@Data
@Accessors(chain = true)
public class FlashOrdersQuery {
    private String keyword;
    private Integer pageSize;
    private Integer pageNumber;
    private Integer status;
}
