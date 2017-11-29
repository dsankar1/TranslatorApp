package com.sankar.translatorapp

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View

class Navigation : AppCompatActivity(),
NavigationView.OnNavigationItemSelectedListener,
Chat.OnFragmentInteractionListener,
Archive.OnFragmentInteractionListener,
Settings.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigator)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        openChat()
        navigationView.menu.getItem(0).isChecked = true
    }

    override fun onBackPressed() {
        val drawer : DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id) {
            R.id.nav_chat -> openChat()
            R.id.nav_archive -> openArchive()
            R.id.nav_settings -> openSettings()
        }
        val drawer : DrawerLayout = findViewById(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openChat() {
        supportActionBar!!.title = "Chat"
        val chat = Chat()
        val manager = fragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.content_container, chat)
        transaction.commit()
    }

    private fun openArchive() {
        supportActionBar!!.title = "Archive"
        val archive = Archive()
        val manager = fragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.content_container, archive)
        transaction.commit()
    }

    private fun openSettings() {
        supportActionBar!!.title = "Settings"
        val settings = Settings()
        val manager = fragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.content_container, settings)
        transaction.commit()
    }

    override fun onFragmentInteraction(uri: Uri) {

    }
}
