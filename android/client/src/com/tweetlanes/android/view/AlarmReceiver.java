package com.tweetlanes.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tweetlanes.android.App;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.ConsumerKeyConstants;
import com.tweetlanes.android.Notifier;
import com.tweetlanes.android.model.AccountDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.*;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Jason
 * Date: 4/8/13
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlarmReceiver extends BroadcastReceiver {

    final String SHARED_PREFERENCES_KEY_ACCOUNT_INDICES = "account_indices_key_v2";

    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            mContext = context;
            Log.d("AlarmReciever", "Woken up to check for messages");
            notifyNewMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    TwitterFetchStatusesFinishedCallback callback = new TwitterFetchStatusesFinishedCallback() {
        @Override
        public void finished(TwitterFetchResult result, TwitterStatuses feed, TwitterContentHandle contentHandle) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (feed != null && feed.getStatusCount() > 0) {
                int notificationId = contentHandle.getCurrentAccountKey().hashCode();
                String name = contentHandle.getScreenName();
                int count = feed.getStatusCount();

                long lastDisplayedMentionId = preferences.getLong(Notifier
                        .SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_DISPLAYED_MENTION_ID +
                        contentHandle.getCurrentAccountKey(), 0);

                TwitterStatus first = feed.getStatus(0);

                String fullDetail = "";
                if (first.mId > lastDisplayedMentionId) {
                    String detail = feed.getStatusCount() == 1 ? "@" + first.getAuthorScreenName() + ": " + first.mStatus
                            : "@" + name + " has " + count + " new " + "mentions";

                    for (int i = 0; i < feed.getStatusCount(); ++i) {
                        TwitterStatus status = feed.getStatus(i);
                        fullDetail += status.mStatus + "\n";
                    }
                    fullDetail = fullDetail.substring(0, fullDetail.length() - 1);

                    String noun = feed.getStatusCount() == 1 ? "mention" : "mention";
                    Notifier.notify("@" + name + ": " + count + " new " + noun, detail, fullDetail, true, notificationId,
                            contentHandle.getCurrentAccountKey(), feed.getStatus(0).mId, mContext);
                }

                Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), count, fullDetail);
            }
            else {
                Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), 0, "");
            }
        }
    };

    private void notifyNewMessage() {
        TwitterManager manager = TwitterManager.get();

        for (AccountDescriptor account : getAccounts(mContext)) {
            initSocialNetLib(account.getSocialNetType(), account.getAccountKey(), account.getOAuthToken(),
                    account.getOAuthSecret());

            TwitterContentHandleBase base = new TwitterContentHandleBase(
                    TwitterConstant.ContentType.STATUSES,
                    TwitterConstant.StatusesType.USER_MENTIONS);
            TwitterContentHandle contentHandle = new TwitterContentHandle(base, account.getScreenName(),
                    Long.valueOf(account.getId()).toString(), account.getAccountKey());

            TwitterPaging paging;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            long lastActionMentionId = preferences.getLong(Notifier
                    .SHARED_PREFERENCES_KEY_NOTIFICATION_LAST_ACTIONED_MENTION_ID  +
                    account.getAccountKey(), 0);

            if (lastActionMentionId == 0) {
                paging = TwitterPaging.createGetMostRecent();
            }
            else {
                paging = TwitterPaging.createGetNewer(lastActionMentionId);
            }

            TwitterManager.get().triggerFetchStatuses(contentHandle, paging, callback, 1);
        }

        if (manager != null) {
            TwitterManager.initModule(manager);
        }
    }

    private void initSocialNetLib(SocialNetConstant.Type socialNetType, String accountKey, String authToken,
            String authSecret) {
        TwitterManager.initModule(socialNetType,
                socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_KEY : ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_SECRET : ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET,
                authToken,
                authSecret,
                accountKey,
                mConnectionStatusCallbacks);
    }

    private ArrayList<AccountDescriptor> getAccounts(Context context) {
        ArrayList<AccountDescriptor> accounts = new ArrayList<AccountDescriptor>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountIndices = preferences.getString(SHARED_PREFERENCES_KEY_ACCOUNT_INDICES, null);

        if (accountIndices != null) {
            try {
                JSONArray jsonArray = new JSONArray(accountIndices);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Long id = jsonArray.getLong(i);

                    String key = App.getAccountDescriptorKey(id);
                    String jsonAsString = preferences.getString(key, null);
                    if (jsonAsString != null) {
                        AccountDescriptor account = new AccountDescriptor(context, jsonAsString);
                        accounts.add(account);
                    }
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return accounts;
    }

    ConnectionStatus.Callbacks mConnectionStatusCallbacks = new ConnectionStatus.Callbacks() {
        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public String getErrorMessageNoConnection() {
            return "No connection";
        }

        @Override
        public void handleError(TwitterFetchResult fetchResult) {
        }
    };
}
