/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusesFilter;
import org.tweetalib.android.model.TwitterUser;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.App;
import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.Constant.SystemEvent;
import com.tweetlanes.android.R;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.util.Util;
import com.tweetlanes.android.view.BaseLaneFragment.InitialDownloadState;
import com.tweetlanes.android.view.ComposeBaseFragment.ComposeListener;
import com.tweetlanes.android.widget.viewpagerindicator.PageIndicator;
import com.tweetlanes.android.widget.viewpagerindicator.TabPageIndicator;
import com.tweetlanes.android.widget.viewpagerindicator.TabPageIndicator.TabCallbacks;

public class BaseLaneActivity extends FragmentActivity implements
        SearchView.OnQueryTextListener {

    private ViewPager mViewPager;
    PageIndicator mPageIndicator;
    private ActionMode mCurrentActionMode;
    private Menu mCurrentMenu;
    private SearchView mSearchView;
    private View mLaneMask;
    private LinearLayout mDummyFocusItem;
    TwitterStatusesFilter mStatusesFilter = new TwitterStatusesFilter();
    private String mShareImagePath;

    private static final int COMPOSE_TWEET = 0;
    private static final int COMPOSE_DIRECT_MESSAGE = 1;
    private ComposeTweetFragment mComposeTweetFragment;
    private View mComposeTweetView;
    private ComposeDirectMessageFragment mComposeDirectMessageFragment;
    private View mComposeDirectMessageView;
    private ComposeBaseFragment mCurrentComposeFragment;

    /*
	 *
	 */
    public App getApp() {
        return (App) getApplication();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.init(getApplicationContext(),
                    Constant.CRITTERCISM_APP_ID);
        }

        // Key the screen from dimming -
        // http://stackoverflow.com/a/4197370/328679
        if (AppSettings.get().isDimScreenEnabled() == false) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        PagerAdapter pagerAdapter = getAdapterForViewPager();
        if (pagerAdapter != null) {

            setTheme(AppSettings.get().getCurrentThemeStyle());
            setContentView(R.layout.default_lanes_layout);

            /*
             * if (getResources().getConfiguration().orientation ==
             * Configuration.ORIENTATION_LANDSCAPE) { mPageIndicator =
             * (ListTabPageIndicator)findViewById(R.id.listTabPageIndicator);
             * findViewById(R.id.tabPageIndicator).setVisibility(View.GONE); }
             * else {
             */
            TabPageIndicator tabIndicator = (TabPageIndicator) findViewById(R.id.tabPageIndicator);
            tabIndicator.setTabCallbacks(new TabCallbacks() {

                @Override
                public void onCurrentItemClicked() {
                    onCurrentLaneReselected();
                };

            });

            // If there's only 1 item in the adapter, hide the item. Used for
            // DMs.
            if (pagerAdapter.getCount() == 1) {
                tabIndicator.setVisibility(View.GONE);
            }

            mPageIndicator = tabIndicator;
            // findViewById(R.id.listTabLayout).setVisibility(View.GONE);
            // }

            int initialLaneIndex = getInitialLaneIndex();

            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(pagerAdapter);
            mViewPager.setCurrentItem(initialLaneIndex);

            mPageIndicator.setViewPager(mViewPager, initialLaneIndex);
            mPageIndicator.setOnPageChangeListener(mOnPageChangeListener);
        }

        mLaneMask = findViewById(R.id.lane_mask);
        mLaneMask.setOnClickListener(mLaneMaskOnClickListener);

        mDummyFocusItem = (LinearLayout) findViewById(R.id.dummyFocusItem);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        mComposeDirectMessageView = findViewById(R.id.composeTweetFragment);
        mComposeDirectMessageFragment = (ComposeDirectMessageFragment) getSupportFragmentManager()
                .findFragmentById(R.id.composeDirectMessageFragment);
        if (mComposeDirectMessageFragment != null) {
            mComposeDirectMessageFragment
                    .setComposeTweetListener(mComposeDirectMessageListener);
            mComposeDirectMessageView.setVisibility(View.GONE);
            // mComposeDirectMessageFragment.setTweetDefaultFromDraft(getApp().getTweetDraftAsString());
            // setCurrentComposeFragment(COMPOSE_DIRECT_MESSAGE);

            ft.hide(mComposeDirectMessageFragment);

        }

        mComposeTweetView = findViewById(R.id.composeTweetFragment);
        mComposeTweetFragment = (ComposeTweetFragment) getSupportFragmentManager()
                .findFragmentById(R.id.composeTweetFragment);
        if (mComposeTweetFragment != null) {
            mComposeTweetFragment
                    .setComposeTweetListener(mComposeTweetListener);
            mComposeTweetFragment.setTweetDefaultFromDraft(getApp()
                    .getTweetDraftAsString());
            mComposeTweetView.setVisibility(View.GONE);
            // setCurrentComposeFragment(COMPOSE_TWEET);

            ft.hide(mComposeTweetFragment);
        }

        ft.commit();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mForceFragmentPagerAdapterRefreshReceiver,
                new IntentFilter(""
                        + SystemEvent.FORCE_FRAGMENT_PAGER_ADAPTER_REFRESH));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mRestartAppReceiver,
                new IntentFilter("" + SystemEvent.RESTART_APP));
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPostCreate(android.os.Bundle)
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setComposeDefault();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mForceFragmentPagerAdapterRefreshReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mRestartAppReceiver);

        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (AppSettings.get().isDirty()) {
            restartActivity();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDisplayToastReceiver,
                new IntentFilter("" + SystemEvent.DISPLAY_TOAST));
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mDisplayToastReceiver);

        if (mComposeTweetFragment != null) {
            mComposeTweetFragment.updateComposeTweetDefault();
            getApp().saveTweetDraft(
                    mComposeTweetFragment.getTweetDefaultDraft());
        }
    }

    /*
	 *
	 */
    private BroadcastReceiver mDisplayToastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = (String) intent.getExtras().get("message");
            if (message != null) {
                Toast.makeText(BaseLaneActivity.this, message,
                        Constant.DEFAULT_TOAST_DISPLAY_TIME).show();
            }
        }
    };

    /*
	 *
	 */
    void setCurrentComposeFragment(int type) {
        if (type == COMPOSE_DIRECT_MESSAGE) {
            if (mCurrentComposeFragment != mComposeDirectMessageFragment) {

                FragmentTransaction ft = getSupportFragmentManager()
                        .beginTransaction();
                if (mComposeTweetFragment != null) {
                    ft.hide(mComposeTweetFragment);
                }

                ft.show(mComposeDirectMessageFragment);
                ft.commit();

                mCurrentComposeFragment = mComposeDirectMessageFragment;
            }
        } else {
            if (mCurrentComposeFragment != mComposeTweetFragment) {

                FragmentTransaction ft = getSupportFragmentManager()
                        .beginTransaction();
                if (mComposeDirectMessageFragment != null) {
                    ft.hide(mComposeDirectMessageFragment);
                }

                ft.show(mComposeTweetFragment);
                ft.commit();

                mCurrentComposeFragment = mComposeTweetFragment;
            }
        }
    }

    /*
	 *
	 */
    private HashMap<Integer, BaseLaneFragment> mLaneFragmentHashMap = new HashMap<Integer, BaseLaneFragment>();
    private int activeInitialDownloadCount = 0;

    /*
	 *
	 */
    protected BaseLaneFragment getFragmentAtIndex(int index) {
        if (mLaneFragmentHashMap != null) {
            return mLaneFragmentHashMap.get(index);
        }

        return null;
    }

    /*
	 *
	 */
    protected void clearFragmentsCache() {
        if (mLaneFragmentHashMap != null) {
            for (Integer key : mLaneFragmentHashMap.keySet()) {
                BaseLaneFragment lane = mLaneFragmentHashMap.get(key);
                lane.clearLocalCache();
            }
        }
    }

    /*
	 *
	 */
    String getCachedData(int laneIndex) {

        return null;
    }

    /*
	 *
	 */
    protected int getCurrentLaneIndex() {
        if (mViewPager != null) {
            return mViewPager.getCurrentItem();
        }
        return 3;
    }

    /*
	 *
	 */
    private int getLaneCount() {
        if (mViewPager != null) {
            return mViewPager.getAdapter().getCount();
        }

        return getApp().getCurrentAccount().getDisplayedLaneDefinitionsSize();
    }

    /*
	 *
	 */
    protected void onLaneFragmentInitialDownloadStateChange(
            BaseLaneFragment fragment) {

        mLaneFragmentHashMap.put(fragment.getLaneIndex(), fragment);

        InitialDownloadState state = fragment.getInitialDownloadState();
        int currentLane = getCurrentLaneIndex();
        BaseLaneFragment currentLaneFragment = mLaneFragmentHashMap
                .get(currentLane);

        // HomeLaneAdapter adapter = (HomeLaneAdapter)mViewPager.getAdapter();
        // Log.d("tweetlanes url fetch", fragment.getIdentifier() + ": State=" +
        // state.toString());

        if (mCurrentComposeFragment == null && currentLaneFragment != null) {
            int composeType = (currentLaneFragment instanceof DirectMessageFeedFragment) ? COMPOSE_DIRECT_MESSAGE
                    : COMPOSE_TWEET;
            setCurrentComposeFragment(composeType);
        }

        switch (state) {
        case WAITING:
            if (fragment.getLaneIndex() == currentLane) {
                fragment.triggerInitialDownload();
                // Log.d("tweetlanes url fetch", "trigger Current lane '" +
                // adapter.getTitle(currentLane) + "' (index = " + currentLane +
                // ")");
            } else if (activeInitialDownloadCount == 0) {
                if (currentLaneFragment != null
                        && currentLaneFragment.getInitialDownloadState() == InitialDownloadState.DOWNLOADED) {
                    fragment.triggerInitialDownload();
                    // Log.d("tweetlanes url fetch", "trigger neighbour lane '"
                    // + adapter.getTitle(fragment.getLaneIndex()) +
                    // "' (index = " + fragment.getLaneIndex() + ")");
                }
            }
            break;

        case DOWNLOADING:
            activeInitialDownloadCount++;
            break;

        case DOWNLOADED:
            activeInitialDownloadCount = Math.max(0,
                    activeInitialDownloadCount - 1);
            //triggerNeighbourInitialDownload(currentLane);
            break;

        default:
            break;
        }
    }

    /*
	 *
	 */
    protected void onLaneChange(int position, int oldPosition) {

        invalidateOptionsMenu();

        boolean triggeredDownload = false;

        BaseLaneFragment fragment = mLaneFragmentHashMap.get(position);
        // fragment will be null if the user scrolls the Tabs to a Fragment not
        // yet created.
        // In that instance, the download will be triggered in
        // onLaneFragmentDownloadStateChanged().
        if (fragment != null) {
            InitialDownloadState state = fragment.getInitialDownloadState();
            if (state == InitialDownloadState.WAITING) {
                if (fragment.getLaneIndex() == position) {
                    fragment.triggerInitialDownload();
                    triggeredDownload = true;
                }
            }
        }

        if (triggeredDownload == false) {
            //triggerNeighbourInitialDownload(position);
        }

        setCurrentComposeFragment((fragment instanceof DirectMessageFeedFragment) ? COMPOSE_DIRECT_MESSAGE
                : COMPOSE_TWEET);
    }

    /*
	 *
	 */
    protected void onCurrentLaneReselected() {

        BaseLaneFragment fragment = mLaneFragmentHashMap
                .get(getCurrentLaneIndex());
        // fragment will be null if the user scrolls the Tabs to a Fragment not
        // yet created.
        // In that instance, the download will be triggered in
        // onLaneFragmentDownloadStateChanged().
        if (fragment != null) {
            fragment.onJumpToTop();
        }
    }

    /*
	 *
	 */
    protected boolean triggerNeighbourInitialDownload(int currentLane) {

        boolean triggeredDownload = false;
        if (currentLane > 0) {
            BaseLaneFragment leftFragment = mLaneFragmentHashMap
                    .get(currentLane - 1);
            if (leftFragment != null
                    && leftFragment.getInitialDownloadState() == InitialDownloadState.WAITING) {
                // Log.d("tweetlanes url fetch", "trigger Left lane '" +
                // adapter.getTitle(currentLane-1) + "'  (index = " +
                // (currentLane-1) + ")");
                leftFragment.triggerInitialDownload();
                triggeredDownload = true;
            }
        }

        if (triggeredDownload == false) {
            if (currentLane + 1 < getLaneCount()) {
                BaseLaneFragment rightFragment = mLaneFragmentHashMap
                        .get(currentLane + 1);
                if (rightFragment != null
                        && rightFragment.getInitialDownloadState() == InitialDownloadState.WAITING) {
                    // Log.d("tweetlanes url fetch",
                    // "trigger Right lane (index =  '" +
                    // adapter.getTitle(currentLane+1) + "' " + (currentLane+1)
                    // + ")");
                    rightFragment.triggerInitialDownload();
                }
            }
        }

        return false;
    }

    /*
	 *
	 */
    ComposeListener mComposeTweetListener = new ComposeListener() {

        @Override
        public void onShowCompose() {
            mLaneMask.setVisibility(View.VISIBLE);

            finishCurrentActionMode();

            if (mCurrentMenu != null) {
                mCurrentMenu.close();
                mCurrentMenu.clear();
            }

            ActionBar actionBar = getActionBar();
            actionBar
                    .setNavigationMode(android.app.ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getApp().getCurrentAccount().getSocialNetType() == SocialNetConstant.Type.Twitter ? R
                    .string.action_bar_tweet_compose_title : R.string.action_bar_tweet_compose_title_adn);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);

            if (mCurrentActionMode != null) {
                // mCurrentActionMode.invalidate();
                // mCurrentActionMode.setCustomView(null);
                // TODO: Come up with a better solution here
                mCurrentActionMode.finish();
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onHideCompose() {
            mLaneMask.setVisibility(View.GONE);

            // Give this dummy item focus to stop the cursor blinking on the
            // compose view when the keyboard is no longer on screen
            mDummyFocusItem.requestFocus();
        }

        @Override
        public void onBackButtonPressed() {
            composeReleaseFocus(true);
        }

        @Override
        public void onStatusUpdateRequest() {
            composeReleaseFocus(true);
        }

        @Override
        public void onStatusUpdateSuccess() {
            setComposeDefault();
            mShareImagePath = null;
        }

        @Override
        public void onMediaAttach() {

            // MenuItem galleryMenuItem = getMenuItem(R.id.action_gallery);
            // if (galleryMenuItem != null) {
            // galleryMenuItem.setIcon(R.drawable.ic_dialog_photo_active_default);
            // }

            mShareImagePath = null;
        }

        @Override
        public void onMediaDetach() {

            // MenuItem galleryMenuItem = getMenuItem(R.id.action_gallery);
            // if (galleryMenuItem != null) {
            // galleryMenuItem.setIcon(R.drawable.ic_dialog_photo_default);
            // }

            mShareImagePath = null;
        }

        @Override
        public void onStatusHintUpdate() {
            updateActionModeTitles();
        }

        @Override
        public void saveDraft(String draftAsJsonString) {
            getApp().saveTweetDraft(draftAsJsonString);
        }

        @Override
        public String getDraft() {
            return getApp().getTweetDraftAsString();
        }
    };

    /*
	 *
	 */
    ComposeListener mComposeDirectMessageListener = new ComposeListener() {

        @Override
        public void onShowCompose() {
            mLaneMask.setVisibility(View.VISIBLE);

            finishCurrentActionMode();

            if (mCurrentMenu != null) {
                mCurrentMenu.close();
                mCurrentMenu.clear();
            }

            ActionBar actionBar = getActionBar();
            actionBar
                    .setNavigationMode(android.app.ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar
                    .setTitle(R.string.action_bar_direct_message_compose_title);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);

            if (mCurrentActionMode != null) {
                // mCurrentActionMode.invalidate();
                // mCurrentActionMode.setCustomView(null);
                // TODO: Come up with a better solution here
                mCurrentActionMode.finish();
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onHideCompose() {
            mLaneMask.setVisibility(View.GONE);

            // Give this dummy item focus to stop the cursor blinking on the
            // compose view when the keyboard is no longer on screen
            mDummyFocusItem.requestFocus();
        }

        @Override
        public void onBackButtonPressed() {
            composeReleaseFocus(true);
        }

        @Override
        public void onStatusUpdateRequest() {
            composeReleaseFocus(true);
        }

        @Override
        public void onStatusUpdateSuccess() {
            setComposeDefault();
        }

        @Override
        public void onMediaAttach() {
        }

        @Override
        public void onMediaDetach() {
        }

        @Override
        public void onStatusHintUpdate() {
            updateActionModeTitles();
        }

        @Override
        public void saveDraft(String draftAsJsonString) {
            // getApp().saveTweetDraft(draftAsJsonString);
        }

        @Override
        public String getDraft() {
            // return getApp().getTweetDraftAsString();
            return null;
        }
    };

    /*
	 *
	 */
    private void updateActionModeTitles() {
        if (mCurrentActionMode != null
                && Util.tagEquals(mCurrentActionMode.getTag(),
                        R.id.tagIdComposeTweetActionBar)) {

            if (mComposeTweetFragment != null) {
                if (mComposeTweetFragment.getInReplyToId() == null) {
                    mCurrentActionMode
                            .setTitle(getApp().getCurrentAccount().getSocialNetType() == SocialNetConstant.Type
                                    .Twitter ? R.string.action_bar_tweet_compose_title : R.string
                                    .action_bar_tweet_compose_title_adn);
                } else {
                    mCurrentActionMode
                            .setTitle(R.string.action_bar_tweet_reply_compose_title);
                }
                mCurrentActionMode.setSubtitle(null);
            }
        }
    }

    /*
	 *
	 */
    MenuItem getMenuItem(int resourceId) {

        if (mCurrentMenu != null) {
            for (int i = 0; i < mCurrentMenu.size(); i++) {
                MenuItem menuItem = mCurrentMenu.getItem(i);
                if (menuItem.getItemId() == resourceId) {
                    return menuItem;
                }
            }
        }

        return null;
    }

    /*
	 *
	 */
    OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            if (mLaneFragmentHashMap != null
                    && mLaneFragmentHashMap.containsKey(position)) {

            }
            getApp().getCurrentAccount().setCurrentLaneIndex(position);
            if (mCurrentActionMode != null) {
                // TODO: Probably shouldn't clear this in the event the
                // ComposeTweet ActionBar is displaying
                mCurrentActionMode.finish();
            }

            onLaneChange(position, -1);
        }
    };

    /*
	 *
	 */
    OnClickListener mLaneMaskOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            composeReleaseFocus(true);
        }

    };

    /*
	 *
	 */
    void restartApp() {
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    /*
	 *
	 */
    void restartActivity() {
        finish();
        getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(getIntent());
    }

    /*
	 *
	 */
    boolean composeReleaseFocus() {

        boolean result = false;

        if (mCurrentComposeFragment != null
                && mCurrentComposeFragment.hasFocus()) {
            mCurrentComposeFragment.releaseFocus(true);
            result = true;
        }

        return result;
    }

    /*
	 *
	 */
    boolean composeHasFocus() {
        if (mCurrentComposeFragment != null) {
            return mCurrentComposeFragment.hasFocus();
        }
        return false;
    }

    /*
	 *
	 */
    boolean composeReleaseFocus(boolean forceCleanup) {

        boolean result = false;

        if (mCurrentComposeFragment != null
                && mCurrentComposeFragment.hasFocus()) {
            mCurrentComposeFragment.releaseFocus(true);
            forceCleanup = true;
            result = true;
        }

        if (forceCleanup) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(null);
            actionBar.setDisplayShowTitleEnabled(false);
            invalidateOptionsMenu();
        }
        return result;
    }

    /**
     * Get a temporary file with a fixed (=known in advance) file name
     *
     * @param context
     *            activity context
     * @return a temp file in the external storage in a package-specific
     *         directory
     */
    private File getFixedTempFile(Context context) {
        File path = new File(Environment.getExternalStorageDirectory(),
                context.getPackageName());

        if (!path.exists()) path.mkdir();

        File tempFile;
        tempFile = new File(path, "image.tmp");
        return tempFile;

    }

    /**
     * Get a temporary file with a unique file name
     *
     * @param context
     *            activity context
     * @return a temp file in the external storage in a package-specific
     *         directory
     */
    private File getTempFile(Context context) {
        File path = new File(Environment.getExternalStorageDirectory(),
                context.getPackageName());

        if (!path.exists()) path.mkdir();

        File tempFile;
        try {
            tempFile = File.createTempFile("img_", ".jpg", path);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customise this generated block
            tempFile = new File(path, "image.tmp");
        }
        Log.d("NewTweetActivity.getTempFile", tempFile.getAbsolutePath());
        return tempFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    /*
     * @Override public boolean onOptionsItemSelected(MenuItem item) { switch
     * (item.getItemId()) { case android.R.id.home: if
     * (tweetComposeReleaseFocus() == true) { return true; } break; } return
     * super.onOptionsItemSelected(item); }
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * android.widget.SearchView.OnQueryTextListener#onQueryTextChange(java.
     * lang.String)
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.widget.SearchView.OnQueryTextListener#onQueryTextSubmit(java.
     * lang.String)
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
        i.putExtra("query", query);
        startActivity(i);

        return false;
    }

    /*
	 *
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (AppSettings.get().isVolScrollEnabled()) {
                    Intent intent = new Intent(""
                            + SystemEvent.VOLUME_UP_KEY_DOWN);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                            intent);
                    return true;
                }

            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (AppSettings.get().isVolScrollEnabled()) {
                    Intent intent = new Intent(""
                            + SystemEvent.VOLUME_DOWN_KEY_DOWN);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                            intent);
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Early exit on these events so that the volume up/down sound doesn't
        // play
        // TODO: Handle user options for volume scrolling
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /*
	 *
	 */
    protected PagerAdapter getAdapterForViewPager() {
        throw new RuntimeException("Derived class must implement me");
    }

    /*
	 *
	 */
    protected FragmentStatePagerAdapter getFragmentStatePagerAdapter() {
        throw new RuntimeException("Derived class must implement me");
    }

    /*
	 *
	 */
    private BroadcastReceiver mForceFragmentPagerAdapterRefreshReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            FragmentStatePagerAdapter adapter = getFragmentStatePagerAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

    };

    /*
	 *
	 */
    private BroadcastReceiver mRestartAppReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            restartApp();
        }
    };

    /*
	 *
	 */
    protected int getInitialLaneIndex() {
        return 0;
    }

    /*
	 *
	 */
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL THIS CAN BE, IF
            // YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String imagePath = null;

        if (requestCode == Constant.REQUEST_CODE_IMAGE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);
                // File mediaFile = null;
                if (selectedImagePath != null) {
                    // mediaFile = new File(selectedImagePath);
                    imagePath = selectedImagePath;
                } else {
                    String fileManagerPath = selectedImageUri.getPath();
                    imagePath = fileManagerPath;
                    if (fileManagerPath != null) {
                        // mediaFile = new File(fileManagerPath);
                    }
                }

                // if (mediaFile != null) {
                // TwitterManager.get().setStatus(new
                // TwitterStatusUpdate("here is a test status", null,
                // mediaFile), null);
                // }
            }
        } else if (requestCode == Constant.REQUEST_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {

                // large size image
                File file = getFixedTempFile(this);
                File newPath = getTempFile(this);
                boolean success = file.renameTo(newPath);
                if (success)
                    imagePath = newPath.getAbsolutePath();
                else
                    imagePath = file.getAbsolutePath();

                // Toast.makeText(this,R.string.picture_attached,Toast.LENGTH_SHORT).show();

            }
        }

        mShareImagePath = imagePath;
        if (mComposeTweetFragment != null) {
            mComposeTweetFragment.setMediaFilePath(imagePath);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onActionModeStarted(android.view.ActionMode)
     */
    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);

        mCurrentActionMode = mode;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onActionModeFinished(android.view.ActionMode)
     */
    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);

        mCurrentActionMode = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        mCurrentMenu = menu;

        if (mCurrentComposeFragment == null) {
            return false;
        }

        if (mComposeTweetFragment != null && mComposeTweetFragment.hasFocus()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.compose_tweet_action_bar, menu);
            return true;
        } else if (mComposeDirectMessageFragment != null
                && mComposeDirectMessageFragment.hasFocus()) {
            return true;
        } else {
            return configureOptionsMenu(menu);
        }
    }

    /*
	 *
	 */
    public boolean configureOptionsMenu(Menu menu) {

        Integer defaultOptionsMenu = getDefaultOptionsMenu();
        if (defaultOptionsMenu != null) {
            MenuInflater inflater = getMenuInflater();

            BaseLaneFragment fragment = mLaneFragmentHashMap
                    .get(getCurrentLaneIndex());

            if (fragment != null) {
                if (fragment.configureOptionsMenu(inflater, menu) == false) {
                    inflater.inflate(defaultOptionsMenu.intValue(), menu);
                }
            } else {
                inflater.inflate(defaultOptionsMenu.intValue(), menu);
            }

            if (menu != null && App.getActionLauncherInstalled() == true) {
                MenuItem buyALP = menu.findItem(R.id.action_buy_alp);
                if (buyALP != null) {
                    buyALP.setVisible(false);
                }
            }

            configureActionBarSearchView(menu);
        }
        return true;
    }

    /*
	 *
	 */
    public Integer getDefaultOptionsMenu() {
        return R.menu.default_action_bar;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            if (composeReleaseFocus(false) == true) {
                return true;
            }
            break;

        case R.id.action_gallery: {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, Constant.REQUEST_CODE_IMAGE_PICKER);

            // startActivityForResult(new Intent(Intent.ACTION_PICK,
            // android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
            // Constant.REQUEST_CODE_IMAGE_PICKER);
            return true;
        }

        case R.id.action_camera: {

            if (Util.isIntentAvailable(this,
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE)) {
                Uri tmpUri = Uri
                        .fromFile(getFixedTempFile(BaseLaneActivity.this));
                Intent intent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, tmpUri);
                startActivityForResult(intent, Constant.REQUEST_CODE_CAMERA);
            }
            return true;
        }

        /*
         * case R.id.action_locate: Toast.makeText(getApplicationContext(),
         * getString(R.string.functionality_not_implemented),
         * Constant.DEFAULT_TOAST_DISPLAY_TIME).show(); break;
         */

        default: {
            BaseLaneFragment fragment = mLaneFragmentHashMap
                    .get(getCurrentLaneIndex());
            if (fragment != null) {
                return fragment.onOptionsItemSelected(item);
            }
        }
        }

        return false;
    }

    /*
	 *
	 */
    public boolean isComposing() {
        return mCurrentComposeFragment != null
                && mCurrentComposeFragment.hasFocus() ? true : false;
    }

    /*
	 *
	 */
    protected void configureActionBarSearchView(Menu menu) {

        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();

        searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        mSearchView.setOnQueryTextListener(this);

        OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                mCurrentComposeFragment.setIgnoreFocusChange(true);
                if (mComposeTweetView != null) {
                    mComposeTweetView.setVisibility(hasFocus ? View.GONE
                            : View.VISIBLE);
                }
                if (mComposeDirectMessageView != null) {
                    mComposeDirectMessageView
                            .setVisibility(hasFocus ? View.GONE : View.VISIBLE);
                }
                mCurrentComposeFragment.setIgnoreFocusChange(false);
            }

        };

        mSearchView.setOnQueryTextFocusChangeListener(onFocusChangeListener);
        mSearchView.setOnFocusChangeListener(onFocusChangeListener);
    }

    /*
	 *
	 */
    protected void finishCurrentActionMode() {
        if (mCurrentActionMode != null) {
            // This is messy, but to prevent a circular loop, clear
            // mCurrentActionMode before calling .finish()
            ActionMode curr = mCurrentActionMode;
            mCurrentActionMode = null;
            curr.finish();
        }
    }

    /*
	 *
	 */
    protected void setDirectMessageOtherUserScreenName(
            String otherUserScreenName) {
        if (mComposeDirectMessageFragment != null) {
            mComposeDirectMessageFragment
                    .setOtherUserScreenName(otherUserScreenName);
        }
    }

    /*
     * Override if necessary
     */
    protected ComposeTweetDefault getComposeTweetDefault() {

        if (mShareImagePath != null) {
            return new ComposeTweetDefault(null, null, null, mShareImagePath);
        }

        return null;
    }

    /*
	 *
	 */
    protected void setComposeTweetDefault(ComposeTweetDefault composeDefault) {
        if (mComposeTweetFragment != null) {
            mComposeTweetFragment.setComposeDefault(composeDefault);
        }
    }

    /*
	 *
	 */
    protected void setComposeTweetDefault() {
        setComposeTweetDefault(getComposeTweetDefault());
    }

    /*
	 *
	 */
    protected void setComposeDefault() {
        if (this.mCurrentComposeFragment == mComposeTweetFragment) {
            setComposeTweetDefault();
        }
    }

    /*
	 *
	 */
    protected void beginShareStatus(String initialStatus) {
        if (mComposeTweetFragment != null) {
            mComposeTweetFragment.beginShare(initialStatus);
        }
    }

    protected void beginShareImage(String imagePath) {
        mShareImagePath = imagePath;
        if (imagePath != null && mComposeTweetFragment != null) {
            mComposeTweetFragment.showCompose();
            mComposeTweetFragment.setMediaFilePath(imagePath);
            // mComposeTweet.setMediaFilePath(imagePath);
        }
    }

    public void beginCompose() {
        if (mCurrentComposeFragment != null) {
            mCurrentComposeFragment.showCompose();
        }
    }

    public void beginQuote(TwitterStatus statusToQuote) {
        if (mComposeTweetFragment != null) {
            mComposeTweetFragment.beginQuote(statusToQuote);
        }
    }

    public void retweetSelected(TwitterStatus status) {
        if (mComposeTweetFragment != null) {

            TwitterUser user = TwitterManager.get().getUser(status.mUserId);
            if (user != null && user.getProtected() == true) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        this);
                alertDialogBuilder
                        .setMessage(getString(R.string.alert_retweet_private));
                alertDialogBuilder.setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                alertDialogBuilder.create().show();
            } else if (mComposeTweetFragment != null) {
                mComposeTweetFragment.retweetSelected(status);
            }
        }
    }

    /*
	 *
	 */
    public void shareSelected(TwitterStatus status) {

        if (status != null) {

            final String statusUrl = status.getTwitterComStatusUrl();
            final String statusText = status.mStatus;
            final ArrayList<String> urls = Util.getUrlsInString(status.mStatus);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.alert_share_title));
            alertDialog.setMessage(getString(R.string.alert_share_message));
            alertDialog
                    .setIcon(AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark ? R.drawable.ic_action_share_dark
                            : R.drawable.ic_action_share_light);
            // TODO: The order these buttons are set looks wrong, but appears
            // correctly. Have to ensure this is consistent on other devices.
            alertDialog.setButton2(getString(R.string.share_tweet_link),
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            shareText(statusUrl);
                        }
                    });

            if (urls != null && urls.size() > 0) {
                alertDialog.setButton3(getString(R.string.share_tweet),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                    int which) {
                                shareText(statusText);
                            }
                        });

                alertDialog.setButton(
                        getString(urls.size() == 1 ? R.string.share_link
                                : R.string.share_first_link),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                    int which) {
                                shareText(urls.get(0));
                            }
                        });
            } else {
                alertDialog.setButton(getString(R.string.share_tweet),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                    int which) {
                                shareText(statusText);
                            }
                        });
            }

            alertDialog.show();
        }
    }

    /*
	 *
	 */
    private void shareText(String string) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, string);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }
}
