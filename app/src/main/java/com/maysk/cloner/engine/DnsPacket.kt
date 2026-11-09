package com.maysk.cloner.engine

import java.nio.ByteBuffer

/**
 * Минимальный разбор и сборка IPv4 + UDP + DNS, достаточный для DNS-фильтра.
 * Мы читаем только имя из вопроса и, при блокировке, отвечаем «адрес не найден».
 */
object Ip {
    const val PROTO_UDP = 17

    class V4(val buf: ByteArray, val length: Int) {
        val ihl = (buf[0].toInt() and 0x0F) * 4
        val protocol = buf[9].toInt() and 0xFF
        val srcPort get() = ((buf[ihl].toInt() and 0xFF) shl 8) or (buf[ihl + 1].toInt() and 0xFF)
        val dstPort get() = ((buf[ihl + 2].toInt() and 0xFF) shl 8) or (buf[ihl + 3].toInt() and 0xFF)
        val udpPayloadOffset get() = ihl + 8
        val udpPayloadLength get() = (((buf[ihl + 4].toInt() and 0xFF) shl 8) or (buf[ihl + 5].toInt() and 0xFF)) - 8

        fun srcAddr(): ByteArray = buf.copyOfRange(12, 16)
        fun dstAddr(): ByteArray = buf.copyOfRange(16, 20)
        val isUdp get() = protocol == PROTO_UDP && length >= ihl + 8
    }

    fun parse(buf: ByteArray, length: Int): V4? {
        if (length < 20) return null
        if ((buf[0].toInt() and 0xF0) != 0x40) return null // только IPv4
        return V4(buf, length)
    }

    /** Собрать ответный IPv4+UDP пакет: меняем местами адреса и порты, подставляем payload. */
    fun buildUdpResponse(req: V4, payload: ByteArray): ByteArray {
        val total = 20 + 8 + payload.size
        val out = ByteArray(total)
        out[0] = 0x45
        out[1] = 0
        out[2] = (total ushr 8).toByte(); out[3] = total.toByte()
        // id/flags/ttl
        out[8] = 64
        out[9] = PROTO_UDP.toByte()
        // src = прежний dst, dst = прежний src
        System.arraycopy(req.dstAddr(), 0, out, 12, 4)
        System.arraycopy(req.srcAddr(), 0, out, 16, 4)
        ipChecksum(out, 0, 20)
        // UDP
        val uoff = 20
        out[uoff] = (req.dstPort ushr 8).toByte(); out[uoff + 1] = req.dstPort.toByte()
        out[uoff + 2] = (req.srcPort ushr 8).toByte(); out[uoff + 3] = req.srcPort.toByte()
        val ulen = 8 + payload.size
        out[uoff + 4] = (ulen ushr 8).toByte(); out[uoff + 5] = ulen.toByte()
        System.arraycopy(payload, 0, out, uoff + 8, payload.size)
        udpChecksum(out, 12, 16, uoff, ulen)
        return out
    }

    private fun ipChecksum(b: ByteArray, off: Int, len: Int) {
        b[off + 10] = 0; b[off + 11] = 0
        var sum = 0L
        var i = off
        while (i < off + len) { sum += (((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)).toLong(); i += 2 }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        val c = sum.inv().toInt()
        b[off + 10] = (c ushr 8).toByte(); b[off + 11] = c.toByte()
    }

    private fun udpChecksum(b: ByteArray, srcOff: Int, dstOff: Int, uoff: Int, ulen: Int) {
        b[uoff + 6] = 0; b[uoff + 7] = 0
        var sum = 0L
        for (k in 0 until 4) sum += (((b[srcOff + k * 2].toInt() and 0xFF) shl 8) or (b[srcOff + k * 2 + 1].toInt() and 0xFF)).toLong()
        sum += PROTO_UDP.toLong()
        sum += ulen.toLong()
        var i = uoff
        while (i < uoff + ulen - 1) { sum += (((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)).toLong(); i += 2 }
        if ((ulen and 1) == 1) sum += ((b[uoff + ulen - 1].toInt() and 0xFF) shl 8).toLong()
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        var c = sum.inv().toInt() and 0xFFFF
        if (c == 0) c = 0xFFFF
        b[uoff + 6] = (c ushr 8).toByte(); b[uoff + 7] = c.toByte()
    }
}

object Dns {
    /** Прочитать доменное имя из первого вопроса DNS-пакета. */
    fun questionName(payload: ByteArray, off: Int, len: Int): String? {
        if (len < 12) return null
        val qd = ((payload[off + 4].toInt() and 0xFF) shl 8) or (payload[off + 5].toInt() and 0xFF)
        if (qd < 1) return null
        var p = off + 12
        val sb = StringBuilder()
        var guard = 0
        while (p < off + len && guard < 128) {
            val l = payload[p].toInt() and 0xFF
            if (l == 0) break
            if (l and 0xC0 != 0) return null // сжатие в вопросе не ожидается
            p++
            if (p + l > off + len) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(payload, p, l, Charsets.US_ASCII))
            p += l
            guard++
        }
        return sb.toString().ifEmpty { null }
    }

    /** Ответ «адрес не найден» (NXDOMAIN) на тот же вопрос. */
    fun nxdomain(payload: ByteArray, off: Int, len: Int): ByteArray {
        val qlen = len
        val out = ByteArray(qlen)
        System.arraycopy(payload, off, out, 0, qlen)
        // flags: QR=1, Opcode=0, AA=0, RD (копируем), RA=1, RCODE=3 (NXDOMAIN)
        out[2] = (0x80 or (out[2].toInt() and 0x01)).toByte()
        out[3] = 0x83.toByte()
        // ANCOUNT/NSCOUNT/ARCOUNT = 0
        out[6] = 0; out[7] = 0; out[8] = 0; out[9] = 0; out[10] = 0; out[11] = 0
        return out
    }
}
