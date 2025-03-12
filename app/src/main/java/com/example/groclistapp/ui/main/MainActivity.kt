package com.example.groclistapp.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.groclistapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var bottomNavigationView: BottomNavigationView? = null
    private var navHostFragment: NavHostFragment? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bnvMainNavView)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fcvMainNavHost) as NavHostFragment
        navController = navHostFragment?.navController

        navController?.let { nc ->
            bottomNavigationView?.let { bnv ->
                NavigationUI.setupWithNavController(bnv, nc)
            }

            nc.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.loginFragment, R.id.signupFragment -> bottomNavigationView?.visibility = View.GONE
                    else -> bottomNavigationView?.visibility = View.VISIBLE
                }
            }
        }

        if (true) { // TODO: check if user login in
           navController?.navigate(R.id.loginFragment)
        }
    }
}