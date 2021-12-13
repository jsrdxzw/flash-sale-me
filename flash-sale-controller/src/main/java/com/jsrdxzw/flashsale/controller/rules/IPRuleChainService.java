package com.jsrdxzw.flashsale.controller.rules;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.controller.exception.ExceptionResponse;
import com.jsrdxzw.flashsale.controller.rules.config.Rule;
import com.jsrdxzw.flashsale.security.SecurityRuleChainService;
import com.jsrdxzw.flashsale.util.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.LIMIT_BLOCK;

/**
 * @author xuzhiwei
 * @date 2021/12/13 5:26 PM
 */
@Slf4j
@Component
public class IPRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {

    @Override
    public String getName() {
        return "IP防护服务";
    }

    /**
     * 对ip的访问限制
     *
     * @param request  请求
     * @param response 响应
     * @return
     */
    @Override
    public boolean run(HttpServletRequest request, HttpServletResponse response) {
        Rule rule = securityRulesConfiguration.getPathRule(request.getServletPath());
        if (!rule.isEnable()) {
            return true;
        }
        try {
            String clientIp = IPUtil.getIpAddr(request);
            boolean result = slidingWindowLimitService.pass(clientIp, rule.getWindowPeriod(), rule.getWindowSize());
            if (!result) {
                ExceptionResponse exceptionResponse = new ExceptionResponse()
                        .setErrorCode(LIMIT_BLOCK.getCode())
                        .setErrorMessage(LIMIT_BLOCK.getDesc());
                response.setContentType(MediaType.APPLICATION_JSON.getType());
                response.setCharacterEncoding("utf-8");
                response.getWriter().write(JSON.toJSONString(exceptionResponse));
                response.getWriter().close();
                log.info("ipLimit|IP被限制|{}", clientIp);
                return false;
            }
        } catch (Exception e) {
            log.error("ipLimit|IP限制异常|{}", e);
            return false;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
