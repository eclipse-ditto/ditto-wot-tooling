/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.wot.kotlin.generator.plugin.property

import org.eclipse.ditto.json.JsonArray
import org.eclipse.ditto.json.JsonFactory
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.json.JsonValue
import kotlin.jvm.optionals.getOrNull
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.SharedTypeRegistry
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.loadJson
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.readJsonValue
import org.slf4j.LoggerFactory
import java.util.Collections

/**
 * Pre-scans a Thing Model's raw JSON to build a mapping of structural fingerprint → tm:ref URL.
 * The fingerprint captures the schema's shape (property names, types, required, enum values)
 * while ignoring metadata (titles, descriptions) that the WoT library may rewrite.
 *
 * **Thread safety:** Uses a thread-safe set for visited URLs. Currently single-threaded in the
 * Maven plugin, but safe if used programmatically in a concurrent context.
 */
object TmRefScanner {

    private val logger = LoggerFactory.getLogger(TmRefScanner::class.java)
    private val visitedUrls: MutableSet<String> = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

    /**
     * Pre-scans the raw Thing Model JSON at [modelUrl], recursively following all `tm:ref`,
     * `tm:submodel`, and `tm:extends` links. For each `tm:ref` target, computes a structural
     * fingerprint and registers it in [SharedTypeRegistry] so the generator can later identify
     * which resolved schema came from which `tm:ref`.
     *
     * Must be called before `DittoBasedWotLoader.load()`, which discards `tm:ref` URLs.
     */
    fun scan(modelUrl: String) {
        visitedUrls.clear()
        SharedTypeRegistry.clearRefMappings()
        val json = loadJson(modelUrl)
        scanJson(json)
        SharedTypeRegistry.markConflictingTitles()
    }

