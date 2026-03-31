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
package org.eclipse.ditto.wot.kotlin.generator.plugin.clazz

import com.squareup.kotlinpoet.ClassName
import org.eclipse.ditto.json.JsonFactory
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.asClassName
import org.eclipse.ditto.wot.model.DataSchemaType
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for deduplicating types across packages.
 *
 * When [org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration.deduplicateReferencedTypes]
 * is enabled, this registry tracks generated classes and enums by:
 * 1. **tm:ref URL** (primary) — types from the same `tm:ref` are generated once; the first encounter
 *    determines the name and package, subsequent encounters reuse it.
 * 2. **Schema JSON content** (fallback) — for types without `tm:ref`, structurally identical schemas
 *    are deduplicated.
 *
 * ## Lifecycle
 *
 * The registry has two clearing stages by design:
 * - [clearRefMappings] — called by `TmRefScanner.scan()` before the pre-scan. Clears only the
 *   `schemaToRefUrl` fingerprint map, then repopulates it from the raw model JSON.
 * - [clear] — called by `ThingModelGenerator.generate()` before class generation. Clears all
 *   generation-time registries (`classRegistry`, `enumRegistry`, ref registries) but preserves
 *   `schemaToRefUrl` so the generator can look up fingerprints during generation.
 *
 * This two-stage design is intentional: the pre-scan runs first and populates `schemaToRefUrl`,
 * then `clear()` resets generation state without losing the pre-scanned fingerprint mappings.
 *
 * **Thread safety:** All internal registries use [ConcurrentHashMap]. The generator still runs
 * in a mostly single-threaded Maven execution model today, but these maps are safe for concurrent
 * access if the generator is invoked programmatically.
 */
object SharedTypeRegistry {

    /** Maps normalized schema JSON → ClassName where the class was first generated. */
    private val classRegistry = ConcurrentHashMap<String, ClassName>()

    /** Maps enum structural key → ClassName where the enum was first generated. */
    private val enumRegistry = ConcurrentHashMap<String, ClassName>()

    /** Maps enum schema JSON → ClassName for schema-based dedup (tm:ref). */
    private val enumSchemaRegistry = ConcurrentHashMap<String, ClassName>()

    /** Maps tm:ref URL → ClassName for classes generated from that ref. */
    private val classRefRegistry = ConcurrentHashMap<String, ClassName>()

    /** Maps tm:ref URL → ClassName for enums generated from that ref (SEPARATE_CLASS only). */
    private val enumRefRegistry = ConcurrentHashMap<String, ClassName>()

    /** Maps resolved schema content → tm:ref URL (populated by TmRefScanner pre-scan). */
    private val schemaToRefUrl = ConcurrentHashMap<String, String>()

    /** Maps tm:ref URL → canonical title from the referenced schema (populated by TmRefScanner pre-scan). */
    private val refTitleRegistry = ConcurrentHashMap<String, String>()

    /** Maps tm:ref URL → number of properties referencing it (populated by TmRefScanner pre-scan). */
    private val refCountRegistry = ConcurrentHashMap<String, Int>()

    /** Maps (packageName, enumName) → ClassName for exact package resolution of deduplicated enums. */
    private val enumPackageRegistry = ConcurrentHashMap<Pair<String, String>, ClassName>()

    /** Set of tm:ref URLs whose titles conflict with another tm:ref's title.
     *  These refs should NOT use the title for naming. Populated by [markConflictingTitles]. */
    private val conflictingTitleRefs = ConcurrentHashMap.newKeySet<String>()

    /**
     * Clears all registered types. Called at the start of each generation run.
     * Does NOT clear [schemaToRefUrl] — that is managed by [TmRefScanner.scan] which
     * calls [clearRefMappings] at the start of each scan.
     */
    fun clear() {
        classRegistry.clear()
        enumRegistry.clear()
        enumSchemaRegistry.clear()
        classRefRegistry.clear()
        enumRefRegistry.clear()
        enumPackageRegistry.clear()
    }

    /**
     * Clears the fingerprint → tm:ref URL mappings. Called by [TmRefScanner.scan] at the
     * start of each scan to prevent stale mappings from a previous model leaking into the
     * current one in multi-execution builds.
     */
    fun clearRefMappings() {
        schemaToRefUrl.clear()
        refTitleRegistry.clear()
        refCountRegistry.clear()
        conflictingTitleRefs.clear()
    }

