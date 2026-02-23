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
package org.eclipse.ditto.wot.kotlin.generator.plugin.dsl

import com.squareup.kotlinpoet.*
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.kotlin.generator.plugin.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.plugin.property.PropertyRole
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.*
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.Property
import kotlin.jvm.optionals.getOrNull

/**
 * DSL generator for creating fluent Kotlin APIs.
 *
 * This object is responsible for generating DSL (Domain Specific Language) functions
 * that provide fluent APIs for creating and configuring generated classes. It supports
 * both regular and suspend DSL functions based on configuration.
 *
 * The DSL generator creates builder functions that allow users to construct objects
 * using a fluent, readable syntax.
 */
object DslGenerator {

    private var config: GeneratorConfiguration? = null

    /**
     * Sets the generator configuration for DSL generation.
     *
     * @param configuration The configuration containing DSL settings
     */
    fun setConfiguration(configuration: GeneratorConfiguration) {
        config = configuration
    }

    /**
     * Generates DSL function specifications for attributes or features.
     *
     * Creates a DSL function that allows fluent construction of attribute or feature objects.
     * The function can be configured to be suspend or regular based on the configuration.
     *
     * @param identifier The identifier for the DSL function
     * @param name The name of the class to generate DSL for
     * @param package The package name for the class
     * @return A [FunSpec] representing the DSL function
     */
    fun generateAttributesOrFeaturesDslFunSpec(identifier: String, name: String, `package`: String): FunSpec {
        val className = ClassName(asPackageName(`package`), asClassName(name))
        val funSpecBuilder = FunSpec.builder(identifier)
            .returns(className)

        // Add suspend modifier if configured
        if (config?.generateSuspendDsl == true) {
            funSpecBuilder.addModifiers(KModifier.SUSPEND)
        }

        // Create appropriate lambda type based on suspend configuration
        val lambdaType = if (config?.generateSuspendDsl == true) {
            LambdaTypeName.get(receiver = className, returnType = UNIT).copy(suspending = true)
        } else {
            LambdaTypeName.get(receiver = className, returnType = UNIT)
        }

        funSpecBuilder.addParameter("block", lambdaType)

        listOf(
            "val·$identifier·=·${asClassName(name)}()",
            "$identifier.block()",
            "this.$identifier·=·$identifier",
            "return·$identifier"
        ).forEach {
            funSpecBuilder.addStatement(it)
        }

        return funSpecBuilder.build()
    }

    /**
     * Generates DSL function specifications for features.
     *
     * Creates a DSL function that allows fluent construction of feature objects.
     * The function can be configured to be suspend or regular based on the configuration.
     *
     * @param feature The feature name
     * @param featurePackage The package name for the feature
     * @param isEntry Whether this is an entry point DSL function
     * @return A [FunSpec] representing the DSL function
     */
    fun generateFeatureDslFunSpec(
        feature: String,
        featurePackage: String,
        isEntry: Boolean = false,
        deprecationNotice: DeprecationNotice? = null
    ): FunSpec {
        val className = ClassName(asPackageName(featurePackage), if (isEntry) feature else asClassName(feature))
        val propertyName = asPropertyName(feature)

        val bodyStatements =
            if (isEntry) generateModelFunBodyStatements(feature) else generateDslFunBodyStatements(feature)

        val funSpecBuilder = FunSpec.builder(propertyName)
            .returns(className)

        // Add suspend modifier if configured
        if (config?.generateSuspendDsl == true) {
            funSpecBuilder.addModifiers(KModifier.SUSPEND)
        }

        // Create appropriate lambda type based on suspend configuration
        val lambdaType = if (config?.generateSuspendDsl == true) {
            LambdaTypeName.get(receiver = className, returnType = UNIT).copy(suspending = true)
        } else {
            LambdaTypeName.get(receiver = className, returnType = UNIT)
        }

        funSpecBuilder.addParameter("block", lambdaType)
            .apply {
                bodyStatements.forEach { addStatement(it) }
            }
        addDeprecationAnnotation(funSpecBuilder, deprecationNotice, withBlockParameter = true)
        return funSpecBuilder.build()
    }

