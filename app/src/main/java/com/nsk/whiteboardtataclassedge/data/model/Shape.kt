package com.nsk.whiteboardtataclassedge.data.model


import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import java.util.UUID

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    LINE,
    POLYGON
}

data class Shape(
    val id: String = UUID.randomUUID().toString(),
//    var centerX: Float, var centerY: Float, var radius: Float, var color: Int = Color.BLACK
    val type: ShapeType,
    var centerX: Float,
    var centerY: Float,
    var width: Float = 100f,    // For rect/line
    var height: Float = 100f,   // For rect
    val points: MutableList<PointF> = mutableListOf(),  // For polygon/line
    var color: Int = Color.BLUE,
    var radius: Float = 100f,     // For circle
    var sides: Int = 6  // ← NEW: 4=square, 5=pentagon, 6=hexagon
)