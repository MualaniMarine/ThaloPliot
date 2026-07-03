package com.mualanimarine.betterreeflightmanager.util

import android.content.Context

class SharedPreferencesUtil private constructor(context: Context) {
    private val dataStore = context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    fun setNetName(value: String?) = putString(KEY_NET_NAME, value)
    fun getNetName(): String? = dataStore.getString(KEY_NET_NAME, null)

    fun setNetNamePass(name: String?, pass: String?) {
        putString(KEY_NET_NAME, name)
        putString(KEY_NET_PASS, pass)
    }

    fun getNetPass(): String? = dataStore.getString(KEY_NET_PASS, null)

    fun setApModel(value: Boolean) {
        dataStore.edit().putBoolean(KEY_AP_MODEL, value).apply()
    }

    fun isApModel(): Boolean = dataStore.getBoolean(KEY_AP_MODEL, true)

    fun setK7(value: String?) = putString(KEY_K7, value)
    fun getK7(): String? = dataStore.getString(KEY_K7, null)

    fun setX4(value: String?) = putString(KEY_X4, value)
    fun getX4(): String? = dataStore.getString(KEY_X4, null)

    fun setLastAddress(value: String?) = putString(KEY_LAST_ADDRESS, value)
    fun getLastAddress(): String = dataStore.getString(KEY_LAST_ADDRESS, "") ?: ""

    fun setDeviceNameByIp(ip: String?, name: String?) {
        if (ip.isNullOrBlank()) return
        putString(deviceNameKey(ip), name)
    }

    fun getDeviceNameByIp(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        return dataStore.getString(deviceNameKey(ip), null)
    }

    fun setCurveGroups(type: Int, value: String?) = putString(curveGroupsKey(type), value)
    fun getCurveGroups(type: Int): String? = dataStore.getString(curveGroupsKey(type), null)

    fun setSelectedCurveGroupId(type: Int, value: String?) = putString(selectedCurveGroupKey(type), value)
    fun getSelectedCurveGroupId(type: Int): String? = dataStore.getString(selectedCurveGroupKey(type), null)

    fun isSet(): Boolean = !getNetName().isNullOrEmpty()

    private fun putString(key: String, value: String?) {
        dataStore.edit().putString(key, value).apply()
    }

    companion object {
        private const val STORE_NAME = "data"
        private const val KEY_NET_NAME = "ssid"
        private const val KEY_NET_PASS = "ssid_pass"
        private const val KEY_K7 = "k7"
        private const val KEY_X4 = "x4"
        private const val KEY_AP_MODEL = "model"
        private const val KEY_LAST_ADDRESS = "last_address"

        private fun curveGroupsKey(type: Int): String = "curve_groups_$type"
        private fun selectedCurveGroupKey(type: Int): String = "curve_selected_$type"
        private fun deviceNameKey(ip: String): String = "device_name_${ip.replace('.', '_')}"

        @Volatile
        private var instance: SharedPreferencesUtil? = null

        fun getInstance(context: Context): SharedPreferencesUtil {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesUtil(context).also { instance = it }
            }
        }
    }
}

