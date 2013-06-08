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

import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUser;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterUser;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.tweetlanes.android.R;
import com.tweetlanes.android.model.ComposeTweetDefault;
import com.tweetlanes.android.model.LaneDescriptor;
import com.tweetlanes.android.widget.viewpagerindicator.TitleProvider;

public class ProfileActivity extends BaseLaneActivity {

    ProfileAdapter mProfileAdapter;
    ViewSwitcher mViewSwitcher;
    TwitterUser mUser;
    String mScreenName;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mScreenName = getIntent().getStringExtra("userScreenName");
        if (mScreenName == null) {
            Uri data = getIntent().getData();
            mScreenName = data.toString()
                    .replace("com.tweetlanes.android.profile://", "")
                    .replace("@", "");
        }

        if (mScreenName == null) {
            restartApp();
            return;
        }

        TwitterFetchUser.FinishedCallback callback = TwitterManager.get()
                .getFetchUserInstance().new FinishedCallback() {

            public void finished(TwitterFetchResult result, TwitterUser user) {
                if (result.isSuccessful()) {
                    mUser = user;
                    updateViewVisibility();
                } else {
                    // TODO: Handle this properly
                    finish();
                }
            }

        };

        boolean requestedUser = false;
        String profileIdAsString = getIntent().getStringExtra("userId");

        Long mappedProfileId = TwitterManager
                .getUserIdFromScreenName(mScreenName);
        if (mappedProfileId != null) {
            mUser = TwitterManager.get().getUser(mappedProfileId, callback);
            requestedUser = true;
        } else if (profileIdAsString != null) {
            long profileId = Long.parseLong(profileIdAsString);
            if (profileId > 0) {
                mUser = TwitterManager.get().getUser(profileId, callback);
                requestedUser = true;
            }
        }
        if (requestedUser == false) {
            mUser = TwitterManager.get().getUser(mScreenName, callback);
        }

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.rootViewSwitcher);
        updateViewVisibility();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.view.BaseLaneActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        // By clearing this variable and checking whether it's valid in
        // updateViewVisibility(), an exception is averted whe nthe Activity is
        // destroyed, yet a callback is still initiated from a Twitter fetch
        // operation. A better solution is needed here, but this works for now.
        mProfileAdapter = null;

        super.onDestroy();
    }

    /*
	 *
	 */
    @Override
    protected ComposeTweetDefault getComposeTweetDefault() {
        return new ComposeTweetDefault(getApp().getCurrentAccountScreenName(),
                "@" + mScreenName + " ", null, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#getAdapterForViewPager()
     */
    @Override
    protected PagerAdapter getAdapterForViewPager() {
        if (mProfileAdapter == null) {
            mProfileAdapter = new ProfileAdapter(getSupportFragmentManager());
        }
        return mProfileAdapter;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#getFragmentStatePagerAdapter
     * ()
     */
    @Override
    protected FragmentStatePagerAdapter getFragmentStatePagerAdapter() {
        return mProfileAdapter;
    }

    /*
	 *
	 */
    private void updateViewVisibility() {

        configureActionBarView();

        mViewSwitcher.reset();

        if (mUser == null) {
            mViewSwitcher.setDisplayedChild(0);
        } else {
            mViewSwitcher.setDisplayedChild(1);
            // Will be NULL if the callback was called after the Activity was
            // released
            if (mProfileAdapter != null) {
                mProfileAdapter.notifyDataSetChanged();
            }
        }
    }

    /*
	 *
	 */
    @Override
    public boolean configureOptionsMenu(Menu menu) {
        super.configureOptionsMenu(menu);

        return configureActionBarView();
    }

    /*
	 *
	 */
    boolean configureActionBarView() {

        if (mScreenName != null) {

            ActionBar actionBar = getActionBar();
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            LayoutInflater inflator = (LayoutInflater) this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            int layout = R.layout.profile_title_thin;
            /*
             * // TODO: This is messy, and likely won't work for large screen
             * devices. Need to come up with a better solution int layout; if
             * (getResources().getConfiguration().orientation ==
             * Configuration.ORIENTATION_LANDSCAPE) { layout=
             * R.layout.profile_title_thin; } else { layout =
             * R.layout.profile_title; }
             */

            View profileTitleView = inflator.inflate(layout, null);
            ((TextView) profileTitleView.findViewById(R.id.screenname))
                    .setText("@" + mScreenName);

            TextView fullNameTextView = (TextView) profileTitleView
                    .findViewById(R.id.fullname);
            if (fullNameTextView != null && mUser != null) {
                fullNameTextView.setText(mUser.getName());
            }

            ImageView verifiedImage = (ImageView) profileTitleView
                    .findViewById(R.id.verifiedImage);
            verifiedImage
                    .setVisibility(mUser != null && mUser.getVerified() ? View.VISIBLE
                            : View.GONE);

            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(profileTitleView);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.tweetlanes.android.view.BaseLaneActivity#onOptionsItemSelected(android
     * .view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return false;
        }
    }

    /*
	 *
	 */
    class ProfileAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

        public ProfileAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment result = null;
            if (mUser != null) {
                LaneDescriptor laneDescriptor = getApp()
                        .getProfileLaneDescriptor(position);

                switch (laneDescriptor.getLaneType()) {
                case PROFILE_PROFILE:
                    result = ProfileFragment.newInstance(position,
                            mUser.getId());
                    break;

                case PROFILE_PROFILE_TIMELINE:
                case PROFILE_MENTIONS:
                case PROFILE_FAVORITES:
                    result = TweetFeedFragment
                            .newInstance(position,
                                    laneDescriptor.getContentHandleBase(),
                                    mUser.getScreenName(),
                                    Long.toString(mUser.getId()),
                                    getApp().getCurrentAccountKey());
                    break;

                default:
                    result = PlaceholderPagerFragment.newInstance(position,
                            laneDescriptor.getLaneTitle(), position);
                    break;
                }
            } else {
                result = LoadingFragment.newInstance(position);
            }
            return result;
        }

        @Override
        public int getCount() {
            return getApp().getProfileLaneDefinitions().size();
        }

        @Override
        public String getTitle(int position) {
            return getApp().getProfileLaneDescriptor(position).getLaneTitle()
                    .toUpperCase();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}
