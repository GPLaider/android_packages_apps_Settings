/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import static android.app.contextualsearch.ContextualSearchManager.FEATURE_CONTEXTUAL_SEARCH;

import android.content.Context;
import android.provider.Settings;
import android.os.UserHandle;

import lineageos.providers.LineageSettings;
import org.lineageos.internal.util.DeviceKeysConstants.Action;

import androidx.annotation.NonNull;

import com.android.settings.core.TogglePreferenceController;

/**
 * Configures behaviour of Contextual Search setting.
 */
public class NavigationSettingsContextualSearchController extends TogglePreferenceController {

    public NavigationSettingsContextualSearchController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        // A custom Lineage HOME action can bypass the contextual-search invocation entirely.
        // Report that state honestly; only an explicit enable changes the user's assignment.
        if (usesHomeButton() && homeAction() != Action.SEARCH.ordinal()) {
            return false;
        }
        boolean onByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_searchAllEntrypointsEnabledDefault);
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, onByDefault ? 1 : 0)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final int previousAction = homeAction();
        final boolean changeHomeAction = isChecked && usesHomeButton()
                && previousAction != Action.SEARCH.ordinal();
        if (changeHomeAction && !LineageSettings.System.putIntForUser(
                mContext.getContentResolver(), LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                Action.SEARCH.ordinal(), UserHandle.USER_CURRENT)) {
            return false;
        }
        final boolean saved = Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, isChecked ? 1 : 0);
        if (!saved && changeHomeAction) {
            LineageSettings.System.putIntForUser(mContext.getContentResolver(),
                    LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION, previousAction,
                    UserHandle.USER_CURRENT);
        }
        return saved;
    }

    private boolean usesHomeButton() {
        return mContext.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode) != 2;
    }

    private int homeAction() {
        final int defaultAction = mContext.getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnHomeBehavior);
        return LineageSettings.System.getIntForUser(mContext.getContentResolver(),
                LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION, defaultAction,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getPackageManager().hasSystemFeature(FEATURE_CONTEXTUAL_SEARCH)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}
