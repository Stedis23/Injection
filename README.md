# Injection

## Description

**Injection** is a lightweight dependency injection (DI) library for Kotlin, implementing the **service locator pattern**. It serves as a simple and efficient alternative to more complex DI frameworks like Dagger or Koin, providing a minimalist approach without unnecessary complexity. The library supports factories, singletons, providers, and qualifiers, allowing flexible management of dependency lifecycles.

A key feature is **dependency graph isolation**. Each module creates its own isolated graph, which is not connected to others. This ensures high modularity, prevents dependency leaks, and simplifies testing and code maintenance.

## Features

- **Service Locator Pattern**: Simple access to dependencies through a centralized container.
- **Factories and Singletons**: Support for creating new instances (factories) or shared instances (singletons).
- **Providers**: Lazy initialization of dependencies to avoid circular dependencies.
- **Qualifiers**: Named bindings to resolve type conflicts.
- **Module Isolation**: Each module has its own dependency graph, ensuring encapsulation and modularity.
- **Module Composition**: Ability to combine modules to create dependency hierarchies.
- **Thread Safety**: Lazy initialization and synchronization for multi-threaded environments.
- **Minimalism**: No external dependencies beyond standard Kotlin libraries.

## Installation

Add the library to your project via Gradle:

```gradle
dependencies {
    implementation("io.github.stedis23:injection:1.0.0")
}
```

## Quick Start

### Simple Example

Create a module and register dependencies:

```kotlin
// Define a module
val appModule = module {
    factory { Repository() }
    singleton { Counter }
}

// Retrieve instances
val repository: Repository = appModule.instance()
val counter: Counter = appModule.instance()
```

### Example with Parameters and Qualifiers

```kotlin
val appModule = module {
    factory<Repository>(qualifier = Qualifier("main")) { MainRepositoryImpl() }
    factory<Repository>(qualifier = Qualifier("backup")) { BackupRepositoryImpl() }
    factory { params ->
        UseCase(
            repository = instance(Qualifier("main")),
            id = params.get()
        )
    }
}

// Usage
val useCase: UseCase = appModule.instance("user123")
```

### Example with Providers (Lazy Initialization)

```kotlin
interface Repository {
    
    fun updateData()
}

//We wrap the repository in a provider for lazy initialization
class ViewModel(val repositoryProvider: Provider<Repository>) {

    fun updateData(): String {
        // The module will create a repository instance only after being called from the provider
        return repositoryProvider.get().updateData()
    }
}

val appModule = module {
    factory { Repository() }
    factory { ViewModel(instance()) }
}

// Retrieve provider
val viewModel: ViewModel = appModule.instance()
val data = viewModel.updateData()  // Repository is initialized lazily
```

### Module Composition and Graph Isolation

Each module creates an isolated dependency graph. This means dependencies from one module are not visible in another unless explicitly combined.

```kotlin
// Core module for shared dependencies
val coreModule = module {
    factory { DatabaseConnection() }
    singleton { Logger() }
}

// App module depending on coreModule
val appModule = module(setOf(coreModule)) {
    factory { params ->
        UserService(instance(), instance(), params.get<String>())
    }
}

// Test module — fully isolated
val testModule = module {
    factory { MockDatabaseConnection() }
    singleton { TestLogger() }
}

// Usage
val userService: UserService = appModule.instance("testUser")
// appModule has access to DatabaseConnection and Logger from coreModule

// testModule has no access to dependencies from appModule or coreModule
val mockDb: MockDatabaseConnection = testModule.instance()
```

In this example, `appModule` inherits dependencies from `coreModule`, but `testModule` remains entirely independent. This prevents accidental dependencies and simplifies unit testing.

## API Overview

### Main Classes and Functions

- **`Module`**: Container for dependencies. Created via the `module` function.
- **`module()`**: Function to create a module with optional dependencies from other modules.
- **`factory()`**: Registers a factory for creating new instances.
- **`singleton()`**: Registers a singleton for a shared instance.
- **`instance()`**: Retrieves a dependency instance.
- **`Provider<T>`**: Interface for lazy initialization.
- **`Qualifier`**: Class for naming dependencies.

Detailed documentation is available in the KDOC comments in the code.