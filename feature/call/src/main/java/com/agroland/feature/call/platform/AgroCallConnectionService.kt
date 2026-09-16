package com.agroland.feature.call.platform

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import com.agroland.feature.call.domain.VoiceCallManager

/**
 * Self-managed Telecom ConnectionService (spec §13): жүйе қоңырауды
 * біледі (do-not-disturb, аудио режимі, құлып экраны). UI біздікі —
 * Connection PROPERTY_SELF_MANAGED, жүйелік dialer шықпайды.
 *
 * Answer/reject/disconnect VoiceCallManager.systemCallActions көпіріне
 * барады (manager start() кезінде бекітіледі). Telecom қолжетімсіз
 * болса (құрылғыға тәуелді) — IncomingCallNotifier try/catch арқылы
 * деградациялайды, бұл сервис шақырылмайды да.
 */
class AgroCallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = buildConnection()

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = buildConnection()

    private fun buildConnection(): Connection =
        object : Connection() {
            init {
                setConnectionProperties(PROPERTY_SELF_MANAGED)
                audioModeIsVoip = true
                connectionCapabilities = CAPABILITY_SUPPORT_HOLD
            }

            override fun onAnswer() {
                VoiceCallManager.systemCallActions?.onSystemAnswer()
            }

            override fun onReject() {
                VoiceCallManager.systemCallActions?.onSystemReject()
            }

            override fun onDisconnect() {
                VoiceCallManager.systemCallActions?.onSystemDisconnect()
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                setActiveConnection(null)
            }
        }.also { setActiveConnection(it) }

    companion object {
        @Volatile
        private var activeConnection: Connection? = null

        private fun setActiveConnection(connection: Connection?) {
            activeConnection = connection
        }

        /** Қоңырау біткенде (cancel/teardown) Telecom күйін жабу. */
        fun reportSystemEnded() {
            try {
                activeConnection?.setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
            } catch (_: Throwable) {
                // Жүйе қосылысты өзі тазартқан — ештеңе жасамаймыз.
            }
            activeConnection = null
        }
    }
}