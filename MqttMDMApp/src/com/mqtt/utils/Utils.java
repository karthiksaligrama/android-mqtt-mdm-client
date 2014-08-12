package com.mqtt.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mqtt.mdmapp.R;

public class Utils {

	public static int getAndroidVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}

	public static String getAppVersion(Context context) {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (NameNotFoundException e) {
			return "0";
		}

	}

	public static String generateUUID(Context context) {
		String android_id = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		Log.i("System out", "android_id : " + android_id);

		final TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		Log.i("System out", "tmDevice : " + tmDevice);
		tmSerial = "" + tm.getSimSerialNumber();
		Log.i("System out", "tmSerial : " + tmSerial);
		androidId = ""
				+ android.provider.Settings.Secure.getString(
						context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);

		UUID deviceUuid = new UUID(androidId.hashCode(),
				((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		String UUID = deviceUuid.toString();
		Log.i("System out", "UUID : " + UUID);
		return UUID;
	}

	public static String generateRandomUUID() {
		String uuid = UUID.randomUUID().toString();
		Log.i("System out", "uuid = " + uuid);
		return uuid;
	}

	// This is TCS specific implementation
	public static String getDeviceID(Context context) {
		String deviceId = null;

		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		deviceId = tm.getDeviceId();
		if (deviceId == null) {
			WifiManager wifiMan = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInf = wifiMan.getConnectionInfo();
			deviceId = wifiInf.getMacAddress();
		}

		return deviceId;
	}

	public static String getMacId(Context context) {
		WifiManager manager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = manager.getConnectionInfo();
		return wifiInfo.getMacAddress();
	}

	public static String getAppName(Context context) {
		return context.getString(R.string.app_name);
	}

	public static String getIMEI(Context context) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	/**
	 * Get IP address from first non-localhost interface
	 * 
	 * @param ipv4
	 *            true=return ipv4, false=return ipv6
	 * @return address or empty string
	 */
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase(
								Locale.ENGLISH);
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								return delim < 0 ? sAddr : sAddr.substring(0,
										delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			Log.e("Utils", "Exception while getting the ip address");
		} // for now eat exceptions
		return "";
	}

	public static int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier(
				"status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static String getManufacturer() {
		return android.os.Build.MANUFACTURER;
	}

	public static String getModel() {
		return android.os.Build.MODEL;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static String getFreeSpaceInBytes(Context context) {
		long availableSpace = -1L;
		try {
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
					.getPath());
			if (android.os.Build.VERSION.SDK_INT >= 18) {
				availableSpace = stat.getAvailableBlocksLong()
						* stat.getBlockSizeLong();
			} else {
				availableSpace = (long) stat.getAvailableBlocks()
						* (long) stat.getBlockSize();
			}
		} catch (Exception e) {
			// suppressed exception
		}
		return String.valueOf(availableSpace);
	}

	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		}
		return false;
	}

}
