/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.omnigears.interfacesettings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.internal.util.omni.DeviceUtils;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.util.omni.OmniSwitchConstants;
import com.android.internal.util.omni.PackageUtils;

import java.util.List;
import java.util.ArrayList;

public class BarsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "BarsSettings";

    private static final String STATUSBAR_BATTERY_STYLE = "statusbar_battery_style";
    private static final String STATUSBAR_BATTERY_PERCENT = "statusbar_battery_percent";
    private static final String NAVIGATION_BAR_CATEGORY = "navbar_category";
    private static final String NAVIGATION_BAR_RECENTS_STYLE = "navbar_recents_style";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

    private ListPreference mBatteryStyle;
    private ListPreference mBatteryPercent;
    private ListPreference mNavbarRecentsStyle;
    private SwitchPreference mStatusBarBrightnessControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bars_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mBatteryStyle = (ListPreference) findPreference(STATUSBAR_BATTERY_STYLE);
        int batteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_STYLE, 0);

        mBatteryStyle.setValue(Integer.toString(batteryStyle));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent = (ListPreference) findPreference(STATUSBAR_BATTERY_PERCENT);
        int batteryPercent = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_PERCENT, 2);

        mBatteryPercent.setValue(Integer.toString(batteryPercent));
        mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        mBatteryPercent.setOnPreferenceChangeListener(this);

        final boolean showNavBar = DeviceUtils.deviceSupportNavigationBar(getActivity());
        final PreferenceCategory navbarCategory =
                (PreferenceCategory) prefScreen.findPreference(NAVIGATION_BAR_CATEGORY);

        mNavbarRecentsStyle = (ListPreference) findPreference(NAVIGATION_BAR_RECENTS_STYLE);
        int recentsStyle = Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_BAR_RECENTS, 0);

        mNavbarRecentsStyle.setValue(Integer.toString(recentsStyle));
        mNavbarRecentsStyle.setSummary(mNavbarRecentsStyle.getEntry());
        mNavbarRecentsStyle.setOnPreferenceChangeListener(this);

        // Start observing for changes on auto brightness
        StatusBarBrightnessChangedObserver statusBarBrightnessChangedObserver =
                new StatusBarBrightnessChangedObserver(new Handler());
        statusBarBrightnessChangedObserver.startObserving();

        mStatusBarBrightnessControl =
            (SwitchPreference) prefScreen.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBatteryStyle) {
            int value = Integer.valueOf((String) newValue);
            int index = mBatteryStyle.findIndexOfValue((String) newValue);
            mBatteryStyle.setSummary(
                    mBatteryStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_STYLE, value);
        } else if (preference == mBatteryPercent) {
            int value = Integer.valueOf((String) newValue);
            int index = mBatteryPercent.findIndexOfValue((String) newValue);
            mBatteryPercent.setSummary(
                    mBatteryPercent.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_PERCENT, value);
        } else if (preference == mNavbarRecentsStyle) {
            int value = Integer.valueOf((String) newValue);
            if (value == 1) {
                if (!isOmniSwitchInstalled()){
                    doOmniSwitchUnavail();
                } else if (!OmniSwitchConstants.isOmniSwitchRunning(getActivity())) {
                    doOmniSwitchConfig();
                }
            }
            int index = mNavbarRecentsStyle.findIndexOfValue((String) newValue);
            mNavbarRecentsStyle.setSummary(
                    mNavbarRecentsStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_RECENTS, value);
        } else if (preference == mStatusBarBrightnessControl) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    (Boolean) newValue ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatusBarBrightnessControl();
    }

    private void updateStatusBarBrightnessControl() {
        try {
            if (mStatusBarBrightnessControl != null) {
                int mode = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    mStatusBarBrightnessControl.setEnabled(false);
                    mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
                } else {
                    mStatusBarBrightnessControl.setEnabled(true);
                    mStatusBarBrightnessControl.setSummary(
                        R.string.status_bar_toggle_brightness_summary);
                }
            }
        } catch (SettingNotFoundException e) {
        }
    }

    private class StatusBarBrightnessChangedObserver extends ContentObserver {
        public StatusBarBrightnessChangedObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateStatusBarBrightnessControl();
        }

        public void startObserving() {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false, this);
        }
    }

    private void doOmniSwitchConfig() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_running)
            .setPositiveButton(R.string.omniswitch_settings,new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    startActivity(OmniSwitchConstants.INTENT_LAUNCH_APP);
                }
            });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void doOmniSwitchUnavail() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_unavail);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private boolean isOmniSwitchInstalled() {
        return PackageUtils.isAvailableApp(OmniSwitchConstants.APP_PACKAGE_NAME, getActivity());
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.bars_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
