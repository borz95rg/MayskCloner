package com.maysk.cloner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.maysk.cloner.ui.AppRoot

class MainActivity : ComponentActivity() {

    /** Колбэк после системного диалога разрешения VPN. */
    private var onVpnResult: ((Boolean) -> Unit)? = null
    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        onVpnResult?.invoke(res.resultCode == Activity.RESULT_OK)
        onVpnResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ClonerApp).container
        container.updates.autoCheck()
        setContent {
            AppRoot(
                container = container,
                requestVpnPermission = { onGranted ->
                    val intent = container.firewall.prepareIntent()
                    if (intent == null) onGranted(true)
                    else { onVpnResult = onGranted; vpnLauncher.launch(intent) }
                },
            )
        }
    }
}
