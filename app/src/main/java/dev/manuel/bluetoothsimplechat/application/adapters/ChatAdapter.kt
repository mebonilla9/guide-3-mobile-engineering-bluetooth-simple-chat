package dev.manuel.bluetoothsimplechat.application.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.manuel.bluetoothsimplechat.R
import dev.manuel.bluetoothsimplechat.application.lasting.Constants
import dev.manuel.bluetoothsimplechat.domain.entities.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(val chatData: List<Message>, val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  val SENT = 0
  val RECEIVED = 1
  var df: SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    when(viewType){
      SENT -> {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_sent,parent,false)
        return SentHolder(view)
      }
      RECEIVED -> {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_received,parent,false)
        return ReceivedHolder(view)
      }
      else -> {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_sent,parent,false)
        return SentHolder(view)
      }
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when(holder.itemViewType){
      SENT -> {
        val sentHolder: SentHolder = holder as SentHolder
        sentHolder.txtSentMessage.text = chatData[position].message
        val timeMilliSeconds = chatData[position].time
        val resultDate = Date(timeMilliSeconds)
        sentHolder.txtTimeStamp.text = df.format(resultDate)
      }
      RECEIVED -> {
        val receiverHolder: ReceivedHolder = holder as ReceivedHolder
        receiverHolder.txtReceivedMessage.text = chatData[position].message
        val timeMilliSeconds = chatData[position].time
        val resultDate = Date(timeMilliSeconds)
        receiverHolder.txtTimeStamp.text = df.format(resultDate)
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    when(chatData[position].type){
      Constants.MESSAGE_TYPE_SENT -> return SENT
      Constants.MESSAGE_TYPE_RECEIVED -> return RECEIVED
    }
    return -1;
  }

  override fun getItemCount(): Int {
    return chatData.size
  }

  inner class SentHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
    var txtSentMessage = itemView.findViewById<TextView>(R.id.txtSentMessage)
    var txtTimeStamp = itemView.findViewById<TextView>(R.id.txtTimeStamp)
  }

  inner class ReceivedHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
    var txtReceivedMessage = itemView.findViewById<TextView>(R.id.txtReceivedMessage)
    var txtTimeStamp = itemView.findViewById<TextView>(R.id.txtTimeStamp)
  }
}