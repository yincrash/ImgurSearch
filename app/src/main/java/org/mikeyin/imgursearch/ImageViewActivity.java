package org.mikeyin.imgursearch;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

public class ImageViewActivity extends AppCompatActivity {

    public static final String KEY_IMAGE_URL = "key_image_url";
    public static final String KEY_TITLE = "key_title";

    public static final String KEY_SHARED_IMAGE = "key_shared_image";

    private Picasso mPicasso;
    private ImageView mImage;
    private TextView mTitle;

    private String mImageUrl;
    private String mTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mTitle = (TextView) findViewById(R.id.title);
        mImage = (ImageView) findViewById(R.id.image);
        mPicasso = ((ImgurSearchApplication) getApplication()).getPicasso();
        ActivityCompat.postponeEnterTransition(this);

        Intent intent = getIntent();
        mTitleText = intent.getStringExtra(KEY_TITLE);
        mImageUrl = intent.getStringExtra(KEY_IMAGE_URL);
        if (!Strings.isNullOrEmpty(mImageUrl)) {
            mTitle.setText(mTitleText);
            loadImage(mImageUrl);
        }
    }

    private void loadImage(final String url) {
        if (mImage.getWidth() == 0) {
            // not yet measured
            mImage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    loadImage(url);
                }
            });
        } else {
            mPicasso.load(url)
                    .resize(mImage.getWidth(), 0)
                    .noFade()
                    .into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            ActivityCompat.startPostponedEnterTransition(ImageViewActivity.this);
                        }

                        @Override
                        public void onError() {
                            ActivityCompat.startPostponedEnterTransition(ImageViewActivity.this);
                        }
                    });
        }
    }
}
