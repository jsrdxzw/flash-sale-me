package com.jsrdxzw.flashsale.controller.rules;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.controller.exception.ExceptionResponse;
import com.jsrdxzw.flashsale.controller.rules.config.Rule;
import com.jsrdxzw.flashsale.security.SecurityRuleChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.LIMIT_BLOCK;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 *
 * @author xuzhiwei
 * @date 2021/12/13 5:42 PM
 */
@Slf4j
@Service
public class ResourcePathRuleChainService extends SecurityRuleChainServiceBase implements SecurityRuleChainService {
    @Override
    public String getName() {
        return "资源安全服务";
    }

    @Override
    public boolean run(HttpServletRequest request, HttpServletResponse response) {
        Rule rule = securityRulesConfiguration.getPathRule(request.getServletPath());
        if (!rule.isEnable()) {
            return true;
        }
        try {
            Long userId = getUserId(request);
            if (userId != null) {
                String userResourcePath = link(userId, request.getServletPath());
                boolean result = slidingWindowLimitService.pass(userResourcePath, rule.getWindowPeriod(), rule.getWindowSize());
                if (!result) {
                    ExceptionResponse exceptionResponse = new ExceptionResponse()
                            .setErrorCode(LIMIT_BLOCK.getCode())
                            .setErrorMessage(LIMIT_BLOCK.getDesc());
                    response.setContentType(MediaType.APPLICATION_JSON.getType());
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(JSON.toJSONString(exceptionResponse));
                    response.getWriter().close();
                    log.info("resourcePathLimit|资源路径限制|{}", userResourcePath);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("resourcePathLimit|资源路径限制异常|{}", e);
            return false;
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
