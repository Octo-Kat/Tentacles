/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oct.tentacles.fragments.sb;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.cm.QSConstants;
import com.oct.tentacles.R;
import com.oct.tentacles.Utils;
import com.oct.tentacles.fragments.sb.QuickSettingsUtil.TileInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class QuickSettingsTiles extends Fragment {

    private static final int MENU_RESET = Menu.FIRST;

    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

    private static final int DLG_RESET         = 0;
    private static final int DLG_SCREENSHOT_DELAY = 1;
    private static final int DLG_SCREENTIMEOUT = 2;
    private static final int DLG_NETWORK_MODE  = 3;
    private static final int DLG_RINGER        = 4;
    private static final int DLG_MUSIC         = 5;

    private DraggableGridView mDragView;
    private ViewGroup mContainer;
    private LayoutInflater mInflater;
    private Resources mSystemUiResources;
    private TileAdapter mTileAdapter;
    private boolean mConfigRibbon;

    private int mTileTextSize;
    private int mTileTextPadding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity());
        mContainer = container;
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        mInflater = inflater;

        QuickSettingsUtil.removeUnsupportedTiles(getActivity());

        // We have both a panel and the ribbon config, see which one we are using
        Bundle args = getArguments();
        if (args != null) {
            mConfigRibbon = args.getBoolean("config_ribbon");
        }

        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int panelWidth = getItemFromSystemUi("notification_panel_width", "dimen");
        if (panelWidth > 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(panelWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            mDragView.setLayoutParams(params);
        }
        int cellGap = getItemFromSystemUi("quick_settings_cell_gap", "dimen");
        if (cellGap != 0) {
            mDragView.setCellGap(cellGap);
        }
        int columnCount = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3,
                UserHandle.USER_CURRENT);
        // do not allow duplication on tablets or any device which do not have
        // flipsettings
        boolean duplicateOnLandScape = Settings.System.getIntForUser(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                1, UserHandle.USER_CURRENT) == 1
                        && mSystemUiResources.getBoolean(mSystemUiResources.getIdentifier(
                        "com.android.systemui:bool/config_hasFlipSettingsPanel", null, null))
                        && isLandscape();

        if (columnCount != 0) {
            mDragView.setColumnCount(duplicateOnLandScape ? (columnCount * 2) : columnCount);
            mTileTextSize = mDragView.getTileTextSize(columnCount);
            mTileTextPadding = mDragView.getTileTextPadding(columnCount);
        }
        mTileAdapter = new TileAdapter(getActivity(), mConfigRibbon);
        return mDragView;
    }

    private int getItemFromSystemUi(String name, String type) {
        if (mSystemUiResources != null) {
            int resId = (int) mSystemUiResources.getIdentifier(name, type, "com.android.systemui");
            if (resId > 0) {
                try {
                    if (type.equals("dimen")) {
                        return (int) mSystemUiResources.getDimension(resId);
                    } else {
                        return mSystemUiResources.getInteger(resId);
                    }
                } catch (NotFoundException e) {
                }
            }
        }
        return 0;
    }

    void genTiles() {
        mDragView.removeAllViews();
        ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
        for (String tileindex : tiles) {
            QuickSettingsUtil.TileInfo tile = QuickSettingsUtil.TILES.get(tileindex);
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.add, null, R.drawable.ic_menu_add, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View tileView = null;
        if (iconRegId != 0) {
            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
            final TextView name = (TextView) tileView.findViewById(R.id.text);
            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
            name.setText(titleId);
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
            name.setPadding(0, mTileTextPadding, 0, 0);
            iv.setImageDrawable(getResources().getDrawable(iconRegId));
        } else {
            final boolean isUserTile = titleId == QuickSettingsUtil.TILES.get(QSConstants.TILE_USER).getTitleResId();
            if (mSystemUiResources != null && iconSysId != null) {
                int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
                if (resId > 0) {
                    try {
                        Drawable d = mSystemUiResources.getDrawable(resId);
                        tileView = null;
                        if (isUserTile) {
                            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_user, null, false);
                            ImageView iv = (ImageView) tileView.findViewById(R.id.user_imageview);
                            TextView tv = (TextView) tileView.findViewById(R.id.tile_textview);
                            tv.setText(titleId);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            iv.setImageDrawable(d);
                        } else {
                            tileView = (View) mInflater.inflate(
                                    R.layout.quick_settings_tile_generic, null, false);
                            final TextView name = (TextView) tileView.findViewById(R.id.text);
                            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
                            name.setText(titleId);
                            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            name.setPadding(0, mTileTextPadding, 0, 0);
                            iv.setImageDrawable(d);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (tileView != null) {
            if (titleId == QuickSettingsUtil.TILES.get(
                        QSConstants.TILE_SCREENTIMEOUT).getTitleResId()
                || titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_RINGER).getTitleResId()
                || titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_SCREENSHOT).getTitleResId()
                || titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_MUSIC).getTitleResId()
                || QuickSettingsUtil.isTileAvailable(QSConstants.TILE_NETWORKMODE)
                        && titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_NETWORKMODE).getTitleResId()) {

                ImageView settings =  (ImageView) tileView.findViewById(R.id.settings);
                if (settings != null) {
                    settings.setVisibility(View.VISIBLE);
                }
            }
            mDragView.addView(tileView, newTile
                    ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        mDragView.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                tiles.remove(index);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                if (arg2 != mDragView.getChildCount() - 1) {
                    if (arg2 == -1) {
                        return;
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_SCREENTIMEOUT)) {
                        showDialogInner(DLG_SCREENTIMEOUT);
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_NETWORKMODE)) {
                        showDialogInner(DLG_NETWORK_MODE);
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_RINGER)) {
                        showDialogInner(DLG_RINGER);
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_SCREENSHOT)) {
                        showDialogInner(DLG_SCREENSHOT_DELAY);
                    }
                    if (tiles.get(arg2).equals(QSConstants.TILE_MUSIC)) {
                        showDialogInner(DLG_MUSIC);
                    }
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tile_choose_title)
                .setAdapter(mTileAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int position) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> curr = QuickSettingsUtil.getTileListFromString(
                                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                                curr.add(mTileAdapter.getTileId(position));
                                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                                        QuickSettingsUtil.getTileStringFromList(curr), mConfigRibbon);
                            }
                        }).start();
                        TileInfo info = QuickSettingsUtil.TILES.get(mTileAdapter.getTileId(position));
                        addTile(info.getTitleResId(), info.getIcon(), 0, true);
                    }
                });
                builder.create().show();
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(getActivity())) {
            mContainer.setPadding(20, 0, 20, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_action_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.tiles_reset_title);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickSettingsUtil.resetTiles(getActivity(), mConfigRibbon);
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    private boolean isLandscape() {
        final boolean isLandscape =
            Resources.getSystem().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
        return isLandscape;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QuickSettingsTiles getOwner() {
            return (QuickSettingsTiles) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_SCREENSHOT_DELAY:
                    String[] dialogEntriesSS = getResources().getStringArray(
                            getResources().getIdentifier("entries_screenshot_delay",
                            "array", "com.android.settings"));
                    final String[] dialogValuesSS = getResources().getStringArray(
                            getResources().getIdentifier("values_screenshot_delay",
                            "array", "com.android.settings"));
                    int actualEntrySS = Settings.System.getInt(
                            getActivity().getContentResolver(),
                            Settings.System.SCREENSHOT_TOGGLE_DELAY, 5000);
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_screenshot_delay_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setSingleChoiceItems(dialogEntriesSS, actualEntrySS,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(
                                getActivity().getContentResolver(),
                                Settings.System.SCREENSHOT_TOGGLE_DELAY,
                                Integer.valueOf(dialogValuesSS[which]));
                                dismiss();
                        }
                    })
                    .create();
                case DLG_SCREENTIMEOUT:
                    String[] dialogEntries = getResources().getStringArray(
                            getResources().getIdentifier("entries_screentimeout_widget",
                            "array", "com.android.settings"));
                    final String[] dialogValuesST = getResources().getStringArray(
                            getResources().getIdentifier("values_screentimeout_widget",
                            "array", "com.android.settings"));
                    int actualEntry = Settings.System.getIntForUser(
                            getActivity().getContentResolver(),
                            Settings.System.EXPANDED_SCREENTIMEOUT_MODE, 0,
                            UserHandle.USER_CURRENT);
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_screentimeout_mode_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setSingleChoiceItems(dialogEntries, actualEntry,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(
                                getActivity().getContentResolver(),
                                Settings.System.EXPANDED_SCREENTIMEOUT_MODE,
                                Integer.valueOf(dialogValuesST[which]));
                                dismiss();
                        }
                    })
                    .create();
                case DLG_NETWORK_MODE:
                    dialogEntries = getResources().getStringArray(
                            getResources().getIdentifier("entries_network_widget",
                            "array", "com.android.settings"));
                    final String[] dialogValuesNM = getResources().getStringArray(
                            getResources().getIdentifier("values_network_widget",
                            "array", "com.android.settings"));
                    actualEntry = Settings.System.getIntForUser(
                            getActivity().getContentResolver(),
                            Settings.System.EXPANDED_NETWORK_MODE, 0,
                            UserHandle.USER_CURRENT);
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_network_mode_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setSingleChoiceItems(dialogEntries, actualEntry,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(
                                getActivity().getContentResolver(),
                                Settings.System.EXPANDED_NETWORK_MODE,
                                Integer.valueOf(dialogValuesNM[which]));
                                dismiss();
                        }
                    })
                    .create();
                case DLG_RINGER:
                    dialogEntries = getResources().getStringArray(
                            getResources().getIdentifier("entries_ring_widget",
                            "array", "com.android.settings"));
                    final String[] dialogValuesSM = getResources().getStringArray(
                            getResources().getIdentifier("values_ring_widget",
                            "array", "com.android.settings"));
                    final int size = dialogValuesSM.length;
                    String storedEntries = Settings.System.getStringForUser(
                            getActivity().getContentResolver(),
                            Settings.System.EXPANDED_RING_MODE,
                            UserHandle.USER_CURRENT);
                    final boolean[] actualSelections = new boolean[size];
                    if (storedEntries == null) {
                        for (int i = 0; i < size; i++) {
                            actualSelections[i] = true;
                        }
                    } else {
                        String [] actualEntries = TextUtils.split(storedEntries, SEPARATOR);
                        for (int i = 0; i < size; i++) {
                            for (int j = 0; j < actualEntries.length; j++) {
                                if (dialogValuesSM[i].equals(actualEntries[j])) {
                                    actualSelections[i] = true;
                                }
                            }
                        }
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_ring_mode_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(dialogEntries, actualSelections,
                        new  DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                            actualSelections[indexSelected] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String finalValues = "";
                            for (int i = 0; i < size; i++) {
                                if (actualSelections[i]) {
                                    if (!finalValues.isEmpty()) {
                                        finalValues += SEPARATOR;
                                    }
                                    finalValues += dialogValuesSM[i];
                                }
                            }
                            // user did not select any entries
                            // reshow dialog and reset to actual
                            // values back.
                            if (finalValues.isEmpty()) {
                                getOwner().showDialogInner(DLG_RINGER);
                                return;
                            }
                            Settings.System.putString(
                                getActivity().getContentResolver(),
                                Settings.System.EXPANDED_RING_MODE,
                                finalValues);
                        }
                    })
                    .create();
                case DLG_MUSIC:
                    int storedMode = Settings.System.getIntForUser(
                            getActivity().getContentResolver(),
                            Settings.System.MUSIC_TILE_MODE, 3,
                            UserHandle.USER_CURRENT);
                    final boolean[] actualMode = new boolean[2];
                    actualMode[0] = storedMode == 1 || storedMode == 3;
                    actualMode[1] = storedMode > 1;

                    final String[] entries =  {
                            getResources().getString(R.string.music_tile_mode_background),
                            getResources().getString(R.string.music_tile_mode_tracktitle)
                    };
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_music_mode_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(entries, actualMode,
                        new  DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                            actualMode[indexSelected] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int mode = 0;
                            if (actualMode[0]) {
                                if (actualMode[1]) {
                                    mode = 3;
                                } else {
                                    mode = 1;
                                }
                            } else {
                                if (actualMode[1]) {
                                    mode = 2;
                                }
                            }
                            Settings.System.putInt(
                                getActivity().getContentResolver(),
                                Settings.System.MUSIC_TILE_MODE,
                                mode);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private static class TileAdapter extends ArrayAdapter<String> {
        private static class Entry {
            public final TileInfo tile;
            public final String tileTitle;
            public Entry(TileInfo tile, String tileTitle) {
                this.tile = tile;
                this.tileTitle = tileTitle;
            }
        }

        private Entry[] mTiles;
        private boolean mIsRibbon;

        public TileAdapter(Context context, boolean isRibbon) {
            super(context, android.R.layout.simple_list_item_1);
            mTiles = new Entry[getCount()];
            mIsRibbon = isRibbon;
            loadItems(context.getResources());
            sortItems();
        }

        private void loadItems(Resources resources) {
            int index = 0;
            for (TileInfo t : QuickSettingsUtil.TILES.values()) {
                mTiles[index++] = new Entry(t, resources.getString(t.getTitleResId()));
            }
        }

        private void sortItems() {
            final Collator collator = Collator.getInstance();
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            collator.setStrength(Collator.PRIMARY);
            Arrays.sort(mTiles, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return collator.compare(e1.tileTitle, e2.tileTitle);
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setEnabled(isEnabled(position));
            return v;
        }

        @Override
        public int getCount() {
            return QuickSettingsUtil.TILES.size();
        }

        @Override
        public String getItem(int position) {
            return mTiles[position].tileTitle;
        }

        public String getTileId(int position) {
            return mTiles[position].tile.getId();
        }

        @Override
        public boolean isEnabled(int position) {
            String usedTiles = QuickSettingsUtil.getCurrentTiles(
                    getContext(), mIsRibbon);
            return !(usedTiles.contains(mTiles[position].tile.getId()));
        }
    }
}
