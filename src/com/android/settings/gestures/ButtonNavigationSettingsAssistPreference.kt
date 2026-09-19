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

/** Keeps the legacy assistant control available on the button-order screen. */
class ButtonNavigationSettingsAssistPreference :
    PreferenceMetadata, PreferenceAvailabilityProvider, PreferenceBinding {

    override val key = "assistant_long_press_home_gesture"
    override val title = R.string.assistant_long_press_home_gesture_title
    override val summary = R.string.assistant_long_press_home_gesture_summary

    override fun isAvailable(context: Context) =
        ButtonNavigationSettingsAssistController(context, key).isAvailable

    override fun createWidget(context: Context) =
        SwitchPreferenceCompat(context).apply {
            setOnPreferenceChangeListener { _, value ->
                value is Boolean &&
                    ButtonNavigationSettingsAssistController(context, key).setChecked(value)
            }
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super<PreferenceBinding>.bind(preference, metadata)
        (preference as SwitchPreferenceCompat).isChecked =
            ButtonNavigationSettingsAssistController(preference.context, key).isChecked
    }
}
