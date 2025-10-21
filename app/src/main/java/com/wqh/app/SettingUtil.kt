package com.wqh.app;

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject

object SettingUtil {
    private const val KEY = "json"
    var data = JSONObject();

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    }

    fun save(context: Context, data: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY, data.trimIndent()).apply()
        this.data = JSONObject(data)
    }

    fun load(context: Context): JSONObject? {
        val prefs = getPreferences(context)
        return prefs.getString(KEY, null)?.let {
            data = JSONObject(it)
            data
        }
    }

    fun getAppCfg(packageName: String?, className: String?): AppCfg {
        val cfg = mergeConfigs(data.optJSONObject("*"), data.optJSONObject(packageName), data.optJSONObject(className))
        return if(cfg.getString("scroll") != "GESTURE") AppCfg(cfg.getString("scroll"), Float.NaN, Float.NaN, Float.NaN, 0)
                else AppCfg(cfg.getString("scroll"), cfg.optDouble("y1").toFloat(), cfg.optDouble("y2").toFloat(), cfg.optDouble("mark1").toFloat(), cfg.optLong("duration"))
    }
    private fun mergeConfigs(vararg configs: JSONObject?): JSONObject {
        val result = JSONObject()
        // 从最低优先级开始，越靠后优先级越高，后面的值会覆盖前面的
        for (config in configs) {
            if (config == null) continue
            for (key in config.keys()) {
                result.put(key, config.get(key))
            }
        }
        return result
    }
    data class AppCfg(val scroll: String, val y1: Float, val y2: Float, val mark1: Float, val duration: Long)
}
