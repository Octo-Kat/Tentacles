/*
 * Copyright (C) 2014 Team OctOS
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;

import com.oct.tentacles.R;
import com.oct.tentacles.preference.SettingsPreferenceFragment;

public class AppSwitcher extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AppSwitcher";


    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";
    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");


    private CheckBoxPreference mRecentClearAll;
    private ListPreference mRecentClearAllPosition;
    private CheckBoxPreference mShowRecentsMemoryIndicator;
    private ListPreference mRecentsMemoryIndicatorPosition;
    private CheckBoxPreference mRecentsUseOmniSwitch;
    private Preference mOmniSwitchSettings;
    private boolean mOmniSwitchInitCalled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_switcher);
        PreferenceScreen prefSet = getPreferenceScreen();

        ContentResolver resolver = getActivity().getContentResolver();

        mRecentsUseOmniSwitch = (CheckBoxPreference)
                prefSet.findPreference(RECENTS_USE_OMNISWITCH);

        try {
            mRecentsUseOmniSwitch.setChecked(Settings.System.getInt(resolver,
                    Settings.System.RECENTS_USE_OMNISWITCH) == 1);
            mOmniSwitchInitCalled = true;
        } catch(SettingNotFoundException e){
            // if the settings value is unset
        }
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);

        mOmniSwitchSettings = (Preference)
                prefSet.findPreference(OMNISWITCH_START_SETTINGS);
        mOmniSwitchSettings.setEnabled(mRecentsUseOmniSwitch.isChecked());
    }
    

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mOmniSwitchSettings){
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentsUseOmniSwitch) {
            boolean value = (Boolean) objValue;
            if (value && !isOmniSwitchInstalled()){
                openOmniSwitchNotInstalledWarning();
                return false;
            }

            // if value has never been set before
            if (value && !mOmniSwitchInitCalled){
                openOmniSwitchFirstTimeWarning();
                mOmniSwitchInitCalled = true;
            }

            Settings.System.putInt(
                    resolver, Settings.System.RECENTS_USE_OMNISWITCH, value ? 1 : 0);
            mOmniSwitchSettings.setEnabled(value && isOmniSwitchInstalled());
            mOmniSwitchSettings.setSummary(isOmniSwitchInstalled() ?
                    getResources().getString(R.string.omniswitch_start_settings_summary) :
                    getResources().getString(R.string.omniswitch_not_installed_summary));
        } else {
            return false;
        }

        return true;

    }
    

    private void openOmniSwitchNotInstalledWarning() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.omniswitch_not_installed_title))
                .setMessage(getResources().getString(R.string.omniswitch_not_installed_message))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                }).show();
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
                .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                }).show();
    }

    private boolean isOmniSwitchInstalled() {
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(OMNISWITCH_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    
   @Override
    public void onResume() {
        super.onResume();
    }
}
