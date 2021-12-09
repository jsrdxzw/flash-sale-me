package com.jsrdxzw.flashsale.controller.rules;

import com.jsrdxzw.flashsale.controller.rules.config.Rule;
import com.jsrdxzw.flashsale.security.SecurityRuleChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class AccountRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {

    @Override
    public boolean run(HttpServletRequest request, HttpServletResponse response) {
        Rule rule = securityRulesConfiguration.getAccountRule();
        if (!rule.isEnable()) {
            return true;
        }
        try {
            // 可在此处调用大数据接口或黑名单接口验证账号
            return true;
        } catch (Exception e) {
            log.error("accountLimit|IP限制异常|{}", e);
            return false;
        }
    }

    @Override
    public String getName() {
        return "账号安全服务";
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
