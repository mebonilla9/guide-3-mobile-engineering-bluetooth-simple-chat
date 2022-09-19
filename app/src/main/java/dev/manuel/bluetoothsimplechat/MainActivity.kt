package dev.manuel.bluetoothsimplechat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.manuel.bluetoothsimplechat.application.events.ItemClickListener
import dev.manuel.bluetoothsimplechat.domain.entities.DeviceData

class MainActivity : AppCompatActivity(), ItemClickListener {



  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  override fun itemClicked(deviceData: DeviceData) {
    TODO("Not yet implemented")
  }
}