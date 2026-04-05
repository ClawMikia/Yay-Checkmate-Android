package com.yaycheckmate.app

import android.app.Application
import com.yaycheckmate.app.data.AppDatabase
import com.yaycheckmate.app.data.LostObjectRepository
import com.yaycheckmate.app.ml.TfliteImageEmbedder

class YayCheckmateApp : Application() {

    lateinit var embedder: TfliteImageEmbedder
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var repository: LostObjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        embedder = TfliteImageEmbedder(this)
        database = AppDatabase.build(this)
        repository = LostObjectRepository(this, database.lostObjectDao(), embedder)
    }
}
