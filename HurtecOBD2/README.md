# Hurtec OBD-II Diagnostics App

A modern, feature-rich Android application for OBD-II vehicle diagnostics built with Jetpack Compose and modern Android development practices.

## 🚗 Features

### Core Functionality
- **Real-time Dashboard**: Animated gauges showing engine RPM, speed, load, and temperature
- **Device Connection**: Bluetooth and USB OBD-II adapter support with auto-discovery
- **Diagnostic Trouble Codes**: Read, display, and clear DTCs with detailed descriptions
- **Freeze Frame Data**: Access freeze frame data for stored diagnostic codes
- **Readiness Monitors**: Check emission system readiness status
- **Data Persistence**: Store diagnostic data and vehicle information locally

### User Experience
- **Modern UI**: Material Design 3 with automotive-inspired theming
- **Smooth Animations**: Spring-based animations and transitions throughout the app
- **Dark/Light Theme**: System-aware theme switching
- **Comprehensive Settings**: Customizable units, data retention, and app preferences
- **Data Export**: Export diagnostic data for analysis

### Technical Features
- **Real OBD Communication**: Actual Bluetooth and USB communication with OBD-II adapters
- **ELM327 Protocol**: Full ELM327 command support and response parsing
- **Room Database**: Local data storage with relationships and migrations
- **Dependency Injection**: Hilt for clean architecture and testability
- **Coroutines**: Async operations with proper error handling
- **MVVM Architecture**: Clean separation of concerns with ViewModels

## 🏗️ Architecture

### Tech Stack
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room with SQLite
- **Async**: Kotlin Coroutines and Flow
- **Communication**: Bluetooth Classic and USB Serial
- **Testing**: JUnit, Mockito, Compose Testing

### Project Structure
```
app/src/main/java/com/hurtec/obd2/diagnostics/
├── data/
│   ├── database/          # Room database, entities, DAOs
│   └── repository/        # Data repositories
├── di/                    # Hilt dependency injection modules
├── obd/
│   ├── communication/     # Bluetooth/USB communication managers
│   ├── elm327/           # ELM327 protocol handler
│   └── processor/        # OBD data processing
└── ui/
    ├── components/       # Reusable UI components and animations
    ├── screens/         # Screen composables and ViewModels
    └── theme/           # App theming and styling
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35
- Kotlin 1.9.0+
- Gradle 8.2+

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/hurtec-obd2.git
   cd hurtec-obd2
   ```

2. Open in Android Studio and sync the project

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

### Hardware Requirements
- Android device with API level 24+ (Android 7.0)
- Bluetooth support for wireless OBD-II adapters
- USB OTG support for wired OBD-II adapters (optional)

## 📱 Usage

### Connecting to OBD-II Adapter
1. Navigate to the **Connection** screen
2. Tap **Scan** to discover available adapters
3. Select your OBD-II adapter from the list
4. Tap **Connect** to establish connection

### Reading Diagnostic Data
1. Ensure adapter is connected
2. Go to **Dashboard** for real-time data
3. Visit **Diagnostics** to read trouble codes
4. Use **Settings** to customize units and preferences

### Supported OBD-II Adapters
- ELM327-based Bluetooth adapters
- ELM327-based USB adapters
- Compatible with most vehicles manufactured after 1996 (OBD-II standard)

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Test Coverage
- Unit tests for ViewModels and business logic
- Integration tests for UI components
- Mock testing for OBD communication
- Database testing with Room

## 🔧 Configuration

### Build Variants
- **Debug**: Development build with logging enabled
- **Release**: Production build with optimizations

### Customization
- Modify `ui/theme/` for custom styling
- Update `obd/elm327/` for additional OBD commands
- Extend `data/database/` for new data types

## 📊 Performance

### Optimizations
- Lazy loading for large datasets
- Efficient database queries with Room
- Coroutine-based async operations
- Memory-efficient image handling
- Optimized animations with Compose

### Monitoring
- Built-in error handling and logging
- Performance metrics collection
- Memory leak prevention
- Battery usage optimization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Write tests for new features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [ELM327 Protocol Documentation](https://www.elmelectronics.com/)
- [OBD-II Standard](https://en.wikipedia.org/wiki/OBD-II_PIDs)
- [Android Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Email: support@hurtecdiagnostics.com
- Documentation: [Wiki](https://github.com/your-username/hurtec-obd2/wiki)

---

**Hurtec OBD-II Diagnostics** - Professional vehicle diagnostics at your fingertips.
