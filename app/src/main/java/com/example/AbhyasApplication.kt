package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.StudyRepository

class AbhyasApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: StudyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "abhyas_study_planner_db"
        )
        .fallbackToDestructiveMigration(true) // ensures simple upgrades for dev
        .build()

        repository = StudyRepository(database)
    }
}
