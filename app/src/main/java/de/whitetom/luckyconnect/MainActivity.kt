package de.whitetom.luckyconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import contacts.async.findWithContext
import contacts.async.groups.findWithContext
import contacts.core.Contacts
import contacts.core.entities.Contact
import contacts.core.entities.Group
import contacts.permissions.groups.queryWithPermission
import contacts.permissions.queryWithPermission
import de.whitetom.luckyconnect.ui.theme.RandomContactChooserTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), CoroutineScope by MainScope() {
    private var contactsRepository: Contacts? = null
    private var contacts = MutableStateFlow(listOf<Contact>())
    private var groups = MutableStateFlow(listOf<Group>())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launch{

            refresh()
        }
        setContent {
            RandomContactChooserTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = contacts.collectAsState().value.joinToString(separator = ";"){group -> group.displayNamePrimary+group.id },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }



    private suspend fun refresh() {
        groups.update{
            getContactsRepository().groups().queryWithPermission().findWithContext()
                // Hide the default and favorites group, just like in the AOSP Contacts app.
                .filter { !it.isDefaultGroup && !it.isFavoritesGroup }
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RandomContactChooserTheme {
        Greeting("halli")
    }
}
