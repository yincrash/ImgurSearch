package org.mikeyin.imgursearch;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.mikeyin.imgursearch.api.ImgurApiService;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

/**
 * Since the app is small, we'll keep services in here rather than use
 * a DI system like dagger.
 */
public class ImgurSearchApplication extends Application {

    private static final String TAG = ImgurSearchApplication.class.getName();
    private static final String OKHTTP_CACHE = "okhttp-cache";
    private static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String BASE_URL = "https://api.imgur.com/";

    private static File createDefaultCacheDir(Context context) {
        File cache = new File(context.getApplicationContext().getCacheDir(), OKHTTP_CACHE);
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }

    private OkHttpClient mOkHttpClient;
    private Retrofit mRetrofit;
    private ImgurApiService mImgurApiService;
    private Picasso mPicasso;

    @Override
    public void onCreate() {
        super.onCreate();
        mOkHttpClient = new OkHttpClient.Builder()
                .cache(new Cache(createDefaultCacheDir(this), MAX_DISK_CACHE_SIZE))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        return chain.proceed(chain.request().newBuilder()
                                .addHeader("Authorization", BuildConfig.IMGUR_API_KEY)
                                .build());
                    }
                })
                .build();
        mRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create())
                .client(mOkHttpClient)
                .build();
        mImgurApiService = mRetrofit.create(ImgurApiService.class);
        mPicasso = new Picasso.Builder(this)
                .listener(new Picasso.Listener() {
                        @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                            exception.printStackTrace();
                        }
                    })
                .downloader(new OkHttp3Downloader(mOkHttpClient)).build();
    }

    public ImgurApiService getImgurApiService() {
        return mImgurApiService;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }
}
