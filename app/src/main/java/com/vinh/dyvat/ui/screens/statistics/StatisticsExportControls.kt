package com.vinh.dyvat.ui.screens.statistics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinh.dyvat.ui.theme.BorderGray
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@Composable
internal fun StatisticsExportControls(
    state: StatisticsUiState,
    viewModel: StatisticsViewModel
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        uri?.let { viewModel.exportReport(context.applicationContext, it) }
    }

    LaunchedEffect(state.exportSuccessMessage) {
        state.exportSuccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedButton(
            onClick = { exportLauncher.launch(viewModel.getDefaultExportFileName()) },
            enabled = !state.isLoading && !state.isExporting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SpotifyGreen,
                disabledContentColor = TextSilver
            ),
            border = BorderStroke(1.dp, if (state.isExporting) BorderGray else SpotifyGreen.copy(alpha = 0.7f))
        ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Xuất Excel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (state.isExporting) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = DarkCard,
            title = {
                Text("Đang tạo báo cáo...", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = SpotifyGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Dữ liệu lớn có thể mất vài phút.",
                        color = TextSilver,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelExport() }) {
                    Text("Hủy", color = TextSilver)
                }
            }
        )
    }

    state.exportError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportMessages() },
            containerColor = DarkCard,
            title = {
                Text("Không thể xuất Excel", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = message, color = TextSilver, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportMessages() }) {
                    Text("Đóng", color = SpotifyGreen)
                }
            }
        )
    }
}
