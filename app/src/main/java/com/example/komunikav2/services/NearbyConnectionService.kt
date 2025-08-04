package com.example.komunikav2.services

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.example.komunikav2.data.UserProfile
import com.example.komunikav2.data.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class NearbyConnectionService private constructor(private val context: Context) {
    
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val discoveredUsers: StateFlow<List<UserProfile>> = _discoveredUsers.asStateFlow()
    
    private val _connectedUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val connectedUsers: StateFlow<List<UserProfile>> = _connectedUsers.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private var currentUserProfile: UserProfile? = null
    private var currentServiceId: String? = null
    private val connectedEndpointIds = mutableSetOf<String>()
    private val sentProfilesToEndpoints = mutableSetOf<String>()
    private val receivedProfilesFromEndpoints = mutableSetOf<String>()
    private val endpointToUserMap = mutableMapOf<String, UserProfile>()
    

    

    
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId")
            Log.d(TAG, "Connection info: ${connectionInfo.endpointName}")
            _connectionState.value = ConnectionState.CONNECTING
            // Accept the connection automatically
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.d(TAG, "Connection accepted with $endpointId")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to accept connection", exception)
                }
        }
        
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "Connection result for $endpointId: ${result.status.statusCode}")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful with $endpointId")
                    connectedEndpointIds.add(endpointId)
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d(TAG, "Added endpoint $endpointId. Total connected endpoints: ${connectedEndpointIds.size}")
                    
                    // Send our profile to the connected device
                    sendUserProfile(endpointId)
                    
                    // Add a small delay to ensure the connection is fully established
                    // and then send our profile again to ensure bidirectional exchange
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Connection fully established with $endpointId")
                        // Send profile again to ensure the other device receives it
                        if (!sentProfilesToEndpoints.contains(endpointId)) {
                            Log.d(TAG, "Sending profile again to $endpointId")
                            sendUserProfile(endpointId)
                        } else {
                            Log.d(TAG, "Profile already sent to $endpointId")
                        }
                    }, 1000)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Connection error with $endpointId")
                    _connectionState.value = ConnectionState.ERROR
                }
            }
        }
        
        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            Log.d(TAG, "Current connected endpoints before removal: $connectedEndpointIds")
            Log.d(TAG, "Current connected users before removal: ${_connectedUsers.value.map { it.name }}")
            
            connectedEndpointIds.remove(endpointId)
            sentProfilesToEndpoints.remove(endpointId)
            receivedProfilesFromEndpoints.remove(endpointId)
            removeConnectedUser(endpointId)
            
            Log.d(TAG, "After removal - connected endpoints: $connectedEndpointIds")
            Log.d(TAG, "After removal - connected users: ${_connectedUsers.value.map { it.name }}")
            
            // Update connection state only if no more connected endpoints
            if (connectedEndpointIds.isEmpty()) {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "No more connected endpoints, setting state to DISCONNECTED")
                
                // Auto-restart advertising and discovery if we have a current user profile
                currentUserProfile?.let { profile ->
                    Log.d(TAG, "Auto-restarting advertising and discovery after disconnection")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (_connectionState.value == ConnectionState.DISCONNECTED) {
                            startAdvertisingAndDiscovery(profile)
                        }
                    }, 2000) // Wait 2 seconds before restarting
                }
            } else {
                Log.d(TAG, "Still have ${connectedEndpointIds.size} connected endpoints")
            }
        }
    }
    
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "Payload received from $endpointId, type: ${payload.type}")
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = String(payload.asBytes()!!)
                    Log.d(TAG, "Received data: $data")
                    handleReceivedData(endpointId, data)
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "Payload transfer update from $endpointId: ${update.status}")
        }
    }
    

    
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId")
            Log.d(TAG, "Service ID: ${discoveredEndpointInfo.serviceId}, Current Service ID: $currentServiceId")
            val serviceId = discoveredEndpointInfo.serviceId
            if (serviceId == currentServiceId) {
                Log.d(TAG, "Service IDs match, requesting connection to $endpointId")
                connectionsClient.requestConnection(
                    generateEndpointName(),
                    endpointId,
                    connectionLifecycleCallback
                ).addOnSuccessListener {
                    Log.d(TAG, "Connection request sent to $endpointId")
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to request connection", exception)
                }
            } else {
                Log.d(TAG, "Service IDs don't match, ignoring endpoint")
            }
        }
        
        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }
    
    fun startAdvertising(userProfile: UserProfile) {
        currentUserProfile = userProfile
        currentServiceId = userProfile.serviceId
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        connectionsClient.startAdvertising(
            userProfile.id,
            userProfile.serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Started advertising")
            _connectionState.value = ConnectionState.ADVERTISING
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to start advertising", exception)
            _connectionState.value = ConnectionState.ERROR
        }
    }
    
    fun startAdvertisingAndDiscovery(userProfile: UserProfile) {
        startAdvertising(userProfile)
        startDiscovery(userProfile.serviceId)
    }
    
    fun startDiscovery(serviceId: String) {
        currentServiceId = serviceId
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Started discovery")
            _connectionState.value = ConnectionState.DISCOVERING
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to start discovery", exception)
            _connectionState.value = ConnectionState.ERROR
        }
    }
    
    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun disconnect() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectedEndpointIds.clear()
        sentProfilesToEndpoints.clear()
        receivedProfilesFromEndpoints.clear()
        endpointToUserMap.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedUsers.value = emptyList()
        _discoveredUsers.value = emptyList()
        Log.d(TAG, "Disconnected and cleared all connection data")
    }
    
    fun resetForReconnection() {
        Log.d(TAG, "Resetting service for reconnection")
        disconnect()
        
        // Clear the current user profile to force a fresh start
        currentUserProfile = null
        currentServiceId = null
        
        // Add a small delay to ensure everything is cleared
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Service reset complete, ready for reconnection")
        }, 500)
    }
    
    fun sendMessage(text: String) {
        Log.d(TAG, "Attempting to send message: '$text'")
        Log.d(TAG, "Connected endpoints: $connectedEndpointIds")
        Log.d(TAG, "Connected users: ${_connectedUsers.value.map { it.name }}")
        
        if (connectedEndpointIds.isEmpty()) {
            Log.w(TAG, "No connected endpoints to send message to")
            return
        }
        
        if (currentUserProfile == null) {
            Log.e(TAG, "Cannot send message: currentUserProfile is null")
            return
        }
        
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            senderId = currentUserProfile?.id ?: "",
            senderName = currentUserProfile?.name ?: "",
            senderAvatar = currentUserProfile?.avatar ?: "",
            timestamp = System.currentTimeMillis(),
            isIncoming = false
        )
        
        val messageJson = json.encodeToString(message)
        val payload = Payload.fromBytes(messageJson.toByteArray())
        
        Log.d(TAG, "Sending message to ${connectedEndpointIds.size} endpoints: $text")
        Log.d(TAG, "Message JSON: $messageJson")
        
        // Send to all connected endpoints
        for (endpointId in connectedEndpointIds) {
            Log.d(TAG, "Sending to endpoint: $endpointId")
            connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    Log.d(TAG, "Message sent successfully to $endpointId")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to send message to $endpointId", exception)
                }
        }
        
        addMessage(message)
        Log.d(TAG, "Message added to local list")
    }
    
    private fun sendUserProfile(endpointId: String) {
        currentUserProfile?.let { profile ->
            val profileJson = json.encodeToString(profile)
            val payload = Payload.fromBytes(profileJson.toByteArray())
            Log.d(TAG, "Sending user profile to $endpointId: ${profile.name}")
            connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    Log.d(TAG, "User profile sent successfully to $endpointId")
                    sentProfilesToEndpoints.add(endpointId)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to send user profile to $endpointId", exception)
                }
        } ?: run {
            Log.e(TAG, "Cannot send user profile: currentUserProfile is null")
        }
    }
    
    private fun handleReceivedData(endpointId: String, data: String) {
        try {
            Log.d(TAG, "Handling received data from $endpointId: $data")
            if (data.contains("\"id\"") && data.contains("\"name\"")) {
                val userProfile = json.decodeFromString<UserProfile>(data)
                Log.d(TAG, "Received user profile: ${userProfile.name}")
                addConnectedUser(userProfile, endpointId)
                receivedProfilesFromEndpoints.add(endpointId)
                
                // Ensure the endpoint is in our connected endpoints set
                if (!connectedEndpointIds.contains(endpointId)) {
                    Log.d(TAG, "Adding endpoint $endpointId to connected endpoints (received profile)")
                    connectedEndpointIds.add(endpointId)
                    _connectionState.value = ConnectionState.CONNECTED
                }
                
                // If we received a profile and haven't sent ours yet, send it now
                if (!sentProfilesToEndpoints.contains(endpointId)) {
                    Log.d(TAG, "Received profile from $endpointId, sending our profile back")
                    sendUserProfile(endpointId)
                }
            } else if (data.contains("\"text\"")) {
                val message = json.decodeFromString<ChatMessage>(data)
                Log.d(TAG, "Received message: ${message.text}")
                addMessage(message.copy(isIncoming = true))
            } else {
                Log.w(TAG, "Unknown data format received: $data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing received data: $data", e)
        }
    }
    
    private fun addConnectedUser(userProfile: UserProfile, endpointId: String? = null) {
        val currentList = _connectedUsers.value.toMutableList()
        if (!currentList.any { it.id == userProfile.id }) {
            currentList.add(userProfile)
            _connectedUsers.value = currentList
            
            // Add to endpoint mapping if endpointId is provided
            endpointId?.let { 
                endpointToUserMap[it] = userProfile
                Log.d(TAG, "Mapped endpoint $it to user ${userProfile.name}")
            }
            
            Log.d(TAG, "Added connected user: ${userProfile.name}. Total connected users: ${currentList.size}")
        } else {
            Log.d(TAG, "User ${userProfile.name} already in connected users list")
        }
    }
    
    private fun removeConnectedUser(endpointId: String) {
        Log.d(TAG, "Removing connected user for endpoint: $endpointId")
        Log.d(TAG, "Current endpoint to user map: ${endpointToUserMap.keys}")
        
        // Find the user profile for this endpoint
        val userProfile = endpointToUserMap[endpointId]
        if (userProfile != null) {
            val currentList = _connectedUsers.value.toMutableList()
            Log.d(TAG, "Current users before removal: ${currentList.map { it.name }}")
            
            currentList.removeAll { it.id == userProfile.id }
            _connectedUsers.value = currentList
            
            endpointToUserMap.remove(endpointId)
            Log.d(TAG, "Successfully removed user ${userProfile.name} for endpoint $endpointId")
            Log.d(TAG, "Users after removal: ${currentList.map { it.name }}")
            
            // Force refresh to ensure UI updates
            forceRefreshConnectedUsers()
        } else {
            Log.w(TAG, "No user profile found for endpoint $endpointId")
            Log.w(TAG, "Available endpoints: ${endpointToUserMap.keys}")
            Log.w(TAG, "Available users: ${endpointToUserMap.values.map { it.name }}")
        }
    }
    
    private fun addMessage(message: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }
    
    private fun generateEndpointName(): String {
        return "Komunika_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    fun getConnectionStatus(): String {
        return "State: ${_connectionState.value}, Endpoints: $connectedEndpointIds, Users: ${_connectedUsers.value.map { it.name }}"
    }
    
    fun forceRefreshConnectedUsers() {
        Log.d(TAG, "Force refreshing connected users")
        val currentUsers = _connectedUsers.value.toList()
        _connectedUsers.value = currentUsers
        Log.d(TAG, "Refreshed users: ${currentUsers.map { it.name }}")
    }
    
    enum class ConnectionState {
        DISCONNECTED,
        ADVERTISING,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    
    companion object {
        private const val TAG = "NearbyConnectionService"
        
        @Volatile
        private var INSTANCE: NearbyConnectionService? = null
        
        fun getInstance(context: Context): NearbyConnectionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NearbyConnectionService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun clearInstance() {
            INSTANCE = null
        }
    }
} 