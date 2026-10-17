package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.activity.compose.LocalActivity
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun DeviceLockScreen(message: String?, onUnlock: () -> Unit) {
    val activity = LocalActivity.current
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад"), { activity?.finish() })
            Text("Takt", style = MaterialTheme.typography.titleLarge)
        }
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(Modifier.size(80.dp).glassSurface(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MenuBook, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(t("Твої дані захищено"), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(t("Підтвердь особу через захист телефона, щоб відкрити свої дані."),
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onUnlock, modifier = Modifier.size(96.dp).glassSurface(shape = CircleShape)) {
                    Icon(Icons.Outlined.Fingerprint, t("Розблокувати"), Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                Text(t("Розблокувати"), style = MaterialTheme.typography.titleLarge)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                TextButton(onClick = onUnlock) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t("Використати блокування екрана"), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
