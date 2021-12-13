package com.jsrdxzw.flashsale.controller.rules;

import com.jsrdxzw.flashsale.app.auth.AuthorizationService;
import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import com.jsrdxzw.flashsale.controller.rules.config.SecurityRulesConfiguration;
import com.jsrdxzw.flashsale.security.SlidingWindowLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
public abstract class SecurityRuleChainServiceBase {

    @Resource
    protected AuthorizationService authorizationService;
    @Resource
    protected SecurityRulesConfiguration securityRulesConfiguration;
    @Resource
    protected SlidingWindowLimitService slidingWindowLimitService;

    @PostConstruct
    public void init() {
        log.info("securityService|{}已初始化", getName());
    }

    protected Long getUserId(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (!StringUtils.hasText(token)) {
            return null;
        }
        AuthResult authResult = authorizationService.auth(token);
        if (authResult.isSuccess()) {
            return authResult.getUserId();
        }
        return null;
    }

    public abstract String getName();
}
