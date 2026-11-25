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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.voidlauncher.ui.theme.VoidDesign

/** Core VOID 2.0 components. Monochrome, sharp, restrained. */
@Composable
fun VoidSectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = VoidDesign.Label)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun VoidPanel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VoidDesign.Panel)
            .border(1.dp, VoidDesign.Line, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun VoidPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VoidDesign.Ink,
            contentColor = VoidDesign.Canvas,
            disabledContainerColor = VoidDesign.PanelRaised,
            disabledContentColor = VoidDesign.Muted,
        ),
    ) {
        Text(
            text.uppercase(),
            style = VoidDesign.Title.copy(fontSize = 15.sp, color = VoidDesign.Canvas),
        )
    }
}

@Composable
fun VoidSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, VoidDesign.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = VoidDesign.InkSoft,
            disabledContentColor = VoidDesign.Faint,
        ),
    ) {
        Text(text, style = VoidDesign.Label.copy(color = VoidDesign.InkSoft))
    }
}

@Composable
fun VoidPill(text: String, modifier: Modifier = Modifier, active: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) VoidDesign.Ink else VoidDesign.PanelRaised)
            .border(1.dp, if (active) VoidDesign.Ink else VoidDesign.Line, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(),
            style = VoidDesign.Label.copy(
                fontSize = 10.sp,
                color = if (active) VoidDesign.Canvas else VoidDesign.Muted,
            ),
        )
    }
}

@Composable
fun VoidDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().height(1.dp).background(VoidDesign.Line))
}

@Composable
fun VoidRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = VoidDesign.Body, modifier = Modifier.weight(1f))
        Text(
            value,
            style = VoidDesign.Caption.copy(color = VoidDesign.InkSoft),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Text("›", color = VoidDesign.Muted, style = VoidDesign.Body)
        }
    }
}

@Composable
fun VoidToggleRow(
    label: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = VoidDesign.Body)
            Text(detail, style = VoidDesign.Caption)
        }
        Box(
            Modifier
                .size(width = 38.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (checked) VoidDesign.Ink else VoidDesign.PanelRaised)
                .border(1.dp, if (checked) VoidDesign.Ink else VoidDesign.Line, RoundedCornerShape(11.dp)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (checked) VoidDesign.Canvas else VoidDesign.Muted),
            )
        }
    }
}

/**
 * A settings row that opens a list of choices instead of cycling one value per tap.
 */
@Composable
fun VoidChoiceRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    var open by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = VoidDesign.Body, modifier = Modifier.weight(1f))
            Text(value, style = VoidDesign.Caption.copy(color = VoidDesign.InkSoft))
            Spacer(Modifier.width(8.dp))
            Text(">", color = VoidDesign.Muted, style = VoidDesign.Body)
        }
        if (detail != null) Text(detail, style = VoidDesign.Caption)
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = VoidDesign.Panel,
            title = { Text(label, style = VoidDesign.Title) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option); open = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option,
                                style = VoidDesign.Body,
                                modifier = Modifier.weight(1f),
                                color = if (option == value) VoidDesign.Ink else VoidDesign.InkSoft,
                            )
                            if (option == value) Text("ACTIVE", style = VoidDesign.Label)
                        }
                        VoidDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Close", color = VoidDesign.Ink) }
            },
        )
    }
}
