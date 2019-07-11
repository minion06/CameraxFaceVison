package com.augray.visionx

import android.app.Activity
import android.content.Intent
import com.augray.visionx.Vxhelper.DIRECTORY_NAME_REQUIRED
import com.augray.visionx.Vxhelper.FILE_NAME_REQUIRED
import com.augray.visionx.Vxhelper.IMAGE_DIRECTORY
import com.augray.visionx.Vxhelper.IMAGE_NAME
import com.augray.visionx.Vxhelper.IS_BACK_CAMERA_ENABLED
import com.augray.visionx.Vxhelper.IS_FACE_OVERLAY_ENABLED
import com.augray.visionx.Vxhelper.MISSING_ACTIVITY_REFRENCE
import com.augray.visionx.ui.CameravxActivity

class Cameravx(var activity: Activity, var isFaceOverlay:Boolean,
                var isBackCameraEnabled:Boolean,var directoryName:String?,var fileName:String?) {


    data class Builder(var activity: Activity?=null,var isFaceOverlay:Boolean = false,var isBackCameraEnabled:Boolean = true,var directoryName:String?=null,var fileName:String?=null){
        /**
         * Enable or disable Face detection true to set detection / false to remove detection
         *
         * <p>By Default face detection is disabled.
         *
         * @param isFaceOverlay Enables face detection if set to true.
         * @return the current Builder.
         */
        fun setFaceOverlay(isFaceOverlay: Boolean) = apply { this.isFaceOverlay =isFaceOverlay }
        /**
         * Enable or disable back camera based on the value passed
         *
         * <p>By Default Back Camera is enabled.
         *
         * @param isBackCameraEnabled Allow back camera or not.
         * @return the current Builder.
         */
        fun setCameraBackEnabled(isBackCameraEnabled: Boolean) = apply { this.isBackCameraEnabled = isBackCameraEnabled}
        /**
         * The Activity that launches Cameravx
         *
         * <p>Throws exception on null or not defined.
         *
         * @param activity Current activity launching Cameravx.
         * @return the current Builder.
         */

        fun setActivity(activity: Activity) = apply { this.activity = activity }
        /**
         * The Directory in which capture file will be stored
         *
         * <p>Throws exception on null or not defined.
         *
         * @param dirName Name of image directory. Make sure it is internal directory
         * @return the current Builder.
         */
        fun setImageDirectory(dirName:String) = apply { this.directoryName = dirName }
        /**
         * The File in which captured image will be stored
         *
         * <p>Throws exception on null or not defined.
         *
         * @param dirName Name of image File. Make sure it is internal directory
         * @return the current Builder.
         */
        fun setFileName(fileName:String) = apply { this.fileName = fileName }
        fun build() = Cameravx(activity?:throw NullPointerException(MISSING_ACTIVITY_REFRENCE),isFaceOverlay,isBackCameraEnabled,directoryName?:throw  NullPointerException(DIRECTORY_NAME_REQUIRED),fileName?: throw NullPointerException(FILE_NAME_REQUIRED)).launch()
    }

    private fun launch() {
        val intent = Intent(this.activity, CameravxActivity::class.java)
        intent.putExtra(IS_BACK_CAMERA_ENABLED,this.isBackCameraEnabled)
        intent.putExtra(IS_FACE_OVERLAY_ENABLED,this.isFaceOverlay)
        intent.putExtra(IMAGE_DIRECTORY,this.directoryName)
        intent.putExtra(IMAGE_NAME,this.fileName)
        activity.startActivityForResult(intent, CAMERAVX_LAUNCH_REQUEST)
    }
    companion object{
        const val IMAGE_SOURCE = "image_source"
        const val IMAGE_PATH ="image_path"
        const val CAMERAVX_LAUNCH_REQUEST = 909
    }
}