package dev.manuel.bluetoothsimplechat

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dev.manuel.bluetoothsimplechat.application.adapters.DevicesRecyclerViewAdapter
import dev.manuel.bluetoothsimplechat.application.events.CommunicationListener
import dev.manuel.bluetoothsimplechat.application.events.ItemClickListener
import dev.manuel.bluetoothsimplechat.application.fragments.ChatFragment
import dev.manuel.bluetoothsimplechat.application.lasting.Constants
import dev.manuel.bluetoothsimplechat.application.services.BluetoothChatService
import dev.manuel.bluetoothsimplechat.domain.entities.DeviceData

class MainActivity : AppCompatActivity(), ItemClickListener, CommunicationListener {

  private val REQUEST_ENABLE_BT = 123
  private lateinit var progressBar: ProgressBar
  private lateinit var recyclerView: RecyclerView
  private lateinit var recyclerPairedView: RecyclerView
  private val mDeviceList = arrayListOf<DeviceData>()
  private lateinit var devicesAdapter: DevicesRecyclerViewAdapter
  private var mBtAdapter: BluetoothAdapter? = null
  private val PERMISSION_REQUEST_LOCATION = 123
  private val PERMISSION_REQUEST_LOCATION_KEY = "PERMISSION_REQUEST_LOCATION"
  private var alreadyAskedForPermission = false
  private lateinit var txtHeaderLabel: TextView
  private lateinit var txtHeaderLabelPaired: TextView
  private lateinit var headerLabelContainer: LinearLayout
  private lateinit var txtStatus: TextView
  private lateinit var imgConnectionDot: ImageView
  private lateinit var mConnectedDeviceName: String
  private var connected: Boolean = false

  private var mChatService: BluetoothChatService? = null
  private lateinit var chatFragment: ChatFragment

  @SuppressLint("MissingPermission")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val txtToolbarTitle = findViewById<TextView>(R.id.txtToolbarTitle)

    //val typeFace = Typeface.createFromAsset(assets, "fonts/product_sans.ttf")
    //txtToolbarTitle.typeface = typeFace

    progressBar = findViewById(R.id.progressBar)
    recyclerView = findViewById(R.id.recyclerView)
    recyclerPairedView = findViewById(R.id.recyclerPairedView)
    txtHeaderLabel = findViewById(R.id.txtHeaderLabel)
    txtHeaderLabelPaired = findViewById(R.id.txtHeaderLabelPaired)
    headerLabelContainer = findViewById(R.id.headerLabelContainer)
    txtStatus = findViewById(R.id.txtStatus)
    imgConnectionDot = findViewById(R.id.imgConnectionDot)

    txtStatus.text = getString(R.string.bluetooth_not_enabled)

    headerLabelContainer.visibility = View.INVISIBLE

    if (savedInstanceState != null)
      alreadyAskedForPermission =
        savedInstanceState.getBoolean(PERMISSION_REQUEST_LOCATION_KEY, false)

    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerPairedView.layoutManager = LinearLayoutManager(this)

    recyclerView.isNestedScrollingEnabled = false
    recyclerPairedView.isNestedScrollingEnabled = false

    findViewById<Button>(R.id.btnSearchDevices).setOnClickListener {
      findDevices()
    }

    findViewById<Button>(R.id.btnMakeVisible).setOnClickListener {
      makeVisible()
    }

    devicesAdapter = DevicesRecyclerViewAdapter(context = this, mDeviceList = mDeviceList)
    recyclerView.adapter = devicesAdapter
    devicesAdapter.setItemClickListener(this)

    // Register for broadcasts when a device is discovered.
    var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    registerReceiver(receiver, filter)

    // Register for broadcasts when discovery has finished
    filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    this.registerReceiver(receiver, filter)

    // Get the local Bluetooth adapter
    var manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    mBtAdapter = manager.adapter

    // Initialize the BluetoothChatService to perform bluetooth connections
    mChatService = BluetoothChatService(this, handler)

    if (mBtAdapter == null)
      showAlertAndExit()
    else {

      if (mBtAdapter?.isEnabled == false) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
      } else {
        txtStatus.text = getString(R.string.not_connected)
      }

      // Get a set of currently paired devices
      val pairedDevices = mBtAdapter?.bondedDevices
      val mPairedDeviceList = arrayListOf<DeviceData>()

