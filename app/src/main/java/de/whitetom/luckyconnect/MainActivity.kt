package de.whitetom.luckyconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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
import kotlin.plus
import kotlin.random.Random


class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private var contactsRepository: Contacts? = null
    private var contacts = MutableStateFlow(listOf<Contact>())
    private var randomContact: MutableState<Contact?> = mutableStateOf(null)
    private var groups = MutableStateFlow(listOf<Group>())
    private var selectedGroup: MutableState<Group?> = mutableStateOf(null)
    private var expanded = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launch{
            refresh()
        }
        setContent {
            RandomContactChooserTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column {
                        if (groups.collectAsState().value.isNotEmpty()) {
                            Button(onClick = { expanded.value = true }) {
                                Text(text = "Gruppe auswählen")
                            }
                            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }, modifier = Modifier.padding(innerPadding)) {
                                groups.collectAsState().value.forEach { group ->
                                    DropdownMenuItem(text = {Text(text = group.title + "; " + group.id)}, onClick = { selectedGroup.value = group; expanded.value = false })
                                }
                            }
                        }
                        else {
                            Text(text= "No groups available")
                        }

                        if (selectedGroup.value != null || groups.collectAsState().value.isEmpty()) {
                            Button(onClick = { randomContact() }) {
                                val text = selectedGroup.value?.title ?: "All Contacts"
                                Text(text = "Random contact from $text")
                            }
                        }

                        Text(text = "expanded: " + expanded.value.toString())
                        Text(text = groups.collectAsState().value.joinToString("; "){group: Group -> group.title + " ; " + group.id })
                        if(randomContact.value != null) {
                            Text(text = randomContact.value!!.displayNamePrimary + randomContact.value!!.id)
                        }
                    }
                }
            }
        }
    }


    private fun randomContact() {
        if (selectedGroup.value != null) {
            contacts.value = contacts.value.filter { contact: Contact -> contact.groupMemberships().any { membership -> membership.groupId == selectedGroup.value!!.id } }
        }
        randomContact.value =  contacts.value[Random.nextInt(contacts.value.size)]
        val intent = Intent(Intent.ACTION_VIEW)
        val uri =
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, randomContact.value!!.id.toString())
        intent.setData(uri)
        this.startActivity(intent)
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