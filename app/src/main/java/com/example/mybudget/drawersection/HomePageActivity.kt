package com.example.mybudget.drawersection

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.mybudget.R
import com.example.mybudget.databinding.HomePageBinding
import com.example.mybudget.start_pages.StartActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class HomePageActivity : AppCompatActivity(){

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: HomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_finance, R.id.nav_goals, R.id.nav_loans, R.id.nav_subs, R.id.nav_setting), drawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.exitButton.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            intent.putExtra("exit", true)
            startActivity(intent)
            this.finishAffinity()
        }

        val headerView = navView.getHeaderView(0)
        val user = Firebase.auth.currentUser
        if (user != null && user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            headerView.findViewById<TextView>(R.id.userNameHeader).text = Firebase.auth.currentUser?.displayName
            val imageView: ImageView = headerView.findViewById(R.id.imageView)
            val photoUrl = user.photoUrl
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.personiconpng)
                .error(R.drawable.personiconpng)
                .into(imageView)
        } else{
            headerView.findViewById<TextView>(R.id.userNameHeader).text = Firebase.auth.currentUser?.email
        }
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_page, menu)
        return true
    }*/

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.newGLSFragment) {
                title(arguments?.getString("key"), arguments?.getString("type"))
            }
        }
        navController.addOnDestinationChangedListener(listener)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun title(key:String?, type:String?){
        if (key == null){
            supportActionBar?.title = when(type){
                "loan"-> "Новый обязательный платеж"
                "goal"-> "Новая цель"
                else -> "Новая подписка"
            }
        } else {
           supportActionBar?.title = when(type){
                "loan"-> "Редактирования обязательного платежа"
                "goal"-> "Редактирование цели"
                else -> "Редактирование подписки"
            }
        }
    }
}