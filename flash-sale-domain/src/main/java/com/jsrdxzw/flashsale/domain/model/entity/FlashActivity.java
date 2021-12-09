package com.jsrdxzw.flashsale.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jsrdxzw.flashsale.domain.model.enums.FlashActivityStatus;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xuzhiwei
 * @date 2021/12/1 5:28 下午
 */
@Data
public class FlashActivity implements Serializable {
    /**
     * 活动ID
     */
    private Long Id;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 活动开始时间
     */
    private Date startTime;
    /**
     * 活动结束时间
     */
    private Date endTime;
    /**
     * 活动状态
     */
    private Integer status;
    /**
     * 活动描述
     */
    private String activityDesc;

    @Override
    public String toString() {
        return JSONUtil.toJSONString(this);
    }

    public boolean validateParamsForCreateOrUpdate() {
        return StringUtils.hasText(activityName) && startTime != null
                && endTime != null && !endTime.before(startTime) && !endTime.before(new Date());
    }

    @JsonIgnore
    public boolean isOnline() {
        return FlashActivityStatus.isOnline(status);
    }

    @JsonIgnore
    public boolean isInProgress() {
        Date now = new Date();
        return startTime.before(now) && endTime.after(now);
    }
}
