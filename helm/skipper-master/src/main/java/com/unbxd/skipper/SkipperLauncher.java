package com.unbxd.skipper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.auth.Auth;
import com.unbxd.auth.exception.AuthSystemException;
import com.unbxd.auth.exception.UnAuthorizedException;
import com.unbxd.console.service.SiteValidationService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.dictionary.exceptionHandler.ExceptionHandler;
import com.unbxd.skipper.model.SiteRequest;
import com.unbxd.skipper.site.MultiRegionRouter;
import com.unbxd.skipper.site.exception.InvalidRegionException;
import com.unbxd.toucan.eventfactory.EventBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.Headers.Builder;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import ro.pippo.controller.Controller;
import ro.pippo.controller.ControllerApplication;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.route.CorsHandler;
import ro.pippo.jackson.JacksonJsonEngine;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static com.unbxd.skipper.model.Constants.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Log4j2
public class SkipperLauncher extends ControllerApplication {

    protected OkHttpClient client;
    private static final String TRACE_HEADER = "Unbxd-Request-Id";

    private static final String[] BLACKLISTED_REQUEST_HEADERS = {"Host", "Accept-Encoding",
            "Accept-Language", "Accept", "content-type"};
    public static final String HEADER_PREFIX_IN_THREAD_CONTEXT = "header-";
    public static final String AUTH_PREFIXES = "x-auth-";

