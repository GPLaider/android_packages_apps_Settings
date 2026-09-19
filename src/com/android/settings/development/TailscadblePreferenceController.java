/*
 * Copyright (C) 2026 Tailscadble contributors
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
package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** Records the system user's explicit request for Tailscadble. */
public class TailscadblePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin,
        LifecycleObserver, OnResume, OnPause {
    static final String SETTING = "tailscadble_enabled";

    private static final String KEY = "toggle_tailscadble";
    private static final String TAILSCALE_PACKAGE = "com.tailscale.ipn";

    private final ContentResolver mContentResolver;
    private final UserManager mUserManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mObserverRegistered;

    private final ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    };

    public TailscadblePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContentResolver = context.getContentResolver();
        mUserManager = context.getSystemService(UserManager.class);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return mUserManager != null
                && mUserManager.isSystemUser()
                && !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        if (mPreference != null) {
            mPreference.setEnabled(isAvailable());
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContentResolver, SETTING, AdbPreferenceController.ADB_SETTING_OFF);
        if (mPreference != null) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        if (!mObserverRegistered) {
            mContentResolver.registerContentObserver(
                    Settings.Global.getUriFor(SETTING), false, mObserver);
            mContentResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, mObserver);
            mContentResolver.registerContentObserver(Settings.Global.getUriFor(
                    TailscadbleTransportPreferenceController.SETTING), false, mObserver);
            mObserverRegistered = true;
        }
        updateState(mPreference);
    }

    @Override
    public void onPause() {
        if (mObserverRegistered) {
            mContentResolver.unregisterContentObserver(mObserver);
            mObserverRegistered = false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        final boolean requested = Settings.Global.getInt(
                mContentResolver, SETTING, AdbPreferenceController.ADB_SETTING_OFF)
                != AdbPreferenceController.ADB_SETTING_OFF;
        ((PrimarySwitchPreference) preference).setChecked(requested);
        preference.setSummary(!requested
                ? R.string.tailscadble_summary_off
                : !isAdbEnabled() ? R.string.tailscadble_summary_adb_required
                        : isTailcatMode() ? R.string.tailscadble_summary_tailcat
                                : R.string.tailscadble_summary_armed);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enable = (Boolean) newValue;
        if (!enable) {
            Settings.Global.putInt(
                    mContentResolver, SETTING, AdbPreferenceController.ADB_SETTING_OFF);
            return true;
        }
        if (!isAvailable() || !isAdbEnabled()) {
            Toast.makeText(mContext, R.string.tailscadble_error_adb_required, Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        final String transport = TailscadbleTransportPreferenceController.getTransport(mContext);
        if (!TailscadbleTransportPreferenceController.TAILCAT.equals(transport)
                && !TailscadbleTransportPreferenceController.TAILSCALE.equals(transport)) {
            Toast.makeText(mContext, R.string.tailscadble_transport_invalid, Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        if (!isTailcatMode() && !isTailscaleInstalled()) {
            Toast.makeText(
                    mContext, R.string.tailscadble_error_tailscale_required, Toast.LENGTH_LONG)
                    .show();
            return false;
        }

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.tailscadble_warning_title)
                .setMessage(isTailcatMode() ? R.string.tailscadble_tailcat_warning_message
                        : R.string.tailscadble_warning_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Do not apply stale consent after a user/policy/transport change.
                    if (!isAvailable() || !isAdbEnabled()
                            || !transport.equals(TailscadbleTransportPreferenceController
                                    .getTransport(mContext))) {
                        updateState(mPreference);
                        return;
                    }
                    Settings.Global.putInt(
                            mContentResolver, SETTING, AdbPreferenceController.ADB_SETTING_ON);
                    updateState(mPreference);
                })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> updateState(mPreference))
                .show();
        return false;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY.equals(preference.getKey()) || !isAvailable()) return false;
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.tailscadble_connection_help_title)
                .setMessage(isTailcatMode() ? R.string.tailscadble_tailcat_help
                        : R.string.tailscadble_tailscale_help)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        return true;
    }

    private boolean isTailcatMode() {
        return TailscadbleTransportPreferenceController.TAILCAT.equals(
                TailscadbleTransportPreferenceController.getTransport(mContext));
    }

    private boolean isAdbEnabled() {
        return Settings.Global.getInt(
                mContentResolver,
                Settings.Global.ADB_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF)
                != AdbPreferenceController.ADB_SETTING_OFF;
    }

    private boolean isTailscaleInstalled() {
        try {
            mContext.getPackageManager().getApplicationInfo(TAILSCALE_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