    private fun scanJson(json: JsonObject) {
        val refs = ReferencePropertyResolver.collectReferenceFields(json)

        for ((_, refUrl) in refs) {
            val cleanUrl = refUrl.replace("\"", "")
            // Always count every reference, even if already visited
            SharedTypeRegistry.incrementRefCount(cleanUrl)
            if (cleanUrl in visitedUrls) continue
            visitedUrls.add(cleanUrl)

            try {
                val hashCount = cleanUrl.count { it == '#' }
                if (hashCount != 1) continue

                val parts = cleanUrl.split("#", limit = 2)
                val baseUrl = parts[0]
                val fragmentPath = parts[1]

                val refJson = loadJson(baseUrl)
                val resolvedContent = readJsonValue(refJson, JsonPointer.of(fragmentPath))

                if (resolvedContent != null && resolvedContent.isObject) {
                    val resolvedObj = resolvedContent.asObject()
                    val fingerprint = computeFingerprint(resolvedObj)
                    SharedTypeRegistry.registerRefMapping(fingerprint, cleanUrl)
                    val title = resolvedObj.getValue("title").getOrNull()
                    if (title != null && title.isString) {
                        SharedTypeRegistry.registerRefTitle(cleanUrl, title.asString())
                    }
                    logger.debug("Registered tm:ref fingerprint for {}", cleanUrl)

                    scanJson(refJson)
                }
            } catch (e: Exception) {
                logger.warn("Could not scan tm:ref $cleanUrl: ${e.message}")
            }
        }

        val linksValue = json.getValue(JsonPointer.of("links")).getOrNull()
        if (linksValue != null && linksValue.isArray) {
            for (link in linksValue.asArray()) {
                if (link.isObject) {
                    val linkObj = link.asObject()
                    val rel = linkObj.getValue("rel").getOrNull()?.asString()
                    val href = linkObj.getValue("href").getOrNull()?.asString()
                    if ((rel == "tm:submodel" || rel == "tm:extends") && href != null && href !in visitedUrls) {
                        visitedUrls.add(href)
                        try {
                            scanJson(loadJson(href))
                        } catch (e: Exception) {
                            logger.warn("Could not scan $rel link $href: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes a structural fingerprint of a JSON schema.
     * Captures the supported schema subset relevant for code generation and deduplication.
     * Ignores: title, description, @type, unit, readOnly, default, format, ditto:category.
     */
    fun computeFingerprint(schema: JsonObject): String {
        return extractSchemaFingerprint(schema).toJson().toString()
    }

    private fun extractSchemaFingerprint(schema: JsonObject): SchemaFingerprint {
        return SchemaFingerprint(
            type = schema.value("type")?.let(::canonicalizeJsonValue),
            objectSchema = extractObjectSchemaFingerprint(schema),
            arraySchema = extractArraySchemaFingerprint(schema),
            stringSchema = extractStringSchemaFingerprint(schema),
            numericSchema = extractNumericSchemaFingerprint(schema),
            enumValues = extractSortedValueArray(schema, "enum"),
            constValue = schema.value("const")?.let(::canonicalizeJsonValue),
            oneOf = extractCompositionSchemas(schema, "oneOf"),
            anyOf = extractCompositionSchemas(schema, "anyOf"),
            allOf = extractCompositionSchemas(schema, "allOf")
        )
    }

    private fun extractCompositionSchemas(schema: JsonObject, key: String): List<SchemaFingerprint> {
        val value = schema.value(key) ?: return emptyList()
        if (!value.isArray) return emptyList()
        return value.asArray().mapNotNull { item ->
            item.takeIf(JsonValue::isObject)?.asObject()?.let(::extractSchemaFingerprint)
        }
    }

    private fun extractObjectSchemaFingerprint(schema: JsonObject): ObjectSchemaFingerprint? {
        val properties = extractSchemaMap(schema, "properties")
        val required = extractSortedStringArray(schema, "required")
        val additionalProperties = schema.value("additionalProperties")?.let(::extractAdditionalPropertiesFingerprint)
        val patternProperties = extractSchemaMap(schema, "patternProperties")
        val minProperties = schema.value("minProperties")?.let(::canonicalizeJsonValue)
        val maxProperties = schema.value("maxProperties")?.let(::canonicalizeJsonValue)

        return ObjectSchemaFingerprint(
            properties = properties,
            required = required,
            additionalProperties = additionalProperties,
            patternProperties = patternProperties,
            minProperties = minProperties,
            maxProperties = maxProperties
        ).takeIf(ObjectSchemaFingerprint::isNotEmpty)
    }

    private fun extractArraySchemaFingerprint(schema: JsonObject): ArraySchemaFingerprint? {
        val itemsValue = schema.value("items")
        val singleItem = itemsValue?.takeIf(JsonValue::isObject)?.asObject()?.let(::extractSchemaFingerprint)
        val tupleItems = itemsValue?.takeIf(JsonValue::isArray)
            ?.asArray()
            ?.mapNotNull { item ->
                item.takeIf(JsonValue::isObject)?.asObject()?.let(::extractSchemaFingerprint)
            }
            .orEmpty()
        val minItems = schema.value("minItems")?.let(::canonicalizeJsonValue)
        val maxItems = schema.value("maxItems")?.let(::canonicalizeJsonValue)
        val uniqueItems = schema.value("uniqueItems")?.let(::canonicalizeJsonValue)

        return ArraySchemaFingerprint(
            singleItem = singleItem,
            tupleItems = tupleItems,
            minItems = minItems,
            maxItems = maxItems,
            uniqueItems = uniqueItems
        ).takeIf(ArraySchemaFingerprint::isNotEmpty)
    }

    private fun extractStringSchemaFingerprint(schema: JsonObject): StringSchemaFingerprint? {
        return StringSchemaFingerprint(
            minLength = schema.value("minLength")?.let(::canonicalizeJsonValue),
            maxLength = schema.value("maxLength")?.let(::canonicalizeJsonValue),
            pattern = schema.value("pattern")?.let(::canonicalizeJsonValue)
        ).takeIf(StringSchemaFingerprint::isNotEmpty)
    }

    private fun extractNumericSchemaFingerprint(schema: JsonObject): NumericSchemaFingerprint? {
        return NumericSchemaFingerprint(
            minimum = schema.value("minimum")?.let(::canonicalizeJsonValue),
            maximum = schema.value("maximum")?.let(::canonicalizeJsonValue),
            multipleOf = schema.value("multipleOf")?.let(::canonicalizeJsonValue)
        ).takeIf(NumericSchemaFingerprint::isNotEmpty)
    }

    private fun extractAdditionalPropertiesFingerprint(value: JsonValue): AdditionalPropertiesFingerprint {
        return when {
            value.isObject -> AdditionalPropertiesSchemaFingerprint(extractSchemaFingerprint(value.asObject()))
            else -> AdditionalPropertiesFlagFingerprint(canonicalizeJsonValue(value))
        }
    }

    private fun extractSchemaMap(schema: JsonObject, key: String): Map<String, SchemaFingerprint> {
        val value = schema.value(key) ?: return emptyMap()
        if (!value.isObject) return emptyMap()

        val obj = value.asObject()
        return obj.keys.map { it.toString() }.sorted().associateWith { fieldName ->
            extractSchemaFingerprint(obj.getField(fieldName).getOrNull()!!.value.asObject())
        }
    }

    private fun extractSortedStringArray(schema: JsonObject, key: String): List<String> {
        val value = schema.value(key) ?: return emptyList()
        if (!value.isArray) return emptyList()
        return value.asArray().map(JsonValue::toString).sorted()
    }

    private fun extractSortedValueArray(schema: JsonObject, key: String): List<JsonValue> {
        val value = schema.value(key) ?: return emptyList()
        if (!value.isArray) return emptyList()
        return value.asArray().map(::canonicalizeJsonValue).sortedBy(JsonValue::toString)
    }

    private fun SchemaFingerprint.toJson(): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        type?.let { builder.set("type", it) }
        objectSchema?.let { builder.set("object", it.toJson()) }
        arraySchema?.let { builder.set("array", it.toJson()) }
        stringSchema?.let { builder.set("string", it.toJson()) }
        numericSchema?.let { builder.set("numeric", it.toJson()) }
        if (enumValues.isNotEmpty()) {
            builder.set("enum", JsonFactory.newArrayBuilder().apply { enumValues.forEach(::add) }.build())
        }
        constValue?.let { builder.set("const", it) }
        if (oneOf.isNotEmpty()) {
            builder.set("oneOf", JsonFactory.newArrayBuilder().apply { oneOf.forEach { add(it.toJson()) } }.build())
        }
        if (anyOf.isNotEmpty()) {
            builder.set("anyOf", JsonFactory.newArrayBuilder().apply { anyOf.forEach { add(it.toJson()) } }.build())
        }
        if (allOf.isNotEmpty()) {
            builder.set("allOf", JsonFactory.newArrayBuilder().apply { allOf.forEach { add(it.toJson()) } }.build())
        }
        return builder.build()
    }

    private fun ObjectSchemaFingerprint.toJson(): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        if (properties.isNotEmpty()) {
            builder.set("properties", JsonFactory.newObjectBuilder().apply {
                properties.forEach { (name, propertySchema) -> set(name, propertySchema.toJson()) }
            }.build())
        }
        if (required.isNotEmpty()) {
            builder.set("required", JsonFactory.newArrayBuilder().apply { required.forEach(::add) }.build())
        }
        additionalProperties?.let { builder.set("additionalProperties", it.toJsonValue()) }
        if (patternProperties.isNotEmpty()) {
            builder.set("patternProperties", JsonFactory.newObjectBuilder().apply {
                patternProperties.forEach { (patternKey, propertySchema) -> set(patternKey, propertySchema.toJson()) }
            }.build())
        }
        minProperties?.let { builder.set("minProperties", it) }
        maxProperties?.let { builder.set("maxProperties", it) }
        return builder.build()
    }

    private fun ArraySchemaFingerprint.toJson(): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        singleItem?.let { builder.set("items", it.toJson()) }
        if (tupleItems.isNotEmpty()) {
            builder.set("tupleItems", JsonFactory.newArrayBuilder().apply {
                tupleItems.forEach { add(it.toJson()) }
            }.build())
        }
        minItems?.let { builder.set("minItems", it) }
        maxItems?.let { builder.set("maxItems", it) }
        uniqueItems?.let { builder.set("uniqueItems", it) }
        return builder.build()
    }

    private fun StringSchemaFingerprint.toJson(): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        minLength?.let { builder.set("minLength", it) }
        maxLength?.let { builder.set("maxLength", it) }
        pattern?.let { builder.set("pattern", it) }
        return builder.build()
    }

    private fun NumericSchemaFingerprint.toJson(): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        minimum?.let { builder.set("minimum", it) }
        maximum?.let { builder.set("maximum", it) }
        multipleOf?.let { builder.set("multipleOf", it) }
        return builder.build()
    }

    private fun canonicalizeJsonValue(value: JsonValue): JsonValue {
        return when {
            value.isObject -> canonicalizeJsonObject(value.asObject())
            value.isArray -> canonicalizeJsonArray(value.asArray())
            else -> value
        }
    }

    private fun canonicalizeJsonObject(jsonObject: JsonObject): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        jsonObject.keys.map { it.toString() }.sorted().forEach { key ->
            jsonObject.getField(key).getOrNull()?.value?.let { fieldValue ->
                builder.set(key, canonicalizeJsonValue(fieldValue))
            }
        }
        return builder.build()
    }

    private fun canonicalizeJsonArray(array: JsonArray): JsonArray {
        val builder = JsonFactory.newArrayBuilder()
        array.forEach { builder.add(canonicalizeJsonValue(it)) }
        return builder.build()
    }

    private fun JsonObject.value(key: String): JsonValue? = getValue(JsonPointer.of(key)).getOrNull()

    private data class SchemaFingerprint(
        val type: JsonValue? = null,
        val objectSchema: ObjectSchemaFingerprint? = null,
        val arraySchema: ArraySchemaFingerprint? = null,
        val stringSchema: StringSchemaFingerprint? = null,
        val numericSchema: NumericSchemaFingerprint? = null,
        val enumValues: List<JsonValue> = emptyList(),
        val constValue: JsonValue? = null,
        val oneOf: List<SchemaFingerprint> = emptyList(),
        val anyOf: List<SchemaFingerprint> = emptyList(),
        val allOf: List<SchemaFingerprint> = emptyList()
    )

    private data class ObjectSchemaFingerprint(
        val properties: Map<String, SchemaFingerprint> = emptyMap(),
        val required: List<String> = emptyList(),
        val additionalProperties: AdditionalPropertiesFingerprint? = null,
        val patternProperties: Map<String, SchemaFingerprint> = emptyMap(),
        val minProperties: JsonValue? = null,
        val maxProperties: JsonValue? = null
    ) {
        fun isNotEmpty(): Boolean {
            return properties.isNotEmpty() ||
                required.isNotEmpty() ||
                additionalProperties != null ||
                patternProperties.isNotEmpty() ||
                minProperties != null ||
                maxProperties != null
        }
    }

    private data class ArraySchemaFingerprint(
        val singleItem: SchemaFingerprint? = null,
        val tupleItems: List<SchemaFingerprint> = emptyList(),
        val minItems: JsonValue? = null,
        val maxItems: JsonValue? = null,
        val uniqueItems: JsonValue? = null
    ) {
        fun isNotEmpty(): Boolean {
            return singleItem != null ||
                tupleItems.isNotEmpty() ||
                minItems != null ||
                maxItems != null ||
                uniqueItems != null
        }
    }

    private data class StringSchemaFingerprint(
        val minLength: JsonValue? = null,
        val maxLength: JsonValue? = null,
        val pattern: JsonValue? = null
    ) {
        fun isNotEmpty(): Boolean = minLength != null || maxLength != null || pattern != null
    }

    private data class NumericSchemaFingerprint(
        val minimum: JsonValue? = null,
        val maximum: JsonValue? = null,
        val multipleOf: JsonValue? = null
    ) {
        fun isNotEmpty(): Boolean = minimum != null || maximum != null || multipleOf != null
    }

    private sealed interface AdditionalPropertiesFingerprint {
        fun toJsonValue(): JsonValue
    }

    private data class AdditionalPropertiesSchemaFingerprint(
        val schema: SchemaFingerprint
    ) : AdditionalPropertiesFingerprint {
        override fun toJsonValue(): JsonValue = schema.toJson()
    }

    private data class AdditionalPropertiesFlagFingerprint(
        val value: JsonValue
    ) : AdditionalPropertiesFingerprint {
        override fun toJsonValue(): JsonValue = value
    }
}
