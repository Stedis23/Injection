package com.stedis.injection

import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.forEach
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Creates a new [Module] with the specified dependencies and declaration.
 * This function allows composing multiple modules into a single one.
 * Each module creates an isolated dependency graph that is not connected to other graphs,
 * ensuring encapsulation, modularity, and preventing unintended cross-dependencies.
 *
 * @param dependencies A set of parent modules whose declarations will be included.
 * @param declaration The module declaration defining factories and singletons for this module.
 * @return A new [Module] instance.
 *
 * Example usage:
 * ```
 * val coreModule = module {
 *     factory { Repository() }
 *     singleton { Counter }
 * }
 *
 * val appModule = module(setOf(coreModule)) {
 *     factory { params ->
 *         UseCase(repository = instance(), id = params.get<String>())
 *     }
 *     factory { ViewModel(counter = instance()) }
 * }
 *
 * val useCase: UseCase = appModule.instance("exampleId")
 * val viewModel: ViewModel = appModule.instance()
 * ```
 */
fun module(dependencies: Set<Module> = emptySet(), declaration: ModuleDeclaration): Module {
    val declarations = mutableListOf<ModuleDeclaration>()

    dependencies.forEach { declarations.addAll(it.getDependencies()) }
    declarations.add(declaration)

    return Module(declarations)
}

/**
 * Represents a module containing dependency declarations and a factory map for resolving instances.
 * Modules can be composed together to build a hierarchy of dependencies.
 *
 * @property dependencies A mutable list of module declarations that define the factories and singletons.
 * @property factoryMap A lazily initialized, thread-safe map of factories keyed by type and qualifier.
 *                      This map is built from the module declarations and is used for dependency resolution.
 */
class Module(private val dependencies: MutableList<ModuleDeclaration> = mutableListOf()) {

    /**
     * A thread-safe map of factories, built lazily on first access.
     * Uses [ConcurrentHashMap] for concurrent read/write operations.
     */
    val factoryMap: Map<String, Factory<*>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ConcurrentHashMap<String, Factory<*>>().apply { putAll(buildFactories()) }
    }

    /**
     * Resolves and returns an instance of the specified type without parameters.
     * If the type is a [Provider], it creates a provider for lazy resolution.
     *
     * @param T The type to resolve.
     * @param qualifier An optional qualifier to distinguish between multiple bindings of the same type.
     * @return An instance of type [T].
     * @throws IllegalFactoryException If no factory is found for the given type and qualifier.
     */
    inline fun <reified T> instance(qualifier: Qualifier? = null): T {
        val type = typeOf<T>()
        return if (type.classifier == Provider::class) {
            createProvider(type, null, qualifier)
        } else {
            factoryMap[getIndexKey(T::class, qualifier)]?.create()
        } as? T
            ?: throw IllegalFactoryException(T::class, qualifier)
    }

    /**
     * Resolves and returns an instance of the specified type with provided parameters.
     * If the type is a [Provider], it creates a provider for lazy resolution.
     *
     * @param T The type to resolve.
     * @param params Variable arguments to pass to the factory for dependency creation.
     * @param qualifier An optional qualifier to distinguish between multiple bindings of the same type.
     * @return An instance of type [T].
     * @throws IllegalFactoryException If no factory is found for the given type and qualifier.
     */
    inline fun <reified T> instance(
        vararg params: Any?,
        qualifier: Qualifier? = null,
    ): T {
        val type = typeOf<T>()
        return if (type.classifier == Provider::class) {
            createProvider(type, params, qualifier)
        } else {
            factoryMap[getIndexKey(T::class, qualifier)]?.create(params)
        } as? T
            ?: throw IllegalFactoryException(T::class, qualifier)
    }

    /**
     * Creates a [Provider] for lazy resolution of the specified type.
     * The provider will resolve the dependency on demand using the provided parameters and qualifier.
     *
     * @param type The [KType] representing the provider type (e.g., Provider<SomeType>).
     * @param params Optional parameters to pass to the factory when resolving the dependency.
     * @param qualifier An optional qualifier for the dependency.
     * @return A [Provider] instance for lazy resolution, or null if the type is not a Provider.
     * @throws IllegalFactoryException If no factory is found for the underlying type and qualifier.
     */
    fun createProvider(
        type: KType,
        params: Array<out Any?>?,
        qualifier: Qualifier? = null,
    ): Provider<Any>? {
        return if (type.classifier == Provider::class) {
            val typeArgument = type.arguments.firstOrNull()?.type
            val clazz = typeArgument?.classifier as? KClass<*> ?: return null

            if (factoryMap[getIndexKey(clazz, qualifier)] == null)
                throw IllegalFactoryException(clazz, qualifier)

            object : Provider<Any> {
                override fun get(): Any {
                    return factoryMap[getIndexKey(clazz, qualifier)]?.create(params)
                        ?: throw IllegalFactoryException(clazz, qualifier)
                }
            }
        } else null
    }

    private fun buildFactories(): Map<String, Factory<*>> {
        val factoryMap = mutableMapOf<String, Factory<*>>()
        dependencies.forEach { currentDeclaration ->
            factoryMap.putAll(ModuleBuilder().also {
                it.factoryMap.putAll(factoryMap)
                it.currentDeclaration()
            }.factoryMap)
        }

        return factoryMap
    }

    internal fun getDependencies() =
        dependencies
}