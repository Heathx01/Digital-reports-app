package com.example.digital_reports_app.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digital_reports_app.ui.navigation.Screen
import com.example.digital_reports_app.ui.navigation.WorkProBottomNavBar
import com.example.digital_reports_app.ui.navigation.horizontalTabSwipeGesture
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.AppLanguageHelper
import com.example.digital_reports_app.worker.SyncReportsWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userName = uiState.currentUser?.name ?: "Usuario"
    val userCode = uiState.currentUser?.userCode ?: "215800"
    val context = LocalContext.current

    // Profile Edit State
    var editNameInput by remember { mutableStateOf(userName) }
    var editCodeInput by remember { mutableStateOf(userCode) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }

    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher to select profile photo from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
            Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    if (uiState.isLoggedOut) {
        LaunchedEffect(Unit) {
            onLogout()
        }
    }

    LaunchedEffect(uiState.currentUser, uiState.showEditProfileDialog) {
        if (uiState.currentUser != null && uiState.showEditProfileDialog) {
            editNameInput = uiState.currentUser?.name ?: ""
            editCodeInput = uiState.currentUser?.userCode ?: ""
            currentPasswordInput = ""
            newPasswordInput = ""
        }
    }

    val themeModeLabel = when (uiState.themeMode) {
        "DARK" -> "Oscuro"
        "LIGHT" -> "Claro"
        else -> "Sistema"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            WorkProBottomNavBar(
                currentRoute = Screen.PROFILE,
                onTabSelected = onTabSelected
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .horizontalTabSwipeGesture(Screen.PROFILE, onTabSelected)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = "Ajustes y Perfil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Header Card with Avatar Photo & Edit Trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 20.dp, prominent = true)
                    .clickable { viewModel.toggleEditProfileDialog(true) }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Avatar Photo Circle with Badge
                        Box(
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Camera Edit Badge Icon
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Código de Usuario: $userCode",
                                style = TelemetryMonoStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Edit Pencil Icon Button
                    IconButton(onClick = { viewModel.toggleEditProfileDialog(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar Perfil y Credenciales",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Non-redundant Settings List Card with Liquid Glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {

                    // 1. Theme Selector: Seleccionar Tema
                    SettingItemValue(
                        icon = Icons.Outlined.Info,
                        title = "Seleccionar Tema",
                        value = themeModeLabel,
                        onClick = { viewModel.toggleThemeModeDialog(true) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    // 2. Language Selector with Dynamic Locale Switching
                    SettingItemValue(
                        icon = Icons.Outlined.Settings,
                        title = "Idioma de la App",
                        value = uiState.selectedLanguage,
                        onClick = { viewModel.toggleLanguageDialog(true) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    // 3. Upload Reports to Cloud
                    SettingItemClickable(
                        icon = Icons.Outlined.Check,
                        title = "Sincronizar Reportes con la Nube",
                        value = "Subir",
                        onClick = {
                            val syncRequest = OneTimeWorkRequestBuilder<SyncReportsWorker>().build()
                            WorkManager.getInstance(context).enqueue(syncRequest)
                            Toast.makeText(context, "Subiendo expedientes locales a la nube...", Toast.LENGTH_LONG).show()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    // 4. Refresh Report Status from Server
                    SettingItemClickable(
                        icon = Icons.Outlined.Refresh,
                        title = "Actualizar Estado de Expedientes",
                        value = "Obtener",
                        onClick = {
                            Toast.makeText(context, "Obteniendo estado reciente de expedientes desde el servidor...", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Button(
                onClick = { viewModel.toggleLogoutConfirmDialog(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Cerrar Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // UNIFIED EDIT PROFILE & CREDENTIALS DIALOG (Interactive & Professional)
    if (uiState.showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleEditProfileDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Perfil y Credenciales", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar & Photo Upload Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 16.dp)
                            .clickable { photoPickerLauncher.launch("image/*") }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = editNameInput.take(1).uppercase().ifBlank { "U" },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Foto de Perfil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Toca para actualizar imagen de perfil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Personal Info Fields
                    Text("Información Personal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nombre Completo *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCodeInput,
                        onValueChange = { editCodeInput = it },
                        label = { Text("Código de Usuario (min. 6 dígitos) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Password Credentials Section
                    Text("Cambiar Contraseña (Opcional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Contraseña Actual") },
                        singleLine = true,
                        visualTransformation = if (isCurrentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                                Text(if (isCurrentPasswordVisible) "Ocultar" else "Mostrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("Nueva Contraseña (min. 8 caract.)") },
                        singleLine = true,
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Text(if (isNewPasswordVisible) "Ocultar" else "Mostrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileAndCredentials(
                            newName = editNameInput,
                            newCode = editCodeInput,
                            currentPasswordInput = currentPasswordInput,
                            newPasswordInput = newPasswordInput
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.toggleEditProfileDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // THEME MODE SELECTOR DIALOG (Oscuro, Claro, Sistema)
    if (uiState.showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleThemeModeDialog(false) },
            title = { Text("Seleccionar Tema", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Oscuro
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("DARK")
                                Toast.makeText(context, "Tema cambiado a Oscuro", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 Oscuro", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (uiState.themeMode == "DARK") {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Option 2: Claro
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("LIGHT")
                                Toast.makeText(context, "Tema cambiado a Claro", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☀️ Claro", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (uiState.themeMode == "LIGHT") {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Option 3: Sistema
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode("SYSTEM")
                                Toast.makeText(context, "Tema del sistema aplicado", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📱 Sistema (Predeterminado)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (uiState.themeMode == "SYSTEM") {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { viewModel.toggleThemeModeDialog(false) }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // LANGUAGE SELECTOR DIALOG WITH REAL LOCALE SWITCHING
    if (uiState.showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleLanguageDialog(false) },
            title = { Text("Seleccionar Idioma", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLanguage("Español")
                                AppLanguageHelper.applyLanguageConfig(context, "Español")
                                viewModel.toggleLanguageDialog(false)
                                Toast.makeText(context, "Idioma cambiado a Español", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Español (Latinoamérica)", style = MaterialTheme.typography.bodyLarge)
                        if (uiState.selectedLanguage == "Español") {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLanguage("English")
                                AppLanguageHelper.applyLanguageConfig(context, "English")
                                viewModel.toggleLanguageDialog(false)
                                Toast.makeText(context, "Language changed to English", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("English (United States)", style = MaterialTheme.typography.bodyLarge)
                        if (uiState.selectedLanguage == "English") {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { viewModel.toggleLanguageDialog(false) }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // LOGOUT CONFIRMATION DIALOG
    if (uiState.showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleLogoutConfirmDialog(false) },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas cerrar sesión? Tendrás que ingresar tus credenciales para volver a entrar.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.toggleLogoutConfirmDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SettingItemClickable(
    icon: ImageVector,
    title: String,
    value: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = "›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingItemValue(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
