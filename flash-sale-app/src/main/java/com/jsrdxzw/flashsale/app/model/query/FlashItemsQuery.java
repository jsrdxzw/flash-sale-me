package com.jsrdxzw.flashsale.app.model.query;

import com.jsrdxzw.flashsale.domain.model.enums.FlashItemStatus;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:26 PM
 */
@Data
@Accessors(chain = true)
public class FlashItemsQuery {
    private String keyword;
    private Integer pageSize;
    private Integer pageNumber;
    private Integer status;
    private Long version;
    private Long activityId;

    public boolean isOnlineFirstPageQuery() {
        return !StringUtils.hasText(keyword) && pageNumber != null && pageNumber == 1 && FlashItemStatus.isOnline(status);
    }
}
