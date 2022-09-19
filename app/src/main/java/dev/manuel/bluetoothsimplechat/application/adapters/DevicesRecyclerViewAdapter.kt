package dev.manuel.bluetoothsimplechat.application.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.manuel.bluetoothsimplechat.R
import dev.manuel.bluetoothsimplechat.application.events.ItemClickListener
import dev.manuel.bluetoothsimplechat.domain.entities.DeviceData

class DevicesRecyclerViewAdapter(
  val mDeviceList: List<DeviceData>,
  val context: Context
) : RecyclerView.Adapter<DevicesRecyclerViewAdapter.ViewHolder>() {

  private var clickListener: ItemClickListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater
      .from(context)
      .inflate(
        R.layout.item_single_recyclerview,
        parent,
        false
      )
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.txtLargeLabel?.text =
      mDeviceList[position].deviceName ?: mDeviceList[position].deviceHardwareAddress
  }

  override fun getItemCount(): Int {
    return mDeviceList.size
  }

  inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {

    var txtLargeLabel: TextView? = itemView?.findViewById(R.id.txtLargeLabel)

    init {
      itemView?.setOnClickListener {
        clickListener?.itemClicked(mDeviceList[adapterPosition])
      }
    }
  }

  fun setItemClickListener(clickListener: ItemClickListener) {
    this.clickListener = clickListener
  }


}