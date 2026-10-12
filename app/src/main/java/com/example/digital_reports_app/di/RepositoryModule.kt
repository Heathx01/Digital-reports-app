package com.example.digital_reports_app.di

import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.data.repository.AuthRepositoryImpl
import com.example.digital_reports_app.data.repository.ReportRepository
import com.example.digital_reports_app.data.repository.ReportRepositoryImpl
import com.example.digital_reports_app.data.repository.TemplateRepository
import com.example.digital_reports_app.data.repository.TemplateRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(
        reportRepositoryImpl: ReportRepositoryImpl
    ): ReportRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        templateRepositoryImpl: TemplateRepositoryImpl
    ): TemplateRepository
}
