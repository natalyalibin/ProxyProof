package com.proxyproof.app.attendance


import android.content.Context
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed class DeviceBindingResult {
    data class Bound(val deviceId: String) : DeviceBindingResult()
    data class AlreadyBound(val deviceId: String) : DeviceBindingResult()
    data class ApprovalRequired(val registeredDeviceId: String) :
        DeviceBindingResult()
    data class Failed(val message: String) : DeviceBindingResult()
}

class DeviceBindingManager(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val appContext = context.applicationContext

    /**
     * Identifies this Android device for the app.
     * This is an identifier, not cryptographic proof of device identity.
     */
    private fun getDeviceId(): String? {
        return Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    /**
     * Checks the registered device for the currently signed-in user.
     *
     * First-time binding requires Firestore rules or a trusted backend
     * that explicitly permits the initial deviceId assignment.
     */
    fun checkOrBindDevice(
        onResult: (DeviceBindingResult) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(DeviceBindingResult.Failed("Please sign in first."))
            return
        }

        val currentDeviceId = getDeviceId()

        if (currentDeviceId.isNullOrBlank()) {
            onResult(
                DeviceBindingResult.Failed(
                    "Could not identify this device."
                )
            )
            return
        }

        val userDocument = firestore.collection("users")
            .document(user.uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocument)

            if (!snapshot.exists()) {
                throw IllegalStateException(
                    "Your user profile does not exist in Firestore."
                )
            }

            val active = snapshot.getBoolean("active") == true
            val role = snapshot.getString("role")

            if (!active || role != "student") {
                throw IllegalStateException(
                    "Only active student accounts can bind a device."
                )
            }

            val registeredDeviceId =
                snapshot.getString("deviceId")

            when {
                registeredDeviceId.isNullOrBlank() -> {
                    transaction.set(
                        userDocument,
                        mapOf("deviceId" to currentDeviceId),
                        SetOptions.merge()
                    )
                    DeviceBindingResult.Bound(currentDeviceId)
                }

                registeredDeviceId == currentDeviceId -> {
                    DeviceBindingResult.AlreadyBound(currentDeviceId)
                }

                else -> {
                    DeviceBindingResult.ApprovalRequired(
                        registeredDeviceId
                    )
                }
            }
        }.addOnSuccessListener { result ->
            onResult(result)
        }.addOnFailureListener { error ->
            onResult(
                DeviceBindingResult.Failed(
                    error.localizedMessage
                        ?: "Could not check device registration."
                )
            )
        }
    }
}
