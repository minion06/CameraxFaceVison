package com.augray.cameravx

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os.bind
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.augray.visionx.Cameravx
import com.augray.visionx.bind
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val txtView by bind<TextView>(R.id.textView)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Cameravx.CAMERAVX_LAUNCH_REQUEST){
            if(resultCode == Activity.RESULT_OK){
                val path = data!!.getStringExtra(Cameravx.IMAGE_PATH)
                txtView.setText(path.toString())
            }
        }
    }

    fun LaunchCameraVx(view: View) {
        Cameravx.Builder().setActivity(this).setCameraBackEnabled(true).setFaceOverlay(true).setImageDirectory("temp").setFileName("myPhoto.png").build()
    }
}
