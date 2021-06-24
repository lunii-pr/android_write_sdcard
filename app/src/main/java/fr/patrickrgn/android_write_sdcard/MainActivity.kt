package fr.patrickrgn.android_write_sdcard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            onButtonClick(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onButtonClick(view: View) {

        val permissionCheckWriteStorage: Int =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        if (permissionCheckWriteStorage != 0) {
            requestPermission()
        }


        Log.d("AppLog", "SDK: ${Build.VERSION.SDK_INT}")
        Log.d("AppLog", "extStorageState: ${Environment.getExternalStorageState()}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("AppLog", "isExternalStorageManager: ${Environment.isExternalStorageManager()}")
        }
        Log.d(
            "AppLog",
            "getExternalStorageDirectory: ${Environment.getExternalStorageDirectory().path}"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(
                "AppLog",
                "Environment.isExternalStorageLegacy(): ${Environment.isExternalStorageLegacy()}"
            )
        }




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sm = this.getSystemService(STORAGE_SERVICE) as StorageManager
            val storages: List<StorageVolume> = sm.storageVolumes
            val sdcard: StorageVolume = storages.last()
            Log.d("AppLog", sdcard.state)
            val intent: Intent = sdcard.createOpenDocumentTreeIntent()
            startActivityForResult(intent, 100)


        }

        Log.d("AppLog", "permissionCheckStorage: $permissionCheckWriteStorage")
        Log.d("AppLog", "isExternalStorageAvailable: ${isExternalStorageAvailable()}")
        Log.d("AppLog", "isExternalStorageReadOnly: ${isExternalStorageReadOnly()}")

        val f = File(getExternalSdCard()?.absolutePath, "test.txt")
//        val f = File("/storage/emulated/", "test.txt")
        Log.d("AppLog", f.absolutePath)
        val fos: FileOutputStream?
        try {
            fos = FileOutputStream(f)
            fos.write("contenu du fichier".toByteArray())
            fos.close()
            Log.d("AppLog", "write file ok")
        } catch (e: Exception) {
            e.message?.let { Log.d("AppLog", it) }
            Log.d("AppLog", "write file ko")
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            0
        )
    }

    private fun listStorages(): List<StorageVolume> {
        val sm = this.getSystemService(STORAGE_SERVICE) as StorageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val i: List<StorageVolume> = sm.storageVolumes

            i
        } else {
            ArrayList()
        }
    }

    private fun getExternalSdCard(): File? {
        var externalStorage: File? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val storage = File("/storage")
            if (storage.exists()) {
                val files = storage.listFiles()
                for (file in files) {
                    if (file.exists()) {
                        try {
                            if (Environment.isExternalStorageRemovable(file)) {
                                externalStorage = file
                                break
                            }
                        } catch (e: java.lang.Exception) {
                            Log.e("TAG", e.toString())
                        }
                    }
                }
            }
        } else {
            // do one of many old methods
            // I believe Doomsknight's method is the best option here
        }
        return externalStorage
    }

    private fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }

    private fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }
}