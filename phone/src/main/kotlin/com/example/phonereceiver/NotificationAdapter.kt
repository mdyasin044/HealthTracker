//package com.example.phonereceiver
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import java.text.SimpleDateFormat
//import java.util.*
//
//class NotificationAdapter(private var notifications: List<NotificationItem>) :
//    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
//
//    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val titleText: TextView = view.findViewById(R.id.notificationTitle)
//        val bodyText: TextView = view.findViewById(R.id.notificationText)
//        val packageText: TextView = view.findViewById(R.id.notificationPackage)
//        val timeText: TextView = view.findViewById(R.id.notificationTime)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_glucose_cell, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = notifications[position]
//        holder.titleText.text = item.title
//        holder.bodyText.text = item.text
//        holder.packageText.text = item.packageName
//        holder.timeText.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//            .format(Date(item.timestamp))
//    }
//
//    override fun getItemCount() = notifications.size
//
//    fun updateData(newList: List<NotificationItem>) {
//        notifications = newList
//        notifyDataSetChanged()
//    }
//}