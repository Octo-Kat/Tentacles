/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.oct.tentacles;

import android.app.LauncherActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;

public class CreateShortcut extends LauncherActivity {

    @Override
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent(Intent.ACTION_MAIN, null);
        targetIntent.addCategory("com.oct.tentacles.SHORTCUT");
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);

        String intentClass = shortcutIntent.getComponent().getClassName();

        shortcutIntent = new Intent();
        shortcutIntent.setClass(getApplicationContext(), SettingsActivity.class);
        shortcutIntent.setAction("com.oct.tentacles.START_NEW_FRAGMENT");
        shortcutIntent.putExtra("tentacles_fragment_name", intentClass);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, getProperShortcutIcon(intentClass)));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, itemForPosition(position).label);
        setResult(RESULT_OK, intent);
        finish();
    }

    private int getProperShortcutIcon(String className) {
        String c = className.substring(className.lastIndexOf(".") + 1);

        if (c.equals("Buttons"))
            return R.drawable.ic_tentacles_button;
        else
            return R.mipmap.ic_launcher;
    }

    @Override
    protected boolean onEvaluateShowIcons() {
        return false;
    }
}
