package com.nsk.whiteboardtataclassedge.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nsk.whiteboardtataclassedge.data.model.DrawAction
import com.nsk.whiteboardtataclassedge.data.model.DrawStroke
import com.nsk.whiteboardtataclassedge.data.model.Shape
import com.nsk.whiteboardtataclassedge.data.model.ShapeType
import com.nsk.whiteboardtataclassedge.data.model.ToolType
import com.nsk.whiteboardtataclassedge.ui.viewModel.WhiteboardViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt


class WhiteboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var c = Color.BLACK
    // Freehand drawing

    private val drawPath = Path()
    private var canvasBitmap: Bitmap? = null
    private val drawCanvas = Canvas()

    // Shapes
    private val shapes = mutableListOf<Shape>()
    private var selectedShape: Shape? = null
    private var currentMode = Mode.FREEHAND

    enum class Mode { FREEHAND, CIRCLE , RECTANGLE , LINE , POLYGON , ERASER, UNDO, REDO }

    private var previousTouchX = 0f
    private var previousTouchY = 0f
    private var motionPointCount = 0

    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE  // ✅ Paint white over your white background
        strokeWidth = 30f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }



    fun setMode(mode: Mode) { currentMode = mode; invalidate() }
    fun setColor(colour : Int) { c = /*Color.parseColor(colour)*/colour; invalidate() }
  //  fun addCircle(x: Float, y: Float) { shapes.add(Shape( centerX = x, centerY = y, radius = 50f)); invalidate() }

    // Add shapes
    fun addShape(type: ShapeType, x: Float, y: Float, sides : Int = 4) {
        val shape = when (type) {
            ShapeType.CIRCLE -> Shape(type = ShapeType.CIRCLE, centerX = x, centerY = y, color = c)
            ShapeType.RECTANGLE -> Shape(type = ShapeType.RECTANGLE, centerX = x, centerY = y, width = 220f, height = 160f, color = c)
            ShapeType.LINE -> Shape(type = ShapeType.LINE, centerX = x, centerY = y, height = 300f, radius = 0f,color = c).apply {
                points.add(PointF(x-75f, y)); points.add(PointF(x+75f, y))
            }
            /*ShapeType.POLYGON -> Shape(type = ShapeType.POLYGON, centerX = x, centerY = y,color = c).apply {
                // Triangle example
                points.add(PointF(x, y-50f)); points.add(PointF(x-50f, y+25f)); points.add(PointF(x+50f, y+25f))
            }
        }*/
            ShapeType.POLYGON -> {
                val polygon = Shape(type = ShapeType.POLYGON, centerX = x, centerY = y, sides = sides, color = c)
                generatePolygonPoints(polygon)  // Auto-generate vertices
                polygon
            }
        }
        shapes.add(shape)
        invalidate()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvasBitmap?.let { drawCanvas.setBitmap(it) }
        drawCanvas.drawColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Freehand bitmap
        canvasBitmap?.let { canvas?.drawBitmap(it, 0f, 0f, null) }
        // Current path
       // canvas?.drawPath(drawPath, drawPaint)

        // Draw current path ONLY for FREEHAND (not eraser/shapes)
        if (currentMode == Mode.FREEHAND) {
             val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND; strokeWidth = 5f
            }

            canvas?.drawPath(drawPath, drawPaint)
        }
        // Shapes
        shapes.forEach { shape ->
            val paint = Paint().apply { color = shape.color; style = Paint.Style.STROKE; strokeWidth = 5f }
          //  canvas?.drawCircle(shape.centerX, shape.centerY, shape.radius, paint)


            when (shape.type) {
                ShapeType.CIRCLE -> canvas.drawCircle(shape.centerX, shape.centerY, shape.radius, paint)
                ShapeType.RECTANGLE -> {
                    val left = shape.centerX - shape.width/2
                    val top = shape.centerY - shape.height/2
                    canvas.drawRect(left, top, left+shape.width, top+shape.height, paint)
                }
                ShapeType.LINE -> if (shape.points.size >= 2) {
                    for (i in 0 until shape.points.size-1) {
                        val start = shape.points[i]
                        val end = shape.points[i+1]
                        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                    }
                }
                ShapeType.POLYGON -> if (shape.points.size >= 3) {
                    val path = Path().apply {
                        moveTo(shape.points[0].x, shape.points[0].y)
                        for (i in 1 until shape.points.size) {
                            lineTo(shape.points[i].x, shape.points[i].y)
                        }
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }


    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        return when (currentMode) {
            Mode.FREEHAND -> handleFreehand(event, touchX, touchY)
            Mode.CIRCLE , Mode.RECTANGLE, Mode.POLYGON, Mode.LINE -> handleShape(event, touchX, touchY)
            Mode.ERASER -> handleEraser(event, touchX, touchY)
            Mode.UNDO -> TODO()
            Mode.REDO -> TODO()

        }
    }

    private fun handleFreehand(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousTouchX = touchX
                previousTouchY = touchY
                motionPointCount = 0
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                motionPointCount += 1
                if (motionPointCount > 1) {  // Skip first move event
                    // Quadratic Bézier: current point, midpoint to previous, previous point
                    val endX = (touchX + previousTouchX) / 2
                    val endY = (touchY + previousTouchY) / 2
                    drawPath.quadTo(previousTouchX, previousTouchY, endX, endY)
                }
                previousTouchX = touchX
                previousTouchY = touchY
            }
            MotionEvent.ACTION_UP -> {
                 val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = c; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND; strokeWidth = 5f
                }
                drawPath.lineTo(touchX, touchY)  // Finish curve
                drawCanvas.drawPath(drawPath, drawPaint)
                drawPath.reset()
            }
        }
        invalidate()
        return true
    }

   /* private fun handleShape(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Hit-test: select nearest shape
                selectedShape = shapes.find { isPointInCircle(touchX, touchY, it) }
                if (selectedShape == null) {
                    // Add new circle
                   // addCircle(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedShape?.let { shape ->
                    shape.centerX = touchX
                    shape.centerY = touchY
                }
            }
            MotionEvent.ACTION_UP -> selectedShape = null
        }
        invalidate()
        return true
    }*/

    private fun handleShape(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedShape = shapes.find { isPointInShape(touchX, touchY, it) }
                if (selectedShape == null) {
                    // Add new shape based on current mode
                    when (currentMode) {
                        Mode.RECTANGLE -> addShape(ShapeType.RECTANGLE, touchX, touchY)
                        Mode.LINE -> addShape(ShapeType.LINE, touchX, touchY)
                        Mode.POLYGON -> addShape(ShapeType.POLYGON, touchX, touchY, 6)
                        else -> addShape(ShapeType.CIRCLE, touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedShape?.let { shape ->
                    // Offset all points by drag delta
                    val dx = touchX - shape.centerX
                    val dy = touchY - shape.centerY
                    shape.centerX = touchX
                    shape.centerY = touchY
                    shape.points.forEachIndexed { index, point ->
                        shape.points[index] = PointF(point.x + dx, point.y + dy)
                    }
                }
            }
            MotionEvent.ACTION_UP -> selectedShape = null
        }
        invalidate()
        return true
    }


    private fun isPointInCircle(x: Float, y: Float, shape: Shape, threshold: Float = 50f): Boolean {
        val dx = x - shape.centerX
        val dy = y - shape.centerY
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat() <= shape.radius + threshold
    }

    /*private fun handleEraser(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousTouchX = touchX
                previousTouchY = touchY
                motionPointCount = 0
                eraserPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                motionPointCount += 1
                if (motionPointCount > 1) {
                    val endX = (touchX + previousTouchX) / 2
                    val endY = (touchY + previousTouchY) / 2
                    eraserPath.quadTo(previousTouchX, previousTouchY, endX, endY)
                }
                previousTouchX = touchX
                previousTouchY = touchY
            }
            MotionEvent.ACTION_UP -> {
                eraserPath.lineTo(touchX, touchY)
                // Erase directly on bitmap (pixel-wise)
                drawCanvas.drawPath(eraserPath, eraserPaint)
                eraserPath.reset()
            }
        }
        invalidate()
        return true
    }*/

    private fun handleEraser(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Draw circle immediately to bitmap (no path, no magenta)
                val radius = eraserPaint.strokeWidth / 2
                drawCanvas.drawCircle(touchX, touchY, radius, eraserPaint)
            }
            MotionEvent.ACTION_UP -> {
                // Optional: Final circle at end point
                val radius = eraserPaint.strokeWidth / 2
                drawCanvas.drawCircle(touchX, touchY, radius, eraserPaint)
            }
        }
        invalidate()
        return true
    }


    private fun isPointInShape(x: Float, y: Float, shape: Shape, threshold: Float = 30f): Boolean {
        return when (shape.type) {
            ShapeType.CIRCLE -> {
                val dx = x - shape.centerX; val dy = y - shape.centerY
                sqrt((dx * dx + dy * dy).toDouble()).toFloat() <= shape.radius + threshold
            }
            ShapeType.RECTANGLE -> {
                val left = shape.centerX - shape.width/2; val right = left + shape.width
                val top = shape.centerY - shape.height/2; val bottom = top + shape.height
                x >= left - threshold && x <= right + threshold && y >= top - threshold && y <= bottom + threshold
            }
            ShapeType.LINE -> shape.points.any { point ->
                hypot((x - point.x).toDouble(), (y - point.y).toDouble()).toFloat() <= threshold
            }
            ShapeType.POLYGON -> shape.points.any { point ->
                hypot((x - point.x).toDouble(), (y - point.y).toDouble()).toFloat() <= threshold
            }
        }
    }


    private fun generatePolygonPoints(shape: Shape, radius: Float = 80f) {
        shape.points.clear()
        val angleStep = (2 * kotlin.math.PI / shape.sides).toFloat()

        for (i in 0 until shape.sides) {
            val angle = i * angleStep - kotlin.math.PI.toFloat() / 2  // Start from top
            val x = shape.centerX + radius * kotlin.math.cos(angle).toFloat()
            val y = shape.centerY + radius * kotlin.math.sin(angle).toFloat()
            shape.points.add(PointF(x, y))
        }
    }

}