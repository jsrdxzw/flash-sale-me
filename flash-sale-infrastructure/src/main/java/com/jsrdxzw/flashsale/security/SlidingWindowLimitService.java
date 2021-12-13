package com.jsrdxzw.flashsale.security;

/**
 * @author xuzhiwei
 * @date 2021/12/13 5:37 PM
 */
public interface SlidingWindowLimitService {
    /**
     * @param userActionKey 用户及行为标识
     * @param period        限流周期，单位毫秒
     * @param size          滑动窗口大小
     * @return 是否通过
     */
    boolean pass(String userActionKey, int period, int size);
}
