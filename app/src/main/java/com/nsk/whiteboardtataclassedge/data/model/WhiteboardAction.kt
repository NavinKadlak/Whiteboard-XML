package com.nsk.whiteboardtataclassedge.data.model

sealed class WhiteboardAction {
    data class AddStroke(val stroke: DrawStroke): WhiteboardAction()
    data class RemoveStroke(val stroke: DrawStroke): WhiteboardAction()

    data class AddShape(val shape: Shape): WhiteboardAction()
    data class RemoveShape(val shape: Shape): WhiteboardAction()

    data class AddText(val text: TextItem): WhiteboardAction()
    data class RemoveText(val text: TextItem): WhiteboardAction()

    data class Erase(val previousStrokes: List<DrawStroke>) : WhiteboardAction()
}