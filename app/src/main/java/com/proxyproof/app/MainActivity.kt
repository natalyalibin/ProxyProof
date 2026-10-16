
package com.proxyproof.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.proxyproof.app.attendance.AttendanceBleController
import com.proxyproof.app.attendance.AttendanceDashboard
import com.proxyproof.app.ui.theme.ProxyProofTheme

class MainActivity : ComponentActivity() {

    private lateinit var bleController: AttendanceBleController
    private var pendingBluetoothAction: (() -> Unit)? = null

    private val bluetoothPermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            val action = pendingBluetoothAction
            pendingBluetoothAction = null

            if (grants.values.all { it }) {
                action?.invoke()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleController = AttendanceBleController(applicationContext)

        setContent {
            ProxyProofTheme {
                var selectedRole by remember {
                    mutableStateOf<String?>(null)
                }

                var signedInRole by remember {
                    mutableStateOf<String?>(null)
                }

                when {
                    signedInRole != null -> {
                        AttendanceDashboard(
                            role = signedInRole!!,
                            bleController = bleController,
                            onRequireBluetooth = ::withBluetoothPermission,
                            onSignOut = {
                                signedInRole = null
                                selectedRole = null
                            }
                        )
                    }

                    selectedRole == null -> {
                        ProxyProofHome(
                            onTeacherClick = {
                                selectedRole = "teacher"
                            },
                            onStudentClick = {
                                selectedRole = "student"
                            }
                        )
                    }

                    else -> {
                        AuthScreen(
                            role = selectedRole!!,
                            onBack = {
                                selectedRole = null
                            },
                            onAuthenticated = {
                                signedInRole = selectedRole
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::bleController.isInitialized) {
            bleController.stop()
        }
        super.onDestroy()
    }

    private fun withBluetoothPermission(
        forTeacher: Boolean,
        action: () -> Unit
    ) {
        if (!bleController.isEnabled) return

        val permissions = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            if (forTeacher) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val alreadyGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (alreadyGranted) {
            action()
        } else {
            pendingBluetoothAction = action
            bluetoothPermissions.launch(permissions)
        }
    }
}
