package com.example.bluetoothterminal.Mod_1

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothterminal.PIDsEnums.FirstModeRequestEnums
import com.example.bluetoothterminal.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class Mod1Activity : AppCompatActivity() {

    private val DEVICE_ADDRESS = "00:00:00:00:11:11"
    private val PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val MOD_1_PREFIX = "41"
    private val PID_NOT_SUPPORTED = "This PID is not supported. \n"

    private lateinit var device: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    private lateinit var startButton: Button
    private lateinit var sendButton: Button
    private lateinit var clearButton: Button
    private lateinit var stopButton: Button
    private lateinit var MOD_1_LIST: Spinner
    private lateinit var textView: TextView

    private var deviceConnected = false
    private lateinit var thread: Thread
    private lateinit var buffer: ByteArray
    private var stopThread = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mod_1_activity)

        startButton = findViewById(R.id.buttonStart)
        sendButton = findViewById(R.id.buttonSend)
        clearButton = findViewById(R.id.buttonClear)
        stopButton = findViewById(R.id.buttonStop)
        textView = findViewById(R.id.textView)
        MOD_1_LIST = findViewById(R.id.mod1list)

        val myAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, FirstModeRequestEnums.values())
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        MOD_1_LIST.adapter = myAdapter

        textView.movementMethod = ScrollingMovementMethod()

        setupUserInterface(false)
    }

    private fun setupUserInterface(enabled: Boolean) {
        startButton.isEnabled = !enabled
        sendButton.isEnabled = enabled
        stopButton.isEnabled = enabled
        textView.isEnabled = enabled
    }

    private fun initializeBluetoothAdapter(): Boolean {
        var found = false
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        }
        if (!bluetoothAdapter?.isEnabled == true) {
            val enableAdapter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableAdapter, 0)
            Thread.sleep(1000)
        }
        val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices ?: emptySet()
        if (bondedDevices.isEmpty()) {
            Toast.makeText(applicationContext, "Device is not paired", Toast.LENGTH_SHORT).show()
        } else {
            for (deviceIterator in bondedDevices) {
                if (deviceIterator.address == DEVICE_ADDRESS) {
                    device = deviceIterator
                    found = true
                    break
                }
            }
        }
        return found
    }

    private fun initializeBluetoothConnection(): Boolean {
        var connected = true
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID)
            socket.connect()
        } catch (e: IOException) {
            e.printStackTrace()
            connected = false
        }
        if (connected) {
            try {
                outputStream = socket.outputStream
                inputStream = socket.inputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return connected
    }

    fun onClickStart(view: View) {
        if (initializeBluetoothAdapter()) {
            if (initializeBluetoothConnection()) {
                setupUserInterface(true)
                deviceConnected = true
                dataListening()
                textView.append("\nConnection Opened!\n")
            }
        }
    }

    private fun dataListening() {
        val handler = Handler()
        stopThread = false
        buffer = ByteArray(1024)
        thread = Thread {
            while (!Thread.currentThread().isInterrupted && !stopThread) {
                try {
                    val byteCount = inputStream.available()
                    if (byteCount > 0) {
                        val rawBytes = ByteArray(byteCount)
                        inputStream.read(rawBytes)
                        val rawResponse = String(rawBytes, Charsets.UTF_8)

                        if (rawResponse.startsWith(MOD_1_PREFIX)) {
                            val response = MOD1ResponseCalculator.MOD1ResponseCalculator(rawResponse)
                            handler.post { textView.append(response) }
                        } else {
                            handler.post { textView.append(rawResponse) }
                        }
                    }
                } catch (ex: IOException) {
                    stopThread = true
                }
            }
        }
        thread.start()
    }

    fun onClickSend(view: View) {
        val PID = MOD_1_LIST.selectedItem.toString()
        val requestPID = "01" + FirstModeRequestEnums.valueOf(PID).value + "\n"
        try {
            outputStream.write(requestPID.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        textView.append("\nRequest: $PID\n")
    }

    fun onClickStop(view: View) {
        stopThread = true
        outputStream.close()
        inputStream.close()
        socket.close()
        setupUserInterface(false)
        deviceConnected = false
        textView.append("\nConnection Closed!\n")
    }

    fun onClickClear(view: View) {
        textView.text = ""
    }
}
