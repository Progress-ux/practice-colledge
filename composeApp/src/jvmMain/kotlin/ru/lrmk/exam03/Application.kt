package ru.lrmk.exam03

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.client.call.*
import kotlinx.coroutines.launch
import ru.lrmk.exam03.database.User


@Composable
fun Application() {
    val client = remember { Client() }
    val scope = rememberCoroutineScope()

    var user: User? by remember { mutableStateOf(null) }

    var login: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }

    var isPasswordVisible: Boolean by remember { mutableStateOf(false) }


    var success: String by remember { mutableStateOf("") }
    var error: String by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        if (user == null) {
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин") },
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = if (!isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    Icon(
                        imageVector = if (!isPasswordVisible) Icons.Default.Lock else Icons.Default.Check,
                        contentDescription = "Password",
                        modifier = Modifier
                            .clickable { isPasswordVisible = !isPasswordVisible }
                    )
                },
            )

            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Button(onClick = {
                if (login.isEmpty() || password.isEmpty()) error = "Пожалуйста, укажите логин и пароль"
                else scope.launch {
                    error = ""
                    val login = client.login(login, password)
                    when (login.status.value) {
                        200 -> user = login.body()
                        401 -> error = "Вы заблокированы. Обратитесь к администратору"
                        403 -> error = "Вы ввели неверный логин или пароль. Пожалуйста, проверьте еще раз введеные данные"
                        else -> error = login.status.toString()
                    }
                }
            }) {
                Text("Войти")
            }
        }
    }
}