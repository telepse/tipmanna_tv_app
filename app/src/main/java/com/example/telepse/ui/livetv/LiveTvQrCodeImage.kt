package com.example.telepse.ui.livetv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun LiveTvQrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
    contentDescription: String = "Checkout QR code",
) {
    val bitmap =
        remember(content, sizePx) {
            generateQrBitmap(content, sizePx)
        }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = contentDescription, modifier = modifier)
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}
