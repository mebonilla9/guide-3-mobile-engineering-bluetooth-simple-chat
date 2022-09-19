package dev.manuel.bluetoothsimplechat.domain.entities

data class Message(
  val message: String,
  val time: Long,
  val type: Int
)
