package com.hurtec.obd2.diagnostics.di

import android.content.Context
import com.hurtec.obd2.diagnostics.obd.communication.BluetoothCommService
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.communication.UsbCommService
import com.hurtec.obd2.diagnostics.obd.communication.WiFiCommService
import com.hurtec.obd2.diagnostics.obd.androbd.AndrObdCommService
import com.hurtec.obd2.diagnostics.obd.elm327.ELM327ProtocolHandler
import com.hurtec.obd2.diagnostics.ui.navigation.NavigationManager
import com.hurtec.obd2.diagnostics.utils.PermissionManager
import com.hurtec.obd2.diagnostics.utils.MemoryManager
import com.hurtec.obd2.diagnostics.hardware.HardwareManager
import com.hurtec.obd2.diagnostics.performance.PerformanceOptimizer
import com.hurtec.obd2.diagnostics.obd.commands.CommandQueue
import com.hurtec.obd2.diagnostics.obd.data.DataProcessor
import com.hurtec.obd2.diagnostics.obd.data.PidInterpreter
import com.hurtec.obd2.diagnostics.obd.data.UnitConverter
import com.hurtec.obd2.diagnostics.obd.data.DataValidator
import com.hurtec.obd2.diagnostics.database.HurtecObdDatabase
import com.hurtec.obd2.diagnostics.database.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.database.repository.ObdDataRepository
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for OBD-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ObdModule {

    @Provides
    @Singleton
    fun provideELM327ProtocolHandler(): ELM327ProtocolHandler {
        return ELM327ProtocolHandler()
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothCommService(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): BluetoothCommService {
        return BluetoothCommService(context, permissionManager)
    }

    @Provides
    @Singleton
    fun provideUsbCommService(@ApplicationContext context: Context): UsbCommService {
        return UsbCommService(context)
    }

    @Provides
    @Singleton
    fun provideWiFiCommService(@ApplicationContext context: Context): WiFiCommService {
        return WiFiCommService(context)
    }

    @Provides
    @Singleton
    fun providePerformanceOptimizer(@ApplicationContext context: Context): PerformanceOptimizer {
        return PerformanceOptimizer(context)
    }

    @Provides
    @Singleton
    fun provideNavigationManager(performanceOptimizer: PerformanceOptimizer): NavigationManager {
        return NavigationManager(performanceOptimizer)
    }

    @Provides
    @Singleton
    fun provideMemoryManager(@ApplicationContext context: Context): MemoryManager {
        return MemoryManager(context)
    }

    @Provides
    @Singleton
    fun provideHardwareManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): HardwareManager {
        return HardwareManager(context, permissionManager)
    }

    @Provides
    @Singleton
    fun provideCommandQueue(): CommandQueue {
        return CommandQueue()
    }

    @Provides
    @Singleton
    fun provideUnitConverter(): UnitConverter {
        return UnitConverter()
    }

    @Provides
    @Singleton
    fun provideDataValidator(): DataValidator {
        return DataValidator()
    }

    @Provides
    @Singleton
    fun providePidInterpreter(): PidInterpreter {
        return PidInterpreter()
    }

    @Provides
    @Singleton
    fun provideDataProcessor(
        pidInterpreter: PidInterpreter,
        unitConverter: UnitConverter,
        dataValidator: DataValidator,
        obdDataRepository: ObdDataRepository
    ): DataProcessor {
        return DataProcessor(pidInterpreter, unitConverter, dataValidator, obdDataRepository)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HurtecObdDatabase {
        return HurtecObdDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVehicleRepository(database: HurtecObdDatabase): VehicleRepository {
        return VehicleRepository(database)
    }

    @Provides
    @Singleton
    fun provideObdDataRepository(database: HurtecObdDatabase): ObdDataRepository {
        return ObdDataRepository(database)
    }

    @Provides
    @Singleton
    fun provideCommunicationManager(
        bluetoothCommService: BluetoothCommService,
        usbCommService: UsbCommService,
        wifiCommService: WiFiCommService,
        commandQueue: CommandQueue,
        dataProcessor: DataProcessor
    ): CommunicationManager {
        return CommunicationManager(
            bluetoothCommService,
            usbCommService,
            wifiCommService,
            commandQueue,
            dataProcessor
        )
    }
}
