package org.mikeyin.imgursearch.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * API response from https://api.imgur.com/endpoints/gallery#gallery-search
 */
public class GallerySearchResponse {
    @SerializedName("data")
    private List<GalleryImage> mImages;

    @SerializedName("success")
    private boolean mSuccess;

    @SerializedName("status")
    private int mStatus;


    public List<GalleryImage> getImages() {
        return mImages;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public int getStatus() {
        return mStatus;
    }
}
