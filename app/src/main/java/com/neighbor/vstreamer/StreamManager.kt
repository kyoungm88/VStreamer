package com.neighbor.vstreamer

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.view.Surface
import com.neighbor.vstream.VStreamConstant
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.math.BigInteger
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


class StreamManager(val context: Context) {
    private val TAG = StreamManager::class.java.simpleName

    private var mediaCodec: MediaCodec? = null
    private var mediaFormat: MediaFormat? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null
//    private var mediaMuxer: MediaMuxer? = null
    private var mediaSurface: Surface?= null

    private val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
//    private val MIME_TYPE_AUDIO = MediaFormat.MIMETYPE_AUDIO_AAC
    private val IFRAME_INTERVAL = 1

    private val VIDEO_ID =  VStreamConstant.HD.ID.value
    private val FRAME_RATE = VStreamConstant.HD.FRAME_RATE.value
    private val BIT_RATE = VStreamConstant.HD.BIT_RATE.value
//    private val SAMPLE_RATE = 44100
    private val TIMEOUT_USEC = 10000L

    private var width = VStreamConstant.HD.WIDTH.value
    private var height = VStreamConstant.HD.HEIGHT.value
    private var generateIndex = 0
//    private var mTrackIndex = 0
    private var mEncodeStarted = false
//    private var mMuxerStarted = false
    private var frameIndex = 0
    private var configData: ByteArray? = null

    private var mRecordStateCallback: onRecordingState ? = null

    enum class NAL_TYPE(val type:Int) {
        P_Frame(1),
        I_Frame(5),
        SPS(7),
        PPS(8),
        AUD(9),
        FU_A(28)
    }

    fun startRecording(callback: onRecordingState) {
        Log.d(TAG, "[startRecording]")
        mRecordStateCallback = callback
        VStreamApplication.connectVStream()
        startEncodeThread()
    }

