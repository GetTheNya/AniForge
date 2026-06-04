package moe.GetTheNya.AniForge.ui.profile

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.GetTheNya.AniForge.core.database.util.LogEntry
import moe.GetTheNya.AniForge.core.database.util.LogLevel
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Sticky Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.profileScreen.userProfile,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            IconButton(
                onClick = { navController.navigate(Screen.Settings) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.misc.settings,
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp)) // padding so content isn't covered by bottom navigation
    }
}

@Composable
fun LogViewerScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val logs by viewModel.logs.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val logListState = rememberLazyListState()

    // Auto-scroll logs to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Sticky Header with Back Button & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = strings.misc.back,
                    tint = TextPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.profileScreen.systemLogsTitle,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.profileScreen.diagnosticsEngineStatus,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Terminal Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(strings.profileScreen.engineLogsCount, logs.size),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val fullLogs = logs.joinToString("\n") { log ->
                                val base = "[${log.getFormattedTime()}] [${log.level}] [${log.tag}]: ${log.message}"
                                val tMsg = log.throwableMessage
                                if (tMsg != null) "$base\n$tMsg" else base
                             }
                            clipboardManager.setText(AnnotatedString(fullLogs))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = CyberTeal)
                    ) {
                        Text(strings.profileScreen.copyLogs, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = viewModel::clearLogs,
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCoral)
                    ) {
                        Text(strings.misc.clear, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070709))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = strings.profileScreen.noLogsYet,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = logs,
                            key = { it.id }
                        ) { log ->
                            LogItem(log = log)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.DEBUG -> CyberTeal
        LogLevel.INFO -> Color(0xFF3B82F6) // Premium bright blue
        LogLevel.WARN -> Color(0xFFF59E0B) // Premium bright amber
        LogLevel.ERROR -> NeonCoral
    }

    val levelTag = when (log.level) {
        LogLevel.DEBUG -> "DBG"
        LogLevel.INFO -> "INF"
        LogLevel.WARN -> "WRN"
        LogLevel.ERROR -> "ERR"
    }

    val annotatedText = remember(log) {
        buildAnnotatedString {
            // Timestamp
            withStyle(style = SpanStyle(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)) {
                append(log.getFormattedTime())
                append(" ")
            }
            // Level tag
            withStyle(style = SpanStyle(color = levelColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)) {
                append("[$levelTag]")
                append(" ")
            }
            // Class Tag
            withStyle(style = SpanStyle(color = Color(0xFFA78BFA), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)) {
                append(log.tag)
                append(": ")
            }
            // Log message
            withStyle(style = SpanStyle(color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)) {
                append(log.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = annotatedText,
            modifier = Modifier.fillMaxWidth()
        )
        val throwableMsg = log.throwableMessage
        if (throwableMsg != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = throwableMsg,
                color = NeonCoral.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }
    }
}
