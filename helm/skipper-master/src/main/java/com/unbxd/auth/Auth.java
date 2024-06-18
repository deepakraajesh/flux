package com.unbxd.auth;

import com.unbxd.auth.exception.AuthSystemException;
import com.unbxd.auth.exception.UnAuthorizedException;
import com.unbxd.auth.model.User;
import com.unbxd.auth.model.UserCred;
import com.unbxd.auth.model.UserToken;

import java.util.Map;

public interface Auth {

    String AUTH_COOKIE_NAME = "_un_sso_uid";

    void register(User user) throws AuthSystemException;

    UserToken login(UserCred user) throws UnAuthorizedException, AuthSystemException;

    Map<String, Object> verify(String cookieKey) throws UnAuthorizedException, AuthSystemException;

    String logout(String authToken) throws UnAuthorizedException, AuthSystemException;
}
