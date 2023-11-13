package com.aureusapps.android.expandablelayout.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aureusapps.android.expandablelayout.ExpandableLayout
import com.google.android.material.button.MaterialButton

class XmlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml)

        val expandableLayout = findViewById<ExpandableLayout>(R.id.expandable_layout)
        val button = findViewById<MaterialButton>(R.id.button)

        button.setOnClickListener {
            expandableLayout.toggleExpanded()
        }
        expandableLayout.addExpandStateChangeListener { isExpanded ->
            button.text = if (isExpanded) "Collapse" else "Expand"
        }
    }

}