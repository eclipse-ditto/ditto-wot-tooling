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
package org.eclipse.ditto.wot.kotlin.generator.strategy

import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.eclipse.ditto.json.JsonValue

/**
 * Interface for enum generation strategies.
 *
 * This interface defines the contract for different enum generation strategies.
 * Implementations can generate enums either inline within classes or as separate files.
 */
interface IEnumGenerationStrategy {

    /**
     * Generates an enum from a schema.
     *
     * @param propertySchema The property schema containing enum information
     * @param propertyName The name of the property
     * @param enumArray The array of enum values
     * @param schemaType The data schema type
     * @param packageName The package for the enum
     * @return The generated enum name
     */
    fun generateEnum(
        propertySchema: SingleDataSchema?,
        propertyName: String?,
        enumArray: MutableSet<JsonValue>,
        schemaType: DataSchemaType,
        packageName: String = "",
        parentClassName: String? = null
    ): String

    /**
     * Generates an action enum from action names.
     *
     * @param featureClassName The name of the feature class
     * @param packageName The package name
     * @param wotActions The list of WoT actions
     * @return The generated enum name
     */
    fun generateActionEnum(
        featureClassName: String,
        packageName: String,
        wotActions: List<String>
    ): String

    /**
     * Adds enums to a type specification.
     *
     * @param fields The fields that may contain enums
     * @param typeSpecBuilder The type spec builder to add enums to
     * @return The updated fields with enum references
     */
    fun addEnumsToTypeSpec(
        fields: List<Pair<String, com.squareup.kotlinpoet.PropertySpec>>,
        typeSpecBuilder: com.squareup.kotlinpoet.TypeSpec.Builder
    ): List<Pair<String, com.squareup.kotlinpoet.PropertySpec>>
}