    /**
     * Generates property DSL function specifications.
     *
     * Creates DSL functions for object properties that allow fluent construction
     * of nested property objects.
     *
     * @param properties The list of property pairs (Property, PropertySpec)
     * @param propertyPackage The package name for the properties
     * @return A list of [FunSpec] representing the property DSL functions
     */
    fun generatePropertyFunSpecs(
        properties: List<Pair<Property, PropertySpec>>,
        propertyPackage: String
    ) = properties.mapNotNull {
        val prop = it.first
        val propSpec = it.second
        val deprecationNotice = extractDeprecationNotice(prop)
        val property = asPropertyName(propSpec.name)
        val propertyType = propSpec.type.copy(nullable = false)
        if (prop.enum.isNotEmpty()) {
            null
        } else if (prop.isArraySchema) {
            val arrayType = propertyType as? ParameterizedTypeName
            val itemType = arrayType?.typeArguments?.firstOrNull()
            if (itemType != null && isPrimitive(itemType)) {
                null
            } else {
                val itemName = "${prop.title.getOrNull()?.let { title -> asPropertyName(title.toString()) } ?:  property}Item"
                // Use the actual class name from the itemType instead of generating it
                val itemAsClass = when (itemType) {
                    is ClassName -> itemType.simpleName
                    is ParameterizedTypeName -> (itemType.rawType as? ClassName)?.simpleName ?: asClassName(itemName)
                    else -> asClassName(itemName)
                }
                val itemsPropertyClass = ClassName(propertyPackage, itemAsClass)
                val funSpecBuilder = FunSpec.builder(itemName)
                    .returns(itemsPropertyClass)

                // Add suspend modifier if configured
                if (config?.generateSuspendDsl == true) {
                    funSpecBuilder.addModifiers(KModifier.SUSPEND)
                }

                // Create appropriate lambda type based on suspend configuration
                val lambdaType = if (config?.generateSuspendDsl == true) {
                    LambdaTypeName.get(receiver = itemsPropertyClass, returnType = UNIT).copy(suspending = true)
                } else {
                    LambdaTypeName.get(receiver = itemsPropertyClass, returnType = UNIT)
                }

                funSpecBuilder.addParameter("block", lambdaType)
                    .addStatement("val·$itemName·=·$itemAsClass()")
                    .addStatement("$itemName.block()")
                    .addStatement("if·($property·==·null)·{")
                    .addStatement("··$property·=·mutableListOf()")
                    .addStatement("}")
                    .addStatement("$property!!.add($itemName)")
                    .addStatement("return $itemName")
                addDeprecationAnnotation(funSpecBuilder, deprecationNotice, withBlockParameter = true)
                funSpecBuilder.build()
            }
        } else if (prop.isBooleanSchema || prop.isStringSchema || prop.isIntegerSchema || prop.isNumberSchema) {
            null
        } else {
            val typeClassName = when (propertyType) {
                is ClassName -> propertyType
                is ParameterizedTypeName -> propertyType.rawType as? ClassName ?: error("Unsupported type for property: $propertyType")
                else -> error("Unsupported type for property: $propertyType")
            }
            val funSpecBuilder = FunSpec.builder(property)
                .returns(typeClassName)

            // Add suspend modifier if configured
            if (config?.generateSuspendDsl == true) {
                funSpecBuilder.addModifiers(KModifier.SUSPEND)
            }

            // Create appropriate lambda type based on suspend configuration
            val lambdaType = if (config?.generateSuspendDsl == true) {
                LambdaTypeName.get(receiver = typeClassName, returnType = UNIT).copy(suspending = true)
            } else {
                LambdaTypeName.get(receiver = typeClassName, returnType = UNIT)
            }

            funSpecBuilder.addParameter("block", lambdaType)
                .addStatement("val·$property·=·${typeClassName.simpleName}()")
                .addStatement("$property.block()")
                .addStatement("this.$property·=·$property")
                .addStatement("return $property")
            addDeprecationAnnotation(funSpecBuilder, deprecationNotice, withBlockParameter = true)
            funSpecBuilder.build()
        }
    }

    /**
     * Generates DSL function specifications for object fields.
     *
     * Creates DSL functions for object properties that allow fluent construction
     * of nested object fields.
     *
     * @param fieldName The name of the field
     * @param packageName The package for the field type
     * @param fieldType The type of the field
     * @return A [FunSpec] representing the object field DSL function
     */
    fun generateObjectFieldDslFunSpec(
        fieldName: String,
        packageName: String,
        fieldType: TypeName,
        deprecationNotice: DeprecationNotice? = null
    ): FunSpec {
        val field = asPropertyName(fieldName)
        val typeClassName = when (fieldType) {
            is ClassName -> fieldType
            is ParameterizedTypeName -> fieldType.rawType as? ClassName ?: error("Unsupported type for field: $fieldType")
            else -> error("Unsupported type for field: $fieldType")
        }
        val funSpecBuilder = FunSpec.builder(field)
            .returns(typeClassName)

        // Add suspend modifier if configured
        if (config?.generateSuspendDsl == true) {
            funSpecBuilder.addModifiers(KModifier.SUSPEND)
        }

        // Create appropriate lambda type based on suspend configuration
        val lambdaType = if (config?.generateSuspendDsl == true) {
            LambdaTypeName.get(receiver = typeClassName, returnType = UNIT).copy(suspending = true)
        } else {
            LambdaTypeName.get(receiver = typeClassName, returnType = UNIT)
        }

        funSpecBuilder.addParameter("block", lambdaType)
            .addStatement("val·$field·=·${typeClassName.simpleName}()")
            .addStatement("$field.block()")
            .addStatement("this.$field·=·$field")
            .addStatement("return·$field")
        addDeprecationAnnotation(funSpecBuilder, deprecationNotice, withBlockParameter = true)
        return funSpecBuilder.build()
    }

    /**
     * Generates DSL function specifications for additional or pattern properties.
     *
     * Creates DSL functions for additional properties or pattern properties defined
     * in object schemas.
     *
     * @param propertyName The name of the property
     * @param objectSchema The object schema containing the property
     * @param propertyPackage The package for the property
     * @param role The role of the property
     * @return A [FunSpec] representing the DSL function or null if not applicable
     */
    fun generateAdditionalOrPatternPropertyDslFunSpec(
        propertyName: String,
        objectSchema: ObjectSchema,
        propertyPackage: String,
        role: PropertyRole
    ): FunSpec? {
        return if (objectSchema.isAdditionalPropertiesSchema()) {
            objectSchema.toJson()
                .getValue(JsonPointer.of("additionalProperties"))
                .getOrNull()?.takeIf { it.isObject }?.asObject()?.let {
                    generateAdditionalOrPatternPropertiesFunBody(propertyName, propertyPackage, null, false, false)
                }
        } else if (objectSchema.isPatternPropertiesSchema()) {
            objectSchema.toJson()
                .getValue(JsonPointer.of("patternProperties"))
                .getOrNull()?.takeIf { it.isObject }?.asObject()?.let { patternProperties ->
                    patternProperties.keys.firstOrNull()?.let {
                        val patternProperty = patternProperties.getField(it).getOrNull()
                        if (patternProperty?.value is JsonObject) {
                            val patternPropertyJson = patternProperty.value as JsonObject
                            val type = patternPropertyJson.getValue(JsonPointer.of("type")).getOrNull()?.asString()
                            val isArray = type == "array"
                            val isPrimitive = type != null && type != "array" && type != "object"
                            generateAdditionalOrPatternPropertiesFunBody(propertyName, propertyPackage, it.toString(), isArray, isPrimitive)
                        } else {
                            throw IllegalArgumentException("Pattern property is not Object: $patternProperties")
                        }
                    }
                }
        } else null
    }

