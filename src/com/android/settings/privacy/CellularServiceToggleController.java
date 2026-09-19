/*
 * SPDX-FileCopyrightText: 2026 The OSverflow Project
 * SPDX-License-Identifier: Apache-2.0
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

package com.android.settings.privacy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** Controls OSverflow's device-wide cellular call and SMS access gates. */
public final class CellularServiceToggleController extends TogglePreferenceController {
    private static final String KEY_CALL_ACCESS = "privacy_call_access";
    private static final String KEY_SMS_ACCESS = "privacy_sms_access";
    private static final String SETTING_CALL_ACCESS = "osverflow_call_access";
    private static final String SETTING_SMS_ACCESS = "osverflow_sms_access";

    private final String mSetting;

    public CellularServiceToggleController(Context context, String key) {
        super(context, key);
        if (KEY_CALL_ACCESS.equals(key)) {
            mSetting = SETTING_CALL_ACCESS;
        } else if (KEY_SMS_ACCESS.equals(key)) {
            mSetting = SETTING_SMS_ACCESS;
        } else {
            throw new IllegalArgumentException("Unknown cellular service toggle: " + key);
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(), mSetting, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(), mSetting, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }
}
