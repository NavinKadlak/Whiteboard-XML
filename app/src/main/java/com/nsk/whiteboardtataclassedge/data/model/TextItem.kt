package com.nsk.whiteboardtataclassedge.data.model


import android.graphics.PointF
import java.util.UUID

data class TextItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var position: PointF,
    val color: Int,
    val size: Float
)