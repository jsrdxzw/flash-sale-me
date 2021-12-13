package com.jsrdxzw.flashsale.security;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xuzhiwei
 * @date 2021/12/13 5:25 PM
 */
@Service
public class DefaultSecurityRuleChainService implements SecurityRuleChainService {
    @Override
    public boolean run(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
