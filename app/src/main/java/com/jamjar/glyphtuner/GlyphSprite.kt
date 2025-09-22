import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.jamjar.glyphtuner.R
import com.jamjar.glyphtuner.util.TuningMode
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlin.math.roundToInt

class GlyphSprite {

    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var context: Context? = null

    fun init(context: Context) {
        this.glyphMatrixManager = GlyphMatrixManager.getInstance(context)
        this.context = context

        val callback = object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
//                Log.d("GlyphToy", "Glyph service connected")
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

    fun renderResource(resourceId: Int) {
        val icon = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, resourceId))
            .build()
        val frame = GlyphMatrixFrame.Builder()
            .addTop(icon)
            .build(context)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    fun renderTuner(backgroundRes: Int, noteRes: Int, cents: Float, tuningOffset: Int, tuningMode: TuningMode) {
        val centsString = cents.roundToInt().coerceIn(-99, 99).toString()

        var textOffset = when (centsString.length) {
            1 -> 10
            2 -> 7
            else -> 5
        }
        if (cents >= 10) textOffset += 1

        val background = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, backgroundRes))
            .build()
        val note = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, noteRes))
            .build()
        val centsObj = GlyphMatrixObject.Builder()
            .setText(centsString)
            .setBrightness(220)
            .setScale(50)
            .setPosition(textOffset, 1)
            .build()

        val tuningLine = generateTuningLine(tuningOffset)
        val tuningLock = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, if (tuningMode == TuningMode.AUTO) {
                R.drawable.empty_frame
            } else {
                R.drawable.tuning_lock
            }))
            .build()


        val backgroundFrame = GlyphMatrixFrame.Builder()
            .addLow(background)
            .addMid(tuningLock)
            .addTop(note)
            .build(context)

        val frame = GlyphMatrixFrame.Builder()
            .addLow(backgroundFrame.render())
            .addMid(centsObj)
            .addTop(tuningLine)
            .build(context)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    private fun generateTuningLine(offset: Int): IntArray {
        val grid = IntArray(WIDTH * HEIGHT) { 0 }
        val clampedOffset = offset.coerceIn(-MID_POINT, MID_POINT)
        val col = MID_POINT + clampedOffset

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
