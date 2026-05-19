package com.tapik.messenger.presentation.ui.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.presentation.ui.theme.TapikBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onChatClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchModal by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSearchActive) {
        showSearchModal = uiState.isSearchActive
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tapik") },
                actions = {
                    IconButton(onClick = {
                        showSearchModal = true
                        viewModel.toggleSearch()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TapikBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (uiState.chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет чатов. Начните новый разговор!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(uiState.chats, key = { it.id }) { chat ->
                    ChatItem(chat = chat, onClick = { onChatClick(chat.id) })
                }
            }
        }
    }

    // Search Modal - полупрозрачное модальное окно
    if (showSearchModal) {
        SearchModal(
            searchQuery = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClose = {
                showSearchModal = false
                viewModel.toggleSearch()
            },
            chats = uiState.chats,
            onChatClick = { chatId ->
                showSearchModal = false
                onChatClick(chatId)
            }
        )
    }
}

@Composable
private fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    color = TapikBlue.copy(alpha = 0.2f)
                ) {}
                Text(
                    text = chat.name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TapikBlue
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    chat.lastMessageTime?.let {
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = chat.lastMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (chat.unreadCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = TapikBlue
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SearchModal(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    chats: List<Chat>,
    onChatClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text("Поиск") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Результаты поиска",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (chats.isEmpty()) {
                    Text("Ничего не найдено", style = MaterialTheme.typography.bodyMedium)
                } else {
                    chats.forEach { chat ->
                        ChatItem(chat = chat, onClick = { onChatClick(chat.id) })
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val hours = (timestamp / 3600000) % 24
    val minutes = (timestamp / 60000) % 60
    return String.format("%02d:%02d", hours, minutes)
}
