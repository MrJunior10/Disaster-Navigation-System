package org.y20k.pdmb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity2 : AppCompatActivity() {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: WifiDirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    private lateinit var peers: MutableList<WifiP2pDevice>
    private lateinit var peerNames: MutableList<String>
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private lateinit var textView: TextView
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Bind views to their IDs
        textView = findViewById(R.id.textView)
        listView = findViewById(R.id.listView)

        // Initialize WiFi P2P components
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)
        receiver = WifiDirectBroadcastReceiver(wifiP2pManager, channel, this)
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        // Set up peer list and adapter
        peers = mutableListOf()
        peerNames = mutableListOf()
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, peerNames)
        listView.adapter = arrayAdapter

        // Set up peer click listener
        listView.setOnItemClickListener { _, _, position, _ ->
            connect(peers[position])
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    fun updatePeers(newPeers: Collection<WifiP2pDevice>) {
        peers.clear()
        peerNames.clear()
        peers.addAll(newPeers)
        peerNames.addAll(newPeers.map { it.deviceName })
        arrayAdapter.notifyDataSetChanged()
    }

    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@MainActivity2, "Connecting to ${device.deviceName}", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@MainActivity2, "Failed to connect to ${device.deviceName}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class WifiDirectBroadcastReceiver(
        private val wifiP2pManager: WifiP2pManager,
        private val channel: WifiP2pManager.Channel,
        private val activity: MainActivity2
    ) : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager.requestPeers(channel) { peerList ->
                        activity.updatePeers(peerList.deviceList)
                    }
                }
            }
        }
    }
}
