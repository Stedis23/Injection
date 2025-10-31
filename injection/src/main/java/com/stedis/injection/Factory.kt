package com.stedis.injection

/**
 * Interface for factories that create instances of type [T].
 * Implementations define how dependencies are instantiated.
 *
 * @param T The type of object the factory produces.
 */
interface Factory<T> {

    fun create(params: Array<out Any?>? = null): T
}