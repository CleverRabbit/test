package com.messenger.core.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * TopAppBar для экранов приложения в стиле Telegram.
 * 
 * @param title заголовок
 * @param actions действия справа (опционально)
 * @param navigationIcon иконка навигации слева (опционально)
 */
@Composable
fun MessengerTopBar(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
) {
    // Реализация через Material3 TopAppBar будет добавлена
    // Временно используется заглушка
    Box(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Пустое состояние экрана.
 * 
 * @param message сообщение для отображения
 * @param modifier модификатор
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Индикатор загрузки на весь экран.
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Базовый Scaffold для экранов мессенджера.
 * 
 * @param title заголовок экрана
 * @param snackbarHostState состояние Snackbar
 * @param isLoading показать индикатор загрузки
 * @param emptyMessage сообщение для пустого состояния
 * @param content контент экрана
 */
@Composable
fun MessengerScaffold(
    title: String,
    snackbarHostState: SnackbarHostState? = null,
    isLoading: Boolean = false,
    emptyMessage: String? = null,
    topBar: @Composable () -> Unit = { MessengerTopBar(title = title) },
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = topBar,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
            
            if (isLoading) {
                LoadingScreen()
            }
            
            if (emptyMessage != null && !isLoading) {
                EmptyState(message = emptyMessage)
            }
        }
    }
}
