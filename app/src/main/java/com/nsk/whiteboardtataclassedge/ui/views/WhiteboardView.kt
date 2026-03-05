package com.nsk.whiteboardtataclassedge.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nsk.whiteboardtataclassedge.data.model.DrawStroke
import com.nsk.whiteboardtataclassedge.data.model.Shape
import com.nsk.whiteboardtataclassedge.data.model.ShapeType
import com.nsk.whiteboardtataclassedge.data.model.TextItem
import com.nsk.whiteboardtataclassedge.data.model.ToolType
import com.nsk.whiteboardtataclassedge.ui.viewModel.WhiteboardViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt


class WhiteboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var viewModel: WhiteboardViewModel? = null

    var onTextRequested: ((PointF) -> Unit)? = null
    var c = Color.BLACK

    // Freehand drawing
    private val drawPath = Path()
    private val currentStrokePoints = mutableListOf<PointF>()

    // Shapes cached from ViewModel
    private var cachedStrokes: List<DrawStroke> = emptyList()
    private var cachedShapes: List<Shape> = emptyList()
    private var cachedText: List<TextItem> = emptyList()

    private var selectedShape: Shape? = null
    private var currentMode = Mode.FREEHAND

    enum class Mode { FREEHAND, CIRCLE , RECTANGLE , LINE , POLYGON , ERASER, UNDO, REDO, TEXT }

    private var previousTouchX = 0f
    private var previousTouchY = 0f
    private var motionPointCount = 0


    private var selectedText: TextItem? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    fun bind(lifecycleOwner: LifecycleOwner, viewModel: WhiteboardViewModel) {
        this.viewModel = viewModel

        lifecycleOwner.lifecycleScope.launch {
            viewModel.toolType.collect { tool ->
                currentMode = when (tool) {
                    ToolType.DRAW -> Mode.FREEHAND
                    ToolType.ERASER -> Mode.ERASER
                    ToolType.RECTANGLE -> Mode.RECTANGLE
                    ToolType.CIRCLE -> Mode.CIRCLE
                    ToolType.LINE -> Mode.LINE
                    ToolType.POLYGON -> Mode.POLYGON
                    ToolType.TEXT -> Mode.TEXT
                }
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            viewModel.color.collect { color ->
                c = color
                invalidate()
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            viewModel.strokes.collect { strokes ->
                cachedStrokes = strokes
                invalidate()
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            viewModel.shapes.collect { shapes ->
                cachedShapes = shapes
                invalidate()
            }
        }
        lifecycleOwner.lifecycleScope.launch {
            viewModel.texts.collect { texts ->
                cachedText = texts
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw persisted strokes from ViewModel
       /* cachedStrokes.forEach { stroke ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.color
                style = Paint.Style.STROKE
                strokeWidth = stroke.width
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            if (stroke.points.size > 1) {
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                canvas.drawPath(path, paint)
            } else if (stroke.points.size == 1) {
                val p = stroke.points[0]
                canvas.drawPoint(p.x, p.y, paint)
            }
        }*/

        cachedStrokes.forEach { stroke ->
            drawSmoothStroke(canvas, stroke)
        }

        // Draw current in-progress freehand path
        if (currentMode == Mode.FREEHAND) {
            val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 5f
            }

            canvas.drawPath(drawPath, drawPaint)
        }

        // Shapes from ViewModel
        cachedShapes.forEach { shape ->
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

        // Draw text

         val textPaint = Paint().apply {
            textSize = 48f
            isAntiAlias = true
        }

        cachedText.forEach { textItem ->

            textPaint.color = textItem.color

            canvas.drawText(
                textItem.text,
                textItem.position.x,
                textItem.position.y,
                textPaint
            )
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
            Mode.TEXT -> handleText(event, touchX, touchY)

        }
    }

    private fun handleFreehand(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousTouchX = touchX
                previousTouchY = touchY
                motionPointCount = 0
                drawPath.reset()
                drawPath.moveTo(touchX, touchY)
                currentStrokePoints.clear()
                currentStrokePoints.add(PointF(touchX, touchY))
            }
            MotionEvent.ACTION_MOVE -> {

                val dx = kotlin.math.abs(touchX - previousTouchX)
                val dy = kotlin.math.abs(touchY - previousTouchY)

                if (dx >= 4f || dy >= 4f) {

                    val midX = (touchX + previousTouchX) / 2
                    val midY = (touchY + previousTouchY) / 2

                    drawPath.quadTo(previousTouchX, previousTouchY, midX, midY)

                    previousTouchX = touchX
                    previousTouchY = touchY

                    currentStrokePoints.add(PointF(touchX, touchY))
                }
            }
            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(touchX, touchY)  // Finish curve
                currentStrokePoints.add(PointF(touchX, touchY))

                viewModel?.let { vm ->
                    if (currentStrokePoints.isNotEmpty()) {
                        vm.addStroke(currentStrokePoints.toList())
                    }
                }

                drawPath.reset()
                currentStrokePoints.clear()
            }
        }
        invalidate()
        return true
    }

    private fun handleEraser(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                // Delegate erase logic to ViewModel (removes nearby strokes)
                viewModel?.erase(PointF(touchX, touchY))
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

    // Add shapes via ViewModel so state is shared with the rest of the app
    private fun addShape(type: ShapeType, x: Float, y: Float, sides : Int = 4): Shape? {
        val shape = when (type) {
            ShapeType.CIRCLE -> Shape(type = ShapeType.CIRCLE, centerX = x, centerY = y, color = c)
            ShapeType.RECTANGLE -> Shape(type = ShapeType.RECTANGLE, centerX = x, centerY = y, width = 220f, height = 160f, color = c)
            ShapeType.LINE -> Shape(type = ShapeType.LINE, centerX = x, centerY = y, height = 300f, radius = 0f,color = c).apply {
                points.add(PointF(x-75f, y)); points.add(PointF(x+75f, y))
            }
            ShapeType.POLYGON -> {
                val polygon = Shape(type = ShapeType.POLYGON, centerX = x, centerY = y, sides = sides, color = c)
                generatePolygonPoints(polygon)  // Auto-generate vertices
                polygon
            }
        }
        viewModel?.addShape(shape)
        return shape
    }

    private fun handleShape(event: MotionEvent, touchX: Float, touchY: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedShape = cachedShapes.find { isPointInShape(touchX, touchY, it) }
                if (selectedShape == null) {
                    // Add new shape based on current mode
                    selectedShape = when (currentMode) {
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
                    viewModel?.updateShape(shape)
                }
            }
            MotionEvent.ACTION_UP -> selectedShape = null
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

    private fun handleText(
        event: MotionEvent,
        touchX: Float,
        touchY: Float
    ): Boolean {

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                selectedText = findTouchedText(touchX, touchY)

                if (selectedText == null) {
                    // create new text
                    onTextRequested?.invoke(PointF(touchX, touchY))
                } else {
                    lastTouchX = touchX
                    lastTouchY = touchY
                }
            }

            MotionEvent.ACTION_MOVE -> {

                selectedText?.let { text ->

                    val dx = touchX - lastTouchX
                    val dy = touchY - lastTouchY

                    val updated = text.copy(
                        position = PointF(
                            text.position.x + dx,
                            text.position.y + dy
                        )
                    )

                    viewModel?.updateText(updated)

                    selectedText = updated
                    lastTouchX = touchX
                    lastTouchY = touchY
                }
            }

            MotionEvent.ACTION_UP -> {
                selectedText = null
            }
        }

        return true
    }

    private fun findTouchedText(x: Float, y: Float): TextItem? {

        return cachedText.lastOrNull { text ->

            val textWidth = text.text.length * text.size
            val textHeight = text.size

            x in text.position.x..(text.position.x + textWidth) &&
                    y in (text.position.y - textHeight)..text.position.y
        }
    }

    private fun drawSmoothStroke(canvas: Canvas, stroke: DrawStroke) {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color
            style = Paint.Style.STROKE
            strokeWidth = stroke.width
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        val points = stroke.points

        if (points.isEmpty()) return

        val path = Path()

        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {

            val prev = points[i - 1]
            val curr = points[i]

            val midX = (prev.x + curr.x) / 2
            val midY = (prev.y + curr.y) / 2

            path.quadTo(prev.x, prev.y, midX, midY)
        }

        canvas.drawPath(path, paint)
    }

}