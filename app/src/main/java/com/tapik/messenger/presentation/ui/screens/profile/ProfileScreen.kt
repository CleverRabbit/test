package com.tapik.messenger.presentation.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tapik.messenger.presentation.ui.theme.TapikBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!uiState.isEditing) {
                        IconButton(onClick = viewModel::toggleEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                    } else {
                        TextButton(onClick = viewModel::saveProfile) {
                            Text("Сохранить")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TapikBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar placeholder
            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.medium,
                color = TapikBlue.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.user?.username?.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TapikBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isEditing) {
                OutlinedTextField(
                    value = uiState.editedUsername,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Имя пользователя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.editedBio,
                    onValueChange = viewModel::onBioChange,
                    label = { Text("О себе") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            } else {
                uiState.user?.let { user ->
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = user.phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    user.bio?.let { bio ->
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (user.isOnline) TapikBlue else Color.Gray
                    ) {
                        Text(
                            text = if (user.isOnline) "Онлайн" else "Оффлайн",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                } ?: run {
                    Text("Профиль не загружен")
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
