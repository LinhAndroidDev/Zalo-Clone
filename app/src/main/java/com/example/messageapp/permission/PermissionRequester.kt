package com.example.messageapp.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.messageapp.R

/**
 * Điểm quản lý xin quyền duy nhất của app.
 *
 * Phải khởi tạo như property của Fragment/Activity (trước khi STARTED):
 * ```
 * private val permissions = PermissionRequester(this)
 * ...
 * permissions.request(AppPermission.VIDEO_CALL) { startVideoCall() }
 * ```
 * Xử lý sẵn: đã có quyền thì chạy luôn; bị từ chối thì báo lý do;
 * từ chối vĩnh viễn thì hỏi mở Cài đặt.
 */
class PermissionRequester private constructor(
    caller: ActivityResultCaller,
    private val contextProvider: () -> Context?,
    private val activityProvider: () -> android.app.Activity?,
) {
    constructor(fragment: Fragment) : this(
        fragment,
        { fragment.context },
        { fragment.activity },
    )

    constructor(activity: ComponentActivity) : this(
        activity,
        { activity },
        { activity },
    )

    private var pendingPermission: AppPermission? = null
    private var pendingOnGranted: (() -> Unit)? = null
    private var pendingOnDenied: ((permanentlyDenied: Boolean) -> Unit)? = null
    private var pendingShowDefaultDenied = true

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> onResult(grants) }

    /**
     * @param onDenied gọi khi bị từ chối (tham số: có bị chặn vĩnh viễn không).
     * @param showDefaultDeniedUi true: tự hiển thị Toast/hộp thoại mở Cài đặt khi bị từ chối.
     */
    fun request(
        permission: AppPermission,
        showDefaultDeniedUi: Boolean = true,
        onDenied: ((permanentlyDenied: Boolean) -> Unit)? = null,
        onGranted: () -> Unit,
    ) {
        val context = contextProvider() ?: return
        if (isGranted(context, permission)) {
            onGranted()
            return
        }
        pendingPermission = permission
        pendingOnGranted = onGranted
        pendingOnDenied = onDenied
        pendingShowDefaultDenied = showDefaultDeniedUi
        launcher.launch(permission.permissions)
    }

    private fun onResult(grants: Map<String, Boolean>) {
        val permission = pendingPermission ?: return
        val onGranted = pendingOnGranted
        val onDenied = pendingOnDenied
        val showUi = pendingShowDefaultDenied
        pendingPermission = null
        pendingOnGranted = null
        pendingOnDenied = null

        val granted = if (permission.requireAll) grants.values.all { it } else grants.values.any { it }
        if (granted) {
            onGranted?.invoke()
            return
        }
        val activity = activityProvider()
        val permanentlyDenied = activity != null && permission.permissions.any {
            grants[it] == false && !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (showUi) showDeniedUi(permission, permanentlyDenied)
        onDenied?.invoke(permanentlyDenied)
    }

    private fun showDeniedUi(permission: AppPermission, permanentlyDenied: Boolean) {
        val activity = activityProvider() ?: return
        if (permanentlyDenied) {
            AlertDialog.Builder(activity)
                .setMessage(permission.deniedMessageRes)
                .setPositiveButton(R.string.permission_open_settings) { _, _ -> openAppSettings(activity) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            Toast.makeText(activity, permission.deniedMessageRes, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun isGranted(context: Context, permission: AppPermission): Boolean {
            if (permission.permissions.isEmpty()) return true
            val check = { p: String ->
                ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            }
            return if (permission.requireAll) permission.permissions.all(check)
            else permission.permissions.any(check)
        }

        fun openAppSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
