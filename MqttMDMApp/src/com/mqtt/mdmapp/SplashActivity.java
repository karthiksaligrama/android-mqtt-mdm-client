package com.mqtt.mdmapp;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mqtt.utils.Utils;

public class SplashActivity extends Activity {

	DevicePolicyManager mDPM;
	ComponentName mDeviceAdmin;

	private static final String TAG = "SplashActivity";
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

	private static final String QUEUE_END_POINT = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		// settings related to the push notification
		SharedPreferences settings = getSharedPreferences(MQTTService.APP_ID, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("broker", QUEUE_END_POINT);
		editor.putString("topic",
				"mqtt/" + Utils.getDeviceID(this));
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

		messageIntentReceiver = new MQTTMessageReceiver();
		IntentFilter intentCFilter = new IntentFilter(
				MQTTService.MQTT_MSG_RECEIVED_INTENT);
		registerReceiver(messageIntentReceiver, intentCFilter);
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
		Intent svc = new Intent(this, MQTTService.class);
		stopService(svc);

		unregisterReceiver(messageIntentReceiver);
	}

	private MQTTMessageReceiver messageIntentReceiver;

	public class MQTTMessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle notificationData = intent.getExtras();
			/* The topic of this message. */
			String newTopic = notificationData
					.getString(MQTTService.MQTT_MSG_RECEIVED_TOPIC);
			/* The message payload. */
			String newData = notificationData
					.getString(MQTTService.MQTT_MSG_RECEIVED_MSG);
			try {
				newData = newData.replace("\\", "");
				JsonParser parser = new JsonParser();
				JsonElement element = parser.parse(newData);
				JsonObject object = element.getAsJsonObject();
				String command = ((JsonElement) object.get("command"))
						.getAsString();
				Log.i(TAG, "Command = " + command);

				initiateCommand(object);
			} catch (JsonSyntaxException e) {
				Log.e(TAG, "Error in parsing the json data");
			} catch (Exception e) {
				Log.e(TAG, "Error in parsing the json data");
			}
		}
	}

	private static final String ADMIN_COMMAND_LOCK_DEVICE = "lock";
	private static final String ADMIN_COMMAND_REMOTE_WIPE = "remote_wipe";
	private static final String ADMIN_RESET_PASSWORD = "reset_password";
	private static final String ADMIN_DISABLE_CAMERA = "disable_camera";
	private static final String ADMIN_ENABLE_CAMERA = "enable_camera";
	private static final String ADMIN_ENABLE_ENCRYPTION = "enable_encryption";
	private static final String ADMIN_DISABLE_ENCRYTION = "disable_encrytion";
	private static final String ADMIN_ENABLE_PASSWORD_POLICY = "password_policy";

	private void initiateCommand(JsonObject object) {
		String command = ((JsonElement) object.get("command")).getAsString();

		if (command == null || command.isEmpty() || TextUtils.isEmpty(command))
			return;

		if (command.equalsIgnoreCase(ADMIN_COMMAND_LOCK_DEVICE)) {
			mDPM.lockNow();
		} else if (command.equalsIgnoreCase(ADMIN_COMMAND_REMOTE_WIPE)) {
			mDPM.wipeData(0);
		} else if (command.equalsIgnoreCase(ADMIN_DISABLE_CAMERA)) {
			mDPM.setCameraDisabled(mDeviceAdmin, true);
		} else if (command.equalsIgnoreCase(ADMIN_ENABLE_CAMERA)) {
			mDPM.setCameraDisabled(mDeviceAdmin, false);
		} else if (command.equalsIgnoreCase(ADMIN_RESET_PASSWORD)) {
			String password = ((JsonElement) object.get("password"))
					.getAsString();
			mDPM.resetPassword(password,
					DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		} else if (command.equalsIgnoreCase(ADMIN_ENABLE_ENCRYPTION)) {
			mDPM.setStorageEncryption(mDeviceAdmin, true);
		} else if (command.equalsIgnoreCase(ADMIN_DISABLE_ENCRYTION)) {
			mDPM.setStorageEncryption(mDeviceAdmin, false);
		} else if (command.equalsIgnoreCase(ADMIN_ENABLE_PASSWORD_POLICY)) {
			Gson gson = new Gson();
			DevicePolicy devicePolicy = gson.fromJson(object.get("policy"),
					DevicePolicy.class);
			Log.i(TAG, "after setting the object");

			mDPM.setCameraDisabled(mDeviceAdmin, devicePolicy.isEnable_camera());
			mDPM.setStorageEncryption(mDeviceAdmin,
					devicePolicy.isEncrypt_device());
			mDPM.setPasswordQuality(mDeviceAdmin,
					devicePolicy.getPassword_quality());
			mDPM.setPasswordMinimumSymbols(mDeviceAdmin,
					devicePolicy.getMimimum_symbols());
			mDPM.setPasswordQuality(mDeviceAdmin,
					devicePolicy.getPassword_quality());
			mDPM.setPasswordMinimumLength(mDeviceAdmin,
					devicePolicy.getPassword_length());
			mDPM.setPasswordMinimumLowerCase(mDeviceAdmin,
					devicePolicy.getMinimum_lowercase());
			mDPM.setPasswordMinimumUpperCase(mDeviceAdmin,
					devicePolicy.getMinimum_uppercase());
			mDPM.setPasswordMinimumNumeric(mDeviceAdmin,
					devicePolicy.getMinimum_numbers());
			mDPM.setPasswordExpirationTimeout(mDeviceAdmin,
					devicePolicy.getPassword_expiry_timeout() * 86400 * 1000);
			mDPM.setPasswordMinimumNonLetter(mDeviceAdmin, 1);
			mDPM.setMaximumTimeToLock(mDeviceAdmin, 1000);
			mDPM.setPasswordHistoryLength(mDeviceAdmin, 1);
			mDPM.setMaximumFailedPasswordsForWipe(mDeviceAdmin, 100);
			//mDPM.setKeyguardDisabledFeatures(admin, which)
			//mDPM.setPasswordMinimumLetters(mDeviceAdmin, )
			
			// trigger new password screen if the policy is not matching
			if (!mDPM.isActivePasswordSufficient()) {
				Intent intent = new Intent(
						DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
				startActivity(intent);
			}
		} else {
			Log.i(TAG, "Other commands not currently implemented");
		}
	}

	public static class MDMReciever extends DeviceAdminReceiver {

		void showToast(Context context, String msg) {
			String status = context.getString(R.string.admin_receiver_status,
					msg);
			Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onEnabled(Context context, Intent intent) {
			showToast(context,
					context.getString(R.string.admin_receiver_status_enabled));
			Intent mqttservice = new Intent(context, MQTTService.class);
			context.startService(mqttservice);

		}

		@Override
		public CharSequence onDisableRequested(Context context, Intent intent) {
			return context
					.getString(R.string.admin_receiver_status_disable_warning);
		}

		@Override
		public void onDisabled(Context context, Intent intent) {

			showToast(context,
					context.getString(R.string.admin_receiver_status_disabled));
		}

		@Override
		public void onPasswordChanged(Context context, Intent intent) {
			showToast(
					context,
					context.getString(R.string.admin_receiver_status_pw_changed));
		}

		@Override
		public void onPasswordFailed(Context context, Intent intent) {
			showToast(context,
					context.getString(R.string.admin_receiver_status_pw_failed));
		}

		@Override
		public void onPasswordSucceeded(Context context, Intent intent) {
			showToast(
					context,
					context.getString(R.string.admin_receiver_status_pw_succeeded));
		}

		@Override
		public void onPasswordExpiring(Context context, Intent intent) {
			DevicePolicyManager dpm = (DevicePolicyManager) context
					.getSystemService(Context.DEVICE_POLICY_SERVICE);
			long expr = dpm.getPasswordExpiration(new ComponentName(context,
					MDMReciever.class));
			long delta = expr - System.currentTimeMillis();
			boolean expired = delta < 0L;
			String message = context
					.getString(expired ? R.string.expiration_status_past
							: R.string.expiration_status_future);
			showToast(context, message);
			Log.v(TAG, message);
		}
	}
}
