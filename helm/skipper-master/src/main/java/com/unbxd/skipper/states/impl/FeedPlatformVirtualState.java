package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;

import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.skipper.states.model.ServeStateType.PIM_UPLOAD_START;

public abstract class FeedPlatformVirtualState extends VirtualServeState {

    protected static final ServeStateType[] prevStatesForPlugin = {API_UPLOAD, API_SELECT,
            SITE_CREATED, PLATFORM_UPLOAD, PLATFORM_SELECT, PIM_UPLOAD_START, PIM_SELECT,
            FILE_FEED_UPLOAD, FILE_FEED_SELECT, FILE_FEED_UPLOAD_ERROR,
    };

}

