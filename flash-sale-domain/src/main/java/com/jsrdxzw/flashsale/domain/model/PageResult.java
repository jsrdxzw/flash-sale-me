package com.jsrdxzw.flashsale.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/1 5:22 下午
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
public class PageResult<T> {
    private List<T> data;
    private int total;

    public static <T> PageResult<T> with(List<T> data, int total) {
        return new PageResult<>(data, total);
    }
}
