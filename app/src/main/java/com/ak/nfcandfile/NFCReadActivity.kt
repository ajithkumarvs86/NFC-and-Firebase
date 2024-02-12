package com.ak.nfcandfile

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

class NFCReadActivity : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var intentFilter: IntentFilter? = null
    private var pendingIntent: PendingIntent? = null
    private val ACTION_NONE = 0
    private val ACTION_READ = 1
    private var intentFiltersArray: Array<IntentFilter?>? = null
    val MIME_TEXT_PLAIN = "text/plain"
    lateinit var readResult:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfcactivity)

        readResult = findViewById(R.id.txt_read_result)
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

    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter!!.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, arrayOf(arrayOf("android.nfc.tech.Ndef")))

    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter!!.disableForegroundDispatch(this)
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "android.nfc.action.TECH_DISCOVERED") {
            getData(intent)
        }
    }

    private fun getData(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val type = intent.type
            if (MIME_TEXT_PLAIN == type) {
                val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                }
               val result = processText(tag)
                readResult.text = "Data on Tag :\n $result"
                Log.e("Result", result.toString())
            } else {
                Log.e("Reader", "Wrong mime type: $type")
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {

            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            val result = processText(tag)
            readResult.text = "Data on Tag :\n $result"
            Log.e("Result", result.toString())
        }
    }

    fun processText(tag: Tag?):String? {

        val ndef = Ndef.get(tag)
        val records = ndef.cachedNdefMessage.records
        for (ndefRecord in records) {
            if (ndefRecord.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(
                    ndefRecord.type,
                    NdefRecord.RTD_TEXT
                )
            ) {
                try {
                    return readText(ndefRecord)
                } catch (e: UnsupportedEncodingException) {
                    Log.e("Native Reader Activity", "Unsupported Encoding", e)
                }
            }
        }
        return null
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readText(record: NdefRecord): String? {
        val payload = record.payload
        // Get the Text Encoding
        val textEncoding = if (payload[0].toInt() and 128 == 0) "UTF-8" else "UTF-16"

        // Get the Language Code
        val languageCodeLength: Int = payload[0].toInt() and 51
        return String(
            payload,
            languageCodeLength + 1,
            payload.size - languageCodeLength - 1,
            Charset.forName(textEncoding)
        )
    }

}