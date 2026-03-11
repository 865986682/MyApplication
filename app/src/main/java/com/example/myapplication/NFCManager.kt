package com.example.myapplication

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Handler
import android.os.Looper
import java.nio.charset.Charset

class NFCManager(private val activity: Activity) {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var nfcTimeoutHandler: Handler? = null
    private var nfcTimeoutRunnable: Runnable? = null

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

    fun enableNFCForegroundDispatchWithTimeout(timeoutMs: Long = 5000) {
        if (nfcAdapter != null) {
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, null)
            
            // 清除之前的超时任务
            nfcTimeoutHandler?.removeCallbacks(nfcTimeoutRunnable ?: return)
            
            // 创建新的超时任务
            nfcTimeoutRunnable = Runnable {
                disableNFCForegroundDispatch()
            }
            nfcTimeoutHandler = Handler(Looper.getMainLooper())
            nfcTimeoutHandler?.postDelayed(nfcTimeoutRunnable!!, timeoutMs)
        }
    }

    fun disableNFCForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            // 取消超时任务
            nfcTimeoutRunnable?.let {
                nfcTimeoutHandler?.removeCallbacks(it)
            }
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

    /**
     * 从 NDEF 消息中提取文本数据
     * @param ndefMessage NDEF 消息
     * @return 提取的文本内容
     */
    private fun extractNdefMessage(ndefMessage: NdefMessage?): String {
       if (ndefMessage == null) return ""
        
        val records = ndefMessage.records
        for (record in records) {
            try {
                val payload = record.payload
                // 检查是否为文本记录（TNF_WELL_KNOWN + RTD_TEXT）
               if (record.tnf == NdefRecord.TNF_WELL_KNOWN && 
                    record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    // 正确解析 NDEF 文本记录格式
                    // payload 结构：[状态字节] + [语言代码] + [实际文本]
                    val statusByte = payload[0].toInt() and 0xFF
                    val languageCodeLength = statusByte and 0x3F  // 低 6 位是语言代码长度
                    
                    // 跳过状态字节和语言代码，直接读取实际文本
                    val textStartIndex = languageCodeLength + 1
                   if (textStartIndex < payload.size) {
                        val text = String(payload, textStartIndex, payload.size - textStartIndex, Charsets.UTF_8)
                        return text
                    }
                } else {
                    // 非文本记录，直接返回 payload 内容
                    return String(payload, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "NFC Data Read"
    }

    /**
     * 写入数据到 NFC 标签
     * @param tag NFC 标签对象
     * @param data 要写入的数据
     * @return 写入结果
     */
    fun writeNFCData(tag: Tag, data: String): Boolean {
        val ndefTag = Ndef.get(tag)
        
        return if (ndefTag != null) {
            // 标签已经格式化，直接写入
            writeNdefData(ndefTag, data)
        } else {
            // 标签未格式化，需要先格式化
            formatAndWriteNdefData(tag, data)
        }
    }

    /**
     * 写入 NDEF 数据到已格式化的标签
     */
    private fun writeNdefData(ndefTag: Ndef, data: String): Boolean {
        return try {
            ndefTag.connect()
            
            // 检查是否可写
            if (!ndefTag.isWritable) {
                ndefTag.close()
                return false
            }
            
            // 创建 NDEF 消息
            val ndefMessage = createNdefMessage(data)
            // 检查容量是否足够
            if (getNdefMessageSize(ndefMessage) > ndefTag.maxSize) {
                ndefTag.close()
                return false
            }
            
            // 写入数据
            ndefTag.writeNdefMessage(ndefMessage)
            ndefTag.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 格式化并写入 NDEF 数据到未格式化的标签
     */
    private fun formatAndWriteNdefData(tag: Tag, data: String): Boolean {
        val ndefFormatable = android.nfc.tech.NdefFormatable.get(tag)
        
        return try {
            ndefFormatable?.connect()
            
            // 创建 NDEF 消息
            val ndefMessage = createNdefMessage(data)
            
            // 格式化并写入
            ndefFormatable?.format(ndefMessage)
            ndefFormatable?.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建 NDEF 消息
     * @param text 要写入的文本数据
     */
    private fun createNdefMessage(text: String): NdefMessage {
        // 创建文本记录
        val record = createTextRecord(text)
        return NdefMessage(arrayOf(record))
    }

    /**
     * 计算 NDEF 消息的大小（字节）
     * @param message NDEF 消息
     * @return 消息大小（字节数）
     */
    private fun getNdefMessageSize(message: NdefMessage): Int {
        return message.records.sumOf { record ->
            // 计算每个记录的大小：类型长度 + 负载长度 + ID 长度 + 固定开销
            record.type.size + record.payload.size + (record.id?.size ?: 0)
        }
    }

    /**
     * 创建 NDEF 文本记录
     * @param text 文本内容
     */
    private fun createTextRecord(text: String): NdefRecord {
        val languageCode = "en" // 语言代码
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val languageBytes = languageCode.toByteArray(Charset.forName("US-ASCII"))
        
        // 构建 payload: 状态字节 + 语言代码 + 文本数据
        val payload = ByteArray(1 + languageBytes.size + textBytes.size)
        
        // 状态字节：高 2 位为 0，第 5 位为 0(UTF-8)，低 6 位为语言代码长度
        payload[0] = ((0x00 shl 7) or (0x00 shl 6) or (0x00 shl 3) or languageBytes.size).toByte()
        
        // 复制语言代码
        System.arraycopy(languageBytes, 0, payload, 1, languageBytes.size)
        
        // 复制文本数据
        System.arraycopy(textBytes, 0, payload, 1 + languageBytes.size, textBytes.size)
        
        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0), // ID 字段为空
            payload
        )
    }
}