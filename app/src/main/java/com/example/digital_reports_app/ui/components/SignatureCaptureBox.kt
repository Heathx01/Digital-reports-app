package com.example.digital_reports_app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignatureCaptureBox(
    modifier: Modifier = Modifier,
    onSignatureCaptured: (Bitmap) -> Unit
) {
    val path = remember { mutableStateOf(Path()) }
    var hasDrawn by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            path.value.moveTo(motionEvent.x, motionEvent.y)
                            hasDrawn = true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            path.value.lineTo(motionEvent.x, motionEvent.y)
                        }
                    }
                    true
                }
        ) {
            drawPath(path = path.value, color = Color.Black, style = Stroke(width = 4f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                path.value = Path()
                hasDrawn = false
            },
            enabled = hasDrawn,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("Limpiar Trazo", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
