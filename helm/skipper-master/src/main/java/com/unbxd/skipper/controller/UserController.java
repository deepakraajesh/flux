package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.auth.Auth;
import com.unbxd.auth.exception.AuthSystemException;
import com.unbxd.auth.exception.UnAuthorizedException;
import com.unbxd.auth.model.User;
import com.unbxd.auth.model.UserCred;
import com.unbxd.auth.model.UserToken;
import com.unbxd.config.Config;
import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.SiteDetails;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.DELETE;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.route.RouteContext;

import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.auth.Auth.AUTH_COOKIE_NAME;
import static java.util.Objects.isNull;

@Log4j2
public class UserController extends Controller {
    private Auth auth;
    private ConsoleOrchestrationService consoleOrchestrationService;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String AUTH_COOKIE_HEADER = "_un_sso_uid";
    private static final String EMAIL = "email";
    private static final String REGIONS = "regions";

    private static final String AUTH_DOMAIN = "auth.domain";
    private String DOMAIN_NAME;

    @Inject
    public UserController(Auth auth,
                          Config config,
                          ConsoleOrchestrationService consoleOrchestrationService  ) {
        this.auth = auth;
        this.consoleOrchestrationService = consoleOrchestrationService;
        this.DOMAIN_NAME = config.getProperty(AUTH_DOMAIN,"unbxd.io");
    }

    @POST("/user")
    public void register() {
        RouteContext routeContext = getRouteContext();
        User user = null;
        try {
            user = mapper.readValue(routeContext.getRequest().getBody(), User.class);
            routeContext.json().status(201);
        } catch (JsonProcessingException|IllegalArgumentException e) {
            String msg = "Error while parsing the request  for registration " ;
            log.error(msg + " reason " + e.getMessage());
            routeContext.json().status(400)
                    .send(Collections.singletonMap("msg", msg));
        }

        try {
            auth.register(user);
        } catch(AuthSystemException e) {
            log.error( "Internal error while registering user " + user.toString()
                    + " reason " + e.getMessage());
            HashMap<Object, Object> errorMsg = new HashMap<>();
            errorMsg.put("msg","Internal Server error");
            errorMsg.put("code", "411");
            routeContext.json().status(500)
                    .send(errorMsg);
        } catch (IllegalArgumentException e) {
            String msg = "Illegal argument " + user.toString();
            log.error(msg);
            routeContext.json().status(400)
                    .send(msg);
        }
    }

    @POST("/skipper/login")
    public void login() {
        RouteContext routeContext = getRouteContext();
        UserCred user = null;
        try {
            user = mapper.readValue(routeContext.getRequest().getBody(), UserCred.class);
        } catch (JsonProcessingException|IllegalArgumentException e) {
            String msg = "Error while parsing the request  for registration " + e.getMessage();
            log.error(msg + " reason " + e.getMessage());
            routeContext.json().status(500)
                    .send(new APIResponse(Collections.singletonList("4  00")));
            return;
        }

        try {
            UserToken token = auth.login(user);
            routeContext.getResponse().cookie("/", DOMAIN_NAME, AUTH_COOKIE_NAME,
                    token.getToken(), 604800, Boolean.FALSE);
        } catch(AuthSystemException | UnAuthorizedException e) {
            log.error( "Internal error while registering user " + user.toString()
                    + " reason " + e.getMessage());

            routeContext.json().status(500)
                    .send(new APIResponse(Collections.singletonList("500")));
            return;
        } catch (IllegalArgumentException e) {
            String msg = "Illegal argument " + user.toString();
            log.error(msg);
            routeContext.json().status(400)
                    .send(msg);
            routeContext.json().status(400)
                    .send(new APIResponse(Collections.singletonList("400")));
            return;
        }
        routeContext.json().status(200).send(new APIResponse<>());
    }

    @GET("/skipper/user")
    public void getUser() {
        Cookie authToken = getRouteContext().getRequest().getCookie(AUTH_COOKIE_HEADER);
        try {
            Map<String, Object> metaData = auth.verify(authToken.getValue());
            getRouteContext().json().status(200).send(new APIResponse<>(metaData));
            return;
        }
        catch (UnAuthorizedException e) {
            getRouteContext().status(HttpConstants.StatusCode.UNAUTHORIZED);
            getRouteContext().send("UnAuthorized call");
        }
        catch (AuthSystemException e){
            getRouteContext().status(HttpConstants.StatusCode.INTERNAL_ERROR);
            getRouteContext().send("Internal error in Auth system");
        }
    }

    @GET("/skipper/sites")
    public void getSites(){
        RouteContext routeContext = getRouteContext();
        String email = getRouteParam(EMAIL,routeContext);
        String regions = getRouteParam(REGIONS,routeContext);
        if(isNull(email) || isNull(regions)) return;
        try{
            List<SiteDetails> sites = consoleOrchestrationService.getSites(email, regions);
            routeContext.status(200).json().send(new APIResponse<>(sites));
        } catch (ConsoleOrchestrationServiceException e)
        {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            routeContext.status(e.getStatusCode()).json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
        }
    }

    @DELETE("/skipper/logout")
    public void logout() {
        RouteContext routeContext = getRouteContext();
        Cookie authToken = routeContext.getRequest().getCookie(AUTH_COOKIE_NAME);
        if(authToken == null) {
            routeContext.status(401).send("UnAuthorized call");
            return;
        }
        try {
            String cookie = auth.logout(authToken.getValue());
            if (cookie == null) {
                log.error("No cookie exist to remove cookie, Failure with logout API");
                routeContext.status(500).json()
                        .send(new APIResponse<>(
                                Collections.singletonList(new  ErrorResponse("Failed to unset cookie"))));
                return;
            }

            routeContext.getResponse().header("Set-Cookie", cookie);
            routeContext.status(200).json().send(new APIResponse<>());
            return;
        }
        catch (UnAuthorizedException e) {
            routeContext.status(HttpConstants.StatusCode.UNAUTHORIZED);
            routeContext.send("UnAuthorized call");
        }
        catch (AuthSystemException e){
            routeContext.status(HttpConstants.StatusCode.INTERNAL_ERROR);
            routeContext.send("Internal error in Auth system");
        }
    }

    private String getRouteParam(String paramName, RouteContext routeContext) {
        ParameterValue param = routeContext.getParameter(paramName);
        if(!param.isNull()) {
            return param.getValues()[0];
        }
        ErrorResponse errorResponse = new ErrorResponse(paramName + " param missing!");
        routeContext.status(400).json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
        return null;
    }
}

