package fr.patrickrgn.android_write_sdcard

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileStreamFactory
import com.github.mjdev.libaums.partition.Partition
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {


    private val actionUsbPermission = "fr.patrickrgn.android_write_sdcard.USB_PERMISSION"
    private var text: String = "Starting...\n"
    private lateinit var editText: EditText
    private lateinit var mUsbManager: UsbManager
    private lateinit var mPermissionIntent: PendingIntent

    private fun fSize(sizeInByte: Long): String {
        return when {
            sizeInByte < 1024 -> String.format(
                "%s",
                sizeInByte
            )
            sizeInByte < 1024 * 1024 -> String.format(
                Locale.FRANCE,
                "%.2fKB",
                sizeInByte / 1024.0
            )
            sizeInByte < 1024 * 1024 * 1024 -> String.format(
                Locale.FRANCE,
                "%.2fMB",
                sizeInByte / 1024.0 / 1024
            )
            else -> String.format(Locale.FRANCE, "%.2fGB", sizeInByte / 1024.0 / 1024 / 1024)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editTextTextMultiLine)
        val checkPermissionButton: Button = findViewById(R.id.checkPermissionButton)
        checkPermissionButton.setOnClickListener { checkPermissions() }

        val writeFileButton: Button = findViewById(R.id.writeFileButton)
        writeFileButton.setOnClickListener { writeFile() }
    }


    fun writeText(value: String) {
        text += "$value\n"
        editText.setText(text)
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            writeText("onReceive: $intent")
            val action: String = intent?.action ?: return

            when (action) {
                actionUsbPermission ->//User Authorized Broadcast
                    synchronized(this) {
                        if (intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED,
                                false
                            )
                        ) { //Allow permission to apply
//                            test();
                        } else {
                            writeText("User is not authorized, access to USB device failed")
                        }
                    }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    text = "USB device plugin"
                    editText.setText(text)
                }


                UsbManager.ACTION_USB_DEVICE_DETACHED ->//USB device unplugs the broadcast
                    writeText("USB device unplugged")
            }

        }
    }


    private fun checkPermissions() {

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent =
            PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission), 0)

        val intentFilter = IntentFilter()
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        intentFilter.addAction(actionUsbPermission)
        registerReceiver(mUsbReceiver, intentFilter)


        //Read and write permissions
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 111
        )

        val storageDevices = UsbMassStorageDevice.getMassStorageDevices(this)

        for (storageDevice in storageDevices) {
            if (!mUsbManager.hasPermission(storageDevice.usbDevice)) {
                mUsbManager.requestPermission(storageDevice.usbDevice, mPermissionIntent)
            }
        }
    }


    private fun writeFile() {
        try {
            val storageDevices = UsbMassStorageDevice.getMassStorageDevices(this)

            for (storageDevice in storageDevices) {
                writeText("-------------")
                storageDevice.init()

                val partitions: List<Partition> = storageDevice.partitions
                if (partitions.isEmpty()) {
                    writeText("Error: Failed to read partition")
                    return
                }

                val fileSystem: FileSystem = partitions[0].fileSystem
                writeText("Volume Label: " + fileSystem.volumeLabel)
                writeText("Capacity: " + fSize(fileSystem.capacity))
                val root: UsbFile = fileSystem.rootDirectory

                // create a new file
                val newFile: UsbFile =
                    root.createFile("hello_" + System.currentTimeMillis() + ".txt")
                writeText("New file: " + newFile.name)

                // write the file
                // OutputStream os = new UsbFileOutputStream(newFile);
                val os: OutputStream =
                    UsbFileStreamFactory.createBufferedOutputStream(newFile, fileSystem)
                os.write(("hi_" + System.currentTimeMillis()).toByteArray())
                os.close()
                writeText("write file: " + newFile.name)

                storageDevice.close()
            }

        } catch (e: Exception) {
            writeText("Error: $e")
        }
    }

}