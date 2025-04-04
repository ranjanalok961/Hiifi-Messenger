package com.demo.hiifi

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.reflect.Field

class HomePage : AppCompatActivity() {

    // UI Components
    private lateinit var searchView: SearchView
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var userSearchAdapter: SearchUserAdapter
    private lateinit var userListAdapter: UserAdapter

    // Data Storage
    private val userProfiles: MutableList<Profile> = mutableListOf()
    private val filteredUserProfiles: MutableList<Profile> = mutableListOf()
    private val messageList: MutableList<Message> = mutableListOf()
    private val connectedUserIds: MutableSet<String> = mutableSetOf()

    // Shared Preferences & ViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatRoomViewModel: ChatRoomViewModel
    private lateinit var currentUserId: String

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        firebaseAuth = FirebaseAuth.getInstance()


        initializeComponents()
        setupRecyclerViews()
        setupSearchView()
        loadUserProfiles()
        loadChatData()

        val profileButton = findViewById<ImageButton>(R.id.profileButton)


        profileButton.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view , Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                R.id.menu_logout -> {
                    firebaseAuth.signOut()
                    val editor = sharedPreferences.edit()
                    editor.putString("User", "")
                    editor.putString("Uid", "")
                    editor.apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        try {
            val fields = PopupMenu::class.java.declaredFields
            for (field in fields) {
                if (field.name == "mPopup") {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popupMenu)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceShowIcon = classPopupHelper.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    setForceShowIcon.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        popupMenu.show()
    }

    private fun initializeComponents() {
        chatRoomViewModel = ViewModelProvider(this)[ChatRoomViewModel::class.java]
        sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("Uid", "").toString()

        searchView = findViewById(R.id.searchView)
        searchView.setIconifiedByDefault(false)
        searchView.isFocusable = true
        searchView.isFocusableInTouchMode = true
        searchView.requestFocus()

        userRecyclerView = findViewById(R.id.userListRecyclerView)
        messageRecyclerView = findViewById(R.id.userListRecyclerView1)
    }

    private fun setupRecyclerViews() {
        userSearchAdapter = SearchUserAdapter(this,userProfiles) { profile -> navigateToProfile(profile) }
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = userSearchAdapter

        currentUserId = sharedPreferences.getString("Uid", "").toString()
        userListAdapter = UserAdapter(this,filteredUserProfiles,currentUserId) { profile -> navigateToChat(profile) }
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = userListAdapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    switchToChatList() // Show chat list when search is empty
                } else {
                    switchToUserSearch() // Show search results

                    val filteredUsers = userProfiles.filter {
                        it.id?.contains(newText, ignoreCase = true) == true // Use 'name' instead of 'id'
                    }
                    userSearchAdapter.updateData(filteredUsers) // Update the adapter
                }
                return true
            }
        })

        getClearButton(searchView)?.setOnClickListener {
            searchView.setQuery("",false)
            switchToChatList() // When clearing the search, switch back to chat list
        }
    }

    private fun loadChatData() {
        val dbRef = FirebaseDatabase.getInstance().reference
        dbRef.child("ChatRoom").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in snapshot.children) {
                    val messageData = messageSnapshot.getValue(Message::class.java)
                    messageData?.let { messageList.add(it) }
                }
                messageList.sortByDescending { it.timestamp }
                connectedUserIds.clear()
                messageList.filter { it.chatId.contains(currentUserId) }
                    .forEach { message ->
                        val connectionId = message.chatId.replace(currentUserId, "")
                        connectedUserIds.add(connectionId)
                    }

                fetchUserNames() // Fetch updated user profiles
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun fetchUserNames() {
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .get()
            .addOnSuccessListener { documents ->
                val profiles = documents.toObjects(Profile::class.java)
                filteredUserProfiles.clear()
                filteredUserProfiles.addAll(
                    profiles.filter { it.id in connectedUserIds }
                        .sortedBy { connectedUserIds.indexOf(it.id) } // Ensure order is same as connectedUserIds
                )
                userListAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
            }
    }

    private fun loadUserProfiles() {
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .get()
            .addOnSuccessListener { documents ->
                userProfiles.clear()
                userProfiles.addAll(documents.toObjects(Profile::class.java))
                userSearchAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
            }
    }
    private fun navigateToProfile(profile: Profile) {
        val intent = Intent(this, ProfileScreen::class.java).apply {
            putExtra("id", profile.id)
        }
        startActivity(intent)
    }
    private fun navigateToChat(profile: Profile) {
        val intent = Intent(this, chatRoomPage::class.java).apply {
            putExtra("name", profile.name)
            putExtra("id", profile.id)
        }
        startActivity(intent)
    }
    private fun switchToUserSearch() {
        userRecyclerView.visibility = View.VISIBLE
        messageRecyclerView.visibility = View.GONE
    }
    private fun switchToChatList() {
        userRecyclerView.visibility = View.GONE
        messageRecyclerView.visibility = View.VISIBLE
    }
    private fun getClearButton(searchView: SearchView): ImageView? {
        return try {
            val closeButtonField: Field = SearchView::class.java.getDeclaredField("mCloseButton")
            closeButtonField.isAccessible = true
            closeButtonField.get(searchView) as? ImageView
        } catch (e: Exception) {
            null
        }
    }
}