package ru.lrmk.exam03

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.ktor.client.call.*
import kotlinx.coroutines.launch
import ru.lrmk.exam03.database.User


@Composable
fun Application() {
    val client = remember { Client() }
    val scope = rememberCoroutineScope()

    var user: User? by remember { mutableStateOf(null) }

    var success: String by remember { mutableStateOf("") }
    var error: String by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        var login: String by remember { mutableStateOf("") }
        var password: String by remember { mutableStateOf("") }

        var isPasswordVisible: Boolean by remember { mutableStateOf(false) }

        if (user == null) {
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин") },
            )
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                isPasswordVisible = isPasswordVisible,
                onPasswordVisible = { isPasswordVisible = !isPasswordVisible }
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
        } else {
            Text("Вы успешно авторизовались", fontSize = 24.sp)

            if (user?.admin == 1L) {
                Text("Редактор пользователей", fontSize = 16.sp)
                Editor(client)
            }
            else {
                var oldPassword: String by remember { mutableStateOf("") }
                var newPassword: String by remember { mutableStateOf("") }
                var testPassword: String by remember { mutableStateOf("") }

                var isPasswordVisible: Boolean by remember { mutableStateOf(false) }

                Text("Пожалуйста, поменяйте пароль:", fontSize = 16.sp)

                PasswordField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = "Старый пароль",
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisible = { isPasswordVisible = !isPasswordVisible }
                )
                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Новый пароль",
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisible = { isPasswordVisible = !isPasswordVisible }
                )
                PasswordField(
                    value = testPassword,
                    onValueChange = { testPassword = it },
                    label = "Новый пароль (еще раз)",
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisible = { isPasswordVisible = !isPasswordVisible }
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                if (success.isNotBlank()) {
                    Text(success, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }
                Button(onClick = {
                    val login = user?.login ?: ""
                    val savedPassword = user?.password ?: ""

                    when {
                        // Проверка на пустые поля
                        oldPassword.isEmpty() || newPassword.isEmpty() || testPassword.isEmpty() ->
                            error = "Пожалуйста, заполните все поля"

                        // Проверка, что набранный пароль соответствует записи в user
                        oldPassword != savedPassword ->
                            error = "Неправильный пароль"

                        // Проверка, что новый пароль не содержит в себе логин
                        newPassword.contains(login, ignoreCase = true) ->
                            error = "Пароль не должен совпадать с логином"

                        // Проверка, что новые пароли совпадают
                        newPassword != testPassword ->
                            error = "Пароли не совпадают"

                        // Смена пароля
                        else -> {
                            error = ""
                            val newUser = user!!.copy(password = newPassword)
                            scope.launch {
                                val request = client.update(newUser, login)
                                when (request.status.value) {
                                    200 -> success = "Пароль успешно изменен"
                                    else -> error = request.status.toString()
                                }
                            }
                        }
                    }
                }) {
                    Text("Сменить пароль")
                }
            }
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPasswordVisible: Boolean,
    onPasswordVisible: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (!isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            Icon(
                imageVector = if (!isPasswordVisible) Icons.Default.Lock else Icons.Default.Check,
                contentDescription = "Password",
                modifier = Modifier.clickable { onPasswordVisible() }
            )
        },
    )
}