package hr.zet.transit.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Provjerava ima li uređaj stvarnu internetsku vezu.
 *
 * Služi za razlikovanje "nema interneta" (problem do korisnika) od
 * "poslužitelj ne odgovara" (problem do nas) pri neuspjelom dohvatu —
 * korisnik treba znati koje od to dvoje.
 */
class ConnectivityChecker(private val context: Context) {

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // INTERNET = mreža postoji; VALIDATED = stvarno ima izlaz na internet.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
