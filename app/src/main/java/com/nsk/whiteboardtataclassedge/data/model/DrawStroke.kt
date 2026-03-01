package com.nsk.whiteboardtataclassedge.data.model

import android.graphics.Path
import android.graphics.PointF
import java.util.UUID

data class DrawStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointF>,
    val color: Int,
    val width: Float
)