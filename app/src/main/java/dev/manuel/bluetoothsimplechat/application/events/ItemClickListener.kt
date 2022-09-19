package dev.manuel.bluetoothsimplechat.application.events

import dev.manuel.bluetoothsimplechat.domain.entities.DeviceData

interface ItemClickListener {

  fun itemClicked(deviceData: DeviceData)

}