package com.maysk.cloner.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.maysk.cloner.MainActivity
import com.maysk.cloner.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * Локальный файрвол на основе VpnService (DNS-sinkhole).
 *
 * Как это работает: служба поднимает локальный туннель и маршрутизирует через него ТОЛЬКО
 * DNS-запросы выбранных приложений (addAllowedApplication). Для каждого запроса определяется
 * домен; если он в чёрном списке этого приложения — служба сама отвечает «адрес не найден»,
 * и обращение к Google/microG не уходит в сеть. Все остальные запросы честно пересылаются
 * настоящему DNS, поэтому приложение и его собственная авторизация работают как обычно.
 *
 * Это не меняет код приложений и не является частью Google — это управление сетью на
 * собственном устройстве пользователя, как в обычном файрволе.
 */
class FirewallVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    private val forwarders = Executors.newFixedThreadPool(4)
    private lateinit var upstream: DatagramSocket

    companion object {
        const val ACTION_START = "com.maysk.cloner.FIREWALL_START"
        const val ACTION_STOP = "com.maysk.cloner.FIREWALL_STOP"
        private const val CHANNEL = "maysk_firewall"
        private const val NOTIF_ID = 42
        private const val TUN_ADDR = "10.111.222.1"
        private const val DNS_PROXY = "10.111.222.2"
        private const val UPSTREAM_DNS = "8.8.8.8"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopFirewall(); return START_NOT_STICKY }
            else -> startFirewall()
        }
        return START_STICKY
    }

    private fun startFirewall() {
        if (running) return
        val packages = FirewallState.routedPackages
        if (packages.isEmpty()) { stopSelf(); return }

        val builder = Builder()
            .setSession("Maysk App Cloner")
            .setMtu(1500)
            .addAddress(TUN_ADDR, 32)
            .addDnsServer(DNS_PROXY)
            .addRoute(DNS_PROXY, 32)
        // Через туннель пускаем только выбранные приложения — остальная система не затрагивается.
        var added = 0
        for (p in packages) {
            try { builder.addAllowedApplication(p); added++ } catch (_: Exception) { /* приложение удалено */ }
        }
        if (added == 0) { stopSelf(); return }

        tun = try { builder.establish() } catch (e: Exception) { null }
        if (tun == null) { stopSelf(); return }

        upstream = DatagramSocket().also { protect(it) }
        running = true
        FirewallState.setRunning(true)
        startForeground(NOTIF_ID, notification(added))

        worker = Thread { loop() }.also { it.start() }
    }

    private fun loop() {
        val input = FileInputStream(tun!!.fileDescriptor)
        val output = FileOutputStream(tun!!.fileDescriptor)
        val packet = ByteArray(32767)
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        while (running) {
            val n = try { input.read(packet) } catch (e: Exception) { break }
            if (n <= 0) continue
            val ip = Ip.parse(packet, n) ?: continue
            if (!ip.isUdp || ip.dstPort != 53) continue
            val payloadOff = ip.udpPayloadOffset
            val payloadLen = ip.udpPayloadLength.coerceAtMost(n - payloadOff)
            if (payloadLen <= 0) continue
            val dnsPayload = packet.copyOfRange(payloadOff, payloadOff + payloadLen)
            val host = Dns.questionName(dnsPayload, 0, dnsPayload.size)
            val uid = ownerUid(cm, ip)
            if (host != null && FirewallState.isBlocked(uid, host)) {
                val resp = Dns.nxdomain(dnsPayload, 0, dnsPayload.size)
                val ipResp = Ip.buildUdpResponse(ip, resp)
                synchronized(output) { runCatching { output.write(ipResp) } }
            } else {
                val reqCopy = Ip.V4(packet.copyOf(n), n)
                forwarders.execute { forward(reqCopy, dnsPayload, output) }
            }
        }
    }

    /** На Android 10+ можно точно узнать приложение, пославшее запрос; иначе — общий список. */
    private fun ownerUid(cm: ConnectivityManager?, ip: Ip.V4): Int {
        if (cm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        return try {
            val local = InetSocketAddress(InetAddress.getByAddress(ip.srcAddr()), ip.srcPort)
            val remote = InetSocketAddress(InetAddress.getByAddress(ip.dstAddr()), ip.dstPort)
            cm.getConnectionOwnerUid(Ip.PROTO_UDP, local, remote)
        } catch (e: Exception) { -1 }
    }

    private fun forward(req: Ip.V4, dnsPayload: ByteArray, output: FileOutputStream) {
        try {
            val sock = DatagramSocket().also { protect(it); it.soTimeout = 5000 }
            sock.use {
                it.send(DatagramPacket(dnsPayload, dnsPayload.size, InetAddress.getByName(UPSTREAM_DNS), 53))
                val resp = ByteArray(2048)
                val dp = DatagramPacket(resp, resp.size)
                it.receive(dp)
                val answer = resp.copyOf(dp.length)
                val ipResp = Ip.buildUdpResponse(req, answer)
                synchronized(output) { runCatching { output.write(ipResp) } }
            }
        } catch (e: Exception) { /* таймаут/сеть — просто пропускаем */ }
    }

    private fun stopFirewall() {
        running = false
        FirewallState.setRunning(false)
        runCatching { worker?.interrupt() }
        runCatching { tun?.close() }
        tun = null
        runCatching { if (::upstream.isInitialized) upstream.close() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        FirewallState.setRunning(false)
        runCatching { tun?.close() }
        forwarders.shutdownNow()
        super.onDestroy()
    }

    override fun onRevoke() { stopFirewall() }

    private fun notification(count: Int): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, getString(R.string.vpn_channel), NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle("Файрвол Maysk активен")
            .setContentText("Google-запросы ограничены для приложений: $count")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }
}
