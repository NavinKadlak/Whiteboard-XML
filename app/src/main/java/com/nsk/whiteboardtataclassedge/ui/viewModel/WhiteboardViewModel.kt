package com.nsk.whiteboardtataclassedge.ui.viewModel

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import com.nsk.whiteboardtataclassedge.data.model.DrawAction
import com.nsk.whiteboardtataclassedge.data.model.DrawStroke
import com.nsk.whiteboardtataclassedge.data.model.Shape
import com.nsk.whiteboardtataclassedge.data.model.TextItem
import com.nsk.whiteboardtataclassedge.data.model.ToolType
import com.nsk.whiteboardtataclassedge.data.model.WhiteboardAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WhiteboardViewModel : ViewModel() {

    private val _strokes = MutableStateFlow<List<DrawStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    private val _shapes = MutableStateFlow<List<Shape>>(emptyList())
    val shapes = _shapes.asStateFlow()

    private val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    val texts = _texts.asStateFlow()

    // Current drawing tool and color exposed to the View layer
    private val _toolType = MutableStateFlow(ToolType.DRAW)
    val toolType = _toolType.asStateFlow()

    private val _color = MutableStateFlow(android.graphics.Color.BLACK)
    val color = _color.asStateFlow()

    var isEraserMode = false

    private val undoStack = ArrayDeque<WhiteboardAction>()
    private val redoStack = ArrayDeque<WhiteboardAction>()

    // ---------- DRAW ----------

    fun addStroke(points: List<PointF>) {
        val stroke = DrawStroke(
            points = points,
            color = _color.value,
            width = 8f
        )

        _strokes.value += stroke
        undoStack.addLast(WhiteboardAction.AddStroke(stroke))
        redoStack.clear()
    }

    // ---------- ERASER ----------

    /*fun erase(point: PointF) {
        val target = _strokes.value.firstOrNull {
            it.points.any { p ->
                distance(p, point) < 60
            }
        }

        target?.let {
            _strokes.value -= it
            undoStack.addLast(
                WhiteboardAction.RemoveStroke(it)
            )
        }
    }*/
    fun erase(point: PointF) {

        val newStrokes = mutableListOf<DrawStroke>()
        val removedStrokes = mutableListOf<DrawStroke>()

        _strokes.value.forEach { stroke ->

            val remainingSegments = mutableListOf<MutableList<PointF>>()
            var currentSegment = mutableListOf<PointF>()

            stroke.points.forEach { p ->

                if (distance(p, point) > 30f) {
                    currentSegment.add(p)
                } else {
                    // break stroke here
                    if (currentSegment.isNotEmpty()) {
                        remainingSegments.add(currentSegment)
                        currentSegment = mutableListOf()
                    }
                }
            }

            if (currentSegment.isNotEmpty()) {
                remainingSegments.add(currentSegment)
            }

            if (remainingSegments.isEmpty()) {
                removedStrokes.add(stroke)
            } else {
                remainingSegments.forEach { segment ->
                    if (segment.size > 1) {
                        newStrokes.add(
                            stroke.copy(points = segment)
                        )
                    }
                }
            }
        }

        _strokes.value = newStrokes
    }

    // ---------- SHAPES ----------

    fun addShape(shape: Shape) {
        _shapes.value += shape
        undoStack.addLast(WhiteboardAction.AddShape(shape))
        redoStack.clear()
    }

    fun updateShape(updated: Shape) {
        _shapes.value = _shapes.value.map { existing ->
            if (existing.id == updated.id) updated else existing
        }
    }

    // ---------- TEXT ----------

    fun addText(text: TextItem) {
        _texts.value += text
        undoStack.addLast(WhiteboardAction.AddText(text))
    }

    // ---------- UNDO ----------

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeLast()
        when (action) {

            is WhiteboardAction.AddStroke ->
                _strokes.value -= action.stroke

            is WhiteboardAction.RemoveStroke ->
                _strokes.value += action.stroke

            is WhiteboardAction.AddShape ->
                _shapes.value -= action.shape

            is WhiteboardAction.AddText ->
                _texts.value -= action.text

            else -> {""}
        }

        redoStack.addLast(action)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeLast()

        when (action) {
            is WhiteboardAction.AddStroke ->
                _strokes.value += action.stroke

            is WhiteboardAction.RemoveStroke ->
                _strokes.value -= action.stroke

            is WhiteboardAction.AddShape ->
                _shapes.value += action.shape

            is WhiteboardAction.AddText ->
                _texts.value += action.text

            else -> {""}
        }

        undoStack.addLast(action)
    }

    // ---------- TOOL / COLOR ----------

    fun setTool(toolType: ToolType) {
        _toolType.value = toolType
        isEraserMode = toolType == ToolType.ERASER
    }

    fun setColor(color: Int) {
        _color.value = color
    }

    private fun distance(a: PointF, b: PointF): Float {
        return kotlin.math.sqrt(
            (a.x - b.x)*(a.x - b.x) +
                    (a.y - b.y)*(a.y - b.y)
        )
    }

    fun updateText(updated: TextItem) {
        _texts.value = _texts.value.map {
            if (it.id == updated.id) updated else it
        }
    }
}