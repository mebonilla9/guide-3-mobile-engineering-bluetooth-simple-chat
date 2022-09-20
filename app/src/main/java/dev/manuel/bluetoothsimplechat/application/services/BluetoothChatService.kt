package dev.manuel.bluetoothsimplechat.application.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import dev.manuel.bluetoothsimplechat.application.lasting.Constants
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothChatService(context: Context, handler: Handler) {

  private var adapter: BluetoothAdapter? = null
  private var ownHandler: Handler? = null
  private var secureAcceptThread: AcceptThread? = null
  private var insecureAcceptThread: AcceptThread? = null
  private var connectThread: ConnectThread? = null
  private var connectedThread: ConnectedThread? = null

  private var connectionState: Int = 0
  private var newState: Int = 0

  private val TAG: String = javaClass.simpleName

  // Unique UUID for this application
  private val MY_UUID_SECURE = UUID.fromString("29621b37-e817-485a-a258-52da5261421a")
  private val MY_UUID_INSECURE = UUID.fromString("d620cd2b-e0a4-435b-b02e-40324d57195b")


  // Name for the SDP record when creating server socket
  private val NAME_SECURE = "BluetoothChatSecure"
  private val NAME_INSECURE = "BluetoothChatInsecure"

  // Constants that indicate the current connection state
  companion object {
    val STATE_NONE = 0       // we're doing nothing
    val STATE_LISTEN = 1     // now listening for incoming connections
    val STATE_CONNECTING = 2 // now initiating an outgoing connection
    val STATE_CONNECTED = 3  // now connected to a remote device
  }

  init {

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    adapter = bluetoothManager.adapter
    connectionState = STATE_NONE
    newState = connectionState
    ownHandler = handler
  }

  @Synchronized
  fun getState(): Int {
    return connectionState
  }

  @Synchronized
  fun start() {
    Log.d(TAG, "start")

    // Cancel any thread attempting to make a connection
    if (connectThread != null) {
      connectThread?.cancel()
      connectThread = null
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread?.cancel()
      connectedThread = null
    }

    // Start the thread to listen on a BluetoothServerSocket
    if (secureAcceptThread == null) {
      secureAcceptThread = AcceptThread(true)
      secureAcceptThread?.start()
    }
    if (insecureAcceptThread == null) {
      insecureAcceptThread = AcceptThread(false)
      insecureAcceptThread?.start()
    }
    // Update UI title
    //updateUserInterfaceTitle()
  }

  @Synchronized
  fun connect(device: BluetoothDevice?, secure: Boolean) {

    Log.d(TAG, "connect to: " + device)

    // Cancel any thread attempting to make a connection
    if (connectionState == STATE_CONNECTING) {
      if (connectThread != null) {
        connectThread?.cancel()
        connectThread = null
      }
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread?.cancel()
      connectedThread = null
    }

    // Start the thread to connect with the given device
    connectThread = ConnectThread(device, secure)
    connectThread?.start()

    // Update UI title
    //updateUserInterfaceTitle()
  }


  @SuppressLint("MissingPermission")
  @Synchronized
  fun connected(socket: BluetoothSocket?, device: BluetoothDevice?, socketType: String) {
    Log.d(TAG, "connected, Socket Type:" + socketType)

    // Cancel the thread that completed the connection
    if (connectThread != null) {
      connectThread?.cancel()
      connectThread = null
    }

    // Cancel any thread currently running a connection
    if (connectedThread != null) {
      connectedThread?.cancel()
      connectedThread = null
    }

    // Cancel the accept thread because we only want to connect to one device
    if (secureAcceptThread != null) {
      secureAcceptThread?.cancel()
      secureAcceptThread = null
    }
    if (insecureAcceptThread != null) {
      insecureAcceptThread?.cancel()
      insecureAcceptThread = null
    }

    // Start the thread to manage the connection and perform transmissions
    connectedThread = ConnectedThread(socket, socketType)
    connectedThread?.start()

    // Send the name of the connected device back to the UI Activity
    val msg = ownHandler?.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
    val bundle = Bundle()
    bundle.putString(Constants.DEVICE_NAME, device?.name)
    msg?.data = bundle
    ownHandler?.sendMessage(msg!!)
    // Update UI title
    //updateUserInterfaceTitle()
  }

  @Synchronized
  fun stop() {
    Log.d(TAG, "stop")

    if (connectThread != null) {
      connectThread?.cancel()
      connectThread = null
    }

    if (connectedThread != null) {
      connectedThread?.cancel()
      connectedThread = null
    }

    if (secureAcceptThread != null) {
      secureAcceptThread?.cancel()
      secureAcceptThread = null
    }

    if (insecureAcceptThread != null) {
      insecureAcceptThread?.cancel()
      insecureAcceptThread = null
    }
    connectionState = STATE_NONE
    // Update UI title
    //updateUserInterfaceTitle()
  }

  fun write(out: ByteArray) {
    // Create temporary object
    var temporalConnection: ConnectedThread?
    // Synchronize a copy of the ConnectedThread
    synchronized(this) {
      if (connectionState != STATE_CONNECTED) return
      temporalConnection = connectedThread
    }
    // Perform the write un-synchronized
    temporalConnection?.write(out)
  }

  private fun connectionFailed() {
    // Send a failure message back to the Activity
    val msg = ownHandler?.obtainMessage(Constants.MESSAGE_TOAST)
    val bundle = Bundle()
    bundle.putString(Constants.TOAST, "Unable to connect device")
    msg?.data = bundle
    ownHandler?.sendMessage(msg!!)

    connectionState = STATE_NONE
    // Update UI title
    //updateUserInterfaceTitle()

    // Start the service over to restart listening mode
    this@BluetoothChatService.start()
  }

  private fun connectionLost() {
    // Send a failure message back to the Activity
    val msg = ownHandler?.obtainMessage(Constants.MESSAGE_TOAST)
    val bundle = Bundle()
    bundle.putString(Constants.TOAST, "Device connection was lost")
    msg?.data = bundle
    ownHandler?.sendMessage(msg!!)

    connectionState = STATE_NONE
    // Update UI title
    // updateUserInterfaceTitle()

    // Start the service over to restart listening mode
    this@BluetoothChatService.start()
  }

  @SuppressLint("MissingPermission")
  private inner class AcceptThread(secure: Boolean) : Thread() {
    private val serverSocket: BluetoothServerSocket?
    private val socketType: String

    init {
      var temporalSocket: BluetoothServerSocket? = null
      socketType = if (secure) "Secure" else "Insecure"

      try {
        if (secure) {
          temporalSocket = adapter?.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
        } else {
          temporalSocket =
            adapter?.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
        }
      } catch (e: IOException) {
        Log.e(TAG, "Socket Type: $socketType listen() failed", e)
      }

      serverSocket = temporalSocket
      connectionState = STATE_LISTEN
    }

    override fun run() {
      Log.d(TAG, "Socket Type: $socketType BEGIN acceptThread $this")
      name = "AcceptThread $socketType"

      var socket: BluetoothSocket?

      while (connectionState != STATE_CONNECTED) {
        try {
          socket = serverSocket?.accept()
        } catch (e: IOException) {
          Log.e(TAG, "Socket Type: $socketType accept() failed", e)
          break
        }

        if (socket != null) {
          synchronized(this@BluetoothChatService) {
            when (connectionState) {
              STATE_LISTEN, STATE_CONNECTING -> connected(socket, socket.remoteDevice, socketType)
              STATE_NONE, STATE_CONNECTED -> try {
                socket.close()
              } catch (e: IOException) {
                Log.e(TAG, "Could not close unwanted socket", e)
              }
              else -> {}
            }
          }
        }
      }
      Log.i(TAG, "END acceptThread, socket Type: $socketType")
    }


    fun cancel() {
      Log.d(TAG, "Socket Type $socketType cancel $this")
      try {
        serverSocket?.close()
      } catch (e: IOException) {
        Log.e(TAG, "Socket Type $socketType close() of server failed", e)
      }

    }
  }

  @SuppressLint("MissingPermission")
  private inner class ConnectThread(private val currentDevice: BluetoothDevice?, secure: Boolean) :
    Thread() {
    private val socket: BluetoothSocket?
    private val socketType: String

    init {
      var temporalSocket: BluetoothSocket? = null
      socketType = if (secure) "Secure" else "Insecure"

      // Get a BluetoothSocket for a connection with the
      // given BluetoothDevice
      try {
        if (secure) {
          temporalSocket = currentDevice?.createRfcommSocketToServiceRecord(
            MY_UUID_SECURE
          )
        } else {
          temporalSocket = currentDevice?.createInsecureRfcommSocketToServiceRecord(
            MY_UUID_INSECURE
          )
        }
      } catch (e: IOException) {
        Log.e(TAG, "Socket Type: $socketType create() failed", e)
      }

      socket = temporalSocket
      connectionState = STATE_CONNECTING
    }

    @SuppressLint("MissingPermission")
    override fun run() {

      Log.i(TAG, "BEGIN mConnectThread SocketType: $socketType")
      name = "ConnectThread $socketType"

      // Always cancel discovery because it will slow down a connection
      adapter?.cancelDiscovery()

      // Make a connection to the BluetoothSocket
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception
        socket?.connect()

      } catch (e: IOException) {
        // Close the socket
        try {
          socket?.close()
        } catch (e2: IOException) {
          Log.e(
            TAG, "unable to close() $socketType socket during connection failure", e2
          )
        }

        connectionFailed()
        return
      }

      // Reset the ConnectThread because we're done
      synchronized(this@BluetoothChatService) {
        connectThread = null
      }

      // Start the connected thread
      connected(socket, currentDevice, socketType)
    }

    fun cancel() {
      try {
        socket?.close()
      } catch (e: IOException) {
        Log.e(TAG, "close() of connect $socketType socket failed", e)
      }

    }
  }

  /**
   * This thread runs during a connection with a remote device.
   * It handles all incoming and outgoing transmissions.
   */
  private inner class ConnectedThread(private val mmSocket: BluetoothSocket?, socketType: String) :
    Thread() {

    private val inputStream: InputStream?
    private val outputStream: OutputStream?

    init {
      Log.d(TAG, "create ConnectedThread: " + socketType)
      var tmpIn: InputStream? = null
      var tmpOut: OutputStream? = null

      // Get the BluetoothSocket input and output streams
      try {
        tmpIn = mmSocket?.inputStream
        tmpOut = mmSocket?.outputStream
      } catch (e: IOException) {
        Log.e(TAG, "temp sockets not created", e)
      }

      inputStream = tmpIn
      outputStream = tmpOut
      connectionState = STATE_CONNECTED
    }

    override fun run() {
      Log.i(TAG, "BEGIN mConnectedThread")
      val buffer = ByteArray(1024)
      var bytes: Int

      // Keep listening to the InputStream while connected
      while (connectionState == STATE_CONNECTED) {
        try {
          // Read from the InputStream
          bytes = inputStream?.read(buffer) ?: 0

          // Send the obtained bytes to the UI Activity
          ownHandler?.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
        } catch (e: IOException) {
          Log.e(TAG, "disconnected", e)
          connectionLost()
          break
        }

      }
    }

    /**
     * Write to the connected OutStream.
     * @param buffer The bytes to write
     */
    fun write(buffer: ByteArray) {
      try {
        outputStream?.write(buffer)

        // Share the sent message back to the UI Activity
        ownHandler?.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
      } catch (e: IOException) {
        Log.e(TAG, "Exception during write", e)
      }

    }

    fun cancel() {
      try {
        mmSocket?.close()
      } catch (e: IOException) {
        Log.e(TAG, "close() of connect socket failed", e)
      }

    }
  }

}