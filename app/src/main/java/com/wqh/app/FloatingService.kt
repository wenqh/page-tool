package com.wqh.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs


class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView1: MyOverlayView
    private lateinit var floatingView2: MyOverlayView

    private var startX = 0f
    private var startY = 0f
    private val touchSlop = 15f // 滑动判定阈值
    private var actionDownTime = 0L

    private var simulatedFlag = false
    private var multiTouch = false

    override fun onCreate() {
        super.onCreate()

        floatingView1 = MyOverlayView(this)
        floatingView2 = MyOverlayView(this)
        floatingView1.setBackgroundColor(Color.TRANSPARENT) // 完全透明
        floatingView2.setBackgroundColor(Color.TRANSPARENT) // 完全透明
//        floatingView.setBackgroundColor(0x22000000) // 半透明
        /*floatingView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1, Color.BLACK)  // 边框
        }*/

        val layoutParams1 = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            width = 50 // 宽度：边缘触发区域
            height = WindowManager.LayoutParams.MATCH_PARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN /*or //全屏
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS //全屏*/
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        val layoutParams2 = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            width = 50 // 宽度：边缘触发区域
            height = WindowManager.LayoutParams.MATCH_PARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN /*or //全屏
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS //全屏*/
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView1, layoutParams1)
        windowManager.addView(floatingView2, layoutParams2)

        val touchListener = View.OnTouchListener{ v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX
                    startY = e.rawY
                    actionDownTime = SystemClock.uptimeMillis()
                    multiTouch = false
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    multiTouch = true  // n指按下
                }

                MotionEvent.ACTION_UP -> {
                    val dx = abs(e.rawX - startX)
                    val dy = abs(e.rawY - startY)
                    if (SystemClock.uptimeMillis() - actionDownTime < 200 && dx < touchSlop && dy < touchSlop) {
                        if (simulatedFlag) {
                            v.setBackgroundColor(Color.BLACK)
                            Handler(Looper.getMainLooper()).postDelayed({v.setBackgroundColor(Color.TRANSPARENT)}, 500)
                            return@OnTouchListener true
                        }
                        
                        val layoutParams = v.layoutParams as WindowManager.LayoutParams
                        
                        layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        windowManager.updateViewLayout(v, layoutParams)

                        //发送点击事件结束后恢复悬浮窗
                        Handler(Looper.getMainLooper()).postDelayed({
                            simulatedFlag = true
                            MyAccessibilityService.instance?.dispatchGesture(
                                GestureDescription.Builder()
                                    .addStroke(GestureDescription.StrokeDescription(
                                        Path().apply { moveTo(e.rawX, e.rawY) }, 0, 50)).build(),
                                object : AccessibilityService.GestureResultCallback() { // Kotlin 中匿名类使用 object 关键字
                                    override fun onCompleted(gestureDescription: GestureDescription?) {
                                        layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                        windowManager.updateViewLayout(v, layoutParams)
                                        simulatedFlag = false
                                    }
                                    override fun onCancelled(gestureDescription: GestureDescription?) {
                                        super.onCancelled(gestureDescription)
                                        onCompleted(gestureDescription)
                                    }
                                }, null
                            )}, 70)
                        v.performClick()
                    } else if(dy > touchSlop && dy > dx) {
                        if(multiTouch && e.rawY > startY) {
                            MyAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                            return@OnTouchListener true
                        }
                        MyAccessibilityService.instance?.scrollApp(e.rawY <= startY)
                        MyAccessibilityService.instance?.appCfg?.apply {
                            floatingView1.redraw(if (!mark1.isNaN()) mark1 else y1, y2)
                            floatingView2.redraw(if (!mark1.isNaN()) mark1 else y1, y2)
                        }
                    } else if (SystemClock.uptimeMillis() - actionDownTime >= 3000L/*500*/) {
                        copyTextToClipboard(MyAccessibilityService.instance?.currentPackageName + "\n"
                                + MyAccessibilityService.instance?.currentClassName +"\n" + e.rawY.toInt())
                        floatingView1.redraw(e.y, null)

                        //换位
                        /*if (SystemClock.uptimeMillis() - actionDownTime >= 5000L) {
                            layoutParams.gravity = (Gravity.START or Gravity.END) xor layoutParams.gravity or Gravity.CENTER_VERTICAL
                            windowManager.updateViewLayout(floatingView1, layoutParams)
                        }*/
                    }

                    /*if(abs(e.rawX - startX) > abs(e.rawY - startY)) {
                        return@setOnTouchListener true;
                    }
                    //滑动松开
                    if (abs(e.rawY - startY) >= threshold) {
                        MyAccessibilityService.instance?.scrollApp(e.rawY <= startY)

                        floatingView.redraw(MyAccessibilityService.instance?.appCfg?.y1, MyAccessibilityService.instance?.appCfg?.y2)

                        return@setOnTouchListener true;
                    }

                    //点击松开
                    if (SystemClock.uptimeMillis() - actionDownTime < 500L) {
                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        windowManager.updateViewLayout(floatingView, params)

                        //发送点击事件结束后恢复悬浮窗
                        Handler(Looper.getMainLooper()).postDelayed({
                            simulatedFlag = true
                            MyAccessibilityService.instance?.dispatchGesture(
                                    GestureDescription.Builder()
                                        .addStroke(GestureDescription.StrokeDescription(
                                            Path().apply { moveTo(e.rawX, e.rawY) }, 0, 50)).build(),
                            object : AccessibilityService.GestureResultCallback() { // Kotlin 中匿名类使用 object 关键字
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                    windowManager.updateViewLayout(floatingView, params)
                                    simulatedFlag = false
                                }
                            }, null
                            )}, 60)


                        *//*Handler(Looper.getMainLooper()).postDelayed({
                           params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                           windowManager.updateViewLayout(floatingView, params)
                       }, 1000)*//*
                        floatingView.performClick()
                    } else { //长按松开
                        copyTextToClipboard(MyAccessibilityService.instance?.currentClassName + " " + e.rawY.toInt())
                        floatingView.redraw(e.y, null)
                    }*/
                    /*
                    val popupMenu = PopupMenu(this, v)
                    popupMenu.menu.add(0, 0, 0, "暂时隐藏");
                    popupMenu.menu.add(0, 1, 1, "切换位置");
                    popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                        val id = item.itemId
                        if (id == 0) {
                            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            windowManager.updateViewLayout(floatingView, layoutParams)

                            Handler(Looper.getMainLooper()).postDelayed({
                                layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                windowManager.updateViewLayout(floatingView, layoutParams)
                            },5000)
                        }
                        else if (id == 1) {
                            layoutParams.gravity = (Gravity.START or Gravity.END) xor layoutParams.gravity or Gravity.CENTER_VERTICAL
                            windowManager.updateViewLayout(floatingView, layoutParams)
                        }
                        true
                    })
                    popupMenu.show()
                    */
                }
            }

            return@OnTouchListener true;
        }
        floatingView1.setOnTouchListener(touchListener)
        floatingView2.setOnTouchListener(touchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView1)
        windowManager.removeView(floatingView2)
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun copyTextToClipboard(textToCopy: String, label: String = "文本内容") {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, textToCopy)
        clipboardManager.setPrimaryClip(clipData)
    }
}
