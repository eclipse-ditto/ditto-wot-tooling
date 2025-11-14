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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import org.eclipse.ditto.wot.kotlin.generator.common.model.DittoJsonDsl

fun buildDittoJsonDslAnnotationSpec(): AnnotationSpec {
    return AnnotationSpec.builder(DittoJsonDsl::class)
        .build()
}

fun buildJsonIncludeAnnotationSpec(): AnnotationSpec {
    val jsonIncludeEnum = ClassName("com.fasterxml.jackson.annotation.JsonInclude", "Include")
    return AnnotationSpec.builder(JsonInclude::class)
        .addMember("%T.%L", jsonIncludeEnum, "NON_NULL")
        .build()
}

fun buildJsonIgnoreAnnotationSpec(): AnnotationSpec {
    return AnnotationSpec.builder(JsonIgnoreProperties::class).addMember(
        CodeBlock.builder()
            .add("ignoreUnknown = true")
            .build()
    ).build()
}

fun buildJsonIncludeNonEmptyAnnotationSpec(): AnnotationSpec {
    val jsonIncludeEnum = ClassName("com.fasterxml.jackson.annotation.JsonInclude", "Include")
    val jsonIncludeAnnotation = AnnotationSpec.builder(JsonInclude::class)
        .addMember("%T.%L", jsonIncludeEnum, "NON_EMPTY")
        .build()
    return jsonIncludeAnnotation
}

fun buildJsonPropertyAnnotationSpec(originalPropertyName: String): AnnotationSpec {
    return AnnotationSpec.builder(JsonProperty::class)
        .addMember("%S", originalPropertyName)
        .build()
}

fun hasJacksonAnnotation(propertySpec: com.squareup.kotlinpoet.PropertySpec): Boolean {
    return propertySpec.annotations.any { annotation ->
        val className = annotation.typeName.toString()
        className.contains("JsonProperty") || className.contains("JsonSetter")
    }
}

fun buildJsonSetterAnnotationSpec(originalPropertyName: String): AnnotationSpec {
    return AnnotationSpec.builder(JsonSetter::class)
        .addMember("%S", originalPropertyName)
        .build()
}