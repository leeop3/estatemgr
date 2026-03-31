package com.estate.manager.rns

/**
 * Kotlin interface that mirrors the callback methods called by rns_backend.py.
 *
 * Python side calls:
 *   kotlin_callback.onNewMessage(sender, content, timestampMs, isImage, isOutgoing, msgHash)
 *   kotlin_callback.onMessageDelivered(msgHash)
 *   kotlin_callback.onAnnounceReceived(destHash, displayName)
 *
 * This interface is passed as `callback_obj` to start_rns().
 * Chaquopy transparently proxies it to Python.
 */
interface RnsCallback {
    /**
     * Fired for every incoming LXMF message.
     * @param senderHash  hex dest hash of the sender (from lxm.source_hash)
     * @param content     decoded text payload OR local file path when isImage=true
     * @param timestampMs Unix epoch milliseconds
     * @param isImage     true when content starts with "IMG:" (Python saves file, passes path)
     * @param isOutgoing  always false for incoming; included for symmetry with sent echo
     * @param msgHash     hex hash of the LXMessage (used for ACK matching)
     */
    fun onNewMessage(
        senderHash: String,
        content: String,
        timestampMs: Long,
        isImage: Boolean,
        isOutgoing: Boolean,
        msgHash: String
    )

    /**
     * Fired when a sent message is acknowledged.
     * @param msgHash  hex hash of the delivered LXMessage (from ACK:<hash> prefix)
     */
    fun onMessageDelivered(msgHash: String)

    /**
     * Fired when a peer announce is received via RNS transport.
     * @param destHash    hex destination hash of the announcing node
     * @param displayName app_data decoded as UTF-8 (convention: "ROLE:Name")
     */
    fun onAnnounceReceived(destHash: String, displayName: String)
}
