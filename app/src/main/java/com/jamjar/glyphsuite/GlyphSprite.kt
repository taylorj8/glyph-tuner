import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jamjar.glyphsuite.R
import com.jamjar.glyphsuite.util.bitmapToIntArray
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy

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
        val musicIcon = GlyphMatrixObject.Builder()
            .setImageSource(BitmapFactory.decodeResource(context?.resources, resourceId))
            .build()
        val musicFrame = GlyphMatrixFrame.Builder()
            .addTop(musicIcon)
            .build(context)



        glyphMatrixManager?.setMatrixFrame(musicFrame.render())
    }
    fun render(array: IntArray) {
        glyphMatrixManager?.setMatrixFrame(array)
    }
}
