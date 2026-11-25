package com.voidlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Grey100
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Hairline
import com.voidlauncher.ui.theme.Surface2
import com.voidlauncher.ui.theme.Surface3
import com.voidlauncher.ui.theme.White

@Composable
fun MonoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface2)
            .border(1.dp, Hairline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

/** The single primary action: solid white, black label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = White,
            contentColor = Amoled,
            disabledContainerColor = Surface3,
            disabledContentColor = Grey400,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { icon(); Spacer(Modifier.width(8.dp)) }
            Text(
                text.uppercase(),
                color = if (enabled) Amoled else Grey400,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = White, disabledContentColor = Grey400),
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Grey400, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, filled: Boolean = false, muted: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) White else Surface3)
            .border(1.dp, if (filled) White else Hairline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (filled) Amoled else if (muted) Grey400 else Grey100,
                letterSpacing = 0.8.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun MonoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White, unfocusedTextColor = White,
            focusedBorderColor = White, unfocusedBorderColor = Hairline,
            focusedLabelColor = White, unfocusedLabelColor = Grey400,
            cursorColor = White,
            focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
        ),
        supportingText = supportingText?.let { { Text(it, color = Grey400) } },
    )
}

@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface3)
            .border(1.dp, Hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial.take(1).uppercase(), color = White, fontWeight = FontWeight.Medium, fontSize = 19.sp)
    }
}

@Composable
fun EmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = White)
            Spacer(Modifier.height(5.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Grey400,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

/** Setting row with a real slider bound to persisted state. */
@Composable
fun SliderRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String = "",
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("$value$unit", style = MaterialTheme.typography.labelMedium, color = Grey100)
        }
        var local by remember { mutableStateOf(value.toFloat()) }
        LaunchedEffect(value) { if (value.toFloat() != local) local = value.toFloat() }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = White, activeTrackColor = White, inactiveTrackColor = Surface3),
        )
    }
}

@Composable
fun SwitchRow(
    label: String,
    detail: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall, color = Grey400)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Amoled, checkedTrackColor = White,
                uncheckedThumbColor = Grey400, uncheckedTrackColor = Surface3,
                uncheckedBorderColor = Hairline,
            ),
        )
    }
}
