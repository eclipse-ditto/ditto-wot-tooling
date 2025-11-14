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

package org.eclipse.ditto.wot.kotlin.generator.common.serialize

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.ResolvableSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * JsonSerializer aware of explicitly set nulls inside the generated WoT based Kotlin DSL.
 */
class ExplicitNullAwareSerializer(
    private val defaultSerializer: JsonSerializer<Any>?
): JsonSerializer<Any>(), ContextualSerializer, ResolvableSerializer {

    companion object {
        const val EXPLICITLY_SET_NULL_FIELD_SUFFIX = "_explicitly_set_null"
    }

    override fun serialize(
        anyThing: Any,
        jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        val kotlinType = anyThing.javaClass.kotlin
        doSerialize(anyThing, kotlinType, jsonGenerator, serializerProvider)
    }

    private fun doSerialize(
        anyThing: Any,
        kotlinType: KClass<Any>,
        jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        val explicitlySetNullProperties = kotlinType.memberProperties.filter { it.isExplicitlySetNullProperty() }
        if (explicitlySetNullProperties.isNotEmpty()) {
            // if any property was explicitly set to null (in applying generated DSL) ..
            val explicitlySettoNullPropertiesMap = explicitlySetNullProperties.associate {
                it.isAccessible = true
                it.name.substring(1, it.name.indexOf(EXPLICITLY_SET_NULL_FIELD_SUFFIX)) to it.get(anyThing) as Boolean
            }

            if (containsAnyExplicitlySetNullProperties(explicitlySettoNullPropertiesMap)) {
                jsonGenerator.writeStartObject()
                explicitlySettoNullPropertiesMap.forEach { (fieldName, isExplicitlyNull) ->
                    if (isExplicitlyNull) {
                        jsonGenerator.writeFieldName(fieldName)
                        jsonGenerator.writeNull()
                    } else {
                        kotlinType.memberProperties.find { it.name == fieldName }?.let {
                            val fieldValue = it.get(anyThing)
                            if (fieldValue != null) {
                                val fieldValueType = fieldValue.javaClass.kotlin
                                if (fieldValueType.isValue) {
                                    doSerialize(fieldValue, fieldValueType, jsonGenerator, serializerProvider)
                                } else {
                                    jsonGenerator.writeFieldName(fieldName)
                                    serializerProvider.defaultSerializeValue(fieldValue, jsonGenerator)
                                }
                            }
                        }
                    }
                }
                jsonGenerator.writeEndObject()
            } else {
                defaultSerializer?.serialize(anyThing, jsonGenerator, serializerProvider)
            }
        } else {
            defaultSerializer?.serialize(anyThing, jsonGenerator, serializerProvider)
        }
    }

    override fun createContextual(prov: SerializerProvider?, property: BeanProperty?): JsonSerializer<*> {
        if (defaultSerializer is ContextualSerializer) {
            val contextual = (defaultSerializer as ContextualSerializer).createContextual(prov, property)
            return ExplicitNullAwareSerializer(contextual as JsonSerializer<Any>?)
        }
        return ExplicitNullAwareSerializer(defaultSerializer)
    }

    private fun containsAnyExplicitlySetNullProperties(explicitlySettoNullFields: Map<String, Boolean>) =
        explicitlySettoNullFields.any { it.value }

    override fun resolve(provider: SerializerProvider?) {
        if (defaultSerializer is ResolvableSerializer) {
            (defaultSerializer as ResolvableSerializer).resolve(provider)
        }
    }
}

fun KProperty1<*, *>.isExplicitlySetNullProperty(): Boolean {
    return this.name.startsWith("_") && this.name.endsWith(ExplicitNullAwareSerializer.EXPLICITLY_SET_NULL_FIELD_SUFFIX)
}
