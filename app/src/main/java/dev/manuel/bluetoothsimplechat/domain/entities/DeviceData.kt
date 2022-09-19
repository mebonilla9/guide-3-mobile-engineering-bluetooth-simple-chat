package dev.manuel.bluetoothsimplechat.domain.entities

data class DeviceData(
  val deviceName: String?,
  val deviceHardwareAddress: String
){

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DeviceData

    if (deviceName != other.deviceName) return false
    if (deviceHardwareAddress != other.deviceHardwareAddress) return false

    return true
  }

  override fun hashCode(): Int {
    var result = deviceName?.hashCode() ?: 0
    result = 31 * result + deviceHardwareAddress.hashCode()
    return result
  }
}
