# The Garmin Connect IQ SDK ships no rules of its own. Android finds its parcelables (IQDevice, IQApp) by class name and
# Garmin Connect finds its binder interfaces by name, so renaming or removing any of it breaks the watch link.
-keep class com.garmin.android.connectiq.** { *; }
-keep class com.garmin.android.apps.connectmobile.connectiq.** { *; }
-keep class com.garmin.monkeybrains.** { *; }
