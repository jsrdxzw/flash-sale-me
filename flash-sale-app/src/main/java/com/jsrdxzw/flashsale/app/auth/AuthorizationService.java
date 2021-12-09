package com.jsrdxzw.flashsale.app.auth;

import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import com.jsrdxzw.flashsale.app.auth.model.ResourceEnum;

/**
 * @author xuzhiwei
 * @date 2021/12/2 10:26 AM
 */
public interface AuthorizationService {
    AuthResult auth(String encryptedToken, ResourceEnum resourceEnum);

    AuthResult auth(String encryptedToken);

    AuthResult auth(Long userId);

    AuthResult auth(Long userId, ResourceEnum resourceEnum);
}
