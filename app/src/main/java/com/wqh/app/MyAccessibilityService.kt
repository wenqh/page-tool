package com.wqh.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


class MyAccessibilityService : AccessibilityService() {
    //屏幕参数：824,1648
    private var centerX: Float = (824/2).toFloat()

//    private var _startY: Float = 2200f
//    private var _endY: Float = 200f
//    private var _duration: Long = 200
    lateinit var appCfg: SettingUtil.AppCfg

    var currentPackageName: String? = null
    var currentClassName: String? = null

    companion object {
        var instance: MyAccessibilityService? = null
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(null, "无障碍服务已连接")

        SettingUtil.load(this)
        startService(Intent(this, FloatingService::class.java))

        /*
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds: Rect = windowManager.currentWindowMetrics.bounds
        centerX = bounds.width() / 2f

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        centerX = (displayMetrics.widthPixels / 2).toFloat()*/
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && it.packageName != null
                && it.packageName != "com.android.systemui"
                && it.packageName != "com.google.android.inputmethod.latin"
                && it.packageName != "com.xrz.standby"
                && it.className != "android.widget.FrameLayout"
            ) {
                currentPackageName = it.packageName?.toString()
                currentClassName = it.className?.toString()
            }
        }
    }

    fun scrollApp(up: Boolean) {
        appCfg = SettingUtil.getAppCfg(currentPackageName, currentClassName)

        if (appCfg.scroll == "ACTION") {
            val action = if (up) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            findScrollableNodeRecursive(rootInActiveWindow, action)?.performAction(action)
            return
        }

        var start = appCfg.y2
        var end = appCfg.y1
        if (!up) {
            start = end.also { end = start }
        }

        val path = Path().apply {
            moveTo(centerX, start)
            lineTo(centerX, end)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, appCfg.duration))
            .build()

        dispatchGesture(gesture, null, null)
    }

    //accessibilityFlags="flagDefault|flagRequestFilterKeyEvents" canRetrieveWindowContent="true"
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // 只处理按键按下的事件
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_F2 -> {
                scrollApp(up = true)
            }
            KeyEvent.KEYCODE_F1/*, KeyEvent.KEYCODE_VOLUME_UP*/ -> {
                scrollApp(up = false)
            }
        }

        return true // 返回true表示事件已被处理，不会传递给系统（即不会调整音量）
    }
    private fun findScrollableNodeRecursive(node: AccessibilityNodeInfo?, action: Int): AccessibilityNodeInfo? {
        if (node == null) return null

        // 检查当前节点是否可滚动且支持指定操作
        if (node.isScrollable && node.isVisibleToUser &&
            /*node.className != "android.widget.HorizontalScrollView" && //下面action已经判断了
            node.className != "androidx.viewpager2.widget.ViewPager2" &&
            node.className != "androidx.viewpager.widget.ViewPager" &&
            node.className != "android.support.v4.view.ViewPager" &&*/
            node.actionList.any { it.id == action/*AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP.id
                        || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN.id
                        || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id
                        || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
                        || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id
                        || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id*/
            } && node.actionList.none({it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
                    || it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
                    //老版本无此字段
                    || it.id == 16908360//AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_LEFT.id
                    || it.id == 16908361}//AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_RIGHT.id
            )) {
            return node // 直接返回当前节点
        }

        // 递归查找子节点
        // 为了优化，可以优先查找特定类型的容器，如 RecyclerView, ListView, ScrollView, ViewPager2等
        // 但一个通用的方法是遍历所有子节点
        for (i in 0 until node.childCount) {
            val find =  findScrollableNodeRecursive(node.getChild(i), action)
            if (find != null) {
                return find
            }
        }
        return null // 未找到
    }

    override fun onInterrupt() {}
}
