package com.unbxd.auth;

import com.unbxd.auth.exception.AuthSystemException;
import com.unbxd.auth.exception.UnAuthorizedException;
import com.unbxd.auth.model.User;
import com.unbxd.auth.model.UserCred;
import com.unbxd.auth.model.UserToken;
import lombok.extern.log4j.Log4j2;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.ThreadContext;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.unbxd.skipper.SkipperLauncher.HEADER_PREFIX_IN_THREAD_CONTEXT;

@Log4j2
public class SSOAuth implements Auth {

    private interface SSOService {
        @POST("users")
        Call<Map<String, Object>> createUser(@Body User user);

	@POST("auth/users/authenticate_token")
        Call<Map<String, Object>> authenticate(@Header("token") String authToken,
                                               @HeaderMap Map<String, String> headers);

        @POST("auth/users/service_login")
        Call<UserToken> login(@Body UserCred user);

        @DELETE("auth/users/logout")
        Call<UserToken> logout(@Header("Cookie") String authToken);
    }

    private SSOService ssoService;

    public SSOAuth(String ssoServiceBaseURL) {
        OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

         ssoService = new Retrofit.Builder()
                .baseUrl(ssoServiceBaseURL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build().create(SSOService.class);
    }

    @Override
    public void register(User user) throws AuthSystemException {
        try {
            if(user == null || user.getEmail() == null || user.getPassword() == null || user.getRegions() == null) {
                throw new IllegalArgumentException("Invalid request");
            }
            Response<Map<String, Object>> response = ssoService.createUser(user).execute();
            Map<String, Object> msg = response.body();

            if (!response.isSuccessful()|| (msg != null &&
                    msg.containsKey("success") && !Boolean.valueOf(String.valueOf(msg.get("success"))))) {
                log.error("Error while registering the user with user  " + user.getEmail() +
                        " threw statusCode:" +  response.code() + " reason:" + response.message() + " msg:" + msg);
                throw new AuthSystemException();
            }
        } catch (IOException e) {
            log.error("Error while creating user " + user.getEmail() +
                    " reason:" +  e.getMessage());
            throw new AuthSystemException();
        }
    }

    @Override
    public UserToken login(UserCred user) throws UnAuthorizedException, AuthSystemException {
        try {
            if(user.getEmail() == null)
                throw new UnAuthorizedException("username is passed empty");
            if(user.getPassword() == null)
                throw new UnAuthorizedException("password is empty");
            Response<UserToken> response = ssoService.
                    login(new UserCred(user.getEmail(), user.getPassword())).execute();
            UserToken msg = response.body();
            if(response.code() == 422) {
                throw new UnAuthorizedException("Invalid user");
            }

            if (!response.isSuccessful()|| !msg.getStatus()) {
                log.error("Error while logging in with with user  " + user.getEmail() +
                        " threw statusCode:" + response.code() + " reason:" + response.message()
                        + " msg:" + msg + " status: " + response.code());
                throw new AuthSystemException();
            }
            return msg;
        } catch(IOException e) {
            log.error("Error while logging in reason being " +  e.getMessage());
            throw new AuthSystemException();
        }
    }

    @Override
    public String logout(String authToken) throws UnAuthorizedException, AuthSystemException {
        try {
            Response<UserToken> response = ssoService.
                    logout(AUTH_COOKIE_NAME + "=" + authToken).execute();
            if(response.code() == 422) {
                throw new UnAuthorizedException("Invalid user");
            }

            if (!response.isSuccessful()) {
                log.error("Error while logging outwith with user threw statusCode:" + response.code()
                        + " reason:" + response.message()
                         + " status: " + response.code());
                throw new AuthSystemException();
            }
            List<String> cookies = response.headers().values("Set-Cookie");
            if(cookies == null)
                return null;
            for(String cookie: cookies) {
                String[] tokens = cookie.split("=");
                if(tokens[0].strip().equalsIgnoreCase(AUTH_COOKIE_NAME))
                    return cookie;
            }
            return null;
        } catch(IOException e) {
            log.error("Error while logging in reason being " +  e.getMessage());
            throw new AuthSystemException();
        }
    }

    public Map<String, Object> verify(String authToken) throws UnAuthorizedException, AuthSystemException {
        Response<Map<String, Object>> response = null;
        try {
            Map<String, String> headers = new HashMap<>();
            for(Map.Entry<String, String> threadContextEntry: ThreadContext.getContext().entrySet()) {
                if(threadContextEntry.getKey().startsWith(HEADER_PREFIX_IN_THREAD_CONTEXT)) {
                    String headerName = threadContextEntry.getKey().substring(HEADER_PREFIX_IN_THREAD_CONTEXT.length());
                    headers.put(headerName, threadContextEntry.getValue());
                }
            }
            response = ssoService.authenticate(authToken, headers).execute();
        } catch (IOException e) {
            log.error("Error while logging in reason being " +  e.getMessage());
            throw new AuthSystemException();
        }
        Map<String, Object> msg = response.body();
        Map<String, Object> metaData = response.body();
        if(response.code() == 422 || response.code() == 401) {
            throw new UnAuthorizedException("Invalid user");
        }

        if (!response.isSuccessful()|| (msg != null &&
                msg.containsKey("success") && !Boolean.valueOf(String.valueOf(metaData.get("success"))))) {
            log.error("Error while logging in"+
                    " threw statusCode:" +  response.code() + " reason:" + response.message() + " msg:" + msg);
            throw new AuthSystemException();
        }
        return metaData;
    }
}

