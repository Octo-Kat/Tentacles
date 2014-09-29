/*
 * Copyright (C) 2012 The Carbon-Rom project
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

package com.oct.tentacles.fragments.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.oct.tentacles.R;
import com.oct.tentacles.preference.SettingsPreferenceFragment;

public class RecentsPanel extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "RecentsPanel";

    private static final String RECENT_MENU_CLEAR_ALL = "recent_menu_clear_all";
    private static final String RECENT_MENU_CLEAR_ALL_LOCATION = "recent_menu_clear_all_location";
    private static final String SHOW_RECENTS_MEMORY_INDICATOR = "show_recents_memory_indicator";
    private static final String RECENTS_MEMORY_INDICATOR_LOCATION =
            "recents_memory_indicator_location";

    private CheckBoxPreference mRecentClearAll;
    private ListPreference mRecentClearAllPosition;
    private CheckBoxPreference mShowRecentsMemoryIndicator;
    private ListPreference mRecentsMemoryIndicatorPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ui_recents_panel);
        PreferenceScreen prefSet = getPreferenceScreen();

        ContentResolver resolver = getActivity().getContentResolver();

        mRecentClearAll = (CheckBoxPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL);
        mRecentClearAll.setChecked(Settings.System.getInt(resolver,
            Settings.System.SHOW_CLEAR_RECENTS_BUTTON, 0) == 1);
        mRecentClearAll.setOnPreferenceChangeListener(this);
        mRecentClearAllPosition = (ListPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL_LOCATION);
        String recentClearAllPosition = Settings.System.getString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION);
        if (recentClearAllPosition != null) {
             mRecentClearAllPosition.setValue(recentClearAllPosition);
        }
        mRecentClearAllPosition.setOnPreferenceChangeListener(this);

        mShowRecentsMemoryIndicator = (CheckBoxPreference)
                prefSet.findPreference(SHOW_RECENTS_MEMORY_INDICATOR);
        mShowRecentsMemoryIndicator.setChecked(Settings.System.getInt(resolver,
                Settings.System.SHOW_RECENTS_MEMORY_INDICATOR, 0) == 1);
        mShowRecentsMemoryIndicator.setOnPreferenceChangeListener(this);
        mRecentsMemoryIndicatorPosition = (ListPreference) prefSet
                .findPreference(RECENTS_MEMORY_INDICATOR_LOCATION);
        String recentsMemoryIndicatorPosition = Settings.System.getString(
                resolver, Settings.System.RECENTS_MEMORY_INDICATOR_LOCATION);
        if (recentsMemoryIndicatorPosition != null) {
            mRecentsMemoryIndicatorPosition.setValue(recentsMemoryIndicatorPosition);
        }
        mRecentsMemoryIndicatorPosition.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentClearAll) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.SHOW_CLEAR_RECENTS_BUTTON, value ? 1 : 0);
        } else if (preference == mRecentClearAllPosition) {
            String value = (String) objValue;
            Settings.System.putString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, value);
        } else if (preference == mShowRecentsMemoryIndicator) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(
                    resolver, Settings.System.SHOW_RECENTS_MEMORY_INDICATOR, value ? 1 : 0);
        } else if (preference == mRecentsMemoryIndicatorPosition) {
            String value = (String) objValue;
            Settings.System.putString(
                    resolver, Settings.System.RECENTS_MEMORY_INDICATOR_LOCATION, value);
        } else {
            return false;
        }

        return true;

    }
}
