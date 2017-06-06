package com.rsinukov.yamblz;

import com.rxsinukov.yamblz.api.YandexTranslateApi;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class Utils {

    public static YandexTranslateApi initRetrofit() {
        final OkHttpClient client = new OkHttpClient();
        final OkHttpClient.Builder retrofitClientBuilder = client.newBuilder();

        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            retrofitClientBuilder.addInterceptor(interceptor);
        }

        final Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl("https://translate.yandex.net/api/v1.5/tr.json/")
                .client(retrofitClientBuilder.build())
                .build();

       return retrofit.create(YandexTranslateApi.class);
    }
}
