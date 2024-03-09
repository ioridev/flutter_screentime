package com.example.flutter_screentime

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import io.flutter.embedding.android.FlutterActivity
import android.view.View
import android.view.WindowManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import android.Manifest



var job = Job()
val scope = CoroutineScope(Dispatchers.Default + job)

class MainActivity: FlutterActivity() {

    private val CHANNEL = "flutter_screentime"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        var isOverlayDisplayed = false
        val userApps = ArrayList<ResolveInfo>()
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()




        fun checkDrawOverlayPermission(activity: Activity): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(activity)) {
                        return false
                    }
                }
                return true
            }

            fun requestDrawOverlayPermission(activity: Activity, requestCode: Int): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(activity)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.packageName)
                        )
                        activity.startActivityForResult(intent, requestCode)
                        return false
                    }
                }
                return true
            }

        fun isServiceRunning(serviceClassName: String, context: Context): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClassName == service.service.className) {
                    return true
                }
            }
            return false
        }

        fun setAlarm(hour: Int, minute: Int, second: Int) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
            }
        
            val intent = Intent(this, AlarmReceiver::class.java)
        
            // Android 12 (API Level 31) 以降で必要な FLAG_IMMUTABLE の追加
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, pendingIntentFlags)
        
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
        
        fun checkQueryAllPackagesPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES)
            } else {
                true
            }
        }

        fun requestQueryAllPackagesPermission(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (activity.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(arrayOf(Manifest.permission.QUERY_ALL_PACKAGES), 2)
                    return false
                }
            }
            return true
        }

        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun requestUsageStatsPermission(context: Context) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }





        super.configureFlutterEngine(flutterEngine)
            MethodChannel(
                flutterEngine.dartExecutor.binaryMessenger,
                CHANNEL
            ).setMethodCallHandler { call, result ->
                when (call.method) {

                    "blockApp" -> {
                        editor.putBoolean("Blocking", true)
                        editor.putBoolean("isBlocking", true)
                        editor.apply()

                        val intent = Intent(this, BlockAppService::class.java)
                        startForegroundService(intent)
                        println("[DEBUG]blockApp")
                        val isRunning = isServiceRunning(BlockAppService::class.java.name, context)
                        println("[DEBUG]isRunning: $isRunning")

                        result.success(null)
                    }
                    "unblockApp" -> {
                        editor.putBoolean("Blocking", false)
                        editor.apply()
                        result.success(null)
                    } "checkPermission" -> {
                    val hasOverlayPermission = checkDrawOverlayPermission(this)
                    val hasQueryPermission = checkQueryAllPackagesPermission(this)
                    val hasUsageStatsPermission = hasUsageStatsPermission(this)

                    if (hasOverlayPermission && hasQueryPermission && hasUsageStatsPermission) {
                        result.success("approved")
                    } else {
                        result.success("denied")
                    }
                }
                    "requestAuthorization" -> {
                        val hasOverlayPermission = requestDrawOverlayPermission(this, 1234)
                        val hasQueryPermission = requestQueryAllPackagesPermission(this)
                        val hasUsageStatsPermission = hasUsageStatsPermission(this)
                        if (!hasUsageStatsPermission) {
                            requestUsageStatsPermission(this)
                        }

                        if (hasOverlayPermission && hasQueryPermission && hasUsageStatsPermission) {
                            result.success("approved")
                        } else {
                            result.success("denied")
                        }
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        }

}

