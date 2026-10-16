package com.proxyproof.app.attendance


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceHistoryScreen(
    repository: AttendanceRepository,
    classCode: String,
    totalSessions: Int,
    modifier: Modifier = Modifier
) {
    var records by remember(classCode) {
        mutableStateOf<List<VerifiedAttendance>>(emptyList())
    }
    var loading by remember(classCode) { mutableStateOf(true) }
    var errorMessage by remember(classCode) { mutableStateOf<String?>(null) }

    DisposableEffect(repository, classCode) {
        val listener = repository.observeAttendance(
            classCode = classCode,
            onUpdate = { updatedRecords ->
                records = updatedRecords
                loading = false
                errorMessage = null
            },
            onError = { message ->
                loading = false
                errorMessage = message.message ?: "Failed to load attendance history"
            }
        )

        onDispose {
            listener.remove()
        }
    }

    val percentage = if (totalSessions > 0) {
        (records.size * 100.0 / totalSessions).coerceAtMost(100.0)
    } else {
        0.0
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Attendance History",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Class: ${classCode.uppercase(Locale.ROOT)}",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Verified attendance records: ${records.size}")
                    Text("Attendance percentage: ${"%.1f".format(Locale.ROOT, percentage)}%")
                    Text(
                        text = "Based on the total sessions supplied by the app.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = "Could not load history: $errorMessage",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                records.isEmpty() -> {
                    Text("No verified attendance records found yet.")
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(records, key = { it.studentId }) { record ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = record.studentName.ifBlank {
                                                record.studentId
                                            },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = SimpleDateFormat(
                                                "dd MMM yyyy, hh:mm a",
                                                Locale.getDefault()
                                            ).format(Date(record.markedAt)),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = "RSSI ${record.signalStrength}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
