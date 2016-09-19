package org.mikeyin.imgursearch;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.common.base.Strings;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class AnimatedViewActivity extends AppCompatActivity {

    public static final String KEY_MP4_URL = "key_mp4_url";
    public static final String KEY_TITLE = "key_title";
    public static final String KEY_PROGRESS = "key_progress";
    public static final String KEY_LOOPING = "key_looping";

    private VideoView mVideo;
    private TextView mTitle;

    private String mVideoUrl;
    private String mTitleText;
    private boolean mIsLooping;
    private int mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animated_view);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mTitle = (TextView) findViewById(R.id.title);
        mVideo = (VideoView) findViewById(R.id.video);
        //ActivityCompat.postponeEnterTransition(this);

        Intent intent = getIntent();
        mTitleText = intent.getStringExtra(KEY_TITLE);
        mIsLooping = intent.getBooleanExtra(KEY_LOOPING, false);
        mProgress = intent.getIntExtra(KEY_PROGRESS, 0);

        mVideoUrl = intent.getStringExtra(KEY_MP4_URL);
        mTitle.setText(mTitleText);
        mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(mIsLooping);
                if (mVideo.canSeekForward()) {
                    mVideo.seekTo(mProgress);
                }
                mVideo.start();
                //ActivityCompat.startPostponedEnterTransition(AnimatedViewActivity.this);
            }
        });
        mVideo.setVideoPath(mVideoUrl);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mVideo.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mVideo.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideo.stopPlayback();
    }
}
