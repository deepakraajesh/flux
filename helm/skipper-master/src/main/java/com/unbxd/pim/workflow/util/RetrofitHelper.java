package com.unbxd.pim.workflow.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RetrofitHelper {

    private static OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.SECONDS)
        .callTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build();

    public static <S> S createService(Retrofit.Builder builder, Class<S> serviceClass) {
        Retrofit retrofit = builder.build();
        return retrofit.create(serviceClass);
    }

    public static OkHttpClient.Builder getOkHttpClientBuilder() {
        return client.newBuilder();
    }

    private static OkHttpClient loadOkHttpClient( Map<String,String> headers  ) {
        return getOkHttpClientBuilder().addInterceptor(chain -> {
            Request.Builder reqBuilder = chain
                    .request()
                    .newBuilder();
            headers.forEach(reqBuilder::addHeader);
            return chain.proceed( reqBuilder.build() );
        }).build();
    }
}
