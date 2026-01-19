package com.wqh.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val cfgEditText = findViewById<EditText>(R.id.editTextJson)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val context = this@MainActivity

        btnAccessibility.setOnClickListener {
            /*if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                return@setOnClickListener
            }*/

            if (!isAccessibilityServiceEnabled(context, MyAccessibilityService::class.java)) {
                Toast.makeText(context, "请开启无障碍权限", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            //startService(Intent(context, FloatingService::class.java))
            Toast.makeText(context, "服务已启动", Toast.LENGTH_SHORT).show()
            btnAccessibility.text = "服务已启动"
        }

        // 初始显示当前配置
        cfgEditText.setText(SettingUtil.data.toString(4) ?: "")
        btnSave.setOnClickListener {
            try {
                val jsonText = JSONObject(cfgEditText.text.toString().trim()).toString(4)
                cfgEditText.setText(jsonText)

                SettingUtil.save(context, jsonText) // 格式化 JSON 保存
                Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "JSON 格式错误", Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServices =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
        return enabledServices.split(":")
            .any { ComponentName.unflattenFromString(it)?.equals(expectedComponentName) == true }
    }
}
