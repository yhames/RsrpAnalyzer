package com.example.rsrpanalyzer.view.navigation

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.rsrpanalyzer.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavBar(private val activity: AppCompatActivity) {

    private val bottomNavigationView: BottomNavigationView = activity.findViewById(R.id.bottom_navigation)

    fun setup() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            handleNavigation(item)
        }
        // Set initial selection
        bottomNavigationView.menu.findItem(R.id.navigation_map).isChecked = true
        bottomNavigationView.menu.findItem(R.id.navigation_realtime).isChecked = true

        // Manually trigger the listener for the initial items to apply their logic
        handleNavigation(bottomNavigationView.menu.findItem(R.id.navigation_map))
        handleNavigation(bottomNavigationView.menu.findItem(R.id.navigation_realtime))
    }

    private fun handleNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_map -> {
                // TODO: Map view logic
                return true
            }
            R.id.navigation_data -> {
                // TODO: Data view logic
                return true
            }
            R.id.navigation_realtime -> {
                // TODO: Real-time logic
                return true
            }
            R.id.navigation_history -> {
                // TODO: History logic
                return true
            }
        }
        return false
    }
}
