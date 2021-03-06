package com.example.aop.part5.chapter_05.di

import android.app.Activity
import com.example.aop.part5.chapter_05.data.api.StationApi
import com.example.aop.part5.chapter_05.data.api.StationStorageApi
import com.example.aop.part5.chapter_05.data.db.AppDatabase
import com.example.aop.part5.chapter_05.data.preference.PreferenceManager
import com.example.aop.part5.chapter_05.data.preference.SharedPreferenceManager
import com.example.aop.part5.chapter_05.data.repository.StationRepository
import com.example.aop.part5.chapter_05.data.repository.StationRepositoryImpl
import com.example.aop.part5.chapter_05.presentation.stations.StationsContract
import com.example.aop.part5.chapter_05.presentation.stations.StationsFragment
import com.example.aop.part5.chapter_05.presentation.stations.StationsPresenter
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

	single { Dispatchers.IO }

	// Database
	single { AppDatabase.build(androidApplication()) }
	single { get<AppDatabase>().stationDao() }

	// Preference
	single { androidContext().getSharedPreferences("preference", Activity.MODE_PRIVATE) }
	single<PreferenceManager> { SharedPreferenceManager(get()) }

	// Api
	single<StationApi> { StationStorageApi(Firebase.storage) }

	// Repository
	single<StationRepository> { StationRepositoryImpl(get(), get(), get(), get()) }

	// Presentation
	scope<StationsFragment> {
		scoped<StationsContract.Presenter> { StationsPresenter(getSource(), get()) }
	}
}