package com.jsrdxzw.flashsale.app.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/5 9:58 AM
 */
@AllArgsConstructor
@Getter
public enum OrderTaskStatus {
    SUBMITTED(0, "初始提交"),
    SUCCESS(1, "下单成功"),
    FAILED(-1, "下单失败");

    private final Integer status;
    private final String desc;

    public static OrderTaskStatus findBy(Integer status) {
        if (status == null) {
            return null;
        }
        for (OrderTaskStatus taskStatus : OrderTaskStatus.values()) {
            if (taskStatus.getStatus().equals(status)) {
                return taskStatus;
            }
        }
        return null;
    }
}
