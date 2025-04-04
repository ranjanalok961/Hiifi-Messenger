package com.demo.hiifi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class chatRoomPage : AppCompatActivity() {
    private lateinit var chatRoomViewModel: ChatRoomViewModel
    private lateinit var sendMessageButton: ImageButton
    private lateinit var messageInput: EditText
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recipientUserId: String
    private var messagesList: MutableList<Message> = mutableListOf()
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUserId: String
    private var messageBeingEdited: Message? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_room_page)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        chatRoomViewModel = ViewModelProvider(this)[ChatRoomViewModel::class.java]

        val recipientUserName = intent.getStringExtra("name")
        recipientUserId = intent.getStringExtra("id")!!
        sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("Uid", "").toString()

        loadMessages()

        messageInput = findViewById(R.id.messageInput)
        sendMessageButton = findViewById(R.id.sendButton)

        val backButton = findViewById<ImageButton>(R.id.toolbarBackButton)
        backButton.setOnClickListener {
            finish()
        }

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = recipientUserName

        val toolbarProfileImage = findViewById<CircleImageView>(R.id.toolbarProfileImage)
        GetImage.getImage(this, recipientUserId) { imageUrl ->
            val finalImageUrl = if (imageUrl == "default") R.drawable.profile else imageUrl
            Glide.with(this)
                .load(finalImageUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(toolbarProfileImage)
        }
        toolbarProfileImage.setOnClickListener {
            val intent = Intent(this, ProfileScreen::class.java)
            intent.putExtra("id",recipientUserId)
            startActivity(intent)
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messagesList, currentUserId, object : MessageEditListener {
            override fun onEditMessageRequested(message: Message) {
                messageBeingEdited = message
                messageInput.setText(message.message)
                sendMessageButton.setImageResource(R.drawable.baseline_send_24)
            }
        })
        chatRecyclerView.adapter = chatAdapter

        sendMessageButton.setOnClickListener {
            if (messageBeingEdited == null) {
                sendMessage()
            } else {
                updateMessage()
            }
        }


    }
    private fun updateMessage() {
        val updatedText = messageInput.text.toString()
        if (updatedText.isNotEmpty() && messageBeingEdited != null) {
            val dbRef = firebaseDatabase.reference.child("ChatRoom")
            val updatedId = currentUserId+""+recipientUserId
            dbRef.orderByChild("chatId").equalTo(updatedId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (messageSnapshot in snapshot.children) {
                            val storedMessage = messageSnapshot.getValue(Message::class.java)
                            if (storedMessage != null &&
                                storedMessage.senderId == messageBeingEdited!!.senderId &&
                                storedMessage.timestamp == messageBeingEdited!!.timestamp
                            ) {
                                val messageId = messageSnapshot.key

                                if (messageId != null) {
                                    dbRef.child(messageId).child("message").setValue(updatedText)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener {
                                        }
                                }
                                messageInput.text.clear()
                                break
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }

    }


    private fun sendMessage() {
        val dbRef = firebaseDatabase.reference
        val chatId = currentUserId + recipientUserId
        val messageId = dbRef.child("ChatRoom").push().key
        val messageData = Message(
            chatId,
            emptyList(),
            messageInput.text.toString(),
            recipientUserId,
            currentUserId,
            System.currentTimeMillis(),
        )

        dbRef.child("ChatRoom").child(messageId.toString()).setValue(messageData)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener { 
            }
    }


    private fun loadMessages() {
        val dbRef = firebaseDatabase.reference.child("ChatRoom")

        dbRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val allMessages: MutableList<Message> = mutableListOf()

                snapshot.children.forEach { data ->
                    val messageData = data.getValue(Message::class.java)
                    messageData?.let { allMessages.add(it) }
                }
                val filteredMessages = allMessages.filter {
                    it.chatId.contains(currentUserId) && (
                            (it.senderId == currentUserId && it.receiverId == recipientUserId ) ||
                                    (it.senderId == recipientUserId && it.receiverId == currentUserId )
                            )
                }
                messagesList.clear()
                messagesList.addAll(filteredMessages)
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messagesList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
