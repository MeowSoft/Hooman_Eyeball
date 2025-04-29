package com.hooman.eyeball

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hooman.eyeball.ui.theme.EyeballTheme

/**
 * Class to hold x and y coords and angle.
 */
data class Dims(
    val x: Int,
    val y: Int,
    val angle: Int
)

/**
 * Get screen width and height.
 */
fun getScreenDims(activity: Activity): Dims {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = activity.windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        return Dims(
            windowMetrics.bounds.width() - insets.left - insets.right,
            windowMetrics.bounds.height() - insets.top - insets.bottom,
            0
        )
    } else {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return  Dims(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            0
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Don't sleep screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // Screen dimensions.
        val screenDims = getScreenDims(this)

        setContent {

            val systemUiController: SystemUiController = rememberSystemUiController()

            systemUiController.isStatusBarVisible = false // Status bar
            systemUiController.isNavigationBarVisible = false // Navigation bar
            systemUiController.isSystemBarsVisible = false // Status & Navigation bars

            // Don't show system bars on touch, only swipe.
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            var smallestDelay = -1L

            EyeballTheme {

                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.White) { innerPadding ->

                    // Eye position and angle.
                    val position = remember { mutableStateOf(Dims(0, 0, 0)) }

                    AnimatedEye(
                        modifier = Modifier.padding(innerPadding),
                        newPos = position.value
                    )  {

                        // Get new  random screen position.
                        val w = ((screenDims.x / 2) * 0.9).toInt()
                        val h = ((screenDims.y / 2) * 0.9).toInt()
                        val newDims = Dims(
                            (-w..w).random(),
                            (-h..h).random(),
                            (-60..60).random()
                        )

                        // After some random time between 0 and 10 seconds...
                        val delay = (1..5000L).random()

                        Log.d("HOOMAN", "NewDims: $newDims delay: $delay smallest delay so far: $smallestDelay")

                        if (smallestDelay < 0 || delay < smallestDelay) {
                            smallestDelay = delay
                        }

                        // Update the position value.
                        do {
                            val success = Handler(Looper.getMainLooper()).postDelayed({
                                position.value = newDims
                            }, delay)

                            Log.d("HOOMAN", "Post success: $success")
                        } while (!success)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEye(
    modifier: Modifier,
    newPos: Dims,
    onAnimationComplete: () -> Unit
) {
    // Animation values:
    val time = (20..500).random()

    // Animate x and y offset.
    val offsetX: Float by animateFloatAsState(
        targetValue = newPos.x.toFloat(),
        animationSpec = tween(
            durationMillis = time.value,
            easing = LinearEasing
        ),
        finishedListener = { _ ->
            onAnimationComplete()
        }
    )
    val offsetY: Float by animateFloatAsState(
        targetValue = newPos.y.toFloat(),
        animationSpec = tween(
            durationMillis = time.value,
            easing = LinearEasing
        )
    )

    // Animate angle.
    val angle: Float by animateFloatAsState(
        targetValue = newPos.angle.toFloat(),
        animationSpec = tween(
            durationMillis = time.value,
            easing = LinearEasing
        )
    )

    // Update animation values.
    LaunchedEffect(newPos) {
        time.value = (20..500).random()
    }

    // Show eyeball image.
    Image(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .rotate(angle),
        painter = painterResource(id = R.drawable.eyeball),
        contentDescription = ""
    )
}