      // If there are paired devices, add each one to the ArrayAdapter
      if (pairedDevices?.size ?: 0 > 0) {
        // There are paired devices. Get the name and address of each paired device.
        for (device in pairedDevices!!) {
          val deviceName = device.name
          val deviceHardwareAddress = device.address // MAC address
          mPairedDeviceList.add(DeviceData(deviceName, deviceHardwareAddress))
        }

        val devicesAdapter =
          DevicesRecyclerViewAdapter(context = this, mDeviceList = mPairedDeviceList)
        recyclerPairedView.adapter = devicesAdapter
        devicesAdapter.setItemClickListener(this)
        txtHeaderLabelPaired.visibility = View.VISIBLE

      }
    }

    //showChatFragment()

  }

  @SuppressLint("MissingPermission")
  private fun makeVisible() {

    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
    startActivity(discoverableIntent)

  }

  private fun checkPermissions() {

    if (alreadyAskedForPermission) {
      // don't check again because the dialog is still open
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Android M Permission check 
      if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
      ) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.need_loc_access))
        builder.setMessage(getString(R.string.please_grant_loc_access))
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setOnDismissListener {
          // the dialog will be opened so we have to save that
          alreadyAskedForPermission = true
          requestPermissions(
            arrayOf(
              ACCESS_COARSE_LOCATION,
              ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_LOCATION
          )
        }
        builder.show()

      } else {
        startDiscovery()
      }
    } else {
      startDiscovery()
      alreadyAskedForPermission = true
    }

  }

  private fun showAlertAndExit() {

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.not_compatible))
      .setMessage(getString(R.string.no_support))
      .setPositiveButton("Exit", { _, _ -> System.exit(0) })
      .show()
  }

  private fun findDevices() {

    checkPermissions()
  }

  @SuppressLint("MissingPermission")
  private fun startDiscovery() {

    headerLabelContainer.visibility = View.VISIBLE
    progressBar.visibility = View.VISIBLE
    txtHeaderLabel.text = getString(R.string.searching)
    mDeviceList.clear()

    // If we're already discovering, stop it
    if (mBtAdapter?.isDiscovering ?: false)
      mBtAdapter?.cancelDiscovery()

    // Request discover from BluetoothAdapter
    mBtAdapter?.startDiscovery()
  }

  // Create a BroadcastReceiver for ACTION_FOUND.
  private val receiver = object : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {

      val action = intent.action

      if (BluetoothDevice.ACTION_FOUND == action) {
        // Discovery has found a device. Get the BluetoothDevice
        // object and its info from the Intent.
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val deviceName = device?.name
        val deviceHardwareAddress = device?.address // MAC address

        val deviceData = deviceHardwareAddress?.let { DeviceData(deviceName, it) }
        mDeviceList.add(deviceData!!)

        val setList = HashSet<DeviceData>(mDeviceList)
        mDeviceList.clear()
        mDeviceList.addAll(setList)

        devicesAdapter.notifyDataSetChanged()
      }

      if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
        progressBar.visibility = View.INVISIBLE
        txtHeaderLabel.text = getString(R.string.found)
      }
    }
  }

  @SuppressLint("MissingPermission")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    progressBar.visibility = View.INVISIBLE

    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
      //Bluetooth is now connected.
      txtStatus.text = getString(R.string.not_connected)

      // Get a set of currently paired devices
      val pairedDevices = mBtAdapter?.bondedDevices
      val mPairedDeviceList = arrayListOf<DeviceData>()

      mPairedDeviceList.clear()

      // If there are paired devices, add each one to the ArrayAdapter
      if (pairedDevices?.size ?: 0 > 0) {
        // There are paired devices. Get the name and address of each paired device.
        for (device in pairedDevices!!) {
          val deviceName = device.name
          val deviceHardwareAddress = device.address // MAC address
          mPairedDeviceList.add(DeviceData(deviceName, deviceHardwareAddress))
        }

        val devicesAdapter =
          DevicesRecyclerViewAdapter(context = this, mDeviceList = mPairedDeviceList)
        recyclerPairedView.adapter = devicesAdapter
        devicesAdapter.setItemClickListener(this)
        txtHeaderLabelPaired.visibility = View.VISIBLE

      }

    }
    //label.setText("Bluetooth is now enabled.")
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(PERMISSION_REQUEST_LOCATION_KEY, alreadyAskedForPermission)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {

      PERMISSION_REQUEST_LOCATION -> {
        // the request returned a result so the dialog is closed
        alreadyAskedForPermission = false
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
          grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
          //Log.d(TAG, "Coarse and fine location permissions granted")
          startDiscovery()
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.fun_limted))
            builder.setMessage(getString(R.string.since_perm_not_granted))
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
          }
        }
      }
    }
  }

  override fun itemClicked(deviceData: DeviceData) {
    connectDevice(deviceData)
  }

  @SuppressLint("MissingPermission")
  private fun connectDevice(deviceData: DeviceData) {

    // Cancel discovery because it's costly and we're about to connect
    mBtAdapter?.cancelDiscovery()
    val deviceAddress = deviceData.deviceHardwareAddress

    val device = mBtAdapter?.getRemoteDevice(deviceAddress)

    txtStatus.text = getString(R.string.connecting)
    imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))

    // Attempt to connect to the device
    mChatService?.connect(device, true)

  }

  override fun onResume() {
    super.onResume()
    // Performing this check in onResume() covers the case in which BT was
    // not enabled during onStart(), so we were paused to enable it...
    // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    if (mChatService != null) {
      // Only if the state is STATE_NONE, do we know that we haven't started already
      if (mChatService?.getState() == BluetoothChatService.STATE_NONE) {
        // Start the Bluetooth chat services
        mChatService?.start()
      }
    }

    if (connected)
      showChatFragment()

  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(receiver)
  }


  /**
   * The Handler that gets information back from the BluetoothChatService
   */
  private val handler = @SuppressLint("HandlerLeak")
  object : Handler() {
    override fun handleMessage(msg: Message) {

      when (msg.what) {

        Constants.MESSAGE_STATE_CHANGE -> {

          when (msg.arg1) {

            BluetoothChatService.STATE_CONNECTED -> {

              txtStatus.text = getString(R.string.connected_to) + " " + mConnectedDeviceName
              imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connected))
              Snackbar.make(
                findViewById(R.id.mainScreen),
                "Connected to " + mConnectedDeviceName,
                Snackbar.LENGTH_SHORT
              ).show()
              //mConversationArrayAdapter.clear()
              connected = true
            }

            BluetoothChatService.STATE_CONNECTING -> {
              txtStatus.text = getString(R.string.connecting)
              imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))
              connected = false
            }

            BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> {
              txtStatus.text = getString(R.string.not_connected)
              imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_red))
              Snackbar.make(
                findViewById(R.id.mainScreen),
                getString(R.string.not_connected),
                Snackbar.LENGTH_SHORT
              ).show()
              connected = false
            }
          }
        }

        Constants.MESSAGE_WRITE -> {
          val writeBuf = msg.obj as ByteArray
          // construct a string from the buffer
          val writeMessage = String(writeBuf)
          //Toast.makeText(this@MainActivity,"Me: $writeMessage",Toast.LENGTH_SHORT).show()
          //mConversationArrayAdapter.add("Me:  " + writeMessage)
          val milliSecondsTime = System.currentTimeMillis()
          chatFragment.communicate(
            dev.manuel.bluetoothsimplechat.domain.entities.Message(
              writeMessage,
              milliSecondsTime,
              Constants.MESSAGE_TYPE_SENT
            )
          )

        }
        Constants.MESSAGE_READ -> {
          val readBuf = msg.obj as ByteArray
          // construct a string from the valid bytes in the buffer
          val readMessage = String(readBuf, 0, msg.arg1)
          val milliSecondsTime = System.currentTimeMillis()
          //Toast.makeText(this@MainActivity,"$mConnectedDeviceName : $readMessage",Toast.LENGTH_SHORT).show()
          //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage)
          chatFragment.communicate(
            dev.manuel.bluetoothsimplechat.domain.entities.Message(
              readMessage,
              milliSecondsTime,
              Constants.MESSAGE_TYPE_RECEIVED
            )
          )
        }
        Constants.MESSAGE_DEVICE_NAME -> {
          // save the connected device's name
          mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME).toString()
          txtStatus.text = getString(R.string.connected_to) + " " + mConnectedDeviceName
          imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connected))
          Snackbar.make(
            findViewById(R.id.mainScreen),
            "Connected to " + mConnectedDeviceName,
            Snackbar.LENGTH_SHORT
          ).show()
          connected = true
          showChatFragment()
        }
        Constants.MESSAGE_TOAST -> {
          txtStatus.text = getString(R.string.not_connected)
          imgConnectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_red))
          msg.data.getString(Constants.TOAST)?.let {
            Snackbar.make(
              findViewById(R.id.mainScreen),
              it,
              Snackbar.LENGTH_SHORT
            ).show()
          }
          connected = false
        }
      }
    }
  }


  private fun sendMessage(message: String) {

    // Check that we're actually connected before trying anything
    if (mChatService?.getState() != BluetoothChatService.STATE_CONNECTED) {
      Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
      return
    }

    // Check that there's actually something to send
    if (message.isNotEmpty()) {
      // Get the message bytes and tell the BluetoothChatService to write
      val send = message.toByteArray()
      mChatService?.write(send)

      // Reset out string buffer to zero and clear the edit text field
      //mOutStringBuffer.setLength(0)
      //mOutEditText.setText(mOutStringBuffer)
    }
  }

  private fun showChatFragment() {

    if (!isFinishing) {
      val fragmentManager = supportFragmentManager
      val fragmentTransaction = fragmentManager.beginTransaction()
      chatFragment = ChatFragment.newInstance()
      chatFragment.setCommunicationListener(this)
      fragmentTransaction.replace(R.id.mainScreen, chatFragment, "ChatFragment")
      fragmentTransaction.addToBackStack("ChatFragment")
      fragmentTransaction.commit()
    }
  }

  override fun onCommunication(message: String) {
    sendMessage(message)
  }

  override fun onBackPressed() {
    if (supportFragmentManager.backStackEntryCount == 0)
      super.onBackPressed()
    else
      supportFragmentManager.popBackStack()
  }
}