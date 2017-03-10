/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;
@SuppressWarnings("all")
/** * There are many types of sources such as sensors.  The type of a
     * sensor could be base (close to hardware) or composite.  This is
     * mostly (only?) applicable to the Android platform.  See
     * https://source.android.com/devices/sensors/index.html for
     * details. */
@org.apache.avro.specific.AvroGenerated
public enum SrcSinkType {
  SOURCE_ACCELEROMETER, SOURCE_TEMPERATURE, SOURCE_GYROSCOPE, SOURCE_MAGNETIC_FIELD, SOURCE_HEART_RATE, SOURCE_LIGHT, SOURCE_PROXIMITY, SOURCE_PRESSURE, SOURCE_RELATIVE_HUMIDITY, SOURCE_LINEAR_ACCELERATION, SOURCE_MOTION, SOURCE_STEP_DETECTOR, SOURCE_STEP_COUNTER, SOURCE_TILT_DETECTOR, SOURCE_ROTATION_VECTOR, SOURCE_GRAVITY, SOURCE_GEOMAGNETIC_ROTATION_VECTOR, SOURCE_CAMERA, SOURCE_GPS, SOURCE_AUDIO, SOURCE_SYSTEM_PROPERTY, SOURCE_ENV_VARIABLE, SOURCE_ACCESSIBILITY_SERVICE, SOURCE_ACTIVITY_MANAGEMENT, SOURCE_ALARM_SERVICE, SOURCE_ANDROID_TV, SOURCE_AUDIO_IO, SOURCE_BACKUP_MANAGER, SOURCE_BINDER, SOURCE_BLUETOOTH, SOURCE_BOOT_EVENT, SOURCE_BROADCAST_RECEIVER_MANAGEMENT, SOURCE_CLIPBOARD, SOURCE_COMPONENT_MANAGEMENT, SOURCE_CONTENT_PROVIDER, SOURCE_CONTENT_PROVIDER_MANAGEMENT, SOURCE_DATABASE, SOURCE_DEVICE_ADMIN, SOURCE_DEVICE_SEARCH, SOURCE_DEVICE_USER, SOURCE_DISPLAY, SOURCE_DROPBOX, SOURCE_EMAIL, SOURCE_EXPERIMENTAL, SOURCE_FILE, SOURCE_FILE_SYSTEM_MANAGEMENT, SOURCE_FINGERPRINT, SOURCE_FLASHLIGHT, SOURCE_HDMI, SOURCE_IDLE_DOCK_SCREEN, SOURCE_IMS, SOURCE_INFRARED, SOURCE_INSTALLED_PACKAGES, SOURCE_JSSE_TRUST_MANAGER, SOURCE_KEYCHAIN, SOURCE_KEYGUARD, SOURCE_LOCATION, SOURCE_MACHINE_LEARNING, SOURCE_MEDIA_LOCAL_MANAGEMENT, SOURCE_MEDIA_LOCAL_PLAYBACK, SOURCE_MEDIA_NETWORK_CONNECTION, SOURCE_MEDIA_REMOTE_PLAYBACK, SOURCE_NETWORK_MANAGEMENT, SOURCE_NFC, SOURCE_NOTIFICATION, SOURCE_PAC_PROXY, SOURCE_PERMISSIONS, SOURCE_PERSISTANT_DATA, SOURCE_POWER_MANAGEMENT, SOURCE_PRINT_SERVICE, SOURCE_PROCESS_MANAGEMENT, SOURCE_RPC, SOURCE_SCREEN_AUDIO_CAPTURE, SOURCE_SERIAL_PORT, SOURCE_SERVICE_MANAGEMENT, SOURCE_SMS_MMS, SOURCE_SPEECH_INTERACTION, SOURCE_STATUS_BAR, SOURCE_SYNC_FRAMEWORK, SOURCE_TELEPHONY, SOURCE_TEXT_SERVICES, SOURCE_THREADING, SOURCE_TIME_EVENT, SOURCE_UI, SOURCE_UI_AUTOMATION, SOURCE_UI_RPC, SOURCE_UID_EVENT, SOURCE_USAGE_STATS, SOURCE_USB, SOURCE_USER_ACCOUNTS_MANAGEMENT, SOURCE_VIBRATOR, SOURCE_WAKE_LOCK, SOURCE_WALLPAPER_MANAGER, SOURCE_WAP, SOURCE_WEB_BROWSER, SOURCE_WIDGETS, SOURCE_SINK_IPC  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"SrcSinkType\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* There are many types of sources such as sensors.  The type of a\\n     * sensor could be base (close to hardware) or composite.  This is\\n     * mostly (only?) applicable to the Android platform.  See\\n     * https://source.android.com/devices/sensors/index.html for\\n     * details.\",\"symbols\":[\"SOURCE_ACCELEROMETER\",\"SOURCE_TEMPERATURE\",\"SOURCE_GYROSCOPE\",\"SOURCE_MAGNETIC_FIELD\",\"SOURCE_HEART_RATE\",\"SOURCE_LIGHT\",\"SOURCE_PROXIMITY\",\"SOURCE_PRESSURE\",\"SOURCE_RELATIVE_HUMIDITY\",\"SOURCE_LINEAR_ACCELERATION\",\"SOURCE_MOTION\",\"SOURCE_STEP_DETECTOR\",\"SOURCE_STEP_COUNTER\",\"SOURCE_TILT_DETECTOR\",\"SOURCE_ROTATION_VECTOR\",\"SOURCE_GRAVITY\",\"SOURCE_GEOMAGNETIC_ROTATION_VECTOR\",\"SOURCE_CAMERA\",\"SOURCE_GPS\",\"SOURCE_AUDIO\",\"SOURCE_SYSTEM_PROPERTY\",\"SOURCE_ENV_VARIABLE\",\"SOURCE_ACCESSIBILITY_SERVICE\",\"SOURCE_ACTIVITY_MANAGEMENT\",\"SOURCE_ALARM_SERVICE\",\"SOURCE_ANDROID_TV\",\"SOURCE_AUDIO_IO\",\"SOURCE_BACKUP_MANAGER\",\"SOURCE_BINDER\",\"SOURCE_BLUETOOTH\",\"SOURCE_BOOT_EVENT\",\"SOURCE_BROADCAST_RECEIVER_MANAGEMENT\",\"SOURCE_CLIPBOARD\",\"SOURCE_COMPONENT_MANAGEMENT\",\"SOURCE_CONTENT_PROVIDER\",\"SOURCE_CONTENT_PROVIDER_MANAGEMENT\",\"SOURCE_DATABASE\",\"SOURCE_DEVICE_ADMIN\",\"SOURCE_DEVICE_SEARCH\",\"SOURCE_DEVICE_USER\",\"SOURCE_DISPLAY\",\"SOURCE_DROPBOX\",\"SOURCE_EMAIL\",\"SOURCE_EXPERIMENTAL\",\"SOURCE_FILE\",\"SOURCE_FILE_SYSTEM_MANAGEMENT\",\"SOURCE_FINGERPRINT\",\"SOURCE_FLASHLIGHT\",\"SOURCE_HDMI\",\"SOURCE_IDLE_DOCK_SCREEN\",\"SOURCE_IMS\",\"SOURCE_INFRARED\",\"SOURCE_INSTALLED_PACKAGES\",\"SOURCE_JSSE_TRUST_MANAGER\",\"SOURCE_KEYCHAIN\",\"SOURCE_KEYGUARD\",\"SOURCE_LOCATION\",\"SOURCE_MACHINE_LEARNING\",\"SOURCE_MEDIA_LOCAL_MANAGEMENT\",\"SOURCE_MEDIA_LOCAL_PLAYBACK\",\"SOURCE_MEDIA_NETWORK_CONNECTION\",\"SOURCE_MEDIA_REMOTE_PLAYBACK\",\"SOURCE_NETWORK_MANAGEMENT\",\"SOURCE_NFC\",\"SOURCE_NOTIFICATION\",\"SOURCE_PAC_PROXY\",\"SOURCE_PERMISSIONS\",\"SOURCE_PERSISTANT_DATA\",\"SOURCE_POWER_MANAGEMENT\",\"SOURCE_PRINT_SERVICE\",\"SOURCE_PROCESS_MANAGEMENT\",\"SOURCE_RPC\",\"SOURCE_SCREEN_AUDIO_CAPTURE\",\"SOURCE_SERIAL_PORT\",\"SOURCE_SERVICE_MANAGEMENT\",\"SOURCE_SMS_MMS\",\"SOURCE_SPEECH_INTERACTION\",\"SOURCE_STATUS_BAR\",\"SOURCE_SYNC_FRAMEWORK\",\"SOURCE_TELEPHONY\",\"SOURCE_TEXT_SERVICES\",\"SOURCE_THREADING\",\"SOURCE_TIME_EVENT\",\"SOURCE_UI\",\"SOURCE_UI_AUTOMATION\",\"SOURCE_UI_RPC\",\"SOURCE_UID_EVENT\",\"SOURCE_USAGE_STATS\",\"SOURCE_USB\",\"SOURCE_USER_ACCOUNTS_MANAGEMENT\",\"SOURCE_VIBRATOR\",\"SOURCE_WAKE_LOCK\",\"SOURCE_WALLPAPER_MANAGER\",\"SOURCE_WAP\",\"SOURCE_WEB_BROWSER\",\"SOURCE_WIDGETS\",\"SOURCE_SINK_IPC\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}