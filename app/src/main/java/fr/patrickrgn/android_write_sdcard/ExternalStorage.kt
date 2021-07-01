package fr.patrickrgn.android_write_sdcard

import android.os.Environment
import java.io.File
import java.util.*


object ExternalStorage {
    const val SD_CARD = "sdCard"
    const val EXTERNAL_SD_CARD = "externalSdCard"

    /**
     * @return True if the external storage is available. False otherwise.
     */
    val isAvailable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }
    val sdCardPath: String
        get() = Environment.getExternalStorageDirectory().path + "/"

    /**
     * @return True if the external storage is writable. False otherwise.
     */
    val isWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }// don't add the default mount path
    // it's already in the list.
    /**
     * @return A map of all storage locations available
     */
    val allStorageLocations: Map<String, File>
        get() {
            val map: MutableMap<String, File> = HashMap(10)
            val mMounts: MutableList<String> = ArrayList(10)
            val mVold: MutableList<String> = ArrayList(10)
//            mMounts.add("/mnt/sdcard")
//            mVold.add("/mnt/sdcard")
            try {
                val mountFile = File("/proc/mounts")
                if (mountFile.exists()) {
                    val scanner = Scanner(mountFile)
                    while (scanner.hasNext()) {
                        val line = scanner.nextLine()
                        if (line.startsWith("/dev/block/vold/")) {
                            val lineElements = line.split(" ").toTypedArray()
                            val element = lineElements[1]

                            // don't add the default mount path
                            // it's already in the list.
                            mMounts.add(element)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val voldFile = File("/system/etc/vold.fstab")
                if (voldFile.exists()) {
                    val scanner = Scanner(voldFile)
                    while (scanner.hasNext()) {
                        val line = scanner.nextLine()
                        if (line.startsWith("dev_mount")) {
                            val lineElements = line.split(" ").toTypedArray()
                            var element = lineElements[2]
                            if (element.contains(":")) element =
                                element.substring(0, element.indexOf(":"))
                            mVold.add(element)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            var i = 0
            while (i < mMounts.size) {
                val mount = mMounts[i]
                if (!mVold.contains(mount)) mMounts.removeAt(i--)
                i++
            }
            mVold.clear()
            val mountHash: MutableList<String> = ArrayList(10)
            for (mount in mMounts) {
                val root = File(mount)
                if (root.exists() && root.isDirectory && root.canWrite()) {
                    val list = root.listFiles()
                    var hash = "["
                    if (list != null) {
                        for (f in list) {
                            hash += f.name.hashCode().toString() + ":" + f.length() + ", "
                        }
                    }
                    hash += "]"
                    if (!mountHash.contains(hash)) {
                        var key = SD_CARD + "_" + map.size
                        if (map.size == 0) {
                            key = SD_CARD
                        } else if (map.size == 1) {
                            key = EXTERNAL_SD_CARD
                        }
                        mountHash.add(hash)
                        map[key] = root
                    }
                }
            }
            mMounts.clear()
            if (map.isEmpty()) {
                map[SD_CARD] = Environment.getExternalStorageDirectory()
            }
            return map
        }
}