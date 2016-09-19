package org.mikeyin.imgursearch;

import android.app.SearchManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jakewharton.rxbinding.support.v7.widget.RecyclerViewScrollEvent;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.squareup.picasso.Picasso;

import org.mikeyin.imgursearch.api.model.GalleryImage;
import org.mikeyin.imgursearch.api.model.GallerySearchResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.adapter.rxjava.Result;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = SearchActivity.class.getSimpleName();

    private static final String IMGUR_URL_FORMAT = "http://i.imgur.com/%s.jpg";
    private static final String KEY_LAST_QUERY = "last_query";

    private Subscription mSearchViewSubscription;
    private Subscription mEndSubscription;
    private Subscription mCurrentRequest;

    private ImgurSearchApplication mApplication;
    private RecyclerView mListResultsView;
    private LinearLayoutManager mListResultsLayoutManager;
    private ResultsAdapter mResultsAdapter;
    private SearchView mSearchView;

    private String mLastQuery;
    private int mCurrentPage;
    /** if true, currently querying api. **/
    private boolean mLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mApplication = (ImgurSearchApplication) getApplication();
        mListResultsView = (RecyclerView) findViewById(R.id.list_results);
        mListResultsView.setHasFixedSize(true);
        mListResultsLayoutManager = new LinearLayoutManager(this);
        mListResultsView.setLayoutManager(mListResultsLayoutManager);
        mResultsAdapter = new ResultsAdapter();
        mListResultsView.setAdapter(mResultsAdapter);
        setupEndListener();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        handleIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LAST_QUERY, mLastQuery);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLastQuery = savedInstanceState.getString(KEY_LAST_QUERY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
        if (!Strings.isNullOrEmpty(mLastQuery)) { // restore state
            searchItem.expandActionView();
            mSearchView.setQuery(mLastQuery, false);
            mSearchView.clearFocus();
        }
        // limits queries so not every letter typed gets sent
        mSearchViewSubscription = RxSearchView.queryTextChanges(mSearchView)
                .debounce(250, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<CharSequence>() {
                    @Override
                    public void call(CharSequence text) {
                        doSearch(text.toString());
                    }
                });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        for (int i = mListResultsLayoutManager.findFirstVisibleItemPosition();
             i <= mListResultsLayoutManager.findLastVisibleItemPosition(); i++) {
            if (i == RecyclerView.NO_POSITION) { break; }
            View view = mListResultsLayoutManager.findViewByPosition(i);
            VideoView video = (VideoView) view.findViewById(R.id.video);
            if (video != null) { video.start(); }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (int i = mListResultsLayoutManager.findFirstVisibleItemPosition();
             i <= mListResultsLayoutManager.findLastVisibleItemPosition(); i++) {
            View view = mListResultsLayoutManager.findViewByPosition(i);
            VideoView video = (VideoView) view.findViewById(R.id.video);
            if (video != null) { video.stopPlayback(); }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchViewSubscription != null) {
            mSearchViewSubscription.unsubscribe();
        }
        if (mEndSubscription != null) {
            mEndSubscription.unsubscribe();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // do search
            Log.d(TAG, "handleIntent(" + query + ")");
            doSearch(query);
        }
    }

    private void doSearch(@NonNull String query) {
        // clear the list
        mResultsAdapter.clear();
        mLastQuery = query;
        mCurrentPage = 1;
        doSearch(query, 1);
    }

    private void doSearch(@NonNull String query, int page) {
        Log.d(TAG, "doSearch(" + query + ", " + page + ")");
        if (Strings.isNullOrEmpty(query)) {
            return;
        }
        mCurrentPage = page;
        if (mCurrentRequest != null) { mCurrentRequest.unsubscribe(); }
        mLoading = true;
        mCurrentRequest = mApplication.getImgurApiService().gallerySearch(page, query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Result<GallerySearchResponse>>() {
                    @Override
                    public void call(Result<GallerySearchResponse> gallerySearchResponseResult) {
                        mResultsAdapter.addResults(
                                gallerySearchResponseResult
                                    .response()
                                    .body()
                                    .getImages());
                        mLoading = false;
                    }
                });

    }

    /**
     * Handles listening for when we reach the end of the list so we can ask for more
     */
    private void setupEndListener() {
        mEndSubscription = RxRecyclerView.scrollEvents(mListResultsView)
                .subscribe(new Action1<RecyclerViewScrollEvent>() {

                    @Override
                    public void call(RecyclerViewScrollEvent recyclerViewScrollEvent) {
                        int last = mListResultsLayoutManager.findLastVisibleItemPosition();
                        if (!mLoading && last != 0 && last == mResultsAdapter.getItemCount() - 1) {
                            // viewing the footer
                            doSearch(mLastQuery, mCurrentPage + 1);
                        }
                    }
                });
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        public CardView mCardView;
        public TextView mTitle;
        public ImageView mImage;
        public String mUrl;

        ImageViewHolder(View view) {
            super(view);
            mCardView = (CardView) view;
            mImage = (ImageView) view.findViewById(R.id.image);
            mTitle = (TextView) view.findViewById(R.id.title);
            mCardView.setTag(this);
        }

    }

    public static class AnimatedViewHolder extends RecyclerView.ViewHolder {

        public CardView mCardView;
        public TextView mTitle;
        public VideoView mVideo;
        public String mUrl;
        public boolean mIsLooping;

        AnimatedViewHolder(View view) {
            super(view);
            mCardView = (CardView) view;
            mVideo = (VideoView) view.findViewById(R.id.video);
            mTitle = (TextView) view.findViewById(R.id.title);
            mCardView.setTag(this);
        }

    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {

        public ProgressBar mProgressBar;

        FooterViewHolder(View view) {
            super(view);
            mProgressBar = (ProgressBar) view;
            mProgressBar.setTag(this);
        }

    }

    private final static int FOOTER_TYPE = 1;
    private final static int IMAGE_TYPE = 0;
    private final static int ANIMATED_TYPE = 2;

    private class ResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

        private List<GalleryImage> mImages = Lists.newArrayList();
        private LayoutInflater mLayoutInflater = LayoutInflater.from(SearchActivity.this);
        private Picasso mPicasso = mApplication.getPicasso();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case FOOTER_TYPE:
                    view = mLayoutInflater.inflate(R.layout.listitem_footer, parent, false);
                    return new FooterViewHolder(view);
                case ANIMATED_TYPE:
                    view = mLayoutInflater.inflate(R.layout.listitem_animated, parent, false);
                    return new AnimatedViewHolder(view);
                case IMAGE_TYPE:
                default:
                    view = mLayoutInflater.inflate(R.layout.listitem_image, parent, false);
                    return new ImageViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type == FOOTER_TYPE) {
                // do nothing
                return;
            }
            if (type == ANIMATED_TYPE) {
                final AnimatedViewHolder viewHolder = (AnimatedViewHolder) holder;
                final GalleryImage image = mImages.get(position);
                viewHolder.mIsLooping = image.isLooping();
                viewHolder.mUrl = image.getMp4();
                viewHolder.mTitle.setText(image.getTitle());
                viewHolder.mVideo.stopPlayback();
                viewHolder.mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(image.isLooping());
                        viewHolder.mVideo.start();
                    }
                });
                viewHolder.mVideo.setVideoPath(viewHolder.mUrl);
                viewHolder.mCardView.setOnClickListener(this);
                return;
            }

            ImageViewHolder viewHolder = (ImageViewHolder) holder;

            // cancel any existing picasso request for the holder
            mPicasso.cancelRequest(viewHolder.mImage);
            viewHolder.mImage.setImageDrawable(null);
            viewHolder.mTitle.setText(null);

            final GalleryImage image = mImages.get(position);
            if (image.getCover() != null) {
                viewHolder.mUrl = String.format(IMGUR_URL_FORMAT, image.getCover());
            } else {
                viewHolder.mUrl = String.format(IMGUR_URL_FORMAT, image.getId());
            }
            viewHolder.mTitle.setText(image.getTitle());
            loadImage(viewHolder);
            viewHolder.mCardView.setOnClickListener(this);
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            if (holder instanceof AnimatedViewHolder) {
                // make sure we stop playback when detaching video
                ((AnimatedViewHolder) holder).mVideo.stopPlayback();
            }
        }

        private void loadImage(final ImageViewHolder holder) {
            if (holder.mImage.getWidth() == 0) {
                // not yet measured
                holder.mImage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        holder.mImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        loadImage(holder);
                    }
                });
            } else {
                mPicasso.load(holder.mUrl)
                        .resize(holder.mImage.getWidth(), 0)
                        .into(holder.mImage);
            }
        }

        @Override
        public int getItemCount() {
            if (mImages.size() == 0) { return 0; }
            return mImages.size() + 1; // for footer
        }

        @Override
        public int getItemViewType(int position) {
            if (position == mImages.size()) {
                return FOOTER_TYPE;
            }
            if (mImages.get(position).isAnimated()) {
                return ANIMATED_TYPE;
            }
            return IMAGE_TYPE;
        }

        public void addResults(List<GalleryImage> results) {
            int start = mImages.size();
            mImages.addAll(results);
            notifyItemRangeInserted(start, results.size());
        }

        public void clear() {
            mImages.clear();
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View view) {
            Object rawHolder = view.getTag();
            View statusBar = findViewById(android.R.id.statusBarBackground);
            View toolBar = findViewById(R.id.toolbar);

            if (rawHolder instanceof AnimatedViewHolder) {
                Intent intent = new Intent(SearchActivity.this, AnimatedViewActivity.class);
                AnimatedViewHolder holder = (AnimatedViewHolder) rawHolder;
                holder.mVideo.pause();
                intent.putExtra(AnimatedViewActivity.KEY_TITLE, holder.mTitle.getText());
                intent.putExtra(AnimatedViewActivity.KEY_MP4_URL, holder.mUrl);
                intent.putExtra(AnimatedViewActivity.KEY_PROGRESS, holder.mVideo.getCurrentPosition());
                intent.putExtra(AnimatedViewActivity.KEY_LOOPING, holder.mIsLooping);
                String transitionImage = getString(R.string.transition_image);
                String transitionTitle = getString(R.string.transition_title);

                ViewCompat.setTransitionName(holder.mVideo, transitionImage);
                ViewCompat.setTransitionName(holder.mTitle, transitionTitle);


                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        SearchActivity.this,
                        Pair.create((View) holder.mVideo, transitionImage),
                        Pair.create((View) holder.mTitle, transitionTitle),
                        Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME),
                        Pair.create(toolBar, getString(R.string.transition_toolbar))
                );
                startActivity(intent, options.toBundle());
            } else if (rawHolder instanceof ImageViewHolder) {
                Intent intent = new Intent(SearchActivity.this, ImageViewActivity.class);
                ImageViewHolder holder = (ImageViewHolder) rawHolder;
                intent.putExtra(ImageViewActivity.KEY_TITLE, holder.mTitle.getText());
                intent.putExtra(ImageViewActivity.KEY_IMAGE_URL, holder.mUrl);

                String transitionImage = getString(R.string.transition_image);
                String transitionTitle = getString(R.string.transition_title);

                ViewCompat.setTransitionName(holder.mImage, transitionImage);
                ViewCompat.setTransitionName(holder.mTitle, transitionTitle);

                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        SearchActivity.this,
                        Pair.create((View) holder.mImage, transitionImage),
                        Pair.create((View) holder.mTitle, transitionTitle),
                        Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME),
                        Pair.create(toolBar, getString(R.string.transition_toolbar))
                );
                startActivity(intent, options.toBundle());
            }
        }
    }
}
