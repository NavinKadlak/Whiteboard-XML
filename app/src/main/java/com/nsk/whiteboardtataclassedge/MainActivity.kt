package com.nsk.whiteboardtataclassedge

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nsk.whiteboardtataclassedge.databinding.ActivityMainBinding
import com.nsk.whiteboardtataclassedge.databinding.ColorChooserBinding
import com.nsk.whiteboardtataclassedge.databinding.PolygonSelectorBinding
import com.nsk.whiteboardtataclassedge.ui.viewModel.WhiteboardViewModel
import com.nsk.whiteboardtataclassedge.ui.views.WhiteboardView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WhiteboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWhiteboard()
        setupToolbar()
        observeViewModel()
    }

    private fun setupWhiteboard() {

    }

    private fun setupToolbar() {

        binding.btnDraw.setOnClickListener {
            itemSelector(binding.btnDraw)
            binding.whiteboard.setMode(WhiteboardView.Mode.FREEHAND)
        }

        /*binding.btnRect.setOnClickListener {
            viewModel.setTool(DrawingTool.RECTANGLE)
        }
*/
        binding.btnCircle.setOnClickListener {
            itemSelector(binding.btnCircle)
            getPolygonSelector()
        }

        /*binding.btnLine.setOnClickListener {
            viewModel.setTool(DrawingTool.LINE)
        }

        binding.btnText.setOnClickListener {
            viewModel.setTool(DrawingTool.TEXT)
        }
*/
        binding.btnErase.setOnClickListener {
            itemSelector(binding.btnErase)
            binding.whiteboard.setMode(WhiteboardView.Mode.ERASER)

        }
        binding.btnColor.setOnClickListener {
            getColorPicker()

        }

       /* binding.btnUndo.setOnClickListener {
            viewModel.undo()
        }

        binding.btnRedo.setOnClickListener {
            viewModel.redo()
        }*/
    }

    private fun observeViewModel() {

        /*viewModel.invalidateCanvas.observe(this) {
            binding.whiteboard.invalidate()
        }*/
    }

    private fun itemSelector(view : View){
        binding.btnErase.isSelected =false
        binding.btnDraw.isSelected =false
        binding.btnCircle.isSelected =false
         view.isSelected=true
    }

    fun getPolygonSelector(){
       val dialogBinding = PolygonSelectorBinding.inflate(LayoutInflater.from(this@MainActivity))
        val dialog = Dialog(this@MainActivity).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
        }

        dialog.show()

        dialogBinding.rectangle.setOnClickListener {
            binding.whiteboard.setMode(WhiteboardView.Mode.RECTANGLE)
            dialog.cancel()
        }
        dialogBinding.circle.setOnClickListener {
            binding.whiteboard.setMode(WhiteboardView.Mode.CIRCLE)
            dialog.cancel()
        }
        dialogBinding.line.setOnClickListener {
            binding.whiteboard.setMode(WhiteboardView.Mode.LINE)
            dialog.cancel()
        }
        dialogBinding.polygon.setOnClickListener {
            binding.whiteboard.setMode(WhiteboardView.Mode.POLYGON)
            dialog.cancel()
        }

    }

    fun getColorPicker(){
        val dialogBinding = ColorChooserBinding.inflate(LayoutInflater.from(this@MainActivity))
        val dialog = Dialog(this@MainActivity).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
        }

        dialog.show()

        dialogBinding.col1.setOnClickListener {
            val chosenColorBox = dialogBinding.col1.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            binding.whiteboard.setColor(colorId)
            dialog.cancel()
        }
        dialogBinding.col2.setOnClickListener {
            val chosenColorBox = dialogBinding.col2.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            binding.whiteboard.setColor(colorId)
            dialog.cancel()
        }
        dialogBinding.col3.setOnClickListener {
            val chosenColorBox = dialogBinding.col3.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            binding.whiteboard.setColor(colorId)
            dialog.cancel()
        }
        dialogBinding.col4.setOnClickListener {
            val chosenColorBox = dialogBinding.col4.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            binding.whiteboard.setColor(colorId)
            dialog.cancel()
        }

    }
}