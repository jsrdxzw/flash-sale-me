package com.jsrdxzw.flashsale.app.auth;

import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import com.jsrdxzw.flashsale.app.auth.model.ResourceEnum;
import com.jsrdxzw.flashsale.app.auth.model.Token;
import com.jsrdxzw.flashsale.controller.AuthException;
import com.jsrdxzw.flashsale.util.Base64Util;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import org.springframework.stereotype.Service;

import static com.jsrdxzw.flashsale.controller.ErrorCode.INVALID_TOKEN;
import static com.jsrdxzw.flashsale.controller.ErrorCode.UNAUTHORIZED_ACCESS;

/**
 * @author xuzhiwei
 * @date 2021/12/2 11:50 AM
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    /**
     * 对token的解析需要结合登录时的令牌加密逻辑；
     */
    @Override
    public AuthResult auth(String encryptedToken, ResourceEnum resourceId) {
        Token token = parseToken(encryptedToken);
        if (token == null) {
            throw new AuthException(INVALID_TOKEN);
        }
        if (!hasAccessAuthorized(token, resourceId)) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        return new AuthResult().setUserId(token.getUserId()).pass();
    }

    @Override
    public AuthResult auth(String encryptedToken) {
        Token token = parseToken(encryptedToken);
        if (token == null) {
            throw new AuthException(INVALID_TOKEN);
        }
        return new AuthResult()
                .setUserId(token.getUserId())
                .pass();
    }

    @Override
    public AuthResult auth(Long userId) {
        return new AuthResult()
                .setUserId(userId)
                .pass();
    }

    @Override
    public AuthResult auth(Long userId, ResourceEnum resourceId) {
        return new AuthResult()
                .setUserId(userId)
                .pass();
    }

    /**
     * 解析令牌
     * Notice：这里使用简单的编码，正式开发中应遵循严格的加解密规则
     *
     * @param encryptedToken 已加密的用户令牌
     * @return 解析后的用户令牌信息
     */
    private Token parseToken(String encryptedToken) {
        try {
            String parsedToken = Base64Util.decode(encryptedToken);
            return JSONUtil.parseObject(parsedToken, Token.class);
        } catch (Exception e) {
            throw new AuthException(INVALID_TOKEN);
        }
    }

    /**
     * 是否已经获得授权访问目标资源
     * Notice：这里直接返回true，正式开发中应该结合权限配置执行严格校验
     *
     * @param token      解析后的用户令牌信息
     * @param resourceId 资源标识
     * @return 授权鉴定结果
     */
    private boolean hasAccessAuthorized(Token token, ResourceEnum resourceId) {
        return true;
    }
}
