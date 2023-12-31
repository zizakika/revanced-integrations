package app.revanced.twitch.settingsmenu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.revanced.shared.settings.SettingsUtils;
import app.revanced.twitch.settings.SettingsEnum;
import app.revanced.twitch.utils.LogHelper;
import app.revanced.twitch.utils.ReVancedUtils;

public class ReVancedSettingsFragment extends PreferenceFragment {

    private boolean registered = false;
    private boolean settingsInitialized = false;

    SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        LogHelper.debug("Setting '%s' changed", key);
        syncPreference(key);
    };

    /**
     * Sync preference
     * @param key Preference to load. If key is null, all preferences are updated
     */
    private void syncPreference(@Nullable String key) {
        for (SettingsEnum setting : SettingsEnum.values()) {
            if (!setting.path.equals(key) && key != null)
                continue;

            Preference pref = this.findPreference(setting.path);
            LogHelper.debug("Syncing setting '%s' with UI", setting.path);

            if (pref instanceof SwitchPreference) {
                SettingsEnum.setValue(setting, ((SwitchPreference) pref).isChecked());
            }
            else if (pref instanceof EditTextPreference) {
                SettingsEnum.setValue(setting, ((EditTextPreference) pref).getText());
            }
            else if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                listPref.setSummary(listPref.getEntry());
                SettingsEnum.setValue(setting, listPref.getValue());
            }
            else {
                LogHelper.error("Setting '%s' cannot be handled!", pref);
            }

            if (ReVancedUtils.getContext() != null && key != null && settingsInitialized && setting.rebootApp) {
                showRestartDialog(getContext());
            }

            // First onChange event is caused by initial state loading
            this.settingsInitialized = true;
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        try {
            PreferenceManager mgr = getPreferenceManager();
            mgr.setSharedPreferencesName(SettingsEnum.REVANCED_PREFS);
            mgr.getSharedPreferences().registerOnSharedPreferenceChangeListener(this.listener);

            addPreferencesFromResource(
                getResources().getIdentifier(
                        SettingsEnum.REVANCED_PREFS,
                        "xml",
                        this.getContext().getPackageName()
                )
            );

            // TODO: for a developer that uses Twitch: remove duplicated settings data
            // 1. remove all default values from the Patches Setting preferences (SwitchPreference, TextPreference, ListPreference)
            // 2. enable this code and verify the default is applied
            if (false) {
                for (SettingsEnum setting : SettingsEnum.values()) {
                    Preference pref = this.findPreference(setting.path);
                    if (pref instanceof SwitchPreference) {
                        ((SwitchPreference) pref).setChecked(setting.getBoolean());
                    } else if (pref instanceof EditTextPreference) {
                        ((EditTextPreference) pref).setText(setting.getObjectValue().toString());
                    } else if (pref instanceof ListPreference) {
                        ((ListPreference) pref).setValue(setting.getObjectValue().toString());
                    }
                }
            }
            // TODO: remove this line.  On load the UI should apply the values from Settings using the code above.
            // It should not apply the UI values to the Settings here
            syncPreference(null);

            this.registered = true;
        } catch (Throwable th) {
            LogHelper.printException("Error during onCreate()", th);
        }
    }

    @Override
    public void onDestroy() {
        if (this.registered) {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this.listener);
            this.registered = false;
        }
        super.onDestroy();
    }

    private void showRestartDialog(@NonNull Context context) {
        new AlertDialog.Builder(context).
                setMessage(ReVancedUtils.getString("revanced_reboot_message")).
                setPositiveButton(ReVancedUtils.getString("revanced_reboot"),
                        (dialog, i) -> SettingsUtils.restartApp(context))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
