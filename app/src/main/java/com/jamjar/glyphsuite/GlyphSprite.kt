import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

class GlyphSprite {

    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var context: Context? = null

    fun init(context: Context) {
        this.glyphMatrixManager = GlyphMatrixManager.getInstance(context)
        this.context = context

        val callback = object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                Log.d("GlyphToy", "Glyph service connected")
                // safe to register and set frame now
                glyphMatrixManager?.register(Glyph.DEVICE_23112)
            }

            override fun onServiceDisconnected(componentName: ComponentName?) {
            }
        }
        glyphMatrixManager?.init(callback)
    }

    fun unInit() {
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        context = null
    }

    fun render(resourceId: Int) {
        val icon = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, resourceId))
            .build()
        val frame = GlyphMatrixFrame.Builder()
            .addTop(icon)
            .build(context)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    fun renderTuner(backgroundRes: Int, noteRes: Int, tuningOffset: Int) {
        val background = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, backgroundRes))
            .build()
        val note = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, noteRes))
            .build()

        val tuningLine = generateTuningLine(tuningOffset)

        val frame = GlyphMatrixFrame.Builder()
            .addLow(background)
            .addMid(note)
            .addTop(tuningLine)
            .build(context)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    private fun generateTuningLine(offset: Int): IntArray {
        val grid = IntArray(WIDTH * HEIGHT) { 0 }

        // Clamp offset to valid range (-12..+12)
        val clampedOffset = offset.coerceIn(-MID_POINT, MID_POINT)

        // Column where the line should be drawn
        val col = MID_POINT + clampedOffset

        // Vertical line of length 9, centered vertically
        val lineLength = 9
        val startRow = MID_POINT - lineLength / 2
        val endRow = startRow + lineLength

        for (row in startRow until endRow) {
            grid[row * WIDTH + col] = 2047
        }

        return grid
    }

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
        private const val MID_POINT = WIDTH / 2  // 12
    }
}
