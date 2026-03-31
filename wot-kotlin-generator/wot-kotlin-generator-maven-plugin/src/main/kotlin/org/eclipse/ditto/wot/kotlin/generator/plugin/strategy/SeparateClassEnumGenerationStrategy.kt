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
package org.eclipse.ditto.wot.kotlin.generator.plugin.strategy

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.kotlin.generator.common.model.enum.JsonEnum
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.ClassGenerator
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.EnumRegistry
import org.eclipse.ditto.wot.kotlin.generator.plugin.clazz.SharedTypeRegistry
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.*
import org.slf4j.LoggerFactory
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema
import kotlin.jvm.optionals.getOrNull

/**
 * Strategy for generating enums as separate class files.
 *
 * This strategy generates each enum as a standalone class file, which provides
 * better separation of concerns and allows for more complex enum structures.
 */
class SeparateClassEnumGenerationStrategy : IEnumGenerationStrategy {

    override val isInline: Boolean = false

    private val classGenerator = ClassGenerator
    private val logger = LoggerFactory.getLogger(SeparateClassEnumGenerationStrategy::class.java)

    override fun generateEnum(
        propertySchema: SingleDataSchema?,
        propertyName: String?,
        enumArray: MutableSet<JsonValue>,
        schemaType: DataSchemaType,
        packageName: String,
        parentClassName: String?,
        tmRefUrl: String?
    ): String {
        val config = classGenerator.getConfig()
        val enumName = try {
            if (config != null && config.classNamingStrategy != null) {
                asClassNameWithStrategy(propertyName ?: "Enum", null, config.classNamingStrategy, emptySet())
            } else {
                asClassName(propertyName ?: "Enum")
            }
        } catch (e: Exception) {
            asClassName(propertyName ?: "Enum")
        }
        val enumValues = asListOf(enumArray, schemaType)
        val enumConstantNames = buildEnumConstantNames(enumName, enumValues, enumArray, schemaType)

        if (config != null && config.deduplicateReferencedTypes && tmRefUrl != null) {
            val existing = SharedTypeRegistry.findEnumByRef(tmRefUrl)
            if (existing != null) {
                logger.debug("Dedup: Reusing enum {} for '{}' via tm:ref {}", existing.canonicalName, enumName, tmRefUrl)
                // Register the existing enum's ClassName for the current package so
                // WrapperTypeChecker can resolve the cross-package reference
                SharedTypeRegistry.registerEnumAlias(packageName, enumName, existing)
                return existing.simpleName!!
            }
        }

        if (config?.deduplicateReferencedTypes == true) {
            val existing = SharedTypeRegistry.findExistingEnum(enumConstantNames, schemaType)
            if (existing != null && existing.simpleName == enumName) {
                logger.debug("Dedup: Reusing enum {} for '{}' (same values)", existing.canonicalName, enumName)
                SharedTypeRegistry.registerEnumAlias(packageName, enumName, existing)
                return existing.simpleName!!
            }
        }

        val resolvedEnumName = resolveEnumNameConflict(
            enumName,
            enumConstantNames,
            packageName,
            parentClassName,
            config?.deduplicateReferencedTypes == true
        )
            ?: return enumName

        val result = when (schemaType) {
            DataSchemaType.INTEGER -> generateEnumWithProperty(
                resolvedEnumName,
                enumValues,
                LONG,
                packageName
            )
            DataSchemaType.NUMBER -> generateEnumWithProperty(
                resolvedEnumName,
                enumValues,
                DOUBLE,
                packageName
            )
            DataSchemaType.STRING -> generateStringEnum(resolvedEnumName, enumValues, packageName)
            DataSchemaType.OBJECT -> generateObjectEnum(propertySchema as ObjectSchema, resolvedEnumName, enumArray, packageName)
            else -> throw IllegalArgumentException("Unsupported type for enum property $schemaType")
        }

        if (config?.deduplicateReferencedTypes == true && tmRefUrl != null) {
            SharedTypeRegistry.registerEnumByRef(tmRefUrl, ClassName(packageName, result))
        }

        return result
    }

