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
import kotlinx.coroutines.launch
import ru.lrmk.exam03.database.User
import java.time.LocalDate

@Composable
fun Editor(client: Client) {
    // В отдельную функцию вынесите администраторскую таблицу для редактирования пользователей
    val users by produceState(mutableListOf()) {
        value = client.users().toMutableStateList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

                },
                delete = {

                }
            )
            HorizontalDivider()
        }
    }
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