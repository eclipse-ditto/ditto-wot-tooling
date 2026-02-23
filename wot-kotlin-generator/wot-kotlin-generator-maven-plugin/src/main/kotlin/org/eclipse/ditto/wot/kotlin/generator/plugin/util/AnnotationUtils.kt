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
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.kotlin.generator.common.model.DittoJsonDsl
import org.eclipse.ditto.wot.model.Action
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.SingleDataSchema
import kotlin.jvm.optionals.getOrNull

private const val DITTO_DEPRECATION_NOTICE = "ditto:deprecationNotice"
private const val DEPRECATED = "deprecated"
private const val SUPERSEDED_BY = "supersededBy"
private const val REMOVAL_VERSION = "removalVersion"

data class DeprecationNotice(
    val deprecated: Boolean,
    val supersededBy: String? = null,
    val removalVersion: String? = null
)

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

fun extractDeprecationNotice(property: Property): DeprecationNotice? = extractDeprecationNotice(property.toJson())

fun extractDeprecationNotice(action: Action): DeprecationNotice? = extractDeprecationNotice(action.toJson())

fun extractDeprecationNotice(schema: SingleDataSchema): DeprecationNotice? = extractDeprecationNotice(schema.toJson())

fun extractDeprecationNotice(jsonObject: JsonObject): DeprecationNotice? {
    val deprecationNotice = jsonObject.getValue(JsonPointer.of(DITTO_DEPRECATION_NOTICE))
        .getOrNull()
        ?.takeIf(JsonValue::isObject)
        ?.asObject()
        ?: return null

    val deprecatedValue = deprecationNotice.getValue(JsonPointer.of(DEPRECATED))
        .getOrNull()
        ?: return null

    if (!deprecatedValue.isBoolean || !deprecatedValue.asBoolean()) {
        return null
    }

    val supersededBy = deprecationNotice.getValue(JsonPointer.of(SUPERSEDED_BY))
        .getOrNull()
        ?.takeIf(JsonValue::isString)
        ?.asString()

    val removalVersion = deprecationNotice.getValue(JsonPointer.of(REMOVAL_VERSION))
        .getOrNull()
        ?.takeIf(JsonValue::isString)
        ?.asString()

    return DeprecationNotice(
        deprecated = true,
        supersededBy = supersededBy,
        removalVersion = removalVersion
    )
}

fun deprecationReplacementIdentifier(notice: DeprecationNotice?): String? {
    val pointer = notice?.supersededBy ?: return null
    if (!pointer.startsWith("#/")) return null
    val targetName = pointer.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
    return asPropertyName(targetName)
}

fun buildDeprecationMessage(
    notice: DeprecationNotice,
    replacementIdentifier: String? = deprecationReplacementIdentifier(notice)
): String {
    val parts = mutableListOf<String>()
    if (replacementIdentifier != null) {
        parts.add("Use $replacementIdentifier() instead.")
    } else {
        parts.add("This element is deprecated.")
    }
    notice.removalVersion?.let { parts.add("Will be removed in version $it.") }
    return parts.joinToString(" ")
}

fun buildDeprecatedAnnotationSpec(
    notice: DeprecationNotice,
    replaceWithExpression: String? = null
): AnnotationSpec {
    val replacementIdentifier = deprecationReplacementIdentifier(notice)
    val message = buildDeprecationMessage(notice, replacementIdentifier)
    val effectiveReplaceWith = replaceWithExpression ?: replacementIdentifier?.let { "$it()" }
    val deprecatedAnnotation = AnnotationSpec.builder(Deprecated::class)
        .addMember("message = %S", message)
    if (effectiveReplaceWith != null) {
        deprecatedAnnotation.addMember("replaceWith = %T(%S)", ClassName("kotlin", "ReplaceWith"), effectiveReplaceWith)
    }
    deprecatedAnnotation.addMember("level = %T.WARNING", DeprecationLevel::class)
    return deprecatedAnnotation.build()
}