    fun stopRecording() {
        Log.d(TAG, "[stopRecording]")
        mEncodeStarted = false
        VStreamApplication.disConnectVStream()
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun prepareEncoder() {
        Log.d(TAG, "[prepareEncoder]")
        try {
            frameIndex = 0
            bufferInfo = MediaCodec.BufferInfo()

            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
            mediaFormat?.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            mediaFormat?.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            mediaFormat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
//                mediaFormat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
//            } else {
//                mediaFormat?.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
//            }
            //2130708361, 2135033992, 21
            mediaFormat?.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

            mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()
            Log.d(TAG, "[prepareEncoder] start()")
            if (mRecordStateCallback != null) {
                mRecordStateCallback?.onReady()
            }

//            val outputFileName = getOutputMediaFileName()
//                    ?: throw IllegalStateException("Failed to get video output file")
//            /**
//             * Create a MediaMuxer. We can't add the video track and start() the
//             * muxer until the encoder starts and notifies the new media format.
//             */
//            try {
//                mediaMuxer = MediaMuxer(
//                        outputFileName!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//            } catch (ioe: IOException) {
//                throw IllegalStateException("MediaMuxer creation failed", ioe)
//            }
//
//            mMuxerStarted = false
//            mediaMuxer = MediaMuxer(Environment.getExternalStorageDirectory().absolutePath + "/temp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun startEncodeThread() {
        mEncodeStarted = true

        val encodeThread = object: Thread() {
            override fun run() {
                super.run()
                prepareEncoder()

                try {
                    while (mEncodeStarted) {
                        encode()
                    }
                    encode()
                } finally {
                    release()
                }
            }
        }
        encodeThread.start()
    }

    private fun encode() {
        if (!mEncodeStarted) {
            mediaCodec?.signalEndOfInputStream()
        }

        while (true) {
            if (!mEncodeStarted) {
                break
            }

            val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)!!
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!mEncodeStarted) {
                    Log.d(TAG, "no output from encoder available")
                    break
                }
//            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                if (mMuxerStarted) {
//                    throw IllegalStateException("format changed twice")
//                }
//                val newFormat = mediaCodec?.outputFormat
//
//                mTrackIndex = mediaMuxer?.addTrack(newFormat)!!
//                mediaMuxer?.start()
//                mMuxerStarted = true
            } else if (encoderStatus < 0) {
                Log.i(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            } else if (encoderStatus >= 0) {
                val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                if (encodedData == null) {
                    Log.i(TAG, "encoderOutputBuffer $encoderStatus was null")
                } else {
                    val endOfStream = bufferInfo?.flags!! and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    // pass to whoever listens to
//                    if (mMuxerStarted) {
//                        try {
//                            mediaMuxer?.writeSampleData(encoderStatus, encodedData, bufferInfo)
//
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
                    if (endOfStream == 0) onEncodedSample(bufferInfo!!, encodedData)
                    // releasing buffer is important
                    mediaCodec?.releaseOutputBuffer(encoderStatus, false)
                    if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) break

                }
            }
        }
    }

    private fun release() {
        if (mediaCodec != null) {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            Log.i(TAG, "RELEASE CODEC")
        }

//        try {
//            if (mMuxerStarted) {
//                if (mediaMuxer != null) {
//                    mediaMuxer?.stop()
//                    mediaMuxer?.release()
//                    mediaMuxer = null
//                }
//
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    fun getSurface(): Surface {
        return mediaSurface!!
    }

    private fun onEncodedSample(info: MediaCodec.BufferInfo, data: ByteBuffer) {
//        Log.d(TAG, "[onEncodedSample]")

        val buffer = ByteArray(info.size - info.offset)
        data.position(info.offset)
        data.limit(info.offset + info.size)
        data.get(buffer, info.offset, info.size)

//        Log.d(TAG, "[onEncodedSample] buffer size : ${buffer.size}")
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
            configData = buffer.clone()
        } else {
            val pts: BigInteger = (info.presentationTimeUs / 1000000).toBigInteger()
            val isIframe = isIFrame(buffer)
            val sendBuffer: ByteArray

            var configDataSize = 0

            if (isIframe) {
                configDataSize = configData?.size!!

                if (configDataSize == 0) {
                    return
                }
            }

            sendBuffer = ByteArray(info.size - info.offset + configDataSize)
            if (configDataSize > 0) {
                System.arraycopy(configData, 0, sendBuffer, 0, configDataSize)
            }
            System.arraycopy(buffer, 0, sendBuffer, configDataSize, buffer.size)

            Log.d(TAG, "[onEncodedSample] Send buffer size : ${buffer.size}, iFrame : $isIframe, pts : $pts")
            val result = VStreamApplication.mVStreamManager?.sendVideoData(isIframe, FRAME_RATE, VIDEO_ID, pts, sendBuffer, sendBuffer.size)
//            Log.d(TAG, "[onEncodedSample] sendVideoData result : $result")
            if (mRecordStateCallback != null) {
                mRecordStateCallback?.onFrameInfo(result!!)
            }

        }
    }

    private fun getOutputMediaFileName(): String? {
        val state = Environment.getExternalStorageState()
        // Check if external storage is mounted
        if (Environment.MEDIA_MOUNTED != state) {
            Log.e(TAG, "External storage is not mounted!")
            return null
        }
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "TestingCamera2")
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory " + mediaStorageDir.path
                        + " for pictures/video!")
                return null
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return mediaStorageDir.getPath() + File.separator +
                "VID_" + timeStamp + ".mp4"
    }

    private fun isSPSData(byteArray: ByteArray): Boolean {
        return byteArray[4].toInt() and 0x1f == NAL_TYPE.SPS.type
    }

    private fun isPPSData(byteArray: ByteArray): Boolean {
        return byteArray[4].toInt() and 0x1f == NAL_TYPE.PPS.type
    }

    private fun isPFrame(byteArray: ByteArray): Boolean {
        return byteArray[4].toInt() and 0x1f == NAL_TYPE.P_Frame.type
    }

    private fun isIFrame(byteArray: ByteArray): Boolean {
        return byteArray[4].toInt() and 0x1f == NAL_TYPE.I_Frame.type
    }

    interface onRecordingState {
        fun onReady()
        fun onStarted()
        fun onFrameInfo(result: Int)
        fun onStop()
    }

}