package com.tapik.messenger.presentation.ui.screens.chatsettings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    chatName: String,
    onNavigateBack: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Chat info header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chatName.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = chatName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last seen recently",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications section
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable Notifications",
                    subtitle = "Receive messages from this chat",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                HorizontalDivider()
                SettingsSwitchItem(
                    icon = Icons.Default.NotificationsOff,
                    title = "Mute Chat",
                    subtitle = "Disable sound and vibrations",
                    checked = isMuted,
                    onCheckedChange = { isMuted = it }
                )
                HorizontalDivider()
                SettingsItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Custom Notification Sound",
                    subtitle = "Choose a custom sound"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy section
            SettingsSection(title = "Privacy") {
                SettingsItem(
                    icon = Icons.Default.Visibility,
                    title = "Show Read Receipts",
                    subtitle = "Let others know when you've read their messages"
                )
                HorizontalDivider()
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Block User",
                    subtitle = "Block this contact"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Media section
            SettingsSection(title = "Media") {
                SettingsItem(
                    icon = Icons.Default.Image,
                    title = "Shared Media",
                    subtitle = "Photos, videos and files"
                )
                HorizontalDivider()
                SettingsItem(
                    icon = Icons.Default.Search,
                    title = "Search in Chat",
                    subtitle = "Find messages and media"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger zone
            SettingsSection(title = "Danger Zone") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear Chat History",
                    subtitle = "Delete all messages",
                    onClick = { /* Clear history */ }
                )
                HorizontalDivider()
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Chat",
                    subtitle = "Remove this conversation",
                    onClick = { /* Delete chat */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
