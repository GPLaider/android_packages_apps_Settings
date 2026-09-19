/*
 * Copyright (C) 2026 Tailscadble contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** Selects the path without carrying an existing debugging grant across paths. */
public class TailscadbleTransportPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String SETTING = "tailscadble_transport";
    static final String TAILSCALE = "tailscale";
    static final String TAILCAT = "tailcat";
    private static final String KEY = "tailscadble_transport";

    public TailscadbleTransportPreferenceController(Context context) {
        super(context);
    }

    static String getTransport(Context context) {
        final String value = Settings.Global.getString(context.getContentResolver(), SETTING);
        return value == null ? TAILSCALE : value;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        final UserManager users = mContext.getSystemService(UserManager.class);
        return users != null && users.isSystemUser()
                && !users.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) return;
        final ListPreference list = (ListPreference) preference;
        final String transport = getTransport(mContext);
        list.setValue(transport);
        list.setSummary(TAILCAT.equals(transport) ? R.string.tailscadble_transport_tailcat_summary
                : TAILSCALE.equals(transport) ? R.string.tailscadble_transport_tailscale_summary
                        : R.string.tailscadble_transport_invalid);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (!isAvailable() || !(TAILSCALE.equals(value) || TAILCAT.equals(value))) return false;
        if (value.equals(getTransport(mContext))) return true;
        // The backend also closes on every transport change, including changes outside Settings.
        if (!Settings.Global.putInt(mContext.getContentResolver(),
                TailscadblePreferenceController.SETTING, 0)) return false;
        final boolean saved = Settings.Global.putString(
                mContext.getContentResolver(), SETTING, (String) value);
        updateState(preference);
        return saved;
    }
}
