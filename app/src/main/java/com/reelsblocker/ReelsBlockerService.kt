package com.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsBlockerService : AccessibilityService() {

    private val ig = "com.instagram.android"
    private var overlay: View? = null
    private var overlayRect: Rect? = null
    private var lastAction = 0L
    private var lastScan = 0L

    private class Scan {
        var reelsTab: AccessibilityNodeInfo? = null
        var homeTab: AccessibilityNodeInfo? = null
        var viewerOpen = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != ig) {
            removeOverlay(); return
        }
        val now = System.currentTimeMillis()
        if (now - lastScan < 120) return
        lastScan = now

        val root = rootInActiveWindow ?: return
        val scan = Scan()
        walk(root, scan, 0)

        // 1) نخلي تاب الريلز مش بيستجيب للمس
        val tab = scan.reelsTab
        if (tab != null) {
            val r = Rect(); tab.getBoundsInScreen(r)
            showOverlay(r)
        } else {
            removeOverlay()
        }

        // 2) لو صفحة الريلز اتفتحت بأي طريقة نرجّع المستخدم
        val onReelsTab = tab?.isSelected == true
        if ((scan.viewerOpen || onReelsTab) && now - lastAction > 700) {
            lastAction = now
            val home = scan.homeTab
            if (onReelsTab && home != null && clickNode(home)) return
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun walk(n: AccessibilityNodeInfo?, s: Scan, depth: Int) {
        if (n == null || depth > 30) return
        val id = n.viewIdResourceName ?: ""
        val desc = n.contentDescription?.toString() ?: ""

        if (id.contains("clips_viewer") || id.contains("clips_swipe_refresh") ||
            id.contains("root_clips_layout") || id.contains("clips_video_container")
        ) s.viewerOpen = true

        if (id.endsWith(":id/clips_tab") ||
            (n.isClickable && isReelsDesc(desc) && isBottomBar(n))
        ) s.reelsTab = n

        if (id.endsWith(":id/feed_tab") ||
            (n.isClickable && isHomeDesc(desc) && isBottomBar(n))
        ) s.homeTab = n

        for (i in 0 until n.childCount) walk(n.getChild(i), s, depth + 1)
    }

    private fun isReelsDesc(d: String) =
        d.contains("reels", true) || d.contains("ريلز")

    private fun isHomeDesc(d: String) =
        d.equals("Home", true) || d.contains("الرئيسية")

    private fun isBottomBar(n: AccessibilityNodeInfo): Boolean {
        val r = Rect(); n.getBoundsInScreen(r)
        val h = resources.displayMetrics.heightPixels
        return r.top > h * 0.85 && r.width() < resources.displayMetrics.widthPixels / 3
    }

    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        while (cur != null) {
            if (cur.isClickable) return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            cur = cur.parent
        }
        return false
    }

    private fun showOverlay(r: Rect) {
        if (r.isEmpty) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(
            r.width(), r.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = r.left; y = r.top
        }
        val v = overlay
        if (v == null) {
            val nv = View(this).apply { setOnTouchListener { _, _ -> true } }
            try { wm.addView(nv, lp); overlay = nv; overlayRect = Rect(r) } catch (_: Exception) {}
        } else if (overlayRect != r) {
            try { wm.updateViewLayout(v, lp); overlayRect = Rect(r) } catch (_: Exception) {}
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v) } catch (_: Exception) {}
        overlay = null; overlayRect = null
    }

    override fun onInterrupt() { removeOverlay() }
    override fun onDestroy() { removeOverlay(); super.onDestroy() }
}
