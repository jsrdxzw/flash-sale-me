package com.jsrdxzw.flashsale.app.security;

import org.springframework.stereotype.Service;

/**
 * @author xuzhiwei
 * @date 2021/12/5 5:24 PM
 */
@Service
public class DefaultSecurityService implements SecurityService {

    @Override
    public boolean inspectRisksByPolicy(Long userId) {
        return true;
    }
}
