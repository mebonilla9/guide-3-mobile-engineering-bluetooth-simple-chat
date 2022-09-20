package dev.manuel.bluetoothsimplechat.application.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.manuel.bluetoothsimplechat.R
import dev.manuel.bluetoothsimplechat.application.adapters.ChatAdapter
import dev.manuel.bluetoothsimplechat.application.events.CommunicationListener
import dev.manuel.bluetoothsimplechat.domain.entities.Message

class ChatFragment : Fragment(), View.OnClickListener {

  private lateinit var txtChatInput: EditText
  private lateinit var btnSendButton: FrameLayout
  private lateinit var recyclerViewChat: RecyclerView
  private var communicationListener: CommunicationListener? = null
  private var chatAdapter: ChatAdapter? = null
  private val messageList = arrayListOf<Message>()

  companion object {
    fun newInstance(): ChatFragment {
      val myFragment = ChatFragment()
      val args = Bundle()
      myFragment.arguments = args
      return myFragment
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view: View = LayoutInflater.from(activity).inflate(R.layout.fragment_chat, container, false)
    initViews(view)
    return view
  }

  private fun initViews(view: View) {
    this.txtChatInput = view.findViewById(R.id.txtChatInput)
    val imgSendIcon: ImageView = view.findViewById(R.id.imgSendIcon)
    this.btnSendButton = view.findViewById(R.id.btnSendButton)
    recyclerViewChat = view.findViewById(R.id.chatRecyclerView)

    this.btnSendButton.isClickable = false
    this.btnSendButton.isEnabled = false

    val linearLayoutManager = LinearLayoutManager(activity)
    linearLayoutManager.reverseLayout = true
    recyclerViewChat.layoutManager = linearLayoutManager

    this.txtChatInput.addTextChangedListener(object : TextWatcher {
      override fun onTextChanged(sequence: CharSequence, start: Int, before: Int, count: Int) {}
      override fun beforeTextChanged(sequence: CharSequence, start: Int, count: Int, after: Int) {}
      override fun afterTextChanged(editable: Editable) {
        if (editable.isNotEmpty()) {
          imgSendIcon.setImageDrawable(activity?.getDrawable(R.drawable.ic_send))
          btnSendButton.isClickable = true
          btnSendButton.isEnabled = true
          return
        }
        imgSendIcon.setImageDrawable(activity?.getDrawable(R.drawable.ic_send_depri))
        btnSendButton.isClickable = false
        btnSendButton.isEnabled = false
      }
    })

    this.btnSendButton.setOnClickListener(this)

    chatAdapter = activity?.let { ChatAdapter(messageList.reversed(), it) }
    recyclerViewChat.adapter = chatAdapter
  }


  override fun onClick(p0: View?) {
    if (this.txtChatInput.text.isNotEmpty()) {
      communicationListener?.onCommunication(txtChatInput.text.toString())
      txtChatInput.setText("")
    }
  }

  fun setCommunicationListener(communicationListener: CommunicationListener) {
    this.communicationListener = communicationListener
  }

  fun communicate(message: Message) {
    messageList.add(message)
    if (activity != null) {
      chatAdapter = activity?.let { ChatAdapter(messageList.reversed(), it) }
      recyclerViewChat.adapter = chatAdapter
      recyclerViewChat.scrollToPosition(0)
    }
  }


}