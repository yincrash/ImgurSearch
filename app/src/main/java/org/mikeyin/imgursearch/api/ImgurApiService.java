package org.mikeyin.imgursearch.api;

import org.mikeyin.imgursearch.api.model.GallerySearchResponse;

import retrofit2.adapter.rxjava.Result;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Retrofit client for the Imgur API
 */
public interface ImgurApiService {
    @GET("3/gallery/search/time/{page}")
    Observable<Result<GallerySearchResponse>> gallerySearch(
            @Path("page") int page,
            @Query("q") String searchQuery
    );
}
