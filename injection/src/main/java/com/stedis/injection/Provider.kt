package com.stedis.injection

/**
 * Interface for providers that lazily resolve dependencies.
 * Providers are useful for breaking circular dependencies or deferring instantiation.
 *
 * @param T The type of object the provider resolves.
 */
interface Provider<T> {

    fun get(): T
}