package com.augray.visionx

object Vxhelper {
    const val IS_BACK_CAMERA_ENABLED = "isBackCameraEnabled"
    const val IS_FACE_OVERLAY_ENABLED = "isFaceOverlayEnabled"
    const val IS_APPLYING_CROPPER ="isApplyingCropping"
    const val CAMERA_PERMISSION_REQUEST_CODE = 900
    const val STORAGE_PERMISSION_REQUEST_CODE = 901
    const val PICK_IMAGE_REQUEST = 910

    const val IMMERSIVE_FLAG_TIMEOUT = 500L

    const val GALLERY = "gallery"
    const val CAMERA = "camera"
    const val IMAGE_REQUIRED_WIDTH = 640
    const val IMAGE_REQUIRED_HEIGHT = 768

    const val EULER_ANGLE_POSITIVE_CONSTANT = 15
    const val EULERY_ANGLE_POSITIVE_CONSTANT = 12
    const val EULER_ANGLE_NEGATIVE_CONSTANT = -15
    const val EULERY_ANGLE_NEGATIVE_CONSTANT = -12
    const val IMAGE_DIRECTORY = "image_directory"
    const val IMAGE_NAME = "image_name"
    const val DIRECTORY_NAME_REQUIRED = "Camervx requires name of the directory to store image. Use setImageDirectory(directoryName)"
    const val FILE_NAME_REQUIRED = "Camervx requires name of the file to store image. Use setFileName(directoryName)"
    const val MISSING_ACTIVITY_REFRENCE = "Activity Reference Missing in Cameravx"
}