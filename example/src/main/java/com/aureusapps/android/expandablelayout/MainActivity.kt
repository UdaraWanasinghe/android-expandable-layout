package com.aureusapps.android.expandablelayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val expandableLayout = findViewById<ExpandableLayout>(R.id.expandable_layout)
        val button = findViewById<MaterialButton>(R.id.button)

        button.setOnClickListener {
            expandableLayout.toggleExpanded()
        }
        expandableLayout.addStateChangeListener(object : ExpandableLayout.OnStateChangeListener {
            override fun onStateChanged(expandableLayout: ExpandableLayout, isExpanded: Boolean) {
                button.text = if (isExpanded) "Collapse" else "Expand"
            }
        })
    }
}