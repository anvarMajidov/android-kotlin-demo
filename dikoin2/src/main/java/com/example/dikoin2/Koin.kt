package com.example.dikoin2

import android.app.Application
import com.example.dikoin.GreetingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module


val appModule = module {
    single<GreetingRepository> { GreetingRepositoryImpl() }
    viewModel { GreetingViewModel(get()) }
}

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(appModule)
        }
    }
}
