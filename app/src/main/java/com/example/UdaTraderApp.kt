package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.pref.AppSettings
import com.example.data.repository.TradingRepository

class UdaTraderApp : Application() {

    lateinit var appSettings: AppSettings
        private set

    lateinit var repository: TradingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(this)
        repository = TradingRepository.getInstance(this)
    }
}