    /**
     * Finds an existing class generated from a structurally identical schema.
     *
     * @param schemaJson The schema JSON of the class to check
     * @return The [ClassName] of the existing class, or null if no match
     */
    fun findExistingClass(schemaJson: JsonObject): ClassName? {
        return classRegistry[normalizeSchema(schemaJson)]
    }

    /**
     * Registers a newly generated class by its schema structure.
     *
     * @param schemaJson The schema JSON of the generated class
     * @param className The [ClassName] where it was generated
     */
    fun registerClass(schemaJson: JsonObject, className: ClassName) {
        classRegistry[normalizeSchema(schemaJson)] = className
    }

    /**
     * Finds an existing enum generated with identical values and schema type.
     *
     * @param enumValues The set of enum constant names
     * @param schemaType The WoT schema type (STRING, INTEGER, etc.)
     * @return The [ClassName] of the existing enum, or null if no match
     */
    fun findExistingEnum(enumValues: Set<String>, schemaType: DataSchemaType): ClassName? {
        return enumRegistry[buildEnumKey(enumValues, schemaType)]
    }

    /**
     * Registers a newly generated enum by its values and schema type.
     *
     * @param enumValues The set of enum constant names
     * @param schemaType The WoT schema type
     * @param className The [ClassName] where it was generated
     */
    fun registerEnum(enumValues: Set<String>, schemaType: DataSchemaType, className: ClassName) {
        enumRegistry[buildEnumKey(enumValues, schemaType)] = className
        enumPackageRegistry[className.packageName to className.simpleName] = className
    }

    /**
     * Finds an existing enum by its simple name (regardless of schema type or values).
     * Used by [org.eclipse.ditto.wot.kotlin.generator.plugin.property.WrapperTypeChecker]
     * to resolve the canonical package for a deduplicated enum.
     *
     * @param enumName The simple name of the enum
     * @return The [ClassName] if found, or null
     */
    fun findExistingEnumByName(enumName: String): ClassName? {
        return enumRegistry.values.firstOrNull { it.simpleName == enumName }
    }

    /**
     * Finds an existing enum by exact package and simple name.
     */
    fun findExistingEnumByNameInPackage(enumName: String, packageName: String): ClassName? {
        return enumPackageRegistry[packageName to enumName]
    }

    /**
     * Registers an alias so that looking up [enumName] in [requestingPackage] returns
     * [canonicalClassName] (which lives in a different package). This enables cross-package
     * dedup for SEPARATE_CLASS enums: the enum is generated once in the first package,
     * and subsequent packages get a reference pointing to the original.
     */
    fun registerEnumAlias(requestingPackage: String, enumName: String, canonicalClassName: ClassName) {
        enumPackageRegistry[requestingPackage to enumName] = canonicalClassName
    }

    /**
     * Finds an existing enum generated from the same schema JSON (tm:ref dedup).
     */
    fun findExistingEnumBySchema(schemaKey: String): ClassName? {
        return enumSchemaRegistry[schemaKey]
    }

    /**
     * Registers an enum by its schema JSON for tm:ref based dedup.
     */
    fun registerEnumBySchema(schemaKey: String, className: ClassName) {
        enumSchemaRegistry[schemaKey] = className
    }

    // --- tm:ref URL-based dedup ---

    /** Finds a class previously generated from the given [refUrl]. */
    fun findClassByRef(refUrl: String): ClassName? = classRefRegistry[refUrl]

    /** Registers a generated class against its source [refUrl] for future dedup lookups. */
    fun registerClassByRef(refUrl: String, className: ClassName) {
        classRefRegistry[refUrl] = className
    }

    /** Finds an enum previously generated from the given [refUrl] (SEPARATE_CLASS strategy only). */
    fun findEnumByRef(refUrl: String): ClassName? = enumRefRegistry[refUrl]

    /** Registers a generated enum against its source [refUrl] for future dedup lookups. */
    fun registerEnumByRef(refUrl: String, className: ClassName) {
        enumRefRegistry[refUrl] = className
        enumPackageRegistry[className.packageName to className.simpleName] = className
    }

