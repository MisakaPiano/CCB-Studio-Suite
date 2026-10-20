package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JOptionPane

/** One brand resource for Compose windows, shared native-dialog owner and packaging. */
object StudioBrand {
    val image:BufferedImage by lazy {StudioBrand::class.java.getResourceAsStream("/brand/ccb-blue.png").use {ImageIO.read(requireNotNull(it))}}
    val painter by lazy {BitmapPainter(image.toComposeImageBitmap())}
    fun configureDefaultWindows() {JOptionPane.getRootFrame().iconImage=image}
}
