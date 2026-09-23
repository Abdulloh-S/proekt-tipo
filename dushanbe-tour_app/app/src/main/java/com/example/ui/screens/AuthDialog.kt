package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.theme.Emerald600

@Composable
fun AuthDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onSaveUser: (String, String, String) -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0: Вход, 1: Регистрация, 2: Восстановление
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var password by remember { mutableStateOf("") }
    var resetSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    0 -> "Вход в аккаунт"
                    1 -> "Регистрация туриста"
                    else -> "Восстановление доступа"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mode == 1) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя и фамилия") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Электронная почта") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode != 2) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Номер телефона (+992)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mode == 2 && resetSuccess) {
                    Surface(
                        color = Emerald600.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Код для сброса пароля отправлен на $email",
                            color = Emerald600,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (mode == 0) {
                        TextButton(onClick = { mode = 2 }) {
                            Text("Забыли пароль?", fontSize = 12.sp)
                        }
                        TextButton(onClick = { mode = 1 }) {
                            Text("Регистрация", fontSize = 12.sp, color = Emerald600)
                        }
                    } else if (mode == 1) {
                        TextButton(onClick = { mode = 0 }) {
                            Text("Уже есть аккаунт? Войти", fontSize = 12.sp, color = Emerald600)
                        }
                    } else {
                        TextButton(onClick = { mode = 0 }) {
                            Text("Вернуться ко входу", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == 2) {
                        resetSuccess = true
                    } else {
                        onSaveUser(
                            if (name.isBlank()) "Алишер Раҳимов" else name,
                            if (email.isBlank()) "tourist.tj@gmail.com" else email,
                            if (phone.isBlank()) "+992 900 12 34 56" else phone
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                Text(
                    text = when (mode) {
                        0 -> "Войти"
                        1 -> "Зарегистрироваться"
                        else -> "Отправить код"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
