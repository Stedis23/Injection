package com.stedis.injection

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public typealias ModuleDeclaration = ModuleBuilder.() -> Unit

/**
 * A builder class for defining factories and singletons within a module.
 * Provides methods to register dependencies and resolve instances during configuration.
 *
 * @property factoryMap A mutable map storing the registered factories, keyed by type and qualifier.
 */
class ModuleBuilder() {

    /**
     * The map of registered factories for this builder.
     */
    val factoryMap: MutableMap<String, Factory<*>> = mutableMapOf()

    /**
     * Resolves an instance of the specified type without parameters, similar to [Module.instance].
     * Used during module configuration for cross-dependencies.
     *
     * @param T The type to resolve.
     * @param qualifier An optional qualifier.
     * @return An instance of type [T].
     * @throws IllegalFactoryException If no factory is found.
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
     * Resolves an instance of the specified type with parameters, similar to [Module.instance].
     * Used during module configuration for cross-dependencies.
     *
     * @param T The type to resolve.
     * @param params Variable arguments for dependency creation.
     * @param qualifier An optional qualifier.
     * @return An instance of type [T].
     * @throws IllegalFactoryException If no factory is found.
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
     * Creates a provider for lazy resolution, similar to [Module.createProvider].
     *
     * @param type The [KType] of the provider.
     * @param params Optional parameters.
     * @param qualifier An optional qualifier.
     * @return A [Provider] instance or null.
     * @throws IllegalFactoryException If no factory is found.
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

    /**
     * Registers a factory for creating instances of type [T] on demand.
     * Factories produce new instances each time they are called.
     *
     * @param T The type to bind the factory to.
     * @param qualifier An optional qualifier for named bindings.
     * @param dependencyProducer A lambda that produces the dependency, optionally using parameters.
     */
    inline fun <reified T : Any> factory(
        qualifier: Qualifier? = null,
        crossinline dependencyProducer: (params: Array<out Any?>?) -> T
    ) {
        factoryMap[getIndexKey(T::class, qualifier)] = object : Factory<T> {
            override fun create(params: Array<out Any?>?): T {
                return dependencyProducer.invoke(params)
            }
        }
    }

    /**
     * Registers a singleton for type [T], ensuring only one instance is created and shared.
     * The instance is created lazily and thread-safely on first access.
     *
     * @param T The type to bind the singleton to.
     * @param qualifier An optional qualifier for named bindings.
     * @param dependencyProducer A lambda that produces the dependency.
     */
    inline fun <reified T : Any> singleton(
        qualifier: Qualifier? = null,
        crossinline dependencyProducer: (params: Array<out Any?>?) -> T
    ) {
        factoryMap[getIndexKey(T::class, qualifier)] = object : Factory<T> {
            @Volatile
            private var _dependency: T? = null

            override fun create(params: Array<out Any?>?): T {
                _dependency?.let { return it }
                synchronized(this) {
                    _dependency?.let { return it }
                    val dependency = dependencyProducer.invoke(params)
                    _dependency = dependency
                    return dependency
                }
            }
        }
    }

    /**
     * Extension function to safely retrieve an element of type [T] from an array by index.
     * Throws exceptions if the array is null, index is out of bounds, or type mismatch occurs.
     *
     * @param T The expected type of the element.
     * @param index The index to retrieve (default 0).
     * @return The element at the specified index, cast to [T].
     * @throws IllegalArgumentException If array is null, index is invalid, or type doesn't match.
     */
    inline fun <reified T> Array<out Any?>?.get(index: Int = 0): T {
        this ?: throw IllegalArgumentException("Array is null")
        require(index in indices) { "Index $index is out of bounds for array of size $size" }
        return this[index] as? T ?: throw IllegalArgumentException(
            "Element at index $index is of type ${this[index]?.let { it::class.simpleName } ?: "null"}, expected ${T::class.simpleName}"
        )
    }

    /**
     * Extension function to find and retrieve the first element of type [T] from an array.
     * Throws exceptions if the array is null or no matching element is found.
     *
     * @param T The expected type of the element.
     * @return The first element of type [T].
     * @throws IllegalArgumentException If array is null or no element of the type is found.
     */
    inline fun <reified T> Array<out Any?>?.get(): T {
        this ?: throw IllegalArgumentException("Array is null")
        return this.firstOrNull { it is T } as? T ?: throw IllegalArgumentException(
            "No element of type ${T::class.simpleName} found in the array"
        )
    }
}

class IllegalFactoryException(
    clazz: KClass<*>,
    qualifier: Qualifier?
) : IllegalArgumentException("No factory found for type ${clazz.simpleName}, qualifier ${qualifier?.value ?: "default"}")

fun getIndexKey(clazz: KClass<*>, qualifier: Qualifier?): String {
    val tq = qualifier?.value ?: ""
    return "${clazz.getFullName()}:$tq"
}

internal fun KClass<*>.getFullName(): String {
    return this.qualifiedName ?: this.simpleName ?: "Unknown"
}