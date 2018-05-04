package com.neighbor.vstreamer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera_media.*


class CameraMediaActivity : AppCompatActivity() {

    private val REQUEST_USED_PERMISSTION = 200

    private val needPermission = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var permissionAccept = true

        when (requestCode) {
            REQUEST_USED_PERMISSTION -> {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        permissionAccept = false
                        break
                    }
                }
            }
        }

        if (!permissionAccept) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_media)

        for (permission in needPermission) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, needPermission, REQUEST_USED_PERMISSTION)
                break
            }
        }

        setContentView(R.layout.activity_camera_media)

        initUI()
    }

    private fun initUI() {
        record_button.setOnClickListener({
            startActivity(getIntent(RecordActivity::class.java))
        })

//        play_button.setOnClickListener({
//            startActivity(getIntent(PlayActivity::class.java))
//        })
    }

    private fun getIntent(cls: Class<*>): Intent {
        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return intent
    }
}
