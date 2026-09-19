/*
 * Copyright (C) 2026 The OSverflow Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.android.settings.gestures

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

/** Exposes the native Contextual Search setting on the button-order screen. */
class ButtonNavigationSettingsContextualSearchPreference :
    PreferenceMetadata, PreferenceAvailabilityProvider, PreferenceBinding {

    override val key = "search_gesture_press_hold"
    override val title = R.string.search_gesture_feature_title
    override val summary = R.string.search_gesture_feature_summary

    override fun isAvailable(context: Context) =
        NavigationSettingsContextualSearchController(context, key).isAvailable

    override fun createWidget(context: Context) =
        SwitchPreferenceCompat(context).apply {
            setOnPreferenceChangeListener { _, value ->
                value is Boolean &&
                    NavigationSettingsContextualSearchController(context, key).setChecked(value)
            }
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super<PreferenceBinding>.bind(preference, metadata)
        (preference as SwitchPreferenceCompat).isChecked =
            NavigationSettingsContextualSearchController(preference.context, key).isChecked
    }
}
