package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkTransport(val displayLabel: String) {
    MOBILE_DATA("Mobile Data"),
    WIFI("Wi-Fi"),
    OTHER("Online"),
    OFFLINE("Offline")
}

data class NetworkConnectionState(
    val isConnected: Boolean = true,
    val transport: NetworkTransport = NetworkTransport.WIFI,
    val isMetered: Boolean = false
) {
    val canStreamOnMobileData: Boolean
        get() = isConnected && transport == NetworkTransport.MOBILE_DATA
}

object NetworkConnectivityHelper {

    fun currentNetworkState(context: Context): NetworkConnectionState {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkConnectionState(isConnected = false, transport = NetworkTransport.OFFLINE)
            val activeNetwork = cm.activeNetwork
                ?: return NetworkConnectionState(isConnected = false, transport = NetworkTransport.OFFLINE)
            val caps = cm.getNetworkCapabilities(activeNetwork)
                ?: return NetworkConnectionState(isConnected = false, transport = NetworkTransport.OFFLINE)
            mapCapabilitiesToState(caps, cm.isActiveNetworkMetered)
        }.getOrElse {
            NetworkConnectionState(isConnected = true, transport = NetworkTransport.OTHER, isMetered = false)
        }
    }

    fun observeNetworkState(context: Context): Flow<NetworkConnectionState> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(NetworkConnectionState(isConnected = false, transport = NetworkTransport.OFFLINE))
            awaitClose { }
            return@callbackFlow
        }

        trySend(currentNetworkState(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentNetworkState(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(mapCapabilitiesToState(networkCapabilities, cm.isActiveNetworkMetered))
            }

            override fun onLost(network: Network) {
                trySend(currentNetworkState(context))
            }

            override fun onUnavailable() {
                trySend(NetworkConnectionState(isConnected = false, transport = NetworkTransport.OFFLINE))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val registered = runCatching {
            cm.registerNetworkCallback(request, callback)
            true
        }.getOrElse { false }

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    internal fun mapCapabilitiesToState(
        caps: NetworkCapabilities,
        isMetered: Boolean
    ): NetworkConnectionState {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) {
            return NetworkConnectionState(
                isConnected = false,
                transport = NetworkTransport.OFFLINE,
                isMetered = isMetered
            )
        }
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.MOBILE_DATA
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            else -> NetworkTransport.OTHER
        }
        return NetworkConnectionState(
            isConnected = true,
            transport = transport,
            isMetered = isMetered || transport == NetworkTransport.MOBILE_DATA
        )
    }
}
