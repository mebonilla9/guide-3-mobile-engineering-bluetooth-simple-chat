package dev.manuel.bluetoothsimplechat.application.events

interface CommunicationListener {
  fun onCommunication(message: String)
}