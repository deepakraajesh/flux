package com.unbxd.config.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface HealthCheckRemoteService {

    @GET
    Call<Void> doHealthCheck(@Url String url);
}
