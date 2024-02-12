package com.ak.nfcandfile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        if (result.resultCode == Activity.RESULT_OK) {
            // The user has picked a file
            val uri = result.data?.data
            if (uri != null) {
                uploadFile(uri)
                Log.e("Ak",uri.path.toString())
            }
        }
    }

    lateinit var lbl_download:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn_read = findViewById<Button>(R.id.btn_read)
        val but_write = findViewById<Button>(R.id.but_write)
        val but_upload = findViewById<Button>(R.id.but_upload)
        val but_download = findViewById<Button>(R.id.but_download)
        lbl_download = findViewById(R.id.lbl_download)
        val but_view_pdf = findViewById<Button>(R.id.but_view_pdf)

        btn_read.setOnClickListener {
            startActivity(Intent(this,NFCReadActivity::class.java))
        }
        but_write.setOnClickListener {
            startActivity(Intent(this,NFCWriteActivity::class.java))
        }
        but_upload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
            }
            filePicker.launch(intent)
        }
        but_download.setOnClickListener {
            val downloadUrl = "https://github.github.com/training-kit/downloads/github-git-cheat-sheet.pdf"
            downloadManagerCSV(downloadUrl)

                }

        but_view_pdf.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.setDataAndType(Uri.parse("https://www.trelleborg.com/marine-and-infrastructure/~/media/marine-systems/resources/guides-and-design-manual/downloads/safepilot_191_user_guide.pdf"), "application/pdf")
            val chooser = Intent.createChooser(browserIntent, "")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK // optional
            startActivity(chooser)
        }
    }


    private fun uploadFile(uri: Uri?) {
        val pdfRef = storageRef.child("pdf/${uri!!.lastPathSegment}")
        val uploadTask = pdfRef.putFile(uri)
        lbl_download.visibility = View.VISIBLE
        Log.e("AK","Upoding...")
        uploadTask.addOnSuccessListener {
            lbl_download.text =""
            lbl_download.visibility = View.GONE
            Toast.makeText(this, "File Uploaded Successfully.", Toast.LENGTH_SHORT).show()
            Log.e("AK","Upload Success")
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "File Upload Failed", Toast.LENGTH_SHORT).show()
            Log.e("AK","Error : {$exception}")
        }.addOnProgressListener { listener->
            lbl_download.text = "Uploading ${bytesIntoHumanReadable(listener.totalByteCount)} / ${bytesIntoHumanReadable(listener.bytesTransferred)}"
        }
    }


    private fun bytesIntoHumanReadable(bytes: Long): String? {
        val kilobyte: Long = 1024
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024
        val terabyte = gigabyte * 1024
        return if (bytes in 0 until kilobyte) {
            "$bytes B"
        } else if (bytes in kilobyte until megabyte) {
            (bytes / kilobyte).toString() + " KB"
        } else if (bytes in megabyte until gigabyte) {
            (bytes / megabyte).toString() + " MB"
        } else if (bytes in gigabyte until terabyte) {
            (bytes / gigabyte).toString() + " GB"
        } else if (bytes >= terabyte) {
            (bytes / terabyte).toString() + " TB"
        } else {
            "$bytes Bytes"
        }
    }

    private fun downloadManagerCSV(url:String){
        lateinit var onComplete: BroadcastReceiver
        val bar = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Downloading...")
            isIndeterminate = true
            setCanceledOnTouchOutside(false)
            show()
        }

        try {
            val filename = url.toUri().lastPathSegment
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("PDF Download")
            request.setDescription("Downloading sample PDF")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE)
            // Start the download service
            val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = downloadManager.enqueue(request)

            //Broadcast service
            onComplete = object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context, intent: Intent) {

                    val query = DownloadManager.Query()
                    query.setFilterById(id)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        // Status of the downloading process
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                Toast.makeText(context,"Download completed.\n" +
                                        "FilePath : "+Environment.DIRECTORY_DOWNLOADS+"/$filename", Toast.LENGTH_LONG).show()
                                context.unregisterReceiver(onComplete)
                                bar.dismiss()
                            }
                            DownloadManager.STATUS_FAILED -> {
                                bar.dismiss()
                                Toast.makeText(this@MainActivity, "Unable to download file !", Toast.LENGTH_SHORT).show()
                                context.unregisterReceiver(onComplete)
                            }
                        }
                    }
                }
            }
            // Register broadcast service
            // with Intent filter method of Download-Complete
            val intent = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            intent.addAction("Success")
            this.registerReceiver(onComplete, intent)
        }
        catch (e : Exception){
            bar.dismiss()
            Toast.makeText(this, "Unable to download file !", Toast.LENGTH_SHORT).show()
            Log.d("excep",e.toString())
        }

    }
}

