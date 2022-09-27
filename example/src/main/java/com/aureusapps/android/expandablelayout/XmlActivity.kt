package com.aureusapps.android.expandablelayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        expandableLayout.addExpandStateChangeListener(object :
            ExpandableLayout.OnExpandStateChangeListener {
            override fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean) {
                button.text = if (isExpanded) "Collapse" else "Expand"
            }
        })
    }
}