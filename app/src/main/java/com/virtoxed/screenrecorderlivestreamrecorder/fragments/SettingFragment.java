package com.virtoxed.screenrecorderlivestreamrecorder.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.virtoxed.screenrecorderlivestreamrecorder.BuildConfig;
import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.services.ControllerService;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;

import java.util.Objects;

public class SettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    static final String TAG = SettingFragment.class.getSimpleName();
    private SharedPreferences mSharedPreferences;
    private PreferenceScreen mPreferenceScreen;
    Preference  key_share_app, key_privacy_policy;

    public SettingFragment() {
        // Required empty public constructor
    }

    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }
    public static SettingFragment getInstance()    {
        return new SettingFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.setting_preferences);

        key_share_app =findPreference(getString(R.string.key_share_app));
        key_share_app.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Hey check out my app at: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            }
        });
        key_privacy_policy =findPreference(getString(R.string.key_privacy_policy));
        key_privacy_policy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getContext(), "Policy", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mPreferenceScreen = getPreferenceScreen();
        mSharedPreferences = mPreferenceScreen.getSharedPreferences();

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_mode));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_size));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_camera_position));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_common_countdown));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_common_orientation));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_bitrate));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_fps));
        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_video_resolution));

        onSharedPreferenceChanged(mSharedPreferences, getString(R.string.setting_audio_source));

    }

    @Override
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            try {
                String summary = sharedPreferences.getString(key, "");
                if (key.equals(getString(R.string.setting_common_countdown)))
                    summary += "s";

                preference.setSummary(summary);
            }
            catch (Exception e){
//                e.printStackTrace();
                Log.e(TAG, "onSharedPreferenceChanged: "+e.getMessage(), e);
            }
        }

        if(isMyServiceRunning(ControllerService.class)) {
            int settingKey = getResources().getIdentifier(key, "string", getActivity().getPackageName());
            if (isCanUpdateSettingImmediately(settingKey)) {
                Log.d(TAG, "onSharedPreferenceChanged: "+key);
                requestUpdateSetting(settingKey);
            }
        }
    }

    private void requestUpdateSetting(int key) {
        Intent intent = new Intent(getActivity(), ControllerService.class);
        intent.setAction(MyUtils.ACTION_UPDATE_SETTING);
        intent.putExtra(MyUtils.ACTION_UPDATE_SETTING, key);
        getActivity().startService(intent);
    }

    private final int[] mList = {
            R.string.setting_camera_mode,
            R.string.setting_camera_size,
            R.string.setting_camera_position
    };


    private boolean isCanUpdateSettingImmediately(int key) {
        for (int s:mList) {
            if(s==key)
                return true;
        }
        return false;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        catch (Exception e){
            return false;
        }

        return false;
    }
    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
