package com.jsrdxzw.flashsale.app.model.query;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

@Data
@Accessors(chain = true)
public class FlashActivitiesQuery {
    private String keyword;
    private Integer pageSize;
    private Integer pageNumber;
    private Integer status;
    private Long version;

    public boolean isFirstPureQuery() {
        return !StringUtils.hasText(keyword) && pageNumber != null && pageNumber == 1;
    }
}
