/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.wot.kotlin.generator.deserialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.ditto.wot.kotlin.generator.model.features.MapObjectProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class MapObjectPropertyDeserializer<T : Any> : JsonDeserializer<MapObjectProperty<T>>(), ContextualDeserializer {

    private var contextualType: JavaType? = null
    private var valueType: JavaType? = null

    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
        val wrapperType = ctxt.contextualType
        val valueType = wrapperType.superClass.bindings.getBoundType(0)

        return MapObjectPropertyDeserializer<T>().apply {
            contextualType = wrapperType
            this.valueType = valueType
        }
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MapObjectProperty<T> {
        val codec = p.codec as ObjectMapper
        val node: ObjectNode = codec.readTree(p)

        val actualType = contextualType ?: throw IllegalArgumentException("Contextual type is null")
        val concreteClass = actualType.rawClass.kotlin

        if (concreteClass.isAbstract) {
            throw IllegalArgumentException("Expected a concrete class, but got an abstract class: ${concreteClass.simpleName}")
        }

        val instance = createInstance(concreteClass)
        val mapObjectProperty = instance as MapObjectProperty<T>

        node.fields().forEachRemaining { (key, valueNode) ->
            val value = p.codec.treeToValue(valueNode, valueType!!.rawClass)
            mapObjectProperty.map[key] = value as T
        }

        return mapObjectProperty
    }

    private fun createInstance(clazz: KClass<*>): Any {
        val noArgsConstructor = clazz.constructors.find { it.parameters.isEmpty() }
        return noArgsConstructor?.call() ?: clazz.primaryConstructor?.callBy(emptyMap())
        ?: throw IllegalArgumentException("No suitable constructor found for ${clazz.simpleName}")
    }
}
