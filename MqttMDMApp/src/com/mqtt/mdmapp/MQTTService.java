package com.mqtt.mdmapp;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

public class MQTTService extends Service implements MqttSimpleCallback {

	public static final String APP_ID = "org.mosquitto.android.mqtt";

	public static final String MQTT_MSG_RECEIVED_INTENT = "org.mosquitto.android.mqtt.MSGRECVD";
	public static final String MQTT_MSG_RECEIVED_TOPIC = "org.mosquitto.android.mqtt.MSGRECVD_TOPIC";
	public static final String MQTT_MSG_RECEIVED_MSG = "org.mosquitto.android.mqtt.MSGRECVD_MSGBODY";

	public static final String MQTT_STATUS_INTENT = "org.mosquitto.android.mqtt.STATUS";
	public static final String MQTT_STATUS_MSG = "org.mosquitto.android.mqtt.STATUS_MSG";

	public static final String MQTT_PING_ACTION = "org.mosquitto.android.mqtt.PING";

	public static final int MQTT_NOTIFICATION_ONGOING = 1;
	public static final int MQTT_NOTIFICATION_UPDATE = 2;

	public enum MQTTConnectionStatus {
		INITIAL, CONNECTING, CONNECTED, NOTCONNECTED_WAITINGFORINTERNET, NOTCONNECTED_USERDISCONNECT, NOTCONNECTED_DATADISABLED, NOTCONNECTED_UNKNOWNREASON
	}

	public static final int MAX_MQTT_CLIENTID_LENGTH = 22;

	private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;

	private String brokerHostName = "";

	private String topicName = "";

	private int brokerPortNumber = 1883;
	private MqttPersistence usePersistence = null;
	private boolean cleanStart = false;
	private int[] qualitiesOfService = { 0 };

	private short keepAliveSeconds = 20 * 60;

	private String mqttClientId = null;

	private IMqttClient mqttClient = null;

	private NetworkConnectionIntentReceiver netConnReceiver;

	private BackgroundDataChangeIntentReceiver dataEnabledReceiver;

	private PingSender pingSender;

	@Override
	public void onCreate() {
		super.onCreate();

		android.os.Debug.waitForDebugger();

		connectionStatus = MQTTConnectionStatus.INITIAL;
		mBinder = new LocalBinder<MQTTService>(this);

		SharedPreferences settings = getSharedPreferences(APP_ID, MODE_PRIVATE);
		brokerHostName = settings.getString("broker", "");
		topicName = settings.getString("topic", "");

		Log.i("MQTTService", "broker host name " + brokerHostName);
		Log.i("MQTTService", "topic name " + topicName);
		dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
		registerReceiver(dataEnabledReceiver, new IntentFilter(
				ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));

		defineConnectionToBroker(brokerHostName);
	}

