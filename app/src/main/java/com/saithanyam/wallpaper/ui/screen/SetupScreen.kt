package com.saithanyam.wallpaper.ui.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saithanyam.wallpaper.service.NumberWallpaperService
import com.saithanyam.wallpaper.service.SaithanyamWallpaperService
import com.saithanyam.wallpaper.viewmodel.SetupViewModel
import com.saithanyam.wallpaper.viewmodel.WallpaperStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: SetupViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // Handle share result toast
    LaunchedEffect(state.shareSuccess) {
        state.shareSuccess?.let { success ->
            val msg = if (success) "Saved to gallery" else "Failed to save"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearShareResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Saithanyam",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your life in 4,000 weeks",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (!state.isConfirmed) {
            // Birthday picker button
            Button(
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("Enter your birthday")
            }
        } else {
            // Show weeks lived / remaining
            val birthday = state.birthday!!
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")

            Text(
                text = "Born ${birthday.format(formatter)}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.weeksLived}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "weeks lived",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.weeksRemaining}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "weeks remaining",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ---------- Style selector ----------
            Text(
                text = "Wallpaper style",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyleCard(
                    label = "Grid",
                    description = "4,000 dots",
                    selected = state.selectedStyle == WallpaperStyle.GRID,
                    onClick = { viewModel.selectStyle(WallpaperStyle.GRID) },
                    modifier = Modifier.weight(1f)
                )
                StyleCard(
                    label = "Number",
                    description = "Remaining weeks",
                    selected = state.selectedStyle == WallpaperStyle.NUMBER,
                    onClick = { viewModel.selectStyle(WallpaperStyle.NUMBER) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Set wallpaper button — sets whichever style is currently selected
            Button(
                onClick = {
                    val serviceClass = when (state.selectedStyle) {
                        WallpaperStyle.GRID -> SaithanyamWallpaperService::class.java
                        WallpaperStyle.NUMBER -> NumberWallpaperService::class.java
                    }
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, serviceClass)
                        )
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("Set Live Wallpaper")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share card button
            OutlinedButton(
                onClick = { viewModel.onShareCard() },
                border = BorderStroke(1.dp, Color.White)
            ) {
                Text("Share card", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Change birthday
            TextButton(onClick = { showDatePicker = true }) {
                Text("Change birthday", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.confirmBirthday(date)
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Tappable card used by the style selector. Selected state is a filled white
 * background with black text; unselected is black with a thin white border.
 */
@Composable
private fun StyleCard(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color.White else Color.Black
    val fg = if (selected) Color.Black else Color.White

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = if (selected) Color(0xFF666666) else Color.Gray
                )
            }
        }
    }
}
