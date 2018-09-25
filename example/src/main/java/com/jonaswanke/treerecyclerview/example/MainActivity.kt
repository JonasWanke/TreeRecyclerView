package com.jonaswanke.treerecyclerview.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jonaswanke.treerecyclerview.example.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.activity = this
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.main_drawer_open, R.string.main_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        initRecyclerView()
    }

    private fun initRecyclerView() {
        val initialFolders = (1..10).map { createDummyFolder(null, it) }
        val adapter = FolderAdapter(this).apply {
            setItems(initialFolders)
        }

        recyclerView.also {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            it.adapter = adapter
        }
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    fun openHomepage() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jonas-wanke.com")))
    }
}
