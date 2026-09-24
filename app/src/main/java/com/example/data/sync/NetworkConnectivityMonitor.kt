package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

/**
 * Monitors network connectivity state and supports simulated offline mode
 * to test recording field observations without internet.
 */
class NetworkConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    // User-controlled simulation toggle: when true, app behaves as completely offline
    private val _isSimulatedOffline = MutableStateFlow(false)
    val isSimulatedOffline = _isSimulatedOffline.asStateFlow()

    fun setSimulatedOffline(offline: Boolean) {
        _isSimulatedOffline.value = offline
    }

    fun toggleSimulatedOffline() {
        _isSimulatedOffline.value = !_isSimulatedOffline.value
    }

    /**
     * Checks if actual hardware network is currently connected and has internet capability
     */
    fun isRealNetworkConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Effective network status: true only if real network is available AND not in simulated offline mode
     */
    fun isConnected(): Boolean {
        if (_isSimulatedOffline.value) return false
        return isRealNetworkConnected()
    }

    /**
     * Reactive Flow of actual hardware network connectivity
     */
    val realNetworkFlow: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isRealNetworkConnected())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        // Send initial state
        trySend(isRealNetworkConnected())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, networkCallback)

        awaitClose {
            try {
                cm.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
        }
    }

    /**
     * Combined reactive flow taking into account both real network and simulation override
     */
    val isOnlineFlow: Flow<Boolean> = combine(realNetworkFlow, _isSimulatedOffline) { realOnline, simulatedOffline ->
        realOnline && !simulatedOffline
    }
}
