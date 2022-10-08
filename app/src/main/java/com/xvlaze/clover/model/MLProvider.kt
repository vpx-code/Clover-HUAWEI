package com.xvlaze.clover.model

import android.net.Uri
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLText
import com.xvlaze.clover.util.MyApplication
import java.io.IOException

object MLProvider {
    fun readTreatmentPackage(imageUri: Uri, callback: IMLRecognitionCallback){
        val analyzer = MLAnalyzerFactory.getInstance().localTextAnalyzer
        try {
            val frame = MLFrame.fromFilePath(MyApplication.getApp()!!.applicationContext, imageUri)
            analyzer.asyncAnalyseFrame(frame)
                .addOnSuccessListener { detectedText: MLText ->
                    callback.onSuccess(detectedText.stringValue.trim())
                    // Recognition success.
                    try {
                        analyzer.stop()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    interface IMLRecognitionCallback {
        fun onSuccess(text: String)
    }
}
