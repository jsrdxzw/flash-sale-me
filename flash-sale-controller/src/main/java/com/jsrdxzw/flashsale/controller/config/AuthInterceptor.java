package com.jsrdxzw.flashsale.controller.config;

import com.jsrdxzw.flashsale.app.auth.AuthorizationService;
import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xuzhiwei
 * @date 2021/12/2 4:01 PM
 */
@Configuration
public class AuthInterceptor implements HandlerInterceptor {
    private static final String USER_ID = "userId";

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object userId = request.getAttribute(USER_ID);
        if (userId != null) {
            return true;
        }
        String token = request.getParameter("token");
        AuthResult authResult = authorizationService.auth(token);
        if (authResult.isSuccess()) {
            HttpServletRequestWrapper authRequestWrapper = new HttpServletRequestWrapper(request);
            authRequestWrapper.setAttribute(USER_ID, authResult.getUserId());
            return true;
        }
        return false;
    }
}
