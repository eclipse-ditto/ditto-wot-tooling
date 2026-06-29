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
package org.eclipse.ditto.wot.kotlin.generator.plugin.util

import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.model.SingleDataSchema
import kotlin.jvm.optionals.getOrNull

/**
 * Builds KDoc documentation strings from WoT data schema metadata.
 *
 * WoT Thing Models carry rich, human- and machine-oriented metadata on properties, actions and data schemas
 * (`title`, `description`, `unit`, semantic `@type`, value constraints, defaults, allowed values, ...). None of
 * this survives into the generated Kotlin types unless it is rendered explicitly. This object turns that metadata
 * into Markdown KDoc so it shows up on hover and during auto-completion in IDEs — for instance making it obvious
 * that a `timeout` property is expressed in seconds rather than milliseconds.
 *
 * The produced text is plain Markdown (the format Dokka/IntelliJ render for KDoc). All values are emitted as
 * literals by the callers (`addKdoc("%L", text)`) so that `%` characters in the model text are never interpreted
 * as KotlinPoet format specifiers.
 */
object KdocGenerator {

    /**
     * Whether KDoc generation is enabled. Toggled once per generation run via [configure] from the
     * [GeneratorConfiguration]'s `generateKdoc` flag.
     */
    @Volatile
    var enabled: Boolean = true
        private set

    /**
     * Numeric constraint fields, looked up from the schema JSON because a [org.eclipse.ditto.wot.model.Property] is
     * not castable to the concrete numeric schema subtypes that expose typed getters for them.
     */
    private val numericConstraints = linkedMapOf(
        "minimum" to "minimum",
        "exclusiveMinimum" to "exclusive minimum",
        "maximum" to "maximum",
        "exclusiveMaximum" to "exclusive maximum",
        "multipleOf" to "multiple of"
    )

    private val stringConstraints = linkedMapOf(
        "minLength" to "min length",
        "maxLength" to "max length",
        "pattern" to "pattern",
        "contentEncoding" to "content encoding",
        "contentMediaType" to "content media type"
    )

    private val arrayConstraints = linkedMapOf(
        "minItems" to "min items",
        "maxItems" to "max items"
    )

    /**
     * Configures whether KDoc should be generated for the current run.
     *
     * @param enabled whether KDoc generation is enabled.
     */
    fun configure(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Builds a Markdown KDoc string for the given WoT data schema (or property, which is a sub-type).
     *
     * @param schema the WoT data schema to document; may be {@code null}.
     * @param includeAllowedValues whether to render the `enum` allowed values. Set to {@code false} when documenting
     * a generated enum type, whose constants already represent the allowed values.
     * @return the KDoc text, or {@code null} when KDoc generation is disabled, the schema is {@code null} or no
     * documentable metadata is present.
     */
    fun forSchema(schema: SingleDataSchema?, includeAllowedValues: Boolean = true): String? {
        if (!enabled || schema == null) {
            return null
        }

        val json = schema.toJson()
        val blocks = mutableListOf<String>()

        schema.title.getOrNull()?.toString()?.takeIf { it.isNotBlank() }?.let { blocks.add(it.trim()) }
        schema.description.getOrNull()?.toString()?.takeIf { it.isNotBlank() }?.let { blocks.add(it.trim()) }

        val facts = mutableListOf<String>()
        schema.unit.getOrNull()?.takeIf { it.isNotBlank() }?.let { facts.add("**Unit:** `$it`") }
        schema.atType.getOrNull()?.toString()?.takeIf { it.isNotBlank() }
            ?.let { facts.add("**Semantic type (`@type`):** `$it`") }
        schema.format.getOrNull()?.takeIf { it.isNotBlank() }?.let { facts.add("**Format:** `$it`") }
        schema.default.getOrNull()?.let { facts.add("**Default:** `${it.formatAsString()}`") }
        schema.const.getOrNull()?.let { facts.add("**Constant value:** `${it.formatAsString()}`") }
        if (includeAllowedValues && schema.enum.isNotEmpty()) {
            facts.add("**Allowed values:** " + schema.enum.joinToString(", ") { "`${it.formatAsString()}`" })
        }
        if (schema.isReadOnly) {
            facts.add("*Read-only.*")
        }
        if (schema.isWriteOnly) {
            facts.add("*Write-only.*")
        }
        if (facts.isNotEmpty()) {
            blocks.add(facts.joinToString("\n\n"))
        }

        val constraints = collectConstraints(json)
        if (constraints.isNotEmpty()) {
            blocks.add("**Constraints:**\n" + constraints.joinToString("\n") { " - ${it.first}: `${it.second}`" })
        }

        return blocks.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
    }

    /**
     * Builds a KDoc string from free-form text parts (e.g. an interaction's title and description), joining the
     * non-blank parts with blank lines. Useful for elements that are not [SingleDataSchema]s, such as WoT actions.
     *
     * @param parts the text fragments to include; {@code null} or blank entries are skipped.
     * @return the KDoc text, or {@code null} when KDoc generation is disabled or no text is present.
     */
    fun forText(vararg parts: String?): String? {
        if (!enabled) {
            return null
        }
        val blocks = parts.filterNotNull().map { it.trim() }.filter { it.isNotBlank() }
        return blocks.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
    }

    private fun collectConstraints(json: org.eclipse.ditto.json.JsonObject): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        (numericConstraints + stringConstraints + arrayConstraints).forEach { (field, label) ->
            json.getValue(JsonPointer.of(field)).getOrNull()?.let { result.add(label to it.formatAsString()) }
        }
        return result
    }
}
