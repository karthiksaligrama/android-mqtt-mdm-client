package com.mqtt.mdmapp;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.mqtt.mdmapp.MQTTMessageReceiver.MDMReciever;
import com.mqtt.utils.Utils;

public class SplashActivity extends Activity {

	DevicePolicyManager mDPM;
	ComponentName mDeviceAdmin;

	private static final String TAG = "SplashActivity";
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

	// update the ip or host name of the end point to listen to
	private static final String QUEUE_END_POINT = "10.154.1.226";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		// settings related to the push notification
		SharedPreferences settings = getSharedPreferences(MQTTService.APP_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("broker", QUEUE_END_POINT);
		editor.putString("topic", "mqtt/" + Utils.getDeviceID(this));
		Log.i(TAG, Utils.getDeviceID(this));
		editor.commit();

		// settings related to the device admin
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, MDMReciever.class);

		// start a device admin if already not added
		if (!isActiveAdmin()) {
			Intent startDeviceAdminIntent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			startDeviceAdminIntent.putExtra(
					DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
			startDeviceAdminIntent.putExtra(
					DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					getString(R.string.description));
			startActivityForResult(startDeviceAdminIntent,
					REQUEST_CODE_ENABLE_ADMIN);
		}

		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Intent mqttservice = new Intent(this, MQTTService.class);
		startService(mqttservice);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
			Log.i(TAG, "on activity result");
			Log.i(TAG, "activity result = " + resultCode);
			if (resultCode == RESULT_OK) {
				Log.i(TAG, "user acepted device policy");

				// set password policies
				mDPM.setPasswordMinimumLength(mDeviceAdmin, 5);
				mDPM.setMaximumTimeToLock(mDeviceAdmin, 60000);
				mDPM.setPasswordMinimumLetters(mDeviceAdmin, 1);
				mDPM.setPasswordMinimumLowerCase(mDeviceAdmin, 1);
				mDPM.setPasswordMinimumNumeric(mDeviceAdmin, 1);
				mDPM.setPasswordMinimumSymbols(mDeviceAdmin, 1);
				mDPM.setPasswordQuality(mDeviceAdmin,
						DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
				// trigger new password screen if the policy is not matching
				if (!mDPM.isActivePasswordSufficient()) {
					Intent intent = new Intent(
							DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
					startActivity(intent);
				}
			} else {
				Log.i(TAG, "Device policy accept cancelled");
			}
		}
	}

	private boolean isActiveAdmin() {
		return mDPM.isAdminActive(mDeviceAdmin);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


	

	
}
