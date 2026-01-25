package de.whitetom.luckyconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import contacts.async.findWithContext
import contacts.async.groups.findWithContext
import contacts.core.Contacts
import contacts.core.entities.Contact
import contacts.core.entities.Group
import contacts.core.util.groupMemberships
import contacts.permissions.groups.queryWithPermission
import contacts.permissions.queryWithPermission
import de.whitetom.luckyconnect.ui.theme.RandomContactChooserTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random


class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private var contactsRepository: Contacts? = null
    private var contacts = MutableStateFlow(listOf<Contact>())
    private var randomContact: MutableState<Contact?> = mutableStateOf(null)
    private var groups = MutableStateFlow(listOf<Group>())
    private var selectedGroup: MutableState<Group?> = mutableStateOf(null)
    private var expanded = mutableStateOf(false)
    private var isLoading = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch {
            refresh()
            isLoading.value = false
        }
        setContent {
            RandomContactChooserTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        groups = groups.collectAsState().value,
                        selectedGroup = selectedGroup.value,
                        isLoading = isLoading.value,
                        onGroupSelected = { selectedGroup.value = it; expanded.value = false },
                        onGroupMenuToggle = { expanded.value = it },
                        isGroupMenuExpanded = expanded.value,
                        onSelectRandomContact = { randomContact() }
                    )
                }
            }
        }
    }

    // ...existing code...

    private fun randomContact() {
        if (selectedGroup.value != null) {
            contacts.value = contacts.value.filter { contact: Contact ->
                contact.groupMemberships().any { membership -> membership.groupId == selectedGroup.value!!.id }
            }
        }
        if (contacts.value.isNotEmpty()) {
            val selected = contacts.value[Random.nextInt(contacts.value.size)]
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, selected.id.toString())
            intent.setData(uri)
            this.startActivity(intent)
        }
    }

    private suspend fun refresh() {
        groups.update{
            getContactsRepository().groups().queryWithPermission().findWithContext()
                .toList()
        }
        contacts.update {
            getContactsRepository().queryWithPermission().findWithContext().toList()
        }
    }

    private fun getContactsRepository(): Contacts {
        val tempContacts: Contacts
        if (contactsRepository == null) {
            tempContacts = Contacts(this)
            contactsRepository = tempContacts
        }
        else {
            tempContacts = contactsRepository as Contacts
        }
        return tempContacts
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    groups: List<Group>,
    selectedGroup: Group?,
    isLoading: Boolean,
    onGroupSelected: (Group) -> Unit,
    onGroupMenuToggle: (Boolean) -> Unit,
    isGroupMenuExpanded: Boolean,
    onSelectRandomContact: () -> Unit
) {
    if (isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading contacts...", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Lucky Connect",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            GroupSelector(
                groups = groups,
                selectedGroup = selectedGroup,
                isExpanded = isGroupMenuExpanded,
                onToggleMenu = { onGroupMenuToggle(it) },
                onGroupSelected = { onGroupSelected(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            RandomContactButton(
                selectedGroup = selectedGroup,
                groups = groups,
                onSelectRandomContact = onSelectRandomContact
            )
        }
    }
}

@Composable
fun GroupSelector(
    modifier: Modifier = Modifier,
    groups: List<Group>,
    selectedGroup: Group?,
    isExpanded: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onGroupSelected: (Group) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Select Group",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (groups.isNotEmpty()) {
            Button(
                onClick = { onToggleMenu(!isExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = selectedGroup?.title ?: "Choose a group...",
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Open group menu",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onToggleMenu(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = { onGroupSelected(group) }
                    )
                }
            }
        } else {
            Text(
                text = "No groups found",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RandomContactButton(
    modifier: Modifier = Modifier,
    selectedGroup: Group?,
    groups: List<Group>,
    onSelectRandomContact: () -> Unit
) {
    ElevatedButton(
        onClick = onSelectRandomContact,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = selectedGroup != null || groups.isEmpty()
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Select random contact",
            modifier = Modifier.padding(end = 8.dp)
        )
        val text = selectedGroup?.title ?: "All Contacts"
        Text(
            text = "Pick Random from $text",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
