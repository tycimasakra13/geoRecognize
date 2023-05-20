package com.example.test1

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.test1.ui.theme.Test1Theme
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenCompose()
        }
    }
}

@Preview
@Composable
fun ScreenCompose() {
    Test1Theme {
        // A surface container using the 'background' color from the theme
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GEOMETRICAL FIGURE RECOGNITION",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val bmp: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val selectedImg:Bitmap = SelectImage(bmp)
                if (OpenCVLoader.initDebug()) {
                    var figuresDetected:Pair<Bitmap, String> = FigureDetection(selectedImg)
                    if(figuresDetected != null ) {
                        Column{
                            Image(bitmap = figuresDetected.first.asImageBitmap(), contentDescription = "text", modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.padding(10.dp))
                            Text(text = figuresDetected.second, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Text("OpenCV failed")
                }
            }
        }
    }
}

@Composable
fun FigureDetection(bmp: Bitmap): Pair<Bitmap, String> {

    var cannyOutput: Mat = Mat() //container for edges
    val mGray: Mat = Mat() //container for gray material
    val contours: List<MatOfPoint> = ArrayList<MatOfPoint>() //list for image contours

    val bmpGray = makeGray(bmp) //convert to gray scale
    Utils.bitmapToMat(bmpGray, mGray); //assign grayscale bitmap to material
    Imgproc.Canny(mGray, cannyOutput, 80.0, 100.0) //find edges
    Imgproc.findContours(cannyOutput, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE) //find contours

    val approx: MatOfPoint2f = MatOfPoint2f() //approximate points
    var imgCaption: String = "Figures recognized: \n"

    val contourColor: Scalar = Scalar(0.0, 255.0, 255.0)
    val contourThickness: Int  = 10
    var figureFilled: Int = -1

    for ((index, value) in contours.withIndex()) { //iterate through contours
        val c = contours[index]
        val mop2f = MatOfPoint2f()
        c.convertTo(mop2f, CvType.CV_32F) //points type conversion for approxPolyDP
        val epsilon = Imgproc.arcLength(mop2f, true)
        Imgproc.approxPolyDP(mop2f, approx, 0.01 * epsilon, true)

        val rect = approx.toList().size //get number of edges/angles

        if (rect == 5) {
            imgCaption += "Pentagon "
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness) //draw contours around found shape
        } else if (rect == 3) {
            imgCaption += "Triangle "
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness)
        } else if (rect == 4) {
            var w = Imgproc.boundingRect(c).width
            var h = Imgproc.boundingRect(c).height

            var ratio:Double = w.toDouble() / h.toDouble()
            if (ratio >= 0.9 && ratio <= 1.1) {
                imgCaption += "Square "
            } else {
                imgCaption += "Rectangle "
            }
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness)
        } else if (rect == 10) {
            imgCaption += "Half-Circle "
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness)
        } else if (rect > 10 && rect <= 15) {
            imgCaption += "Multi-Angle "
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness)
        } else if (rect > 15) {
            imgCaption += "Circle "
            Imgproc.drawContours(mGray, contours, figureFilled, contourColor, contourThickness)
        } else {
            imgCaption = "Shape not recognized!"
        }
    }

    Utils.matToBitmap(mGray, bmp) //convert material to bitmap

    return Pair(bmp, imgCaption)
}


@Composable
fun makeGray(bitmap: Bitmap): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat) //convert bitmap to material
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); //convert to grayscale
    Utils.matToBitmap(mat, bitmap); //convert material to bitmap

    return bitmap;
}

@Composable
fun SelectImage(btm: Bitmap): Bitmap {
    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val context = LocalContext.current

    var selectedImage: Bitmap = btm.copy(btm.config, true)
    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Button(onClick = {
        launcher.launch("image/*")
    },
    modifier = Modifier.padding(10.dp)) {
        Text(text = "Pick image")
    }

    imageUri?.let {
        if (Build.VERSION.SDK_INT < 28) {
            selectedImage = MediaStore.Images
                .Media.getBitmap(context.contentResolver,it)

        } else {
            val source = ImageDecoder
                .createSource(context.contentResolver,it)
            selectedImage = ImageDecoder.decodeBitmap(source)
        }
    }

    return selectedImage
}

@Preview
@Composable
fun selectImgPrev() {
    SelectImage(ImageBitmap.imageResource(id = R.drawable.shape1).asAndroidBitmap())
}