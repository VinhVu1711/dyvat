package com.vinh.dyvat.data.repository

import android.content.Context
import android.net.Uri
import com.vinh.dyvat.BuildConfig
import com.vinh.dyvat.data.model.ProductImportCommitResponse
import com.vinh.dyvat.data.model.ProductImportValidateResponse
import com.vinh.dyvat.data.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_IMPORT_FILE_BYTES = 10L * 1024L * 1024L

@Singleton
class ProductImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val httpClient = HttpClient(Android)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun validateFile(
        uri: Uri,
        fileName: String,
        fileSizeBytes: Long?
    ): Result<ProductImportValidateResponse> = withContext(Dispatchers.IO) {
        try {
            val normalizedName = fileName.ifBlank { "SanPhamKinhDoanh.xlsx" }
            if (!normalizedName.lowercase().endsWith(".xlsx")) {
                return@withContext Result.Error("Chỉ hỗ trợ file Excel .xlsx")
            }

            if (fileSizeBytes != null && fileSizeBytes > MAX_IMPORT_FILE_BYTES) {
                return@withContext Result.Error("File vượt quá giới hạn 10 MB. Vui lòng chia nhỏ file import.")
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: return@withContext Result.Error("Không thể đọc file đã chọn")

            if (bytes.size > MAX_IMPORT_FILE_BYTES) {
                return@withContext Result.Error("File vượt quá giới hạn 10 MB. Vui lòng chia nhỏ file import.")
            }

            val accessToken = currentAccessToken()
            val responseText = httpClient.submitFormWithBinaryData(
                url = functionUrl("validate"),
                formData = formData {
                    append(
                        key = "file",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, excelContentType(normalizedName))
                            append(HttpHeaders.ContentDisposition, "filename=\"$normalizedName\"")
                        }
                    )
                }
            ) {
                headers {
                    appendAuthHeaders(accessToken)
                }
            }.bodyAsText()

            val response = json.decodeFromString<ProductImportValidateResponse>(responseText)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể kiểm tra file import", e)
        }
    }

    suspend fun commitImport(importToken: String): Result<ProductImportCommitResponse> = withContext(Dispatchers.IO) {
        try {
            val accessToken = currentAccessToken()
            val response = httpClient.post(functionUrl("commit")) {
                headers {
                    appendAuthHeaders(accessToken)
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(json.encodeToString(CommitRequest(importToken)))
            }
            val responseText = response.bodyAsText()
            val parsed = json.decodeFromString<ProductImportCommitResponse>(responseText)
            if (response.status.isSuccess()) {
                Result.Success(parsed)
            } else {
                Result.Error(parsed.message.ifBlank { "Import thất bại. Chưa có dữ liệu nào được nhập." })
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Không thể import sản phẩm", e)
        }
    }

    private suspend fun currentAccessToken(): String {
        val session = supabaseClient.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Phiên đăng nhập không hợp lệ")
        return session.accessToken
    }

    private fun HeadersBuilder.appendAuthHeaders(accessToken: String) {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
        append("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
    }

    private fun functionUrl(action: String): String {
        return "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/import-products-xlsx/$action"
    }

    private fun excelContentType(fileName: String): String {
        return if (fileName.lowercase().endsWith(".xlsx")) {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        } else {
            "application/octet-stream"
        }
    }
}

@Serializable
private data class CommitRequest(
    val importToken: String
)
