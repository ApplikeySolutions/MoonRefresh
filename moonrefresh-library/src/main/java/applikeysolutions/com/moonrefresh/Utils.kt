package applikeysolutions.com.moonrefresh

import android.content.Context
import android.util.TypedValue

object Utils {
    fun convertDpToPixel(context: Context, dp: Int): Int {
        val r = context.resources
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics)
        return Math.round(px)
    }
}
