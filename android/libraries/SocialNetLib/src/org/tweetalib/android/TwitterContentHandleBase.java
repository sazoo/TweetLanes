/*
 * Copyright (C) 2013 Chris Lacy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.tweetalib.android;

import java.io.Serializable;

import org.tweetalib.android.TwitterConstant.ContentType;
import org.tweetalib.android.TwitterConstant.DirectMessagesType;
import org.tweetalib.android.TwitterConstant.StatusType;
import org.tweetalib.android.TwitterConstant.StatusesType;
import org.tweetalib.android.TwitterConstant.UsersType;

public class TwitterContentHandleBase implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 4132026070249216175L;

    public TwitterContentHandleBase(TwitterContentHandleBase other) {
        mContentType = other.mContentType;
        mDirectMessagesType = other.mDirectMessagesType;
        mStatusType = other.mStatusType;
        mStatusesType = other.mStatusesType;
        mUsersType = other.mUsersType;
    }

    public TwitterContentHandleBase(ContentType contentType,
            DirectMessagesType directMessagesType) {
        this(contentType, directMessagesType, null, null, null);
    }

    public TwitterContentHandleBase(ContentType contentType) {
        this(contentType, null, null, null, null);
    }

    public TwitterContentHandleBase(ContentType contentType,
            StatusType statusType) {
        this(contentType, null, statusType, null, null);
    }

    public TwitterContentHandleBase(ContentType contentType,
            StatusesType statusesType) {
        this(contentType, null, null, statusesType, null);
    }

    public TwitterContentHandleBase(ContentType contentType, UsersType usersType) {
        this(contentType, null, null, null, usersType);
    }

    public TwitterContentHandleBase(ContentType contentType,
            DirectMessagesType directMessagesType, StatusType statusType,
            StatusesType statusesType, UsersType usersType) {
        mContentType = contentType;
        mDirectMessagesType = directMessagesType;
        mStatusType = statusType;
        mStatusesType = statusesType;
        mUsersType = usersType;
    }

    public String getTypeAsString() {
        String result = mContentType.toString();
        if (mDirectMessagesType != null) {
            result += "_" + mDirectMessagesType.toString();
        }
        if (mStatusType != null) {
            result += "_" + mStatusType.toString();
        }
        if (mStatusesType != null) {
            result += "_" + mStatusesType.toString();
        }
        if (mUsersType != null) {
            result += "_" + mUsersType;
        }
        return result;
    }

    public ContentType getContentType() {
        return mContentType;
    }

    public DirectMessagesType getDirectMessagesType() {
        return mDirectMessagesType;
    }

    public StatusType getStatusType() {
        return mStatusType;
    }

    public StatusesType getStatusesType() {
        return mStatusesType;
    }

    public UsersType getUsersType() {
        return mUsersType;
    }

    protected ContentType mContentType;
    protected DirectMessagesType mDirectMessagesType;
    protected StatusType mStatusType;
    protected StatusesType mStatusesType;
    protected UsersType mUsersType;
}
