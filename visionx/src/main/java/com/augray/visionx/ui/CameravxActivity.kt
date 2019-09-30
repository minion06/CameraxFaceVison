package com.augray.visionx.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.*
import android.view.TextureView
import android.view.View
import android.view.View.GONE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.augray.visionx.AutoFitPreviewBuilder
import com.augray.visionx.Cameravx.Companion.IMAGE_PATH
import com.augray.visionx.Cameravx.Companion.IMAGE_SOURCE
import com.augray.visionx.FLAGS_FULLSCREEN
import com.augray.visionx.R
import com.augray.visionx.Vxhelper.CAMERA
import com.augray.visionx.Vxhelper.CAMERA_PERMISSION_REQUEST_CODE
import com.augray.visionx.Vxhelper.EULER_ANGLE_NEGATIVE_CONSTANT
import com.augray.visionx.Vxhelper.EULER_ANGLE_POSITIVE_CONSTANT
import com.augray.visionx.Vxhelper.GALLERY
import com.augray.visionx.Vxhelper.IMAGE_DIRECTORY
import com.augray.visionx.Vxhelper.IMAGE_NAME
import com.augray.visionx.Vxhelper.IMAGE_REQUIRED_HEIGHT
import com.augray.visionx.Vxhelper.IMAGE_REQUIRED_WIDTH
import com.augray.visionx.Vxhelper.IMMERSIVE_FLAG_TIMEOUT
import com.augray.visionx.Vxhelper.IS_APPLYING_CROPPER
import com.augray.visionx.Vxhelper.IS_BACK_CAMERA_ENABLED
import com.augray.visionx.Vxhelper.IS_FACE_OVERLAY_ENABLED
import com.augray.visionx.Vxhelper.PICK_IMAGE_REQUEST
import com.augray.visionx.Vxhelper.STORAGE_PERMISSION_REQUEST_CODE
import com.augray.visionx.bind
import com.augray.visionx.cropper.CropImage
import com.augray.visionx.cropper.CropImageView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class CameravxActivity: AppCompatActivity(), LifecycleOwner {

    private val APP_TAG: String? ="visionx"
    private var mIsFrontFacing = true
    private val faceOverLayImage by bind<ImageView>(R.id.faceGuideOverlay)
    private var fileUserPhoto: File? = null
    private var storageMode = 0
    private var isFaceOverlay: Boolean = false
    private var disableBackForCamera: Boolean = false
    private var isRectangleCropNeeded: Boolean = false
    private var isNavigateBack: Boolean = true
    private var isNavigationType: String? = null
    private val imgBtnCameraBack by bind<ImageButton>(R.id.imgBtnCameraBack)
    private val imgBtnTakePicture by bind<ImageButton>(R.id.imgBtnTakePicture)
    private val imgBtnCameraInfo by bind<ImageButton>(R.id.imgBtnCameraInfo)
    private val viewFinder by bind<TextureView>(R.id.viewFinder)
    private val flipButton by bind<ImageButton>(R.id.flipButton)
    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.FRONT
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val topLayout by bind<RelativeLayout>(R.id.topLayout)

    private var imageDirectory: String? = null
    private var isApplyCropper : Boolean =false
    private var imageName:String? = null
    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager
    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = topLayout.let { view ->
            if (displayId == this@CameravxActivity.displayId) {
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layoutcameravx)
        if(intent!=null){
            isFaceOverlay = intent.extras!!.getBoolean(IS_FACE_OVERLAY_ENABLED,false)
            //storageMode = intent.extras!!.getInt(AppConstants.STORAGE_MODE_INTENT_STRING)
            disableBackForCamera = intent.extras!!.getBoolean(IS_BACK_CAMERA_ENABLED,false)
            imageDirectory = intent!!.extras!!.getString(IMAGE_DIRECTORY)
            isApplyCropper = intent!!.extras!!.getBoolean(IS_APPLYING_CROPPER, false)
            imageName = intent!!.extras!!.getString(IMAGE_NAME)
           // isRectangleCropNeeded = intent.extras!!.getBoolean(AppConstants.RECTANGLE_CROP_REQUIRED_CODE, false)
           // isNavigateBack = intent.extras!!.getBoolean(AppConstants.NAVIGATE_BCK, false)
           // isNavigationType = intent.extras!!.getString(AppConstants.NAVIGATE_TYPE, "")
        }
        displayManager = viewFinder.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
        //Apply default color tint for overlay image
        if(isFaceOverlay) {
            faceOverLayImage.setColorFilter(
                ContextCompat.getColor(this, R.color.darkish_pink), PorterDuff.Mode.SRC_ATOP
            )
            faceOverLayImage.visibility = View.VISIBLE
        } else {
            imgBtnCameraBack.visibility = View.VISIBLE
            faceOverLayImage.visibility = View.INVISIBLE
            imgBtnCameraInfo.visibility = GONE
        }
        createFileDirectory()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        val permission = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
        if(permission!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),CAMERA_PERMISSION_REQUEST_CODE)
        }else{
            initializeCamera()
        }
    }
    private fun checkStoragePermission(){
        val permission = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(permission!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),STORAGE_PERMISSION_REQUEST_CODE)
        }else{
            pickGalleryImage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_PERMISSION_REQUEST_CODE ->{
                if(grantResults.isEmpty()|| grantResults[0]==PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this,getString(R.string.missing_camera_permission),Toast.LENGTH_SHORT).show()
                }else{
                    initializeCamera()
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE ->{
                if(grantResults.isEmpty()|| grantResults[0]==PackageManager.PERMISSION_DENIED){
                    Toast.makeText(this,getString(R.string.missing_camera_permission),Toast.LENGTH_SHORT).show()
                }else{
                    pickGalleryImage()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewFinder.postDelayed({
            viewFinder.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
    }
    @SuppressLint("RestrictedApi")
    private fun initializeCamera() {
        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls and bind all camera use cases
            imgBtnTakePicture.setOnClickListener {
                if(fileUserPhoto!!.exists()){
                    fileUserPhoto!!.delete()
                    createFileDirectory()
                }
                imageCapture?.let {
                    // Setup image capture metadata
                  /*  val metadata = ImageCapture.Metadata().apply {
                        // Mirror image when using the front camera
                        isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                        isReversedVertical = true
                    }*/
                    // Setup image capture listener which is triggered after photo has been taken
                    // imageCapture!!.takePicture(fileUserPhoto, imageSavedListener, metadata)
                    imageCapture!!.takePicture(fileUserPhoto, imageSavedListener)
                }
            }
            flipButton.setOnClickListener {
                mIsFrontFacing = !mIsFrontFacing
                lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
                    CameraX.LensFacing.BACK
                } else {
                    CameraX.LensFacing.FRONT
                }
                try {
                    if(CameraX.isBound(preview))
                        CameraX.unbindAll()
                    // Only bind use cases if we can query a camera with this orientation
                    CameraX.getCameraWithLensFacing(lensFacing)

                    Log.e(APP_TAG, "getCameraWithLensFacing issue")
                     bindCameraUseCases()
                    Log.e(APP_TAG, "bindCameraUseCases issue")
                } catch (exc: Exception) {
                    Log.e(APP_TAG, exc.message)
                    // Do nothing
                }
            }
            bindCameraUseCases()
        }
    }

    private fun createFileDirectory() {
        val dir = File(getExternalFilesDir(null), imageDirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        fileUserPhoto = File(dir.absolutePath, imageName)
        try {
            fileUserPhoto!!.createNewFile()
        } catch (ex: IOException) {
            Log.e(APP_TAG, "IOException:" + ex.localizedMessage)
        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        //val screenResolutio:Size = Size(mRequestedPreviewWidth, mRequestedPreviewHeight)
        Log.d(APP_TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenSize = Size(metrics.widthPixels,metrics.heightPixels)
        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(screenSize)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)
        // preview = Preview(viewFinderConfig)
        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetResolution(screenSize)
            setFlashMode(FlashMode.OFF)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)
            //isFaceOverlay -> true Setup image analysis pipeline that computes average pixel luminance in real time
        if(isFaceOverlay) {
            val analyzerConfig = ImageAnalysisConfig.Builder().apply {
                setLensFacing(lensFacing)
                // Use a worker thread for image analysis to prevent preview glitches
                val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
                setCallbackHandler(Handler(analyzerThread.looper))
                // In our analysis, we care more about the latest image than analyzing *every* image
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                setTargetRotation(viewFinder.display.rotation)

            }.build()
            imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
                analyzer = FaceAnalyzer().apply {
                    onImageAnalyzed {
                        runOnUiThread {
                            if (it) {
                                faceOverLayImage.setColorFilter(
                                    ContextCompat.getColor(
                                        this@CameravxActivity,
                                        R.color.green
                                    ), PorterDuff.Mode.SRC_ATOP
                                )
                                imgBtnTakePicture.visibility = View.VISIBLE
                            } else {
                                faceOverLayImage.setColorFilter(
                                    ContextCompat.getColor(
                                        this@CameravxActivity,
                                        R.color.darkish_pink
                                    ), PorterDuff.Mode.SRC_ATOP
                                )
                                imgBtnTakePicture.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
            }
            // Apply declared configs to CameraX using the same lifecycle owner
            CameraX.bindToLifecycle(
                this, preview, imageCapture, imageAnalyzer
            )
        }else{
            CameraX.bindToLifecycle(this,preview,imageCapture)
        }

    }
    /** Define callback that will be triggered after a photo has been taken and saved to disk */
    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onError(
            imageCaptureError: ImageCapture.ImageCaptureError,
            message: String,
            cause: Throwable?
        ) {
            Log.e(APP_TAG, "Photo capture failed: $message")
            cause?.printStackTrace()
        }

       /* override fun onError(
            error: ImageCapture.UseCaseError, message: String, exc: Throwable?
        ) {
            Log.e(APP_TAG, "Photo capture failed: $message")
            exc?.printStackTrace()
        }*/

        override fun onImageSaved(photoFile: File) {
            val width: Int = IMAGE_REQUIRED_WIDTH
            val height: Int = IMAGE_REQUIRED_HEIGHT
            Log.d(APP_TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
            // We can only change the foreground Drawable using API level 23+ API
            try {
                val src = BitmapFactory.decodeFile(fileUserPhoto!!.absolutePath)
                val byteos = ByteArrayOutputStream()
                src.compress(Bitmap.CompressFormat.PNG,100,byteos)
                val imageByte = byteos.toByteArray()
                byteos.close()
                val fos = FileOutputStream(fileUserPhoto)
                var isFrontFacing = true
                if(lensFacing == CameraX.LensFacing.BACK)
                    isFrontFacing = false
                val realImage = makeSquareImageX(imageByte, isFrontFacing, width, height)
                // Testing ***********************************************************
                //Bitmap realImage = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                val isSaved = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                Log.d(APP_TAG, "saveCameraImage:$isSaved")
                fos.close()
                sendResultToActivity(CAMERA)
            } catch (e: Exception) {
                finish()
            }
            finish()
            // If the folder selected is an external media directory, this is unnecessary
            // but otherwise other apps will not be able to access our images unless we
            // scan them using [MediaScannerConnection]
            /*   val mimeType = MimeTypeMap.getSingleton()
                   .getMimeTypeFromExtension(photoFile.extension)
               MediaScannerConnection.scanFile(
                   this@TaDaCameraXActivity, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null
               )*/
        }
    }

    private class FaceAnalyzer : ImageAnalysis.Analyzer {
        // private var isFaceVisible = false
        private  var listener = {isFaceVisible:Boolean->Unit}
        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.35f)
            //.setProminentFaceOnly(mIsFrontFacing)
            //.setMinFaceSize(if (mIsFrontFacing) 0.35f else 0.15f)
            .build()

        private var lastAnalyzedTimestamp = 0L
        override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)) {
                lastAnalyzedTimestamp = currentTimestamp
                val y = image!!.planes[0]
                val u = image.planes[1]
                val v = image.planes[2]
                val Yb = y.buffer.remaining()
                val Ub = u.buffer.remaining()
                val Vb = v.buffer.remaining()
                val data = ByteArray(Yb + Ub + Vb)
                y.buffer.get(data, 0, Yb)
                u.buffer.get(data, Yb, Ub)
                v.buffer.get(data, Yb + Ub, Vb)
                val metadata = FirebaseVisionImageMetadata.Builder()
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setHeight(image.height)
                    .setWidth(image.width)
                    .setRotation(getRotation(rotationDegrees))
                    .build()
                val visionImage = FirebaseVisionImage.fromByteArray(data, metadata)
                val detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts)
                detector.detectInImage(visionImage)
                    .addOnSuccessListener { faces ->
                        // Task completed successfully
                        // ....
                        if (faces.size <= 0) {
                            listener.invoke(false)
                            Log.d("GooglyFaceTracker", " faces ${faces.size}");

                        } else {
                            for (face in faces) {
                                face.boundingBox
                                val smiling = face!!.smilingProbability
                                val eulerZAngle = face.headEulerAngleZ
                                val eulerYAngle = face.headEulerAngleY
                                Log.d("GooglyFaceTracker" ,"face available")

                                Log.d("GooglyFaceTracker", "is Smiling $smiling eulerZAngle $eulerZAngle eulerYAngle $eulerYAngle")
                                if (eulerYAngle in -12.0..12.0) {
                                    Log.d("GooglyFaceTracker", " true")
                                    listener.invoke(true)

                                    //listener(false)
                                    //faceRecognisationListner.onFaceDeducted(false);
                                } else {
                                    listener.invoke(false)
                                    Log.d("GooglyFaceTracker", "false no face");
                                    // faceRecognisationListner.onFaceDeducted(true);
                                }

                            }
                        }
                    }
                    .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: java.lang.Exception) {
                                // Task failed with an exception
                                // ...
                                Log.d("GooglyFaceTracker ", "onFailure $e");
                                listener.invoke(false)
                            }
                        })
            }
        }
        private fun getRotation(rotationCompensation: Int) : Int{
            val result: Int
            when (rotationCompensation) {
                0 -> result = FirebaseVisionImageMetadata.
                    ROTATION_0

                90 -> result = FirebaseVisionImageMetadata.ROTATION_90

                180 -> result = FirebaseVisionImageMetadata.ROTATION_180

                270 -> result = FirebaseVisionImageMetadata.ROTATION_270

                else -> {
                    result = FirebaseVisionImageMetadata.
                        ROTATION_0

                }
            }
            return result
        }
        fun onImageAnalyzed(listener: (isFaceAvailable:Boolean) -> Unit){
            this.listener = listener
        }
    }
    fun makeSquareImageX(data: ByteArray, isFrontFacing: Boolean, pic_width: Int, pic_height: Int): Bitmap {
        val width: Int
        val height: Int
        val matrix = Matrix()
        // Convert ByteArray to Bitmap
        val bitPic = BitmapFactory.decodeByteArray(data, 0, data.size)
        width = bitPic.width
        height = bitPic.height

        // Perform matrix rotations/mirrors depending on camera that took the photo
        if (isFrontFacing) {
            val mirrorY = floatArrayOf(-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
            val matrixMirrorY = Matrix()
            matrixMirrorY.setValues(mirrorY)
            matrix.postConcat(matrixMirrorY)
        }
        matrix.postRotate(90f)

        // Create new Bitmap out of the old one
        val bitPicFinal = Bitmap.createBitmap(bitPic, 0, 0, width, height, matrix, true)
        bitPic.recycle()
        val desWidth: Int
        val desHeight: Int
        desWidth = bitPicFinal.width
        desHeight = desWidth
        var croppedBitmap =
            Bitmap.createBitmap(bitPicFinal, 0, bitPicFinal.height / 2 - bitPicFinal.width / 2, desWidth, desHeight)
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, pic_width, pic_height, true)
        return croppedBitmap
    }

    fun onCameraInfoClick(view: View) {startActivity(Intent(this@CameravxActivity, GuidelineActivity::class.java))}
    fun onFlashInfoClick(view: View) {
        Log.d(APP_TAG,"Flash clciked")
        val flashMode = imageCapture?.flashMode
        if(flashMode == FlashMode.ON) imageCapture?.flashMode = FlashMode.OFF
        else imageCapture?.flashMode = FlashMode.ON
    }
    fun onGalleryClick(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(APP_TAG, "SDK >= 23")
           checkStoragePermission()
            //ApplicationUtilities.getPermissionFetcher().checkPermissionsStatus(TaDaCameraActivity.this, ApplicationUtilities.storagePermissionArray, ApplicationUtilities.STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            pickGalleryImage()
        }
    }
    private fun pickGalleryImage() {
        val intent = Intent()
        // call android default gallery
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        try {
            intent.putExtra("return-data", true)
            startActivityForResult(Intent.createChooser(intent, "Complete action using"),PICK_IMAGE_REQUEST)
        } catch (e: ActivityNotFoundException) {
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            try {
                val selectedImage = data.data
                callImageCropping(selectedImage, isRectangleCropNeeded)
            } catch (ex: Exception) {
                Log.e(APP_TAG, "Exception:" + "From Cropping library")
            }

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = CropImage.getActivityResult(data)
            val selectedImage = result.uri
            try {
                val temp = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
                val bmp = getResizedBitmap(temp, IMAGE_REQUIRED_WIDTH, IMAGE_REQUIRED_HEIGHT)
                if (isFaceOverlay) {
                    if (validateFacesOnSelectedImage(this@CameravxActivity, bmp)) {
                        saveBitmapToLocation(bmp, fileUserPhoto)
                    }
                } else {
                    saveBitmapToLocation(bmp, fileUserPhoto)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            sendResultToActivity(GALLERY)
        }
    }
    private fun callImageCropping(selectedImage: Uri?, isRectNeeded: Boolean?) {
        val activityBuilder = CropImage.activity(selectedImage)
        if ((!isRectNeeded!!)) {
            activityBuilder.setAspectRatio(2, 3)
            activityBuilder.setFixAspectRatio(false)
        } else {
            activityBuilder.setAspectRatio(2, 2)
            activityBuilder.setFixAspectRatio(true)
        }
        activityBuilder
            .setAllowRotation(true)
            .setAutoZoomEnabled(false)
            .setGuidelines(CropImageView.Guidelines.OFF)
            .setMultiTouchEnabled(false)
            .start(this)
    }

    private fun saveBitmapToLocation(bitmap: Bitmap, filename: File?) {
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(filename)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun sendResultToActivity(source: String) {
        runOnUiThread {
            if(isApplyCropper){
                val uri = Uri.parse(source)
                callImageCropping(uri, isRectangleCropNeeded)
            }else {
                val data = Intent()
                data.putExtra(IMAGE_SOURCE, source)
                data.putExtra(IMAGE_PATH, fileUserPhoto!!.absolutePath)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }
    fun getResizedBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

        val scaleX = newWidth / bitmap.width.toFloat()
        val scaleY = newHeight / bitmap.height.toFloat()
        val pivotX = 0f
        val pivotY = 0f

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY)

        val canvas = Canvas(resizedBitmap)
        canvas.matrix = scaleMatrix
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

        return resizedBitmap
    }

    private fun validateFacesOnSelectedImage(context: Context, croppedBitmap: Bitmap): Boolean {
        val detector = FaceDetector.Builder(context)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()
        if (detector.isOperational) {
            val frame = Frame.Builder().setBitmap(croppedBitmap).build()
            var faces: SparseArray<Face>? = detector.detect(frame)
            if (faces!!.size() > 0) {
                if (faces.size() > 1) {
                    return false
                } else {
                    val face = faces.get(faces.keyAt(0))
                    if (face != null)
                        if (face.eulerZ < EULER_ANGLE_POSITIVE_CONSTANT && face.eulerZ > EULER_ANGLE_NEGATIVE_CONSTANT && face.eulerY < EULER_ANGLE_POSITIVE_CONSTANT && face.eulerY > EULER_ANGLE_NEGATIVE_CONSTANT) {
                            faces.clear()
                            faces = null
                            return true
                        } else {
                            faces.clear()
                            faces = null
                            return false
                        }
                }
            }
            faces.clear()
            faces = null
            return false
        } else {
            return true
        }
    }

    fun onHeaderBackClick(view: View) {
        finish()
    }
}