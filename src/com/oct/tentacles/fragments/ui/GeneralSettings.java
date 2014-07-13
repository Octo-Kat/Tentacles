/*
 * Copyright (C) 2014 The CarbonRom project
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

import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.oct.tentacles.R;
import com.oct.tentacles.Utils;
import com.oct.tentacles.preference.SettingsPreferenceFragment;

public class GeneralSettings extends SettingsPreferenceFragment {
    private static final String TAG = "GeneralSettings";

    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
	
    private static final String KERNELTWEAKER_START = "kerneltweaker_start";
	
    // Package name of the kernel tweaker app
    public static final String KERNELTWEAKER_PACKAGE_NAME = "com.dsht.kerneltweaker";
    // Intent for launching the kernel tweaker main actvity
    public static Intent INTENT_KERNELTWEAKER = new Intent(Intent.ACTION_MAIN)
            .setClassName(KERNELTWEAKER_PACKAGE_NAME, KERNELTWEAKER_PACKAGE_NAME + ".MainActivity");

    private CheckBoxPreference mVolumeAdustSound;

    private Preference mKernelTweaker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ui_general_settings);
		
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mVolumeAdustSound = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdustSound.setPersistent(false);
        mVolumeAdustSound.setChecked(Settings.System.getInt(resolver,
        Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED, 1) == 1);

        mKernelTweaker = (Preference)
                prefSet.findPreference(KERNELTWEAKER_START);
    }
	

    @Override
    public void onResume() {
        super.onResume();
    }
	
    @Override
    public void onPause() {
        super.onPause();
    }
	
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mKernelTweaker){
            startActivity(INTENT_KERNELTWEAKER);
        } else if (preference == mVolumeAdustSound) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                    mVolumeAdustSound.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
