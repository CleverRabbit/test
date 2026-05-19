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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tapik.messenger.domain.model.Chat
import com.tapik.messenger.presentation.viewmodel.ChatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tapik") },
                actions = {
                    IconButton(onClick = { showSearchModal = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.chats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chats yet")
                }
            } else {
                LazyColumn {
                    items(uiState.chats, key = { it.id }) { chat ->
                        ChatListItem(chat = chat, onClick = { onChatClick(chat.id) })
                    }
                }
            }
        }
    }

    // Search Modal - полупрозрачное модальное окно
    if (showSearchModal) {
        SearchModal(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onClose = {
                viewModel.closeSearch()
                showSearchModal = false
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
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chat.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (chat.lastMessage != null) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (chat.lastMessageTime != null) {
                    Text(
                        text = formatTime(chat.lastMessageTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchModal(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    chats: List<Chat>,
    onChatClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .clickable(onClick = onClose)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search chats...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (chats.isNotEmpty()) {
                    LazyColumn {
                        items(chats, key = { it.id }) { chat ->
                            ChatListItem(chat = chat, onClick = { onChatClick(chat.id) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results found")
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val seconds = timestamp / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours < 1 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${hours / 24}d"
    }
}
