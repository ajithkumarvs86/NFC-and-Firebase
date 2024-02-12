package com.ak.nfcandfile

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.IOException

class NFCWriteActivity : AppCompatActivity() {

    lateinit var txt_input:EditText
    lateinit var btn_write_tag:Button
    lateinit var txt_tap_write:TextView

    private var mNfcAdapter: NfcAdapter? = null
    private var intentFilter: IntentFilter? = null
    private var pendingIntent: PendingIntent? = null
    private var tag: Tag? = null
    private val ACTION_NONE = 0
    private val ACTION_READ = 1
    private var intentFiltersArray: Array<IntentFilter?>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfcwrite)

        txt_input = findViewById(R.id.txt_input)
        btn_write_tag = findViewById(R.id.btn_write_tag)
        txt_tap_write = findViewById(R.id.txt_tap_write)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        intentFilter = IntentFilter("android.nfc.action.TECH_DISCOVERED")
        val intentFiltersArr = arrayOfNulls<IntentFilter>(ACTION_READ)
        intentFiltersArr[ACTION_NONE] = intentFilter
        intentFiltersArray = intentFiltersArr
        intent = Intent(this, this.javaClass)
        pendingIntent = PendingIntent.getActivity(this, ACTION_NONE, intent!!.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        btn_write_tag.setOnClickListener {
            txt_tap_write.visibility = View.VISIBLE
            if (txt_input.hasFocus()){
                txt_input.clearFocus()
            }
            mNfcAdapter!!.enableForegroundDispatch(
                this,
                pendingIntent,
                intentFiltersArray,
                arrayOf(arrayOf("android.nfc.tech.Ndef"))
            )
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "android.nfc.action.TECH_DISCOVERED") {
             tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                val ndefMessage = createTextMessage(txt_input.text.trim().toString())
                if (writeTag(tag, ndefMessage)) {
                    txt_tap_write.visibility = View.GONE
                    txt_input.text.clear()
                    Toast.makeText(this, "Data Written on Tag Successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("NFC_WRITE", "Content write into Tag")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter!!.disableForegroundDispatch(this)
    }

    fun createTextMessage(content: String): NdefMessage? {
        try {
            val lang = "en".toByteArray(charset("UTF-8"))
            val text = content.toByteArray(charset("UTF-8"))
            val langSize = lang.size
            val textLength = text.size
            val payload = ByteArrayOutputStream(1 + langSize + textLength)
            payload.write((langSize and 0x1F).toByte().toInt())
            payload.write(lang, 0, langSize)
            payload.write(text, 0, textLength)
            val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload.toByteArray())
            return NdefMessage(arrayOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun writeTag(tag: Tag?, message: NdefMessage?): Boolean {
        if (tag != null) {
            var ndefTag: Ndef? = null
            try {
                ndefTag = Ndef.get(tag)
                if (ndefTag == null) {
                    // Let's try to format the Tag in NDEF
                    val nForm = NdefFormatable.get(tag)
                    if (nForm != null) {
                        nForm.connect()
                        nForm.format(message)
                        nForm.close()
                        return true
                    }
                } else {
                    ndefTag.close()
                    ndefTag.connect()
                    ndefTag.writeNdefMessage(message)
                    ndefTag.close()
                    return true
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                if (ndefTag == null) {
                    try {
                        ndefTag?.close()
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    }
                }
                return false
            }
        }
        return false
    }

}