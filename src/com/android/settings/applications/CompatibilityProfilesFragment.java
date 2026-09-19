/*
 * SPDX-FileCopyrightText: 2026 The OSverflow Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applications;

import android.app.settings.SettingsEnums;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.ext.compat.CompatibilityProfileConfig;
import android.ext.compat.CompatibilityProfileConfig.Applicability;
import android.ext.compat.CompatibilityProfileConfig.Profile;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/** Read-only status for AVB-protected, package-scoped PlatformCompat profiles. */
public class CompatibilityProfilesFragment extends SettingsPreferenceFragment {
    private static final String TAG = "CompatibilityProfiles";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(requireContext()));
        requireActivity().setTitle(R.string.compatibility_profiles_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<Profile> profiles;
        try {
            profiles = CompatibilityProfileConfig.loadSystem();
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Unable to read compatibility profiles", e);
            addStatusPreference(R.string.compatibility_profiles_invalid);
            return;
        }
        if (profiles.isEmpty()) {
            addStatusPreference(R.string.compatibility_profiles_empty);
            return;
        }

        PackageManager pm = requireContext().getPackageManager();
        long now = System.currentTimeMillis();
        for (Profile profile : profiles) {
            Preference preference = new Preference(getPrefContext());
            preference.setSelectable(false);
            preference.setTitle(getApplicationLabel(pm, profile.packageName()));
            preference.setSummary(getString(R.string.compatibility_profile_summary,
                    getApplicabilityLabel(profile.getApplicability(pm, now)),
                    getSecurityImpactLabel(profile.securityImpact()),
                    DateFormat.getDateFormat(requireContext())
                            .format(new Date(profile.expiresAtMillis())),
                    profile.reason(), profile.packageName()));
            screen.addPreference(preference);
        }
    }

    private void addStatusPreference(int summaryRes) {
        Preference preference = new Preference(getPrefContext());
        preference.setSelectable(false);
        preference.setSummary(summaryRes);
        getPreferenceScreen().addPreference(preference);
    }

    private CharSequence getApplicationLabel(PackageManager pm, String packageName) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName,
                    PackageManager.MATCH_ANY_USER);
            return pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private String getApplicabilityLabel(Applicability applicability) {
        return getString(switch (applicability) {
            case ACTIVE -> R.string.compatibility_profile_status_active;
            case RETIRED -> R.string.compatibility_profile_status_retired;
            case EXPIRED -> R.string.compatibility_profile_status_expired;
            case NOT_INSTALLED -> R.string.compatibility_profile_status_not_installed;
            case SIGNER_MISMATCH -> R.string.compatibility_profile_status_signer_mismatch;
            case VERSION_MISMATCH -> R.string.compatibility_profile_status_version_mismatch;
        });
    }

    private String getSecurityImpactLabel(
            CompatibilityProfileConfig.SecurityImpact securityImpact) {
        return getString(switch (securityImpact) {
            case NONE -> R.string.compatibility_profile_impact_none;
            case REDUCES_APP_SECURITY -> R.string.compatibility_profile_impact_app;
            case REDUCES_PLATFORM_SECURITY -> R.string.compatibility_profile_impact_platform;
        });
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_APPLICATIONS;
    }
}
