package com.voidlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Hairline
import com.voidlauncher.ui.theme.Surface2
import com.voidlauncher.ui.theme.White

/**
 * Small floating toast anchored above the tab bar. Pure black pill, single hairline,
 * spring slide-up. No Material snackbar chrome.
 */
@Composable
fun VoidToast(message: String?, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialOffsetY = { it },
            ) + fadeIn(tween(160)),
            exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(140)),
        ) {
            Row(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Surface2)
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(White),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = com.voidlauncher.ui.theme.Grey100,
                    maxLines = 2,
                )
            }
        }
    }
}
