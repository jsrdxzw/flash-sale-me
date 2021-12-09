package com.jsrdxzw.flashsale.controller.config;

import com.jsrdxzw.flashsale.security.SecurityRuleChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xuzhiwei
 * @date 2021/12/2 4:08 PM
 */
@Configuration
public class SecurityRulesInterceptor implements HandlerInterceptor {

    @Autowired
    private List<SecurityRuleChainService> securityRuleChainServices;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        for (SecurityRuleChainService securityRuleChainService : getSecurityRuleChainServices()) {
            if (!securityRuleChainService.run(request, response)) {
                return false;
            }
        }
        return true;
    }

    private List<SecurityRuleChainService> getSecurityRuleChainServices() {
        if (CollectionUtils.isEmpty(securityRuleChainServices)) {
            return new ArrayList<>();
        }
        return securityRuleChainServices
                .stream()
                .sorted(Comparator.comparing(SecurityRuleChainService::getOrder))
                .collect(Collectors.toList());
    }
}
