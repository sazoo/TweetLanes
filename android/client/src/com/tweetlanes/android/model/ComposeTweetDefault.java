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

package com.tweetlanes.android.model;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterUsers;

public class ComposeTweetDefault {

    final boolean USE_DEFAULT_STATUS_HACK = false;

    String mUserScreenName;
    String mStatus;
    String mInitialStatus;
    boolean mInitialStatusIsPlaceholder;
    Long mInReplyToStatusId;
    String mMediaFilePath;

    public static final String KEY_USER_SCREEN_NAME = "userScreenName";
    public static final String KEY_STATUS = "status";
    public static final String KEY_IN_REPLY_TO_STATUS_ID = "inReplyToStatusId";
    public static final String KEY_MEDIA_FILE_PATH = "mediaFilePath";

    /*
	 * 
	 */
    public String toString() {
        JSONObject object = new JSONObject();
        try {
            if (mUserScreenName != null) {
                object.put(KEY_USER_SCREEN_NAME, mUserScreenName);
            }
            if (mStatus != null) {
                object.put(KEY_STATUS, mStatus);
            }
            if (mInReplyToStatusId != null) {
                object.put(KEY_IN_REPLY_TO_STATUS_ID, mInReplyToStatusId);
            }
            if (mMediaFilePath != null) {
                object.put(KEY_MEDIA_FILE_PATH, mMediaFilePath);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.length() > 0 ? object.toString() : "";
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String jsonAsString) {

        try {
            JSONObject object = new JSONObject(jsonAsString);
            if (object.has(KEY_USER_SCREEN_NAME)) {
                mUserScreenName = object.getString(KEY_USER_SCREEN_NAME);
            }
            if (object.has(KEY_STATUS)) {
                mStatus = object.getString(KEY_STATUS);
            }
            if (object.has(KEY_IN_REPLY_TO_STATUS_ID)) {
                mInReplyToStatusId = object.getLong(KEY_IN_REPLY_TO_STATUS_ID);
            }
            if (object.has(KEY_MEDIA_FILE_PATH)) {
                mMediaFilePath = object.getString(KEY_MEDIA_FILE_PATH);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String userScreenName, String defaultStatus,
            Long inReplyToStatusId, String mediaFilePath) {

        mUserScreenName = userScreenName;
        mStatus = defaultStatus;
        mInitialStatus = defaultStatus;
        mInitialStatusIsPlaceholder = false;
        mInReplyToStatusId = inReplyToStatusId;
        mMediaFilePath = mediaFilePath;
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String userScreenName,
            TwitterStatuses inReplyToStatusList) {

        this(
                userScreenName,
                getReplyToUserNamesAsString(userScreenName, inReplyToStatusList),
                (inReplyToStatusList != null
                        && inReplyToStatusList.getStatusCount() > 0 ? inReplyToStatusList
                        .getStatus(0).mId : null), null);
        mInitialStatusIsPlaceholder = true;
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String userScreenName,
            TwitterUsers inReplyToUserList) {

        this(userScreenName, getTwitterUsersAsString(inReplyToUserList), null,
                null);
        mInitialStatusIsPlaceholder = true;
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String userScreenName, String defaultStatus) {
        this(userScreenName, defaultStatus, null);
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(String userScreenName, String defaultStatus,
            String mediaFilePath) {
        this(userScreenName, defaultStatus, null, mediaFilePath);
    }

    /*
	 * 
	 */
    public ComposeTweetDefault() {
        this(null, null, null, null);
    }

    /*
	 * 
	 */
    public ComposeTweetDefault(ComposeTweetDefault other) {

        mUserScreenName = other.mUserScreenName;
        mStatus = other.mStatus;
        mInitialStatus = other.mInitialStatus;
        mInitialStatusIsPlaceholder = other.mInitialStatusIsPlaceholder;
        mInReplyToStatusId = other.mInReplyToStatusId;
        mMediaFilePath = other.mMediaFilePath;
    }

    public String getMediaFilePath() {
        return mMediaFilePath;
    }

    public void setMediaFilePath(String filePath) {
        mMediaFilePath = filePath;
    }

    public Long getInReplyToStatusId() {
        return mInReplyToStatusId;
    }

    /*
	 * 
	 */
    @SuppressWarnings("unused")
    public String getStatus() {

        String result = null;
        if (USE_DEFAULT_STATUS_HACK == true) {
            result = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque sed quam dolor";
        } else if (mStatus != null) {
            result = mStatus;
        }
        return result;
    }

    /*
	 * 
	 */
    public void updateStatus(String status) {
        mStatus = new String(status);
    }

    /*
	 * 
	 */
    public boolean isPlaceholderStatus() {
        if (mInitialStatus != null && mStatus != null
                && mInitialStatusIsPlaceholder == true
                && mInitialStatus.equals(mStatus) == true) {
            return true;
        }

        return false;
    }

    /*
	 * 
	 */
    private static String getReplyToUserNamesAsString(String userScreenName,
            TwitterStatuses inReplyToStatusList) {
        String replyingToUsers = "";
        Set<String> screenNameSet = new HashSet<String>();

        // Note: There are 2 for loops here so that we cleanly handle the case
        // where a user replies to their own tweet.

        for (int i = 0; i < inReplyToStatusList.getStatusCount(); i++) {
            TwitterStatus status = inReplyToStatusList.getStatus(i);
            String author = status.getAuthorScreenName();
            if (screenNameSet.contains(author.toLowerCase()) == false) {
                screenNameSet.add(author.toLowerCase());
                replyingToUsers += "@" + author + " ";
            }

            if (status.mIsRetweet) {
                String tweeter = status.mUserScreenName;
                if (screenNameSet.contains(tweeter.toLowerCase()) == false) {
                    screenNameSet.add(tweeter.toLowerCase());
                    replyingToUsers += "@" + tweeter + " ";
                }
            }
        }

        if (userScreenName != null) {
            screenNameSet.add(userScreenName.toLowerCase());
        }

        for (int i = 0; i < inReplyToStatusList.getStatusCount(); i++) {
            TwitterStatus status = inReplyToStatusList.getStatus(i);
            String[] userMentions = status.mUserMentions;
            if (userMentions != null) {
                for (String screenName : userMentions) {
                    if (screenNameSet.contains(screenName.toLowerCase()) == false) {
                        screenNameSet.add(screenName.toLowerCase());
                        replyingToUsers += "@" + screenName + " ";
                    }
                }
            }
        }

        return replyingToUsers;
    }

    /*
	 * 
	 */
    private static String getTwitterUsersAsString(TwitterUsers inReplyToUserList) {
        String result = null;
        if (inReplyToUserList.getUserCount() > 0) {
            result = "";
            for (int i = 0; i < inReplyToUserList.getUserCount(); i++) {
                result += "@" + inReplyToUserList.getUser(i).getScreenName()
                        + " ";
            }
        }

        return result;
    }

}
