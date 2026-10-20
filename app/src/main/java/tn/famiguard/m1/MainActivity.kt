package tn.famiguard.m1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = dynamicIfAvailable()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }

    @Composable
    private fun dynamicIfAvailable() =
        if (android.os.Build.VERSION.SDK_INT >= 31)
            androidx.compose.material3.darkColorScheme() else darkColorScheme()
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    // Recharger quand on revient dans l'app (après avoir donné une autorisation)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val hasUsage = remember(refreshKey) { UsageRepository.hasUsageAccess(ctx) }
    val accOn = remember(refreshKey) { BlockAccessibilityService.isEnabled(ctx) }
    val apps = remember(refreshKey, hasUsage) {
        if (hasUsage) UsageRepository.todayUsage(ctx) else emptyList()
    }
    var blockedState by remember(refreshKey) { mutableStateOf(BlockStore.blocked(ctx)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Temps d'écran", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Aujourd'hui, par application", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!hasUsage) {
            item {
                SetupCard(
                    title = "Autoriser l'accès aux données d'usage",
                    body = "Pour mesurer le temps d'écran, activez FamiGuard dans la liste.",
                    cta = "Ouvrir les réglages",
                    onClick = { UsageRepository.openUsageAccessSettings(ctx) }
                )
            }
        }
        if (!accOn) {
            item {
                SetupCard(
                    title = "Activer le blocage d'applications",
                    body = "Activez le service d'accessibilité FamiGuard pour pouvoir bloquer une application.",
                    cta = "Ouvrir l'accessibilité",
                    onClick = { BlockAccessibilityService.openAccessibilitySettings(ctx) }
                )
            }
        }

        if (hasUsage && apps.isEmpty()) {
            item { Text("Aucun usage mesuré aujourd'hui pour l'instant.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        items(apps, key = { it.packageName }) { app ->
            val isBlocked = blockedState.contains(app.packageName)
            AppRow(app, isBlocked) {
                BlockStore.toggle(ctx, app.packageName)
                blockedState = BlockStore.blocked(ctx)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Le contenu des messages et les photos ne sont jamais lus. " +
                    "Seul le temps d'usage est mesuré, sur l'appareil.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetupCard(title: String, body: String, cta: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 13.sp)
            Button(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(cta) }
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, blocked: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            app.icon?.let {
                Image(it.toBitmap(72, 72).asImageBitmap(), contentDescription = null,
                    modifier = Modifier.size(40.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(UsageRepository.formatDuration(app.millisToday),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onToggle) {
                Text(if (blocked) "Débloquer" else "Bloquer")
            }
        }
    }
}
