package com.nsk.whiteboardtataclassedge.data.model

data class WhiteboardState(
    val strokes: List<DrawStroke>,
    val shapes: List<Shape>,
    val texts: List<TextItem>
)