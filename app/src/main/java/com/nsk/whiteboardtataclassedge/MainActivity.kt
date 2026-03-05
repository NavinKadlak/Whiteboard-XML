package com.nsk.whiteboardtataclassedge

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nsk.whiteboardtataclassedge.data.model.TextItem
import com.nsk.whiteboardtataclassedge.databinding.ActivityMainBinding
import com.nsk.whiteboardtataclassedge.databinding.ColorChooserBinding
import com.nsk.whiteboardtataclassedge.databinding.PolygonSelectorBinding
import com.nsk.whiteboardtataclassedge.data.model.ToolType
import com.nsk.whiteboardtataclassedge.ui.viewModel.WhiteboardViewModel
import com.nsk.whiteboardtataclassedge.ui.views.WhiteboardView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WhiteboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(binding.root)


        hideStatusBar()
        setupWhiteboard()
        setupToolbar()
        observeViewModel()
    }

    private fun setupWhiteboard() {
        // Bind custom view to ViewModel for MVVM
        binding.whiteboard.bind(this, viewModel)
    }

    private fun setupToolbar() {

        binding.btnDraw.isSelected =true
        binding.btnDraw.setOnClickListener {
            itemSelector(binding.btnDraw)
            viewModel.setTool(ToolType.DRAW)
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
            viewModel.setTool(ToolType.ERASER)
        }
        binding.btnColor.setOnClickListener {
            getColorPicker()

        }

        binding.btnUndo.setOnClickListener {
            viewModel.undo()
        }

        binding.btnRedo.setOnClickListener {
            viewModel.redo()
        }
        binding.btnText.setOnClickListener {
            itemSelector(binding.btnText)

            viewModel.setTool(ToolType.TEXT)
        }

        binding.btnSave.setOnClickListener {
            saveJsonFile()
        }

        binding.btnImport.setOnClickListener {

            pickJsonLauncher.launch("application/json")
        }
    }

    private fun observeViewModel() {

        /*viewModel.invalidateCanvas.observe(this) {
            binding.whiteboard.invalidate()
        }*/


        binding.whiteboard.onTextRequested = { point ->
            showTextDialog(point)
        }
    }

    private fun itemSelector(view : View){
        binding.btnErase.isSelected =false
        binding.btnDraw.isSelected =false
        binding.btnCircle.isSelected =false
        binding.btnText.isSelected =false
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
            viewModel.setTool(ToolType.RECTANGLE)
            dialog.cancel()
        }
        dialogBinding.circle.setOnClickListener {
            viewModel.setTool(ToolType.CIRCLE)
            dialog.cancel()
        }
        dialogBinding.line.setOnClickListener {
            viewModel.setTool(ToolType.LINE)
            dialog.cancel()
        }
        dialogBinding.polygon.setOnClickListener {
            viewModel.setTool(ToolType.POLYGON)
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
            viewModel.setColor(colorId)
            dialog.cancel()
            binding.btnColor.setBackgroundColor(colorId)
        }
        dialogBinding.col2.setOnClickListener {
            val chosenColorBox = dialogBinding.col2.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            viewModel.setColor(colorId)
            dialog.cancel()
            binding.btnColor.setBackgroundColor(colorId)

        }
        dialogBinding.col3.setOnClickListener {
            val chosenColorBox = dialogBinding.col3.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            viewModel.setColor(colorId)
            dialog.cancel()
            binding.btnColor.setBackgroundColor(colorId)

        }
        dialogBinding.col4.setOnClickListener {
            val chosenColorBox = dialogBinding.col4.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            viewModel.setColor(colorId)
            dialog.cancel()
            binding.btnColor.setBackgroundColor(colorId)

        }

        dialogBinding.col5.setOnClickListener {
            val chosenColorBox = dialogBinding.col5.getBackground() as ColorDrawable
            val colorId = chosenColorBox.color
            viewModel.setColor(colorId)
            dialog.cancel()
            binding.btnColor.setBackgroundColor(colorId)

        }

    }

    private fun showTextDialog(point: PointF) {

        val editText = EditText(this)
        editText.hint = "Enter text"

        AlertDialog.Builder(this)
            .setTitle("Add Text")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->

                val text = editText.text.toString()

                if (text.isNotEmpty()) {

                    viewModel.addText(
                        TextItem(
                            id = UUID.randomUUID().toString(),
                            text = text,
                            position = PointF(point.x, point.y),
                            color = viewModel.color.value,
                            size = 20f
                        )
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun saveJsonFile() {

        val json = viewModel.exportWhiteboard()

        saveJsonToDownloads(this@MainActivity,json)
    }

    private val pickJsonLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            uri ?: return@registerForActivityResult

            val inputStream = contentResolver.openInputStream(uri)

            val json = inputStream?.bufferedReader().use { it?.readText() }

            json?.let {
                viewModel.importWhiteboard(it)
            }
        }

    fun saveJsonToDownloads(context: Context, json: String) {


        val fileName = generateFileName()

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { output ->
                output.write(json.toByteArray())

                runOnUiThread {
                    Toast.makeText(this@MainActivity,"file saved", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = formatter.format(Date())
        return "whiteboard_${timestamp}.json"
    }

    private fun hideStatusBar() {

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(WindowInsetsCompat.Type.statusBars())

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}