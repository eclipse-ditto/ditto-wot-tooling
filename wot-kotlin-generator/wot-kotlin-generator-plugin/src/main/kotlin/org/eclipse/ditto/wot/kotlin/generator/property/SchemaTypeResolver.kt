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
package org.eclipse.ditto.wot.kotlin.generator.property

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import org.eclipse.ditto.wot.kotlin.generator.clazz.ClassGenerator
import org.eclipse.ditto.wot.model.ArraySchema
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

/**
 * Resolves WoT data schemas to Kotlin type names.
 *
 * This object is responsible for converting WoT data schemas into appropriate Kotlin types.
 * It handles all WoT data types (boolean, integer, number, string, object, array, null)
 * and determines the appropriate Kotlin type representation.
 *
 * The resolver works in conjunction with [WrapperTypeChecker] to determine if primitive
 * types should be wrapped in enum types or other wrapper classes.
  *
 */
object SchemaTypeResolver {
    private val classGenerator = ClassGenerator
    private val wrapperTypeChecker = WrapperTypeChecker
    private val logger = LoggerFactory.getLogger(SchemaTypeResolver::class.java)

    /**
     * Resolves a WoT data schema to a Kotlin type name.
     *
     * This method handles all WoT data types:
     * - Boolean → Boolean (nullable)
     * - Integer/Number → Wrapped type (may be enum) or primitive
     * - String → Wrapped type (may be enum) or String
     * - Object → Generated class or enum
     * - Array → List<T> (nullable)
     * - Null → Unit (nullable)
     *
     * @param dataSchema The WoT data schema to resolve
     * @param packageName The package where generated classes should be placed
     * @param role The role of the property in the hierarchy
     * @param fieldName Optional field name for schema naming
     * @param parentClassName Optional parent class name for nested schemas
     * @return A Kotlin type name representing the schema
     */
    /**
     * Resolves the Kotlin type for a schema.
     *
     * @param schema The schema to resolve
     * @param packageName The package for the type
     * @param role The role of the property
     * @param fallbackTitle The fallback title for the type
     * @return The resolved Kotlin type name
     */
    fun resolveSchemaType(
        dataSchema: SingleDataSchema,
        packageName: String,
        role: PropertyRole,
        fieldName: String? = null,
        parentClassName: String? = null
    ): TypeName {
        val schemaTitle = selectSchemaName(dataSchema.title.getOrNull()?.toString(), fieldName)
        return when (dataSchema.type.get()) {
            DataSchemaType.BOOLEAN -> BOOLEAN.copy(nullable = true)
            DataSchemaType.INTEGER -> wrapperTypeChecker.checkForWrapperType(
                dataSchema,
                schemaTitle,
                packageName,
                DataSchemaType.INTEGER,
                role,
                parentClassName = parentClassName
            )
            DataSchemaType.NUMBER -> wrapperTypeChecker.checkForWrapperType(
                dataSchema,
                schemaTitle,
                packageName,
                DataSchemaType.NUMBER,
                role,
                parentClassName = parentClassName
            )
            DataSchemaType.STRING -> wrapperTypeChecker.checkForWrapperType(
                dataSchema,
                schemaTitle,
                packageName,
                DataSchemaType.STRING,
                role,
                parentClassName = parentClassName
            )
            DataSchemaType.OBJECT -> {
                if ((dataSchema as ObjectSchema).enum.isNotEmpty()) {
                    wrapperTypeChecker.checkForWrapperType(
                        dataSchema,
                        schemaTitle,
                        packageName,
                        DataSchemaType.OBJECT,
                        role,
                        parentClassName = parentClassName
                    )
                }else{
                    classGenerator.generateClassFrom(
                        schemaTitle!!,
                        dataSchema,
                        packageName,
                        role,
                        parentClassName = parentClassName
                    )
                }

            }

            DataSchemaType.ARRAY -> resolveArraySchemaType(dataSchema as ArraySchema, packageName, role, schemaTitle!!)
            DataSchemaType.NULL -> UNIT.copy(nullable = true)
        }
    }

    /**
     * Resolves an array schema to a Kotlin list type.
     *
     * This method handles array schemas by:
     * 1. Determining the type of array items
     * 2. Recursively resolving nested arrays
     * 3. Generating classes for object items if needed
     * 4. Creating a List<T> type with appropriate item type
     *
     * @param arraySchema The array schema to resolve
     * @param packageName The package for generated classes
     * @param role The property role
     * @param fallbackTitle Fallback title for naming
     * @return A Kotlin type name representing List<T>
     */
    /**
     * Resolves the Kotlin type for an array schema.
     *
     * @param arraySchema The array schema to resolve
     * @param packageName The package for the type
     * @param role The role of the property
     * @param fallbackTitle The fallback title for the type
     * @return The resolved Kotlin type name
     */
    fun resolveArraySchemaType(arraySchema: ArraySchema, packageName: String, role: PropertyRole, fallbackTitle: String): TypeName {
        logger.debug("Resolving array items schema: ${arraySchema.toJson()}")
        val arrayName = arraySchema.title.getOrNull()?.toString() ?: fallbackTitle
        val parametrizedType = arraySchema.items.get().let {
            when (it) {
                is ArraySchema -> resolveArraySchemaType(it, arrayName, role, "${fallbackTitle}Array")
                // Note that ObjectSchema is SingleDataSchema, so we first need to
                // check for ObjectSchema as it is more specific
                is ObjectSchema -> classGenerator.generateClassFrom("${arrayName}Item", it, packageName, role, parentClassName = arrayName)
                is SingleDataSchema -> resolveSchemaType(it, packageName, role, parentClassName = arrayName)
                else -> {
                    throw IllegalArgumentException("Unsupported type for array items")
                }
            }
        }
        return MUTABLE_LIST.parameterizedBy(parametrizedType.copy(nullable = false)).copy(nullable = true)
    }

    /**
     * Selects the appropriate name for a schema.
     *
     * Prioritizes the field name over the schema title for better naming.
     *
     * @param title The schema title
     * @param fieldName The field name
     * @return The selected name for the schema
     */
    private fun selectSchemaName(title: String?, fieldName: String?): String? {
        return fieldName ?: title
    }
}
