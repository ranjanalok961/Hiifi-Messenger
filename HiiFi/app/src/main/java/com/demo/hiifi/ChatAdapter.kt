package com.demo.hiifi

import android.content.Context
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

interface MessageEditListener {
    fun onEditMessageRequested(message: Message)
}

class ChatAdapter(
    private val messageList: MutableList<Message>,
    private val currentUserId: String,
    private val messageEditListener: MessageEditListener // Add this
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val view = inflater.inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messageList.size

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = formatTimestamp(message.timestamp)

            itemView.setOnLongClickListener {
                showPopupMenu(itemView.context, it, message, message.senderId == currentUserId)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = formatTimestamp(message.timestamp)

            itemView.setOnLongClickListener {
                showPopupMenu(itemView.context, it, message, false)
                true
            }
        }
    }

    private fun showPopupMenu(context: Context, view: View, message: Message, isSender: Boolean) {
        val popupMenu = PopupMenu(context, view , Gravity.BOTTOM)
        popupMenu.menuInflater.inflate(R.menu.message_options_menu, popupMenu.menu)

        popupMenu.menu.findItem(R.id.editMessage).isVisible = isSender
        popupMenu.menu.findItem(R.id.deleteForEveryone).isVisible = isSender

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.deleteForMe -> deleteMessageForMe(message)
                R.id.deleteForEveryone -> deleteMessageForEveryone(message)
                R.id.editMessage -> messageEditListener.onEditMessageRequested(message)
            }
            true
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



    private fun deleteMessageForMe(message: Message) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("ChatRoom")
        val updatedId = message.senderId+""+message.receiverId
        dbRef.orderByChild("chatId").equalTo(updatedId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnapshot in snapshot.children) {
                        val storedMessage = messageSnapshot.getValue(Message::class.java)
                        if (storedMessage != null &&
                            storedMessage.senderId == message.senderId &&
                            storedMessage.timestamp == message.timestamp
                        ) {
                            val messageId = messageSnapshot.key
                            val newChatId = updatedId.replace(currentUserId,"")
                            dbRef.child(messageId.toString()).child("chatId").setValue(newChatId)
                                .addOnSuccessListener {
                                }
                                .addOnFailureListener {
                                }
                            break
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun deleteMessageForEveryone(message: Message) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("ChatRoom")
        val updatedId = message.senderId+""+message.receiverId
        dbRef.orderByChild("chatId").equalTo(updatedId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnapshot in snapshot.children) {
                        val storedMessage = messageSnapshot.getValue(Message::class.java)
                        if (storedMessage != null &&
                            storedMessage.senderId == message.senderId &&
                            storedMessage.timestamp == message.timestamp
                        ) {
                            val messageId = messageSnapshot.key
                            dbRef.child(messageId.toString()).removeValue()
                                .addOnSuccessListener {
                                    deleteMessageForMe(message)
                                }
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
