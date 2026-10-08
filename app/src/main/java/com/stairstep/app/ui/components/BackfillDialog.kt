package com.stairstep.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stairstep.app.ui.theme.MintEmerald
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BackfillDialog(
    defaultWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (date: String, floors: Int, weight: Double?) -> Unit
) {
    val yesterday = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    var dateText by remember { mutableStateOf(yesterday) }
    var floorsText by remember { mutableStateOf("18") }
    var weightText by remember {
        mutableStateOf(if (defaultWeight > 0) String.format("%.1f", defaultWeight) else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "补录历史记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "漏记了某天的运动？仅需补填日期、爬升楼层与体重即可",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 补录日期
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("补录日期 (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 爬升总楼层数
            OutlinedTextField(
                value = floorsText,
                onValueChange = { if (it.all { char -> char.isDigit() }) floorsText = it },
                label = { Text("爬升总楼层数") },
                suffix = { Text("层") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 当日体重 (选填)
            OutlinedTextField(
                value = weightText,
                onValueChange = {
                    if (it.matches(Regex("""^\d*\.?\d{0,1}$"""))) weightText = it
                },
                label = { Text("当日体重 (选填)") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Button(
                    onClick = {
                        val parsedFloors = floorsText.toIntOrNull()
                        val parsedWeight = weightText.toDoubleOrNull()
                        if (dateText.isNotBlank() && parsedFloors != null && parsedFloors > 0) {
                            onConfirm(dateText.trim(), parsedFloors, parsedWeight)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald)
                ) {
                    Text("确认补录")
                }
            }
        }
    }
}
