package by.ciszkin.herdmanager.di

import by.ciszkin.herdmanager.data.repository.RegistryRepositoryImpl
import by.ciszkin.herdmanager.domain.repository.RegistryRepository

actual fun getRegistryRepository(): RegistryRepository = RegistryRepositoryImpl()
