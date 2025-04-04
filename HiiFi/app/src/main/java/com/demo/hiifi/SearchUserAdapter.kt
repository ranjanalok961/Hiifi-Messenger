package com.demo.hiifi

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class SearchUserAdapter(
    private val context: Context,
    private var userList: MutableList<Profile>,
    private val onClick: (Profile) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userId: TextView = itemView.findViewById(R.id.tv_username)
        private val userName: TextView = itemView.findViewById(R.id.tv_name)
        private val userImage : CircleImageView = itemView.findViewById(R.id.tv_profile_image)

        fun bind(user: Profile, context: Context) {
            userId.text = user.id
            userName.text = user.name

            GetImage.getImage(context, user.id.toString()) { imageUrl ->
                val finalImageUrl = if (imageUrl == "default") R.drawable.profile else imageUrl
                Glide.with(context)
                    .load(finalImageUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(userImage)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position],context)
        holder.itemView.setOnClickListener {
            onClick(userList[position])
        }
    }

    override fun getItemCount(): Int = userList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<Profile>) {
        userList = newList.toMutableList()
        notifyDataSetChanged()
    }
}


