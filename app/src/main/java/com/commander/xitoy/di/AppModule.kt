package com.commander.xitoy.di

import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.data.remote.XitoyApi
import com.commander.xitoy.data.repository.ProductRepositoryImpl
import com.commander.xitoy.domain.repository.ProductRepository
import com.commander.xitoy.domain.use_case.GetProductsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideXitoyApi(): XitoyApi {
        val APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwYNusH54O3kyMAVcdkzpOaBiejRLrvj6EcXtfgh1m37aG79ZiUYRG_OcOEUa3GSkFi8A/"

        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(APPS_SCRIPT_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XitoyApi::class.java)
    }

    // Telegram login API — admin.eliboyev.uz serveri (Apps Script dan alohida base URL)
    @Provides
    @Singleton
    fun provideAuthApi(): AuthApi {
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://admin.eliboyev.uz/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderApi(): OrderApi {
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://admin.eliboyev.uz/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrderApi::class.java)
    }

    // 2. Repository ga Api ni ulab beramiz
    @Provides
    @Singleton
    fun provideProductRepository(api: XitoyApi): ProductRepository {
        return ProductRepositoryImpl(api)
    }

    // 3. Use Case ga Repository ni ulab beramiz
    @Provides
    @Singleton
    fun provideGetProductsUseCase(repository: ProductRepository): GetProductsUseCase {
        return GetProductsUseCase(repository)
    }
}