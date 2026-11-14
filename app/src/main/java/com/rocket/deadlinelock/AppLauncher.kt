package com.rocket.deadlinelock

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLauncher {
    private val vpnPackages = listOf(
        "com.vpn.client", "de.blinkt.openvpn", "com.wireguard.android",
        "org.openvpn.client", "com.psiphon3",
        "com.cloudflare.onedotonedotonedotone",
        "com.nordvpn.android", "com.expressvpn.vpn"
    )
    private const val TELEGRAM_PKG = "org.telegram.messenger"

    fun openTelegram(ctx: Context): Boolean {
        val launch = ctx.packageManager.getLaunchIntentForPackage(TELEGRAM_PKG)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launch)
            return true
        }
        return openUrl(ctx, "https://t.me/qwerty112005")
    }

    fun openVpn(ctx: Context): Boolean {
        for (pkg in vpnPackages) {
            val launch = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launch)
            return true
        }
        val settings = Intent(android.provider.Settings.ACTION_VPN_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { ctx.startActivity(settings); true } catch (_: Exception) { false }
    }

    private fun openUrl(ctx: Context, url: String): Boolean = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) { false }
}