    /**
     * Registers a mapping from a structural fingerprint to its source `tm:ref` URL.
     * Called by [TmRefScanner] during the pre-scan phase, before the WoT library resolves references.
     */
    fun registerRefMapping(fingerprint: String, refUrl: String) {
        schemaToRefUrl[fingerprint] = refUrl
    }

    /**
     * Registers the canonical title from a `tm:ref` target schema.
     * Called by [TmRefScanner] during the pre-scan phase. This title represents the
     * type's semantic name as defined at the reference source, before local overrides
     * (e.g., a shared schema referenced by multiple properties with context-specific names).
     */
    fun registerRefTitle(refUrl: String, title: String) {
        refTitleRegistry[refUrl] = title
    }

    /**
     * Finds the canonical title for a `tm:ref` URL, as defined in the referenced schema.
     */
    fun findRefTitle(refUrl: String): String? = refTitleRegistry[refUrl]

    /**
     * Increments the reference count for a `tm:ref` URL. Called by [TmRefScanner] each time
     * a property referencing this URL is encountered during the pre-scan.
     */
    fun incrementRefCount(refUrl: String) {
        refCountRegistry.merge(refUrl, 1, Int::plus)
    }

    /**
     * Returns how many properties reference this `tm:ref` URL.
     */
    fun getRefCount(refUrl: String): Int = refCountRegistry.getOrDefault(refUrl, 0)

    /**
     * Returns true if the given tm:ref URL has a title that conflicts with another tm:ref's title.
     * Must be called after [markConflictingTitles].
     */
    fun hasConflictingTitle(refUrl: String): Boolean = refUrl in conflictingTitleRefs

    /**
     * Analyzes all registered tm:ref titles and marks refs whose titles would produce the same
     * class name as another ref's title. Called by [TmRefScanner.scan] after the pre-scan completes.
     * E.g., two distinct tm:ref URLs whose target schemas share the same title
     * would produce the same class name, causing a collision.
     */
    fun markConflictingTitles() {
        // Group refs by the class name their title would produce (using the same
        // asClassName() transform used for actual code generation)
        val titleToRefs = refTitleRegistry.entries
            .groupBy { (_, title) -> asClassName(title) }
            .filter { (_, refs) -> refs.size > 1 }

        for ((_, refs) in titleToRefs) {
            for ((refUrl, _) in refs) {
                conflictingTitleRefs.add(refUrl)
            }
        }
    }

    /**
     * Finds the `tm:ref` URL for a resolved schema by matching its structural fingerprint
     * against the pre-scanned registry. Returns null if no match (schema did not come from a `tm:ref`).
     */
    fun findRefUrlByFingerprint(fingerprint: String): String? {
        return schemaToRefUrl[fingerprint]
    }

    /**
     * Normalizes a schema JSON for class-level dedup comparison by sorting keys recursively.
     * This ensures structurally identical schemas with different key insertion order produce
     * the same string. Unlike [TmRefScanner.computeFingerprint], this preserves all fields
     * (including title, description, etc.) since schemas with different titles may represent
     * intentionally distinct types even if their structural shape is identical.
     */
    private fun normalizeSchema(schemaJson: JsonObject): String = sortKeysRecursive(schemaJson).toString()

    private fun sortKeysRecursive(jsonObject: JsonObject): JsonObject {
        val builder = JsonFactory.newObjectBuilder()
        jsonObject.keys.map { it.toString() }.sorted().forEach { key ->
            val field = jsonObject.getField(key).orElse(null) ?: return@forEach
            val value = field.value
            builder.set(key, sortValueRecursive(value))
        }
        return builder.build()
    }

    private fun sortValueRecursive(value: JsonValue): JsonValue {
        return when {
            value.isObject -> sortKeysRecursive(value.asObject())
            value.isArray -> {
                val arrayBuilder = JsonFactory.newArrayBuilder()
                value.asArray().forEach { arrayBuilder.add(sortValueRecursive(it)) }
                arrayBuilder.build()
            }
            else -> value
        }
    }

    private fun buildEnumKey(enumValues: Set<String>, schemaType: DataSchemaType): String {
        return "${schemaType.name}:${enumValues.sorted().joinToString(",")}"
    }
}
