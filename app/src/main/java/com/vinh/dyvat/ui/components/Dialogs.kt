package com.vinh.dyvat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@Composable
fun AddEditDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = title,
                color = TextWhite
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = TextSilver,
                    focusedLabelColor = SpotifyGreen,
                    unfocusedLabelColor = TextSilver,
                    cursorColor = SpotifyGreen,
                    focusedContainerColor = NearBlack,
                    unfocusedContainerColor = NearBlack
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Lưu", color = NearBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

@Composable
fun AddEditDialogWithTwoFields(
    title: String,
    label1: String,
    label2: String,
    initialValue1: String = "",
    initialValue2: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text1 by remember { mutableStateOf(initialValue1) }
    var text2 by remember { mutableStateOf(initialValue2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = title,
                color = TextWhite
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text1,
                    onValueChange = { text1 = it },
                    label = { Text(label1) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = TextSilver,
                        focusedLabelColor = SpotifyGreen,
                        unfocusedLabelColor = TextSilver,
                        cursorColor = SpotifyGreen,
                        focusedContainerColor = NearBlack,
                        unfocusedContainerColor = NearBlack
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text2,
                    onValueChange = { text2 = it },
                    label = { Text(label2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = TextSilver,
                        focusedLabelColor = SpotifyGreen,
                        unfocusedLabelColor = TextSilver,
                        cursorColor = SpotifyGreen,
                        focusedContainerColor = NearBlack,
                        unfocusedContainerColor = NearBlack
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text1, text2) },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Lưu", color = NearBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(text = title, color = TextWhite)
        },
        text = {
            Text(
                text = "Bạn có chắc muốn xóa \"$itemName\" không?",
                color = TextSilver
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = title,
                color = TextWhite
            )
        },
        text = {
            Text(
                text = message,
                color = TextSilver
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Đã hiểu", color = NearBlack)
            }
        }
    )
}

@Composable
fun SuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = title,
                color = SpotifyGreen
            )
        },
        text = {
            Text(
                text = message,
                color = TextSilver
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("OK", color = NearBlack)
            }
        }
    )
}