    @Inject
    public SkipperLauncher(
            SiteValidationService validationService,
            Set<Controller> controllers,
            Set<ExceptionHandler> exceptionHandlers,
            MultiRegionRouter multiRegionRouter,
            Auth auth
    ) {
        client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(58, TimeUnit.SECONDS)
            .build();
        registerContentTypeEngine(JacksonJsonEngine.class);

        // Enabling cors
        final CorsHandler corsHandler = new CorsHandler("*").allowMethods("GET, POST, PUT, DELETE")
                .allowHeaders("Content-Type,Accept");
        ANY("/.*", corsHandler);

        ANY("/.*", routeContext -> {
            ThreadContext.removeAll(ThreadContext.getContext().keySet());
            long timeBeforeRequestStarts = System.currentTimeMillis();
            // Add all headers except BLACKLISTED_REQUEST_HEADERS to thread context
            Enumeration<String> headerNames = routeContext.getRequest().getHeaderNames();
            while(headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if(Arrays.stream(BLACKLISTED_REQUEST_HEADERS).anyMatch(headerName::equalsIgnoreCase))
                    continue;
                ThreadContext.putIfNull(HEADER_PREFIX_IN_THREAD_CONTEXT + headerName,
                        routeContext.getHeader(headerName));
            }

            EventBuilder eventBuilder = new EventBuilder();
            routeContext.setLocal(EVENT_BUILDER, eventBuilder);
            String traceHeader = routeContext.getHeader(TRACE_HEADER);
            traceHeader = isEmpty(traceHeader) ? UUID.randomUUID().toString() : traceHeader;

            routeContext.setHeader(TRACE_HEADER, traceHeader);
            ThreadContext.put(TRACE_HEADER, traceHeader);
            eventBuilder.withTraceId(traceHeader);
            routeContext.next();
            long timeTaken = System.currentTimeMillis() - timeBeforeRequestStarts;
            log.info("URL: " + routeContext.getRequestUri() + " took " + timeTaken + "ms");
        });

        ANY("/skipper/(?!login)([a-zA-Z0-9\\-/_])*",routeContext -> {
            Enumeration<String> headers = routeContext.getRequest().getHeaderNames();
            while(headers.hasMoreElements()) {
                String header = headers.nextElement();
                if(header.startsWith(AUTH_PREFIXES))
                    routeContext.setLocal(header.substring(AUTH_PREFIXES.length()),
                            StringUtils.strip(routeContext.getHeader(header).trim(), "\""));
            }
            if(routeContext.getLocal(EMAIL) != null)
                ThreadContext.put(EMAIL, routeContext.getLocal(EMAIL));
            routeContext.next();
        });

        //validate authorization
        ANY("/skipper/site/{" + SITEKEY_PARAM + "}/(?!login)([a-zA-Z0-9\\-/_])*", routeContext -> {
            Cookie cookieReq = routeContext.getRequest().getCookie(UN_SSO_UID);
            if(cookieReq == null) {
                routeContext.status(HttpConstants.StatusCode.UNAUTHORIZED);
                routeContext.json().send(APIResponse.getInstance(ErrorResponse
                        .getInstance("No cookie passed"), 401));
                return;
            }
            String cookie = UN_SSO_UID + "=" + cookieReq.getValue();
            ParameterValue param = routeContext.getParameter(SITEKEY_PARAM);
            if (param.isNull()) {
                routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
                routeContext.json().send(APIResponse.getInstance(ErrorResponse
                        .getInstance("siteKey param missing."), 400));
            } else {
                String siteKey = param.getValues()[0];
                boolean siteValid = validationService.isSiteValid(cookie, siteKey);
                if (siteValid) {
                    ThreadContext.put(SITEKEY_PARAM, siteKey);
                    routeContext.next();
                } else {
                    routeContext.status(HttpConstants.StatusCode.FORBIDDEN);
                    routeContext.json().send(APIResponse.getInstance(ErrorResponse
                            .getInstance("Unauthorized"), 403));
                }
            }
        });


        // multi region routing for site creation
        // TODO: Need to bring multi-region for all requests
        ANY("/skipper/site", routeContext -> {
            ObjectMapper mapper = new ObjectMapper();
            String body = routeContext.getRequest().getBody();

            try {
                SiteRequest siteRequest = mapper.readValue(body, SiteRequest.class);
                String region = siteRequest.getRegions();
                String redirectURL = multiRegionRouter.redirect(region);

                if(redirectURL != null) {
                    log.info("Creating site with name:" + siteRequest.getName() + " in region: " + region +
                            " url: " + redirectURL);
                    Builder headerBuilder = new Builder().build().newBuilder();
                    Enumeration<String> headerNames = routeContext.getRequest().getHeaderNames();
                    while(headerNames.hasMoreElements()) {
                        String headerName = headerNames.nextElement();
                        if(!headerName.equalsIgnoreCase("Accept-Encoding"))
                        headerBuilder.add(headerName, routeContext.getHeader(headerName));
                    }
                    RequestBody requestBody = RequestBody.create(
                            MediaType.parse("application/json"), body);

                    Request request = new Request.Builder()
                            .url(redirectURL + "/skipper/site")
                            .headers(headerBuilder.build())
                            .post(requestBody)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body().string();
                        routeContext.status(response.code()).json().send(responseBody);
                    }
                } else {
                    routeContext.next();
                }
            } catch (InvalidRegionException e) {
                routeContext.json().status(400).send(APIResponse.
                        getInstance(ErrorResponse.getInstance(e.getMessage()), 400));
            } catch (IllegalStateException e) {
                routeContext.json().status(500).send(APIResponse.
                        getInstance(
                                ErrorResponse.getInstance("Selected region is not configured properly"), 500));
            } catch (JsonProcessingException e) {
                routeContext.json().status(400).send(APIResponse.
                        getInstance(ErrorResponse.getInstance("Unable to parse request json."), 400));
            } catch (IOException e) {
                String msg = "Error while creating site";
                log.error(msg + " reason: " + e.getMessage());
                routeContext.json().status(500).send(APIResponse.
                        getInstance(
                                ErrorResponse.getInstance(msg), 500));
            }
        });

        controllers.forEach(this::addControllers);
        exceptionHandlers.forEach(eh -> getErrorHandler().setExceptionHandler(
                eh.exception(), eh.handler()
        ));
    }
}

