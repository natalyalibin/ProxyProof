
package com.proxyproof.app.attendance

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import java.util.Date

data class VerifiedAttendance(
    val studentId: String,
    val studentName: String,
    val classCode: String,
    val markedAt: Long,
    val signalStrength: Int
)

class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val sessions = firestore.collection("classSessions")

    fun startSession(
        classCode: String,
        teacherId: String,
        token: ByteArray,
        durationMillis: Long = 5 * 60 * 1000L,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val code = classCode.trim().uppercase()

        if (code.isBlank()) {
            onError(IllegalArgumentException("Class code cannot be empty"))
            return
        }

        val data = hashMapOf<String, Any>(
            "teacherId" to teacherId,
            "active" to true,
            "tokenHash" to token.sha256(),
            "minimumRssi" to -75,
            "expiresAt" to Date(System.currentTimeMillis() + durationMillis),
            "startedAt" to FieldValue.serverTimestamp()
        )

        sessions.document(code)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun endSession(
        classCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        sessions.document(classCode.trim().uppercase())
            .update(
                "active", false,
                "endedAt", FieldValue.serverTimestamp()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun observeAttendance(
        classCode: String,
        onUpdate: (List<VerifiedAttendance>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return sessions.document(classCode.trim().uppercase())
            .collection("attendance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val records = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val markedAt = when (val value = doc.get("markedAt")) {
                        is Timestamp -> value.toDate().time
                        is Date -> value.time
                        is Number -> value.toLong()
                        else -> null
                    } ?: return@mapNotNull null

                    VerifiedAttendance(
                        studentId = doc.getString("studentId") ?: doc.id,
                        studentName = doc.getString("studentName") ?: "Student",
                        classCode = classCode.trim().uppercase(),
                        markedAt = markedAt,
                        signalStrength = (doc.getLong("signalStrength") ?: 0L).toInt()
                    )
                }

                onUpdate(records)
            }
    }

    fun getSession(
        classCode: String,
        onSuccess: (active: Boolean, expiresAt: Date?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        sessions.document(classCode.trim().uppercase())
            .get()
            .addOnSuccessListener { document ->
                onSuccess(
                    document.getBoolean("active") == true,
                    document.getDate("expiresAt")
                )
            }
            .addOnFailureListener { onError(it) }
    }
}
