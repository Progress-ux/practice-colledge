package ru.lrmk.exam03

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.websocket.Frame
import kotlinx.coroutines.launch
import ru.lrmk.exam03.database.User
import sun.security.jgss.GSSUtil.login
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun Editor(client: Client) {
    val users by produceState(mutableListOf()) {
        value = client.users().toMutableStateList()
    }
    val scope = rememberCoroutineScope()

    var success: String by remember { mutableStateOf("") }
    var error: String by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            if (success.isNotEmpty()) {
                Text(success, color = Color.Green)
            }
        }
        item {
            UserRow(
                login = "Логин",
                onLoginChange = {},
                password = "Пароль",
                onPasswordChange = {},
                isHeader = true,
                onAdminChange = {},
                edit = {},
                delete = {}
            )
            HorizontalDivider()
        }
        items(users, { it.login }) { user ->
            var login by remember(user) { mutableStateOf(user.login) }
            var password by remember(user) { mutableStateOf(user.password) }
            var admin by remember(user) { mutableStateOf(user.admin == 1L) }
            val block = remember(user) {
                LocalDate.parse(user.last) <= LocalDate.now().minusMonths(1)
            }
            UserRow(
                login = login,
                onLoginChange = {
                    login = it
                },
                password = password,
                onPasswordChange = {
                    password = it
                },
                isAdmin = admin,
                onAdminChange = {
                    admin = it
                },
                block = block,
                edit = {
                    success = ""
                    error = ""
                    if (user.login != login) {
                        if (users.any { it.login == login }) {
                            error = "Такой логин уже существует"
                            return@UserRow
                        }
                    }
                    val newUser = user.copy(
                        login = login,
                        password = password,
                        admin = if (admin) 1 else 0,
                        last = LocalDate.now().toString()
                    )
                    scope.launch {
                        val statusEdit = client.update(newUser, user.login)
                        when (statusEdit.status.value) {
                            200 -> {
                                users.replaceAll { if (it.login == user.login) newUser else it }
                                success = "Профиль успешно обновлен"
                            }

                            else -> error = "Не удалось отредактировать запись"
                        }
                    }
                },
                delete = {
                    success = ""
                    error = ""
                    scope.launch {
                        val statusDelete = client.delete(user.login)
                        when (statusDelete.status.value) {
                            200 -> {
                                users.remove(user)
                                success = "Профиль успешно удален"
                            }
                            else -> error = "Не удалось запись"
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
    var newLoginUser by remember { mutableStateOf("") }
    OutlinedTextField(
        value = newLoginUser,
        onValueChange = {newLoginUser = it},
        modifier = Modifier.padding(16.dp),
        label = { Text("Новый пользователь") },
        trailingIcon = {
            IconButton(onClick = {
                success = ""
                error = ""

                if (newLoginUser.isEmpty()) {
                    error = "Введите логин для добавления"
                    return@IconButton
                }

                if (users.any { it.login == newLoginUser }) {
                    error = "Такой логин уже существует"
                    return@IconButton
                }

                scope.launch {
                    val statusInsert = client.insert(newLoginUser)
                    when (statusInsert.status.value) {
                        200 -> {
                            val newUser = User(
                                newLoginUser,
                                newLoginUser,
                                LocalDate.now().toString(),
                                null,
                                0L,
                                0L
                            )
                            users.add(newUser)
                            newLoginUser = ""
                            success = "Пользователь успешно добавлен"
                        }
                        else -> error = "Не удалось добавить пользователя"
                    }
                }
            }) {
                Icon(Icons.Filled.AddCircle, null)
            }
        }
    )
}

@Composable
fun UserRow(
    login: String,
    onLoginChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isAdmin: Boolean = false,
    onAdminChange: (Boolean) -> Unit,
    edit: () -> Unit,
    delete: () -> Unit,
    block: Boolean = false,
    isHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (block) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(login, onValueChange = onLoginChange, modifier = Modifier.weight(3f).padding(8.dp))
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))

        BasicTextField(password, onValueChange = onPasswordChange, modifier = Modifier.weight(3f).padding(8.dp))
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isHeader)
                Icon(Icons.Default.Settings, null)
            else
                Checkbox(checked = isAdmin, onCheckedChange = onAdminChange)
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isHeader)
                Icon(Icons.Default.Edit, null)
            else
                IconButton(onClick = edit) {
                    Icon(
                        Icons.Default.Check,
                        null,
                    )
                }
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isHeader)
                Icon(Icons.Default.Delete, null)
            else
                IconButton(onClick = delete) {
                    Icon(
                        Icons.Default.Close,
                        null,
                    )
                }
        }
    }
}