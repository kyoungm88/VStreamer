package com.neighbor.vstreamer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_play.*
import java.io.File

class PlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        initUI()
    }

    private fun initUI() {
        val mediaController = MediaController(this)
        video_view.setMediaController(mediaController)

        video_view.setOnPreparedListener({
            video_view.start()
            mediaController.show()
        })

        video_view.setVideoPath(getOutputMediaFile().absolutePath)
    }

    private fun getOutputMediaFile(): File {
        val recordPath = externalCacheDir.absolutePath
        return File(recordPath + File.separator + "record.mp4")
    }
}