	@Override
	public void onStart(final Intent intent, final int startId) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, "MQTTservice").start();
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handleStart(intent, startId);
			}
		}, "MQTTservice").start();

		return START_STICKY;
	}

	synchronized void handleStart(Intent intent, int startId) {
		if (mqttClient == null) {
			stopSelf();
			return;
		}

		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getBackgroundDataSetting() == false) {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
			broadcastServiceStatus("Not connected - background data disabled");
			return;
		}
		rebroadcastStatus();
		rebroadcastReceivedMessages();

		if (isAlreadyConnected() == false) {
			connectionStatus = MQTTConnectionStatus.CONNECTING;
			if (isOnline()) {
				if (connectToBroker()) {
					subscribeToTopic(topicName);
				}
			} else {
				connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
				broadcastServiceStatus("Waiting for network connection");
			}
		}
		if (netConnReceiver == null) {
			netConnReceiver = new NetworkConnectionIntentReceiver();
			registerReceiver(netConnReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));
		}

		if (pingSender == null) {
			pingSender = new PingSender();
			registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		disconnectFromBroker();
		broadcastServiceStatus("Disconnected");
		if (dataEnabledReceiver != null) {
			unregisterReceiver(dataEnabledReceiver);
			dataEnabledReceiver = null;
		}

		if (mBinder != null) {
			mBinder.close();
			mBinder = null;
		}
	}

	private void broadcastServiceStatus(String statusDescription) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MQTT_STATUS_INTENT);
		broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
		sendBroadcast(broadcastIntent);
	}

	private void broadcastReceivedMessage(String topic, String message) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
		broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
		broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
		sendBroadcast(broadcastIntent);
	}

	private LocalBinder<MQTTService> mBinder;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder<S> extends Binder {
		private WeakReference<S> mService;

		public LocalBinder(S service) {
			mService = new WeakReference<S>(service);
		}

		public S getService() {
			return mService.get();
		}

		public void close() {
			mService = null;
		}
	}

	public MQTTConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public void rebroadcastStatus() {
		String status = "";

		switch (connectionStatus) {
		case INITIAL:
			status = "Please wait";
			break;
		case CONNECTING:
			status = "Connecting...";
			break;
		case CONNECTED:
			status = "Connected";
			break;
		case NOTCONNECTED_UNKNOWNREASON:
			status = "Not connected - waiting for network connection";
			break;
		case NOTCONNECTED_USERDISCONNECT:
			status = "Disconnected";
			break;
		case NOTCONNECTED_DATADISABLED:
			status = "Not connected - background data disabled";
			break;
		case NOTCONNECTED_WAITINGFORINTERNET:
			status = "Unable to connect";
			break;
		}

		broadcastServiceStatus(status);
	}

	public void disconnect() {
		disconnectFromBroker();
		connectionStatus = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;
		broadcastServiceStatus("Disconnected");
	}

	public void connectionLost() throws Exception {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		wl.acquire();

		if (isOnline() == false) {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
			broadcastServiceStatus("Connection lost - no network connection");
		} else {
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Connection lost - reconnecting...");
			if (connectToBroker()) {
				subscribeToTopic(topicName);
			}
		}
		wl.release();
	}

	@Override
	public void publishArrived(String topic, byte[] payloadbytes, int qos,
			boolean retained) {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		wl.acquire();
		String messageBody = new String(payloadbytes);
		if (addReceivedMessageToStore(topic, messageBody)) {
			broadcastReceivedMessage(topic, messageBody);
		}
		scheduleNextPing();
		wl.release();
	}

	private void defineConnectionToBroker(String brokerHostName) {
		String mqttConnSpec = "tcp://" + brokerHostName + "@"
				+ brokerPortNumber;
		try {
			mqttClient = MqttClient.createMqttClient(mqttConnSpec,
					usePersistence);
			mqttClient.registerSimpleHandler(this);
		} catch (MqttException e) {
			mqttClient = null;
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Invalid connection parameters");
		}
	}

	private boolean connectToBroker() {
		try {
			mqttClient
					.connect(generateClientId(), cleanStart, keepAliveSeconds);
			broadcastServiceStatus("Connected");
			connectionStatus = MQTTConnectionStatus.CONNECTED;
			scheduleNextPing();
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
			connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
			broadcastServiceStatus("Unable to connect");
			scheduleNextPing();
			return false;
		}
	}

	private void subscribeToTopic(String topicName) {
		boolean subscribed = false;
		if (isAlreadyConnected() == false) {
			Log.e("mqtt", "Unable to subscribe as we are not connected");
		} else {
			try {
				String[] topics = { topicName };
				mqttClient.subscribe(topics, qualitiesOfService);
				subscribed = true;
			} catch (MqttNotConnectedException e) {
				Log.e("mqtt", "subscribe failed - MQTT not connected", e);
			} catch (IllegalArgumentException e) {
				Log.e("mqtt", "subscribe failed - illegal argument", e);
			} catch (MqttException e) {
				Log.e("mqtt", "subscribe failed - MQTT exception", e);
			}
		}

		if (subscribed == false) {
			broadcastServiceStatus("Unable to subscribe");
		}
	}

	private void disconnectFromBroker() {
		try {
			if (netConnReceiver != null) {
				unregisterReceiver(netConnReceiver);
				netConnReceiver = null;
			}

			if (pingSender != null) {
				unregisterReceiver(pingSender);
				pingSender = null;
			}
		} catch (Exception eee) {
			Log.e("mqtt", "unregister failed", eee);
		}

		try {
			if (mqttClient != null) {
				mqttClient.disconnect();
			}
		} catch (MqttPersistenceException e) {
			Log.e("mqtt", "disconnect failed - persistence exception", e);
		} finally {
			mqttClient = null;
		}

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

	private boolean isAlreadyConnected() {
		return ((mqttClient != null) && (mqttClient.isConnected() == true));
	}

	private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(final Context ctx, final Intent intent) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
					WakeLock wl = pm.newWakeLock(
							PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
					wl.acquire();
					ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
					if (cm.getBackgroundDataSetting()) {
						defineConnectionToBroker(brokerHostName);
						handleStart(intent, 0);
					} else {
						connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
						broadcastServiceStatus("Not connected - background data disabled");
						disconnectFromBroker();
					}
					wl.release();
				}
			}, "MQTTservice").start();
		}
	}

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {

			new Thread(new Runnable() {

				@Override
				public void run() {
					PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
					WakeLock wl = pm.newWakeLock(
							PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
					wl.acquire();
					if (isOnline()) {
						if (connectToBroker()) {
							subscribeToTopic(topicName);
						}
					}
					wl.release();
				}
			}, "MQTTService").start();
		}
	}

	private void scheduleNextPing() {
		PendingIntent pendingIntent = PendingIntent
				.getBroadcast(this, 0, new Intent(MQTT_PING_ACTION),
						PendingIntent.FLAG_UPDATE_CURRENT);
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	public class PingSender extends BroadcastReceiver {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						mqttClient.ping();
					} catch (MqttException e) {
						Log.e("mqtt", "ping failed - MQTT exception", e);
						try {
							mqttClient.disconnect();
						} catch (MqttPersistenceException e1) {
							Log.e("mqtt",
									"disconnect failed - persistence exception",
									e1);
						}
						if (connectToBroker()) {
							subscribeToTopic(topicName);
						}
					}
					scheduleNextPing();
				}
			}, "MQTTService").start();
		}
	}

	private Hashtable<String, String> dataCache = new Hashtable<String, String>();

	private boolean addReceivedMessageToStore(String key, String value) {
		String previousValue = null;
		if (value.length() == 0) {
			previousValue = dataCache.remove(key);
		} else {
			previousValue = dataCache.put(key, value);
		}
		return ((previousValue == null) || (previousValue.equals(value) == false));
	}

	public void rebroadcastReceivedMessages() {
		Enumeration<String> e = dataCache.keys();
		while (e.hasMoreElements()) {
			String nextKey = e.nextElement();
			String nextValue = dataCache.get(nextKey);
			broadcastReceivedMessage(nextKey, nextValue);
		}
	}

	private String generateClientId() {
		if (mqttClientId == null) {
			String timestamp = "" + (new Date()).getTime();
			String android_id = Settings.System.getString(getContentResolver(),
					Secure.ANDROID_ID);
			mqttClientId = timestamp + android_id;
			if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
				mqttClientId = mqttClientId.substring(0,
						MAX_MQTT_CLIENTID_LENGTH);
			}
		}
		return mqttClientId;
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		}
		return false;
	}
}
