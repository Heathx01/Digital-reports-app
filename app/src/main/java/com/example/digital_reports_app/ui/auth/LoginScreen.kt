package com.example.digital_reports_app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_reports_app.ui.theme.TelemetryMonoStyle
import com.example.digital_reports_app.ui.theme.liquidGlass
import com.example.digital_reports_app.util.BiometricAuthManager
import com.example.digital_reports_app.util.BiometricAuthResult

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (uiState.isLoggedIn) {
        LaunchedEffect(Unit) {
            onNavigateToHome()
        }
    }

    val triggerLoginWithBiometrics: () -> Unit = {
        focusManager.clearFocus()
        if (activity != null) {
            val biometricAuthManager = BiometricAuthManager(activity)
            if (biometricAuthManager.canAuthenticate()) {
                biometricAuthManager.showBiometricPrompt { result ->
                    when (result) {
                        is BiometricAuthResult.Success -> {
                            viewModel.login()
                        }

                        is BiometricAuthResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            viewModel.login()
                        }
                    }
                }
            } else {
                viewModel.login()
            }
        } else {
            viewModel.login()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header and Brand Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .liquidGlass(cornerRadius = 24.dp, prominent = true),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "DocuField Logo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "DocuField Enterprise",
                        color = MaterialTheme.colorScheme.primary,
                        style = TelemetryMonoStyle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "BIENVENIDO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Inicia sesion con tu usuario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Liquid Glass Card Form
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 24.dp, prominent = true)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Código de Usuario",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.userCode,
                            onValueChange = viewModel::onUserCodeChanged,
                            placeholder = { Text("ej. 215800") },
                            singleLine = true,
                            isError = uiState.userCodeError != null,
                            supportingText = {
                                if (uiState.userCodeError != null) {
                                    Text(
                                        text = uiState.userCodeError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Contraseña",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChanged,
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            isError = uiState.passwordError != null,
                            supportingText = {
                                if (uiState.passwordError != null) {
                                    Text(
                                        text = uiState.passwordError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                TextButton(onClick = viewModel::togglePasswordVisibility) {
                                    Text(
                                        text = if (uiState.isPasswordVisible) "Ocultar" else "Mostrar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    triggerLoginWithBiometrics()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val isSubmitEnabled = uiState.userCode.isNotBlank() &&
                                uiState.password.isNotBlank() &&
                                uiState.userCodeError == null &&
                                uiState.passwordError == null &&
                                !uiState.isLoading

                        // Primary Login Button
                        Button(
                            onClick = {
                                triggerLoginWithBiometrics()
                            },
                            enabled = isSubmitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Iniciar Sesión",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}
