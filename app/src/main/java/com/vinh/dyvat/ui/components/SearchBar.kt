package com.vinh.dyvat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@Composable
fun DyvatSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var internalQuery by remember { mutableStateOf(query) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MidDark, RoundedCornerShape(500.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tim kiem",
                tint = TextSilver,
                modifier = Modifier.size(20.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                if (internalQuery.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = internalQuery,
                    onValueChange = {
                        internalQuery = it
                        onQueryChange(it)
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                    singleLine = true,
                    cursorBrush = SolidColor(TextSilver),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (internalQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        internalQuery = ""
                        onQueryChange("")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xoa",
                        tint = TextSilver,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
