package com.demo.hiifi

// UserAdapter.kt
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class User(val name: String, val status: String, val profileImage: Int)

class UserAdapter(private val context : Context ,private val profileList: MutableList<Profile>, private val userId : String , private var onClick : (Profile) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user, parent, false)
        return UserViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = profileList[position]
        holder.userName.text = user.name

        retrieveMessages(user.id.toString()){message ->
            holder.messageTime.text = formatTimestamp(message.timestamp)
            if(message.senderId == userId){
                holder.lastMessage.text = "You : ${message.message}"
            }else{
                holder.lastMessage.text = message.message
            }
        }

        holder.itemView.setOnClickListener {
            onClick(user)
        }

        GetImage.getImage(context, user.id.toString()) { imageUrl ->
            val finalImageUrl = if (imageUrl == "default") R.drawable.profile else imageUrl
            Glide.with(context)
                .load(finalImageUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.userProfileImage)
        }
    }


    override fun getItemCount(): Int = profileList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        val lastMessage : TextView = itemView.findViewById(R.id.lastMessage)
        val userProfileImage : CircleImageView = itemView.findViewById(R.id.userProfileImage)
    }

    private fun retrieveMessages(userId2: String , callback: (Message) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("ChatRoom")


        dbRef.orderByChild("chatId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messageList = mutableListOf<Message>()

                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null && message.chatId.contains(userId) &&((message.senderId == userId && message.receiverId == userId2) || (message.senderId == userId2 && message.receiverId == userId))){
                            messageList.add(message)
                        }
                    }
                    messageList.sortByDescending { it.timestamp }
                    callback(messageList[0])
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun formatTimestamp(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val currentDate = Calendar.getInstance()

        return when {
            // If the message was sent today, show time (e.g., "10:30 PM")
            isSameDay(messageDate, currentDate) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(messageDate.time)
            }

            // If the message was sent yesterday, show "Yesterday"
            isYesterday(messageDate, currentDate) -> {
                "Yesterday"
            }

            // Otherwise, show the full date (e.g., "Mar 10, 2025")
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(messageDate.time)
            }
        }
    }
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        cal2.add(Calendar.DAY_OF_YEAR, -1) // Move back one day
        return isSameDay(cal1, cal2)
    }
}

