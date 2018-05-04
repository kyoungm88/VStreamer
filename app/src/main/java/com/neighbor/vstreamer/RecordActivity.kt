package com.neighbor.vstreamer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_record.*
import java.io.File
import java.util.*


class RecordActivity : AppCompatActivity() {

    private val TAG = RecordActivity::class.java.simpleName

    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var previewSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

//    private var mediaRecorder: MediaRecorder? = null
    private var startRecord = false

    private var streamManager: StreamManager? = null

    private var sensorOrientation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        initData()
        initUI()
    }

    private fun initData() {
        streamManager = StreamManager(this)
    }

    private fun initUI() {
        record_button.setOnClickListener({
            if (isRecording()) {
                stopRecording(true)
            } else {
                startRecording()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        startPreview()
    }

    override fun onPause() {
        super.onPause()

        stopRecording(false)

        if (streamManager != null) {
            streamManager?.stopRecording()
            streamManager = null
        }
    }

    override fun onStop() {
        super.onStop()
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startPreview() {
        if (preview.isAvailable) {
            openCamera()
        } else {
            preview.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun stopPreview() {
        if (previewSession != null) {
            previewSession?.close()
            previewSession = null
        }

        if (cameraDevice != null) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            var backCameraId: String? = null

            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!

                if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = cameraId
                    break
                }
            }

            if (backCameraId == null) {
                return
            }

            val cameraCharacteristics = manager.getCameraCharacteristics(backCameraId)
            val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val displayRotation = windowManager.defaultDisplay.rotation
            val width = preview.width
            val height = preview.height

            sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            val swappedDimensions = areDimensionsSwapped(displayRotation)

            val displaySize = Point()
            windowManager.defaultDisplay.getSize(displaySize)

            val rotatedPreviewWidth = if (swappedDimensions) height else width
            val rotatedPreviewHeight = if (swappedDimensions) width else height
            var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

            val largest = Collections.max(
                    Arrays.asList(*streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea())

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest)

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                preview.setAspectRatio(previewSize?.width!!, previewSize?.height!!)
            } else {
                preview.setAspectRatio(previewSize?.height!!, previewSize?.width!!)
            }
//            previewSize = streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java)[0]

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            manager.openCamera(backCameraId, deviceStateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun updatePreview() {
        try {
            previewSession?.setRepeatingRequest(previewBuilder?.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun showPreview() {
        Log.d(TAG, "[showPreview]")
        val surfaceTexture = preview.surfaceTexture
        val surface = Surface(surfaceTexture)
        val rotation = windowManager.defaultDisplay.rotation

        try {
            previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                addTarget(surface)
                set(CaptureRequest.JPEG_ORIENTATION, (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }

            cameraDevice?.createCaptureSession(Arrays.asList(surface), captureStateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startRecording() {
        record_button.text = "중지"
        startRecord = true

        if (streamManager == null) {
            streamManager = StreamManager(this)
        }
//        streamManager?.readyRecoding()
        streamManager?.startRecording(recordStateCallback)


    }

    private fun startRecordingInternal() {
        Log.d(TAG, "[startRecordingInternal]")
        val surfaces = ArrayList<Surface>()

        val surfaceTexture = preview.surfaceTexture
        val previewSurface = Surface(surfaceTexture)
        surfaces.add(previewSurface)
        surfaces.add(streamManager?.getSurface()!!)

        try {
            previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            previewBuilder?.addTarget(previewSurface)
            previewBuilder?.addTarget(streamManager?.getSurface()!!)

            cameraDevice?.createCaptureSession(surfaces, captureStateCallback, null)
//            mediaRecorder?.start()


        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording(showPreview: Boolean) {
        record_button.text = "녹화"
        startRecord = false

        stopPreview()

        if (showPreview) {
            startPreview()
        } else {
            if (streamManager != null) {
                streamManager?.stopRecording()
                streamManager = null
            }
        }
    }

    private fun isRecording(): Boolean {
        return startRecord
    }

    private fun getOutputMediaFile(): File {
        val recordPath = externalCacheDir.absolutePath
        return File(recordPath + File.separator + "record.mp4")
    }

    val surfaceTextureListener = object: SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            openCamera()
        }

    }

    val deviceStateCallback = object: CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice?) {
            cameraDevice = camera
            showPreview()
        }

        override fun onClosed(camera: CameraDevice?) {
            super.onClosed(camera)
            stopRecording(false)
        }

        override fun onDisconnected(camera: CameraDevice?) {

        }

        override fun onError(camera: CameraDevice?, error: Int) {

        }

    }

    val captureStateCallback = object: CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession?) {

        }

        override fun onConfigured(session: CameraCaptureSession?) {
            previewSession = session
            updatePreview()
        }

    }

    val recordStateCallback = object: StreamManager.onRecordingState {
        override fun onReady() {
            runOnUiThread {
                startRecordingInternal()
            }
        }

        override fun onStarted() {
            runOnUiThread({
                tvResult.text = ""
            })
        }

        override fun onFrameInfo(result: Int) {
            runOnUiThread({
                tvResult.text = result.toString()
            })
        }

        override fun onStop() {

        }
    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    companion object {

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()
        private val FRAGMENT_DIALOG = "dialog"

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Tag for the [Log].
         */
        private val TAG = "Camera2BasicFragment"

        /**
         * Camera state: Showing camera preview.
         */
        private val STATE_PREVIEW = 0

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        private val STATE_WAITING_LOCK = 1

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        private val STATE_WAITING_PRECAPTURE = 2

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        private val STATE_WAITING_NON_PRECAPTURE = 3

        /**
         * Camera state: Picture was taken.
         */
        private val STATE_PICTURE_TAKEN = 4

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic private fun chooseOptimalSize(
                choices: Array<Size>,
                textureViewWidth: Int,
                textureViewHeight: Int,
                maxWidth: Int,
                maxHeight: Int,
                aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                        option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

    }
}
