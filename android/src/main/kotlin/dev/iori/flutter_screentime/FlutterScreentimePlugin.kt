package dev.iori.flutter_screentime

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterScreentimePlugin :
    FlutterPlugin,
    MethodChannel.MethodCallHandler,
    ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "flutter_screentime")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result,
    ) {
        when (call.method) {
            "checkAuthorization" -> result.success(currentAuthorizationStatus())
            "requestAuthorization" -> requestAuthorization(result)
            "setSharedContainerId" -> result.success(null)
            "configureBlockScreen" -> {
                val arguments = call.arguments as? Map<*, *>
                if (arguments == null) {
                    result.error("invalid_args", "Expected a configuration map.", null)
                    return
                }
                ScreenTimePreferences.saveOverlayConfiguration(applicationContext, arguments)
                result.success(null)
            }
            "setBlockedPackages" -> {
                val packageNames = (call.arguments as? List<*>)?.filterIsInstance<String>()
                if (packageNames == null) {
                    result.error("invalid_args", "Expected a list of package names.", null)
                    return
                }
                ScreenTimePreferences.prefs(applicationContext)
                    .edit()
                    .putStringSet(
                        ScreenTimePreferences.keyBlockedPackages,
                        packageNames.toSet(),
                    )
                    .apply()
                result.success(null)
            }
            "selectBlockedApps" -> {
                result.error(
                    "unsupported",
                    "Android does not expose a native blocked apps picker. Use setBlockedPackages instead.",
                    null,
                )
            }
            "startBlocking" -> {
                ScreenTimePreferences.prefs(applicationContext)
                    .edit()
                    .putBoolean(ScreenTimePreferences.keyBlockingEnabled, true)
                    .apply()
                val intent = Intent(applicationContext, BlockAppService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }
                result.success(null)
            }
            "stopBlocking" -> {
                ScreenTimePreferences.prefs(applicationContext)
                    .edit()
                    .putBoolean(ScreenTimePreferences.keyBlockingEnabled, false)
                    .apply()
                applicationContext.stopService(
                    Intent(applicationContext, BlockAppService::class.java),
                )
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun requestAuthorization(result: MethodChannel.Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error(
                "no_activity",
                "requestAuthorization requires a foreground activity.",
                null,
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(currentActivity)
        ) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${currentActivity.packageName}"),
            )
            currentActivity.startActivity(overlayIntent)
        }

        if (!hasUsageStatsPermission(currentActivity)) {
            currentActivity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        result.success(currentAuthorizationStatus())
    }

    private fun currentAuthorizationStatus(): String {
        val hasOverlayPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(applicationContext)

        return if (hasOverlayPermission && hasUsageStatsPermission(applicationContext)) {
            "approved"
        } else {
            "denied"
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
