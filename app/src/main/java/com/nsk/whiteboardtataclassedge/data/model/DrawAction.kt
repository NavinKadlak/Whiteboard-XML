package com.nsk.whiteboardtataclassedge.data.model

sealed class DrawAction {
    data class Stroke(val stroke: DrawStroke) : DrawAction()
    data class ShapeDraw(val shape: Shape) : DrawAction()
}