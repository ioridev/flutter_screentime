package com.example.flutter_screentime

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

const val CHANNEL_ID = "BlockAppService_Channel_ID"
const val NOTIFICATION_ID = 1


class BlockAppService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val CHANNEL = "flutter_screentime"
    private var isOverlayDisplayed = false
    private val userApps = ArrayList<ResolveInfo>()

    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )


    fun isDeviceLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        return keyguardManager.isKeyguardLocked
    }


    fun blockApps() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        fun isAppInForeground(context: Context): Boolean {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000 * 60 * 60 * 24 * 3  // 3 days

            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            var lastEvent: UsageEvents.Event? = null
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEvent = event
                }
            }

            // Check if the most recent app is in the userApps list
            if (lastEvent != null) {
                for (app in userApps) {
                    if (app.activityInfo.packageName == lastEvent.packageName) {
                        return true
                    }
                }
            }
            return false
        }

        fun isLauncherApp(resolveInfo: ResolveInfo, context: Context): Boolean {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            val defaultLauncher = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return resolveInfo.activityInfo.packageName == defaultLauncher?.activityInfo?.packageName
        }


        fun blockApp() {
            val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
                    val shouldRunLogic = sharedPreferences.getBoolean("Blocking", false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(this@BlockAppService)|| !hasUsageStatsPermission(this@BlockAppService)) {
                            editor.putBoolean("isBlocking", false)
                            editor.apply()
                        }
                    }
                    if (isAppInForeground(this@BlockAppService) && !isDeviceLocked(this@BlockAppService)) {
                        if (!isOverlayDisplayed && shouldRunLogic && overlayView?.windowToken == null) {
                            isOverlayDisplayed = true
                            windowManager?.addView(overlayView, params)
                        }
                    } else {

                        if (isOverlayDisplayed && overlayView?.windowToken != null) {
                            isOverlayDisplayed = false
                            windowManager?.removeView(overlayView)
                        }
                    }
                    blockApp()
                }
            }, 500)
        }
        userApps.clear()
        // Filter out system apps
        for (app in apps) {
            // Exclude system apps
            if ((app.activityInfo.packageName == "com.android.chrome" ) ||
                ((app.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                !app.activityInfo.name.contains("com.android.launcher") &&
                !isLauncherApp(app, this) &&
                app.activityInfo.packageName != this.packageName)
            ) {
                userApps.add(app) 
            }
        }
        blockApp()
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("[DEBUG] onStartCommand()")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.block_overlay, null)

        // 通知チャンネルを作成（APIレベル26以降）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "BlockAppService Channel", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false);
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // フォアグラウンドサービスのための通知を作成
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BlockAppService")
            .setContentText("Service is running...")
            .build()

        // startForegroundを呼び出す
        startForeground(NOTIFICATION_ID, notification)

        blockApps()

        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
        // 必要に応じてリソースの解放や終了処理を行います。
    }
}
