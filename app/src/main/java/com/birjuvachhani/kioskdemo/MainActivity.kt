package com.birjuvachhani.kioskdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/*
 * Created by Birju Vachhani on 08 August 2019
 * Copyright © 2019 KioskDemo. All rights reserved.
 */

class MainActivity : AppCompatActivity() {

    private val kioskManager: KioskManager = KioskManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        kioskManager.setKioskMode(true)
    }

}
