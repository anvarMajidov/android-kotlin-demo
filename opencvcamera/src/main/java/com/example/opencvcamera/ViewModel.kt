package com.example.opencvcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.CvType


class ImageViewModel : ViewModel(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val _imageBitmap = MutableLiveData<Bitmap>()
    val imageBitmap: LiveData<Bitmap> = _imageBitmap
    private var cameraFrame: Mat? = null
    var cameraController: CameraController? = null

    interface CameraController {
        fun startCameraView()
        fun stopCameraView()
    }

    fun loadImage(context: Context, resourceId: Int) {
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
            return
        }

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Process the image with OpenCV here, e.g., convert to grayscale

        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)
        _imageBitmap.postValue(resultBitmap)
    }

    fun processVideoFrame(context: Context, videoUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!OpenCVLoader.initLocal()) {
                Log.e("OpenCV", "Unable to load OpenCV!")
                return@launch
            }

            val mediaMetadataRetriever = MediaMetadataRetriever()
            try {
                mediaMetadataRetriever.setDataSource(context, videoUri)
                val videoLengthInUs = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )!!.toLong() * 1000 // convert to microseconds

                for (time in 0 until videoLengthInUs step 10000000L) { // adjust step based on your needs
                    val bitmap = mediaMetadataRetriever.getFrameAtTime(
                        time,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    bitmap?.let {
                        val mat = Mat()
                        Utils.bitmapToMat(it, mat)
                        // Apply your OpenCV processing here, e.g., convert to grayscale
//                        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
                        val processedBitmap =
                            Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(mat, processedBitmap)
                        _imageBitmap.postValue(processedBitmap) // Post the processed frame
                    }
                    delay(1000) // wait for 1 second before processing the next frame
                }
            } catch (e: Exception) {
                Log.e("VideoProcessing", "Error processing video frame", e)
            } finally {
                mediaMetadataRetriever.release()
            }
        }
    }
    override fun onCameraViewStarted(width: Int, height: Int) {
        cameraFrame = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        cameraFrame?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        val mat = inputFrame.rgba()
        // Apply your OpenCV processing here, e.g., convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.Canny(mat, mat, 50.0, 150.0)
        val processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, processedBitmap)
        _imageBitmap.postValue(processedBitmap) // Post the processed frame
        return mat
    }

    fun chooseInputSource(context: Context, source: String, videoUri: Uri? = null) {
        if (source == "video") {
            videoUri?.let {
                processVideoFrame(context, it)
            }
        } else if (source == "camera") {
            cameraController?.startCameraView()
        }
    }
}
