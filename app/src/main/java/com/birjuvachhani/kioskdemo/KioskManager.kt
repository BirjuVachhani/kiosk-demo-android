package com.birjuvachhani.kioskdemo

import android.annotation.SuppressLint
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.os.BatteryManager
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/*
 * Created by Birju Vachhani on 08 August 2019
 * Copyright © 2019 KioskDemo. All rights reserved.
 */

class KioskManager(private val activity: FragmentActivity) : ContextWrapper(activity) {

    private var musicIntentReceiver: MusicIntentReceiver = MusicIntentReceiver()
    private var isHeadsetPlugged = false
    private val mAdminComponentName: ComponentName by lazy {
        KioskDeviceAdminReceiver.getComponentName(this)
    }

    private val mDevicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val isAdmin: Boolean by lazy {
        mDevicePolicyManager.isDeviceOwnerApp(packageName)
    }

    companion object {
        const val TAG = "KioskManager"
    }

    init {
        activity.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
                registerReceiver(musicIntentReceiver, filter)
            }

            fun onStop() {
                unregisterReceiver(musicIntentReceiver)
            }
        })
    }

    private inner class MusicIntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                when (intent.getIntExtra("state", -1)) {
                    0 -> {
                        isHeadsetPlugged = false
                        Log.d(TAG, "Headset is unplugged")
                    }
                    1 -> {
                        isHeadsetPlugged = true
                        Log.d(TAG, "Headset is plugged")
                    }
                    else -> Log.wtf(TAG, "I have no idea what the headset state is")
                }
            }
        }
    }

    fun setKioskMode(enable: Boolean) {
        if (isAdmin) {
            setRestrictions(enable)
            enableStayOnWhilePluggedIn(enable)
            setUpdatePolicy(enable)
            setAsHomeApp(enable)
            setKeyGuardEnabled(enable)
        }
        setLockTask(enable)
        setImmersiveMode(enable)
    }

    // region restrictions
    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
        setUserRestriction(UserManager.DISALLOW_APPS_CONTROL, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction)
    } else {
        mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction)
    }

    private fun enableStayOnWhilePluggedIn(active: Boolean) = if (active) {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_USB or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    } else {
        mDevicePolicyManager.setGlobalSetting(mAdminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0")
    }

    private fun setLockTask(start: Boolean) {
        if (isAdmin) {
            mDevicePolicyManager.setLockTaskPackages(
                mAdminComponentName,
                if (start) arrayOf(packageName) else arrayOf()
            )
        }
        if (start) {
            activity.startLockTask()
        } else {
            activity.stopLockTask()
        }
    }

    @SuppressLint("NewApi")
    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            mDevicePolicyManager.setSystemUpdatePolicy(
                mAdminComponentName,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null)
        }
    }

    private fun setAsHomeApp(enable: Boolean) {
        if (enable) {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            mDevicePolicyManager.addPersistentPreferredActivity(
                mAdminComponentName, intentFilter, ComponentName(packageName, MainActivity::class.java.name)
            )
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                mAdminComponentName, packageName
            )
        }
    }

    @SuppressLint("NewApi")
    private fun setKeyGuardEnabled(enable: Boolean) {
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, !enable)
    }

    private fun setImmersiveMode(enable: Boolean) {
        if (enable) {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            activity.window.decorView.systemUiVisibility = flags
        } else {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            activity.window.decorView.systemUiVisibility = flags
        }
    }

    class KioskDeviceAdminReceiver : DeviceAdminReceiver() {
        companion object {
            fun getComponentName(context: Context): ComponentName {
                return ComponentName(context.applicationContext, KioskDeviceAdminReceiver::class.java)
            }

            private val TAG = KioskDeviceAdminReceiver::class.java.simpleName
        }

        override fun onLockTaskModeEntering(context: Context?, intent: Intent?, pkg: String?) {
            super.onLockTaskModeEntering(context, intent, pkg)
            Log.d(TAG, "onLockTaskModeEntering")
        }

        override fun onLockTaskModeExiting(context: Context?, intent: Intent?) {
            super.onLockTaskModeExiting(context, intent)
            Log.d(TAG, "onLockTaskModeExiting")
        }
    }
}