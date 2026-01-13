package com.example.myapplication

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Parcelable
import java.nio.charset.Charset

class NFCManager(private val activity: Activity) {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter != null) {
            val intent = Intent(activity, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            
            val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            val ndefDetected = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            val techDetected = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            intentFilters = arrayOf(tagDetected, ndefDetected, techDetected)
        }
    }

    fun isNFCAvailable(): Boolean {
        return nfcAdapter != null && nfcAdapter!!.isEnabled
    }

    fun enableNFCForegroundDispatch() {
        if (nfcAdapter != null) {
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, null)
        }
    }

    fun disableNFCForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleNfcIntent(intent: Intent, onNfcDataReceived: (String) -> Unit) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val nfcData = readNFCData(tag)
                if (nfcData.isNotEmpty()) {
                    onNfcDataReceived(nfcData)
                }
            }
        }
    }

    private fun readNFCData(tag: Tag): String {
        val ndefTag = Ndef.get(tag)
        if (ndefTag != null) {
            try {
                ndefTag.connect()
                val ndefMessage = ndefTag.ndefMessage
                ndefTag.close()
                
                if (ndefMessage != null) {
                    return extractNdefMessage(ndefMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 如果无法通过Ndef读取，尝试其他方式
        val payload = tag.id
        return String(payload, Charset.forName("UTF-8"))
    }

    private fun extractNdefMessage(ndefMessage: NdefMessage?): String {
        if (ndefMessage == null) return ""
        
        val records = ndefMessage.records
        for (record in records) {
            try {
                val payload = record.payload
                // 简单处理文本记录
                if (payload.isNotEmpty() && payload[0].toInt() == 0x01) { // 检查是否为文本记录
                    val languageCodeLength = payload[0].toInt() and 0x3F
                    val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charsets.UTF_8)
                    return text
                } else {
                    return String(payload, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "NFC Data Read"
    }
}