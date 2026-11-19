package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyFormatter

@Composable
fun CurrencyInputField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String = "مبلغ",
    currencyUnit: String = "تومان",
    usePersianDigits: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = "currency_input_field"
) {
    var rawText by remember(value) {
        mutableStateOf(if (value > 0) value.toString() else "")
    }

    val amountInWords = remember(value, currencyUnit) {
        if (value > 0) CurrencyFormatter.amountToWordsPersian(value, currencyUnit) else ""
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = if (usePersianDigits) CurrencyFormatter.toPersianDigits(rawText) else rawText,
            onValueChange = { input ->
                val englishDigits = CurrencyFormatter.toEnglishDigits(input).filter { it.isDigit() }
                rawText = englishDigits
                val parsed = englishDigits.toLongOrNull() ?: 0L
                onValueChange(parsed)
            },
            label = { Text(label) },
            trailingIcon = {
                Text(
                    text = currencyUnit,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        if (value > 0) {
            Text(
                text = "معادل: $amountInWords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
