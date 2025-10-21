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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import org.eclipse.ditto.wot.kotlin.generator.clazz.ClassGenerator
import org.eclipse.ditto.wot.kotlin.generator.util.*
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.model.*
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

object PropertyResolver {

    private val classGenerator = ClassGenerator
    private val wrapperTypeChecker = WrapperTypeChecker
    private val schemaTypeResolver = SchemaTypeResolver
    private val logger = LoggerFactory.getLogger(PropertyResolver::class.java)

    /**
     * Resolves properties from a properties map.
     *
     * @param properties The properties map to resolve
     * @param packageName The package for the properties
     * @param role The role of the properties
     * @return A map of property pairs to their specifications
     */
    fun resolveProperties(
        properties: Properties?,
        parentPackage: String,
        role: PropertyRole,
        feature: String? = null
    ): MutableMap<Pair<Property, PropertySpec>, String?> {
        return properties?.filter { it.value.type.isPresent || it.value.toJson().contains("tm:ref") }?.map {
            val dittoCategory = extractPropertyCategory(it.value.toJson())
            val type = resolvePropertyType(it.value, parentPackage, role, feature, dittoCategory)
            Pair(
                it.value,
                createPropertySpec(it.key, type)
            ) to dittoCategory
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
    }

    /**
     * Converts fields to property specifications.
     *
     * @param schemaMap The schema map containing field definitions
     * @param packageName The package for the properties
     * @param role The role of the properties
     * @param className The class name for the properties
     * @return A list of property pairs
     */
    fun convertFieldsToPropertySpecs(
        fields: Map<String, SingleDataSchema>,
        packageName: String,
        role: PropertyRole,
        parentClassName: String? = null
    ): List<Pair<String, PropertySpec>> {
        return fields.map {
            val poetType = schemaTypeResolver.resolveSchemaType(it.value, packageName, role, it.key, parentClassName)
            it.key to createPropertySpec(it.key, poetType)
        }
    }


    /**
     * Resolves oneOf properties from a schema.
     *
     * @param oneOfSchema The oneOf schema to resolve
     * @param packageName The package for the properties
     * @param role The role of the properties
     * @param className The class name for the properties
     * @return A list of property pairs
     */
    fun resolveOneOfProperties(
        objectSchema: ObjectSchema,
        packageName: String,
        role: PropertyRole
    ): List<Pair<String, PropertySpec>> {
        return objectSchema.oneOf.map {
            val oneOfProps = (it as ObjectSchema).properties
            val propCount = oneOfProps.size
            val firstPropName = oneOfProps.keys.firstOrNull().toString()
            val firstPropType = oneOfProps[firstPropName]?.type?.get()
            // If there is one primitive property, we will create alias
            if (isPrimitive(firstPropType) && propCount == 1) {
                val aliasClassName = asClassName(firstPropName)
                val primitiveType = oneOfProps[firstPropName]?.let { it1 -> asPrimitiveClassName(it1) }
                primitiveType?.let { it1 ->
                    classGenerator.generatePrimitiveTypeAlias(it1, aliasClassName, packageName)
                }
                listOf(
                    firstPropName to createPropertySpec(
                        firstPropName,
                        ClassName(packageName, aliasClassName).copy(nullable = true)
                    )
                )
            } else {
                val oneOfTitle = it.title.getOrNull()?.toString() ?: firstPropName
                val oneOfWithTitle = it.toBuilder().setTitle(Title.of(oneOfTitle)).build()
                convertFieldsToPropertySpecs(mapOf(oneOfTitle to oneOfWithTitle), packageName, role, oneOfTitle)
            }
        }.flatten()
    }


    private fun extractPropertyCategory(propertyJson: JsonObject): String? {
        val propertyCategoryString = propertyJson.getValue(JsonPointer.of("ditto:category"))
            .getOrNull()?.asString()
        logger.debug("Extracted property category: $propertyCategoryString")
        return propertyCategoryString
    }

    private fun resolvePropertyType(
        property: Property,
        propertyPackage: String,
        role: PropertyRole,
        feature: String?,
        dittoCategory: String?
    ): TypeName {
        return when (property.type.get()) {
            DataSchemaType.BOOLEAN -> wrapperTypeChecker.checkForWrapperType(
                property,
                property.propertyName,
                propertyPackage,
                DataSchemaType.BOOLEAN,
                role,
                feature,
                dittoCategory,
                parentClassName = null
            )

            DataSchemaType.INTEGER -> wrapperTypeChecker.checkForWrapperType(
                property,
                property.propertyName,
                propertyPackage,
                DataSchemaType.INTEGER,
                role,
                feature,
                dittoCategory,
                parentClassName = null
            )

            DataSchemaType.NUMBER  -> wrapperTypeChecker.checkForWrapperType(
                property,
                property.propertyName,
                propertyPackage,
                DataSchemaType.NUMBER,
                role,
                feature,
                dittoCategory,
                parentClassName = null
            )

            DataSchemaType.STRING  -> wrapperTypeChecker.checkForWrapperType(
                property,
                property.propertyName,
                propertyPackage,
                DataSchemaType.STRING,
                role,
                feature,
                dittoCategory,
                parentClassName = null
            )

            DataSchemaType.OBJECT  -> {
                if ((property.asObjectSchema()).enum.isNotEmpty()) {
                    wrapperTypeChecker.checkForWrapperType(
                        property.asObjectSchema(),
                        property.propertyName,
                        propertyPackage,
                        DataSchemaType.OBJECT,
                        role,
                        feature,
                        dittoCategory,
                        parentClassName = null
                    )
                } else {
                    val objectSchema = property.asObjectSchema()
                    classGenerator.generateClassFrom(
                        property.propertyName,
                        objectSchema,
                        propertyPackage,
                        role,
                        asObjectProperty = objectSchema.isPatternPropertiesSchema() || objectSchema.isAdditionalPropertiesSchema(),
                        feature = feature,
                        parentClassName = null,
                        dittoCategory = dittoCategory
                    )
                }
            }

            DataSchemaType.ARRAY   -> schemaTypeResolver.resolveArraySchemaType(
                property.asArraySchema(),
                propertyPackage,
                role,
                property.propertyName
            )

            DataSchemaType.NULL    -> UNIT.copy(nullable = true)
        }
    }

    // create property spec
    private fun createPropertySpec(
        name: String,
        type: TypeName
    ): PropertySpec {
        val propertyName = asPropertyName(name)
        val builder = PropertySpec.builder(propertyName, type.copy(nullable = true))
            .mutable(true)
            .initializer("null")
        if (propertyName != name) {
            builder.addAnnotation(buildJsonPropertyAnnotationSpec(name))
        }
        return builder.build()
    }


}



