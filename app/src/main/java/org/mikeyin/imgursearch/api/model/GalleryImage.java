package org.mikeyin.imgursearch.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * https://api.imgur.com/models/gallery_image
 */
public class GalleryImage {
    @SerializedName("id")
    private String mId;

    @SerializedName("cover")
    private String mCover;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("type")
    private String mType;

    @SerializedName("link")
    private String mLink;

    @SerializedName("mp4")
    private String mMp4;

    @SerializedName("looping")
    private boolean mLooping;

    @SerializedName("animated")
    private boolean mAnimated;

    @SerializedName("images")
    private List<GalleryImage> mImages;

    public String getId() {
        return mId;
    }

    public String getCover() {
        return mCover;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getType() {
        return mType;
    }

    public String getLink() {
        return mLink;
    }

    public String getMp4() {
        return mMp4;
    }

    public boolean isLooping() {
        return mLooping;
    }

    public boolean isAnimated() {
        return mAnimated;
    }

    public List<GalleryImage> getImages() {
        return mImages;
    }
}
