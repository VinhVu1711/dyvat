package com.vinh.dyvat.ui.screens.products

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.ProductImportCommitResult
import com.vinh.dyvat.data.model.ProductImportError
import com.vinh.dyvat.data.model.ProductImportSummary
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductImportScreen(
    onNavigateBack: () -> Unit,
    onImportSuccess: () -> Unit,
    viewModel: ProductImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = context.readImportFileMeta(uri)
            viewModel.validateFile(uri, meta.name, meta.size)
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IMPORT SẢN PHẨM",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GuideCard(
                    fileName = uiState.fileName,
                    phase = uiState.phase,
                    onChooseFile = {
                        filePicker.launch(
                            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        )
                    },
                    onReset = viewModel::reset
                )
            }

            uiState.message?.let { message ->
                item {
                    MessageCard(
                        phase = uiState.phase,
                        message = message
                    )
                }
            }

            uiState.summary?.let { summary ->
                item {
                    SummaryCard(summary = summary)
                }
            }

            when (uiState.phase) {
                ProductImportPhase.READY -> {
                    item {
                        Button(
                            onClick = { viewModel.commitImport() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NearBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Import dữ liệu",
                                color = NearBlack,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                ProductImportPhase.SUCCESS -> {
                    uiState.commitResult?.let { result ->
                        item {
                            CommitResultCard(result = result)
                        }
                    }
                    item {
                        Button(
                            onClick = onImportSuccess,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "Xem danh sách sản phẩm",
                                color = NearBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> Unit
            }

            if (uiState.errors.isNotEmpty()) {
                item {
                    Text(
                        text = "Lỗi cần sửa",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(uiState.errors) { error ->
                    ImportErrorCard(error = error)
                }
                if (uiState.summary?.hasMoreErrors == true) {
                    item {
                        Text(
                            text = "File còn thêm lỗi khác. Vui lòng sửa các lỗi đầu tiên rồi import lại.",
                            color = WarningOrange,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    fileName: String,
    phase: ProductImportPhase,
    onChooseFile: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "SanPhamKinhDoanh.xlsx",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Text(
                text = "File cần có 4 sheet: LoaiSanPham, DonViTinh, NhaCungCap, SanPham. App chỉ tải file lên máy chủ để kiểm tra, không đọc Excel trên điện thoại.",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )
            if (fileName.isNotBlank()) {
                StatusBadge(label = fileName, type = StatusType.INFO)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onChooseFile,
                    enabled = phase != ProductImportPhase.VALIDATING && phase != ProductImportPhase.COMMITTING,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = NearBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Chọn file",
                        color = NearBlack,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (phase != ProductImportPhase.IDLE) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextSilver,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Làm lại",
                            color = TextSilver,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    phase: ProductImportPhase,
    message: String
) {
    val color = when (phase) {
        ProductImportPhase.NEEDS_FIX,
        ProductImportPhase.ERROR -> NegativeRed
        ProductImportPhase.SUCCESS,
        ProductImportPhase.READY -> SpotifyGreen
        ProductImportPhase.VALIDATING,
        ProductImportPhase.COMMITTING -> TextSilver
        ProductImportPhase.IDLE -> TextSilver
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (phase) {
                ProductImportPhase.VALIDATING,
                ProductImportPhase.COMMITTING -> CircularProgressIndicator(
                    color = SpotifyGreen,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(22.dp)
                )
                ProductImportPhase.NEEDS_FIX,
                ProductImportPhase.ERROR -> Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = NegativeRed,
                    modifier = Modifier.size(22.dp)
                )
                else -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = message,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: ProductImportSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Kết quả kiểm tra",
                color = TextWhite,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            SummaryRow("Loại sản phẩm", summary.categoriesToCreate, summary.categoriesToReuse)
            SummaryRow("Đơn vị tính", summary.unitsToCreate, summary.unitsToReuse)
            SummaryRow("Nhà cung cấp", summary.suppliersToCreate, summary.suppliersToReuse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidDark, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sản phẩm sẽ thêm", color = TextSilver)
                Text("${summary.productsToImport}", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
            if (summary.errorCount > 0) {
                Text(
                    text = "Có ${summary.errorCount} lỗi. Chưa có dữ liệu nào được nhập.",
                    color = NegativeRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, createCount: Int, reuseCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidDark, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSilver)
        Text(
            text = "Mới $createCount · Có sẵn $reuseCount",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ImportErrorCard(error: ProductImportError) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${error.sheet} · Dòng ${error.rowNumber}",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(label = error.column, type = StatusType.WARNING)
            }
            Text(error.message, color = NegativeRed, style = MaterialTheme.typography.bodyMedium)
            if (error.value.isNotBlank()) {
                Text("Giá trị: ${error.value}", color = TextSilver, style = MaterialTheme.typography.bodySmall)
            }
            Text(error.suggestion, color = TextSilver, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CommitResultCard(result: ProductImportCommitResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Đã nhập thành công",
                color = SpotifyGreen,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Sản phẩm mới: ${result.productsCreated}", color = TextWhite)
            Text("Loại sản phẩm mới: ${result.categoriesCreated} · dùng lại ${result.categoriesReused}", color = TextSilver)
            Text("Đơn vị mới: ${result.unitsCreated} · dùng lại ${result.unitsReused}", color = TextSilver)
            Text("Nhà cung cấp mới: ${result.suppliersCreated} · dùng lại ${result.suppliersReused}", color = TextSilver)
        }
    }
}

private data class ImportFileMeta(
    val name: String,
    val size: Long?
)

private fun Context.readImportFileMeta(uri: Uri): ImportFileMeta {
    var name = "SanPhamKinhDoanh.xlsx"
    var size: Long? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return ImportFileMeta(name, size)
}