    override fun generateActionEnum(
        featureClassName: String,
        packageName: String,
        wotActions: List<String>
    ): String {
        val enumName = asClassName(featureClassName) + "Action"
        classGenerator.checkForEnumConflict(packageName, enumName)

        val primaryConstructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("actionName", STRING).build())
            .build()

        val actionsEnumSpec = TypeSpec.enumBuilder(enumName)
            .primaryConstructor(primaryConstructor)
            .addProperty(PropertySpec.builder("actionName", STRING).initializer("actionName").build())

        wotActions.forEach {
            actionsEnumSpec.addEnumConstant(
                asScreamingSnakeCase(it),
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%S", it)
                    .build()
            )
        }

        val enumClassName = ClassName(packageName, enumName)
        actionsEnumSpec.addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder("fromActionName")
                        .returns(enumClassName.copy(nullable = true))
                        .addParameter("actionName", STRING.copy(nullable = true))
                        .addCode("return entries.firstOrNull { it.actionName == actionName }")
                        .build()
                )
                .build()
        )

        ClassGenerator.generateNewClass(actionsEnumSpec.build(), enumName, packageName)
        return enumName
    }

    override fun addEnumsToTypeSpec(
        fields: List<Pair<String, PropertySpec>>,
        typeSpecBuilder: TypeSpec.Builder
    ): List<Pair<String, PropertySpec>> {
        return fields
    }

    private fun generateStringEnum(enumName: String, enumValues: List<Any>, packageName: String): String {
        val config = classGenerator.getConfig()
        val firstEnumValue = enumValues.first().toString()
        val isSimpleEnum = asEnumConstantName(enumName, firstEnumValue) == firstEnumValue

        val enumSpecBuilder = TypeSpec.enumBuilder(enumName)

        val enumConstantNames = enumValues.map {
            asEnumConstantName(enumName, it.toString())
        }.toSet()

        if (isSimpleEnum) {
            enumValues.forEach {
                enumSpecBuilder.addEnumConstant(asEnumConstantName(enumName, it.toString()))
            }

            enumSpecBuilder.addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(buildFromNameFunc(ClassName("", enumName)))
                    .build()
            )
        } else {
            val valueType = STRING

            enumSpecBuilder.primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("_value", valueType)
                    .build()
            )
            .addProperty(PropertySpec.builder("_value", valueType).initializer("_value").build())

            addCustomEnumConstants(enumSpecBuilder, enumName, enumValues, valueType)
            enumSpecBuilder.addFunction(buildToValueFunc(valueType))
            enumSpecBuilder.addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(buildFromValueFunc(ClassName("", enumName), valueType))
                    .build()
            )
        }

        val enumSpec = enumSpecBuilder.build()
        
        classGenerator.checkForEnumConflict(packageName, enumName, enumConstantNames)
        EnumRegistry.registerEnum(enumSpec, packageName, enumConstantNames)
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        if (config?.deduplicateReferencedTypes == true) {
            SharedTypeRegistry.registerEnum(enumConstantNames, DataSchemaType.STRING, ClassName(packageName, enumName))
        }

        return enumName
    }

    private fun generateEnumWithProperty(
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName,
        packageName: String
    ): String {
        val config = classGenerator.getConfig()
        val enumSpecBuilder = TypeSpec.enumBuilder(enumName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("_value", valueType)
                    .build()
            )
            .addProperty(PropertySpec.builder("_value", valueType).initializer("_value").build())

        val enumConstantNames = enumValues.map {
            asEnumConstantName(enumName, it.toString())
        }.toSet()

        addCustomEnumConstants(enumSpecBuilder, enumName, enumValues, valueType)

        val enumClassName = ClassName("", enumName)
        enumSpecBuilder.addFunction(buildToValueFunc(valueType))
        enumSpecBuilder.addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(buildFromNameFunc(enumClassName))
                .addFunction(buildFromValueFunc(enumClassName, valueType))
                .build()
        )

        val enumSpec = enumSpecBuilder.build()
        
        classGenerator.checkForEnumConflict(packageName, enumName, enumConstantNames)
        EnumRegistry.registerEnum(enumSpec, packageName, enumConstantNames)
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        if (config?.deduplicateReferencedTypes == true) {
            val schemaType = if (valueType == LONG) DataSchemaType.INTEGER else DataSchemaType.NUMBER
            SharedTypeRegistry.registerEnum(enumConstantNames, schemaType, ClassName(packageName, enumName))
        }

        return enumName
    }

    private fun generateObjectEnum(
        propertySchema: ObjectSchema,
        enumName: String,
        enumArray: MutableSet<JsonValue>,
        packageName: String
    ): String {
        val config = classGenerator.getConfig()
        val enumSpecBuilder = TypeSpec.classBuilder(enumName)
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(JsonEnum::class)

        val primaryConstructorBuilder = FunSpec.constructorBuilder()
        val propertiesCodeBuilder = CodeBlock.builder()
        propertiesCodeBuilder.add("return mapOf(")

        propertySchema.properties.forEach { (propertyName, schema) ->
            val adjustedPropertyName = asPropertyName(propertyName)
            val typeName = asPrimitiveClassName(schema)
            val propertySpecBuilder = PropertySpec.builder(adjustedPropertyName, typeName)
                .initializer(adjustedPropertyName)

            if (adjustedPropertyName != propertyName) {
                propertySpecBuilder.addAnnotation(
                    AnnotationSpec.builder(JsonProperty::class)
                        .addMember("%S", propertyName)
                        .build()
                )
            }

            primaryConstructorBuilder.addParameter(adjustedPropertyName, typeName)
            enumSpecBuilder.addProperty(propertySpecBuilder.build())
            propertiesCodeBuilder.add("%S to this.$adjustedPropertyName, ", propertyName)
        }

        propertiesCodeBuilder.add(")")
        enumSpecBuilder.primaryConstructor(primaryConstructorBuilder.build())
        enumSpecBuilder.addFunction(
            FunSpec.builder("getProperties")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Map::class.asClassName().parameterizedBy(String::class.asClassName(),
                    Any::class.asTypeName().copy(nullable = true)))
                .addCode(propertiesCodeBuilder.build())
                .build()
        )

        enumArray.forEach { enumValue ->
            val enumObject = enumValue.asObject()
            val objectName = asValidEnumConstant(enumObject.getValue("name").getOrNull().toString())
            val objectBuilder = TypeSpec.objectBuilder(objectName)
                .superclass(ClassName("", enumName))

            propertySchema.properties.forEach { (propertyName, schema) ->
                val jsonValue = enumObject.getValue(propertyName).getOrNull()
                val value = extractAndFormatValue(schema.type.get(), jsonValue)
                objectBuilder.addSuperclassConstructorParameter(value.first, value.second)
            }

            enumSpecBuilder.addType(objectBuilder.build())
        }

        val enumClassName = ClassName("", enumName)

        val companionBuilder = TypeSpec.companionObjectBuilder()
        propertySchema.properties.forEach { (propertyName, schema) ->
            val adjustedPropertyName = asPropertyName(propertyName)
            val typeName = asPrimitiveClassName(schema)
            companionBuilder.addFunction(
                FunSpec.builder("from${adjustedPropertyName.replaceFirstChar { it.titlecase() }}")
                    .returns(enumClassName.copy(nullable = true))
                    .addParameter(adjustedPropertyName, typeName.copy(nullable = true))
                    .addCode("return INSTANCES.firstOrNull { it.$adjustedPropertyName == $adjustedPropertyName }")
                    .build()
            )
        }

        val jsonCreatorBuilder = FunSpec.builder("create")
            .addAnnotation(JsonCreator::class)
            .addAnnotation(JvmStatic::class)
            .returns(enumClassName.copy(nullable = true))

        val parameterSpecBuilders = propertySchema.properties.map { (propertyName, schema) ->
            val adjustedPropertyName = asPropertyName(propertyName)
            ParameterSpec.builder(adjustedPropertyName, asPrimitiveClassName(schema).copy(nullable = true))
                .addAnnotation(
                    AnnotationSpec.builder(JsonProperty::class)
                        .addMember("%S", propertyName)
                        .build()
                )
                .build()
        }

        parameterSpecBuilders.forEach { jsonCreatorBuilder.addParameter(it) }

        val conditions = propertySchema.properties.keys.joinToString(" && ") {
            "it.${asPropertyName(it)} == ${asPropertyName(it)}"
        }
        jsonCreatorBuilder.addCode("return INSTANCES.firstOrNull { $conditions }")

        companionBuilder.addFunction(jsonCreatorBuilder.build())
        companionBuilder.addProperty(
            PropertySpec.builder("INSTANCES", List::class.asClassName().parameterizedBy(enumClassName))
                .addModifiers(KModifier.PUBLIC)
                .delegate(
                    CodeBlock.of(
                        "lazy { listOf(${enumArray.joinToString(", ") {
                            asValidEnumConstant(it.asObject().getValue("name").getOrNull().toString())
                        }}) }"
                    )
                )
                .build()
        )

        val pathFunSpec = ClassGenerator.createPathFunSpec(enumClassName.simpleName, false)
        companionBuilder.addSuperinterface(ClassName(Const.COMMON_PACKAGE_PATH, "HasPath"))
        companionBuilder.addFunction(pathFunSpec)

        enumSpecBuilder.addType(companionBuilder.build())

        val enumConstantNames = enumArray.map {
            asValidEnumConstant(it.asObject().getValue("name").getOrNull().toString())
        }.toSet()

        val enumSpec = enumSpecBuilder.build()
        classGenerator.checkForEnumConflict(packageName, enumName, enumConstantNames)
        EnumRegistry.registerEnum(enumSpec, packageName, enumConstantNames)
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        if (config?.deduplicateReferencedTypes == true) {
            SharedTypeRegistry.registerEnum(enumConstantNames, DataSchemaType.OBJECT, ClassName(packageName, enumName))
        }

        return enumName
    }

    private fun buildToValueFunc(valueType: TypeName) = EnumBuilderHelper.buildToValueFunc(valueType)
    private fun buildFromValueFunc(enumClassName: ClassName, valueType: TypeName) = EnumBuilderHelper.buildFromValueFunc(enumClassName, valueType)
    private fun buildFromNameFunc(enumClassName: ClassName) = EnumBuilderHelper.buildFromNameFunc(enumClassName)

    private fun addCustomEnumConstants(
        enumSpecBuilder: TypeSpec.Builder,
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName
    ) = EnumBuilderHelper.addCustomEnumConstants(enumSpecBuilder, enumName, enumValues, valueType, useSeparateClassNaming = true)

    /**
     * Resolves enum naming conflicts by prefixing with the parent class name
     * when an enum with the same name but different values already exists.
     * Returns null to signal "reuse existing" when values are identical (dedup).
     */
    private fun resolveEnumNameConflict(
        enumName: String,
        enumConstantNames: Set<String>,
        packageName: String,
        parentClassName: String?,
        deduplicateReferencedTypes: Boolean
    ): String? {
        val existingEnum = EnumRegistry.getEnumByNameInPackage(enumName, packageName)
        if (existingEnum != null) {
            val existingConstants = EnumRegistry.getEnumValuesInPackage(packageName, enumName)
                ?: existingEnum.enumConstants.keys.toSet()
            if (existingConstants == enumConstantNames) {
                if (deduplicateReferencedTypes) {
                    logger.debug("Dedup: Reusing enum '{}' in {} (same values)", enumName, packageName)
                    return null  // signal: reuse existing
                }
            }
            if (parentClassName != null) {
                val prefixed = "${asClassName(parentClassName)}${asClassName(enumName)}"
                logger.debug("Enum name conflict for '$enumName' in $packageName - resolving as '$prefixed'")
                return prefixed
            }
        }
        return enumName
    }

    private fun buildEnumConstantNames(
        enumName: String,
        enumValues: List<Any>,
        enumArray: MutableSet<JsonValue>,
        schemaType: DataSchemaType
    ): Set<String> {
        return when (schemaType) {
            DataSchemaType.STRING,
            DataSchemaType.INTEGER,
            DataSchemaType.NUMBER -> enumValues.map { asEnumConstantName(enumName, it.toString()) }.toSet()
            DataSchemaType.OBJECT -> enumArray.map {
                asValidEnumConstant(it.asObject().getValue("name").getOrNull().toString())
            }.toSet()
            else -> emptySet()
        }
    }

}