    private fun generateModelFunBodyStatements(modelName: String): List<String> {
        val propertyName = asPropertyName(modelName)
        return listOf(
            "val·$propertyName·=·${asClassName(modelName)}()",
            "$propertyName.block()",
            "return·$propertyName"
        )
    }

    private fun generateDslFunBodyStatements(feature: String): List<String> {
        val featureAsProperty = asPropertyName(feature)
        return listOf(
            "val·$featureAsProperty·=·${asClassName(feature)}()",
            "$featureAsProperty.block()",
            "this.$featureAsProperty·=·$featureAsProperty",
            "return·$featureAsProperty"
        )
    }

    private fun generateAdditionalOrPatternPropertiesFunBody(
        propertyName: String,
        propertyPackage: String,
        keyPattern: String?,
        isArray: Boolean,
        isPrimitive: Boolean
    ): FunSpec {
        val singleItemNameAsClass = asClassName("${propertyName}Item")
        val singleItemNameClass = ClassName(propertyPackage, singleItemNameAsClass)

        val builder = if (isPrimitive) {
            val funSpecBuilder = FunSpec.builder("item")
                .returns(singleItemNameClass)
                .addParameter("key", String::class)
                .addParameter("value", singleItemNameClass)

            // Add suspend modifier if configured
            if (config?.generateSuspendDsl == true) {
                funSpecBuilder.addModifiers(KModifier.SUSPEND)
            }

            funSpecBuilder
        } else {
            val funSpecBuilder = FunSpec.builder("item")
                .returns(singleItemNameClass)
                .addParameter("key", String::class)

            // Add suspend modifier if configured
            if (config?.generateSuspendDsl == true) {
                funSpecBuilder.addModifiers(KModifier.SUSPEND)
            }

            // Create appropriate lambda type based on suspend configuration
            val lambdaType = if (config?.generateSuspendDsl == true) {
                LambdaTypeName.get(receiver = singleItemNameClass, returnType = UNIT).copy(suspending = true)
            } else {
                LambdaTypeName.get(receiver = singleItemNameClass, returnType = UNIT)
            }

            funSpecBuilder.addParameter("block", lambdaType)
        }

        if (keyPattern != null) {
            val escapedPattern = keyPattern.replace("\\", "\\\\")
            builder.addStatement("val keyPattern = java.util.regex.Pattern.compile(\"$escapedPattern\")")
            builder.addStatement(
                "check(keyPattern.matcher(key).matches()) {\n" +
                    "\"Key <\$key> did not match patternProperties pattern \" +\n\"<$escapedPattern> \" +\n" +
                    "\"of property <$propertyName>\"\n}"
            )
        }

        if (isPrimitive) {
            builder.addStatement("map[key]·=·value")
            builder.addStatement("return·value")
        } else {
            builder
                .addStatement("val·item·=·${singleItemNameAsClass}()")
                .addStatement("item.block()")

            if (isArray) {
                builder.addStatement("val list = map.getOrPut(key) { mutableListOf() }")
                builder.addStatement("list.add(item)")
            } else {
                builder.addStatement("map[key]·=·item")
            }

            builder.addStatement("return·item")
        }

        return builder.build()
    }

    private fun addDeprecationAnnotation(
        builder: FunSpec.Builder,
        notice: DeprecationNotice?,
        withBlockParameter: Boolean
    ) {
        if (notice == null || !notice.deprecated) return
        val replacementIdentifier = deprecationReplacementIdentifier(notice)
        val replaceWithExpression = if (replacementIdentifier != null && withBlockParameter) {
            "$replacementIdentifier(block)"
        } else if (replacementIdentifier != null) {
            "$replacementIdentifier()"
        } else {
            null
        }
        builder.addAnnotation(buildDeprecatedAnnotationSpec(notice, replaceWithExpression))
    }
}
