package com.neighbor.vstreamer

import android.app.Application
import android.util.Log
import com.neighbor.vstream.VStreamManager

class VStreamApplication: Application(){

    companion object {
        private val TAG = VStreamApplication::class.java.simpleName

        private const val identify = "VStreamApp"
        private const val serverIp = "211.189.132.27"
        private const val serverPort = 10015


        var mVStreamManager: VStreamManager? = null

        init {
            System.loadLibrary("VStream")
            Log.d(TAG, "[VStreamApplication] loadLibrary(libVStream.so)")
        }

        fun connectVStream() {
            if (mVStreamManager?.isConnected == false) {
                mVStreamManager?.initSession(identify, serverIp, serverPort)
            }
        }

        fun disConnectVStream() {
            if (mVStreamManager?.isConnected == true) {
                mVStreamManager?.destroySession()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mVStreamManager = VStreamManager()

    }

    override fun onTerminate() {
        super.onTerminate()

        if (mVStreamManager != null) {
            disConnectVStream()
            mVStreamManager?.delete()
            mVStreamManager = null
        }
    }
}