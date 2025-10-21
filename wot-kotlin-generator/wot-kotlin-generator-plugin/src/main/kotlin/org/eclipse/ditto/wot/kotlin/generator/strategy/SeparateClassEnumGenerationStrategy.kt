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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.ditto.wot.kotlin.generator.clazz.ClassGenerator
import org.eclipse.ditto.wot.kotlin.generator.model.enum.JsonEnum
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.kotlin.generator.util.*
import kotlin.jvm.optionals.getOrNull
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue as JacksonJsonValue

/**
 * Strategy for generating enums as separate class files.
 *
 * This strategy generates each enum as a standalone class file, which provides
 * better separation of concerns and allows for more complex enum structures.
 */
class SeparateClassEnumGenerationStrategy : IEnumGenerationStrategy {

    private val classGenerator = ClassGenerator

    override fun generateEnum(
        propertySchema: SingleDataSchema?,
        propertyName: String?,
        enumArray: MutableSet<JsonValue>,
        schemaType: DataSchemaType,
        packageName: String,
        parentClassName: String?
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
        return when (schemaType) {
            DataSchemaType.INTEGER -> generateEnumWithProperty(
                enumName,
                enumValues,
                LONG,
                packageName
            )
            DataSchemaType.NUMBER -> generateEnumWithProperty(
                enumName,
                enumValues,
                DOUBLE,
                packageName
            )
            DataSchemaType.STRING -> generateStringEnum(enumName, enumValues, packageName)
            DataSchemaType.OBJECT -> generateObjectEnum(propertySchema as ObjectSchema, enumName, enumArray, packageName)
            else -> throw IllegalArgumentException("Unsupported type for enum property $schemaType")
        }
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
        val firstEnumValue = enumValues.first().toString()
        val isSimpleEnum = asEnumConstantName(enumName, firstEnumValue) == firstEnumValue

        val enumSpecBuilder = TypeSpec.enumBuilder(enumName)

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

        val enumClassName = ClassName("", enumName)
        val enumSpec = enumSpecBuilder.build()
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        return enumName
    }

    private fun generateEnumWithProperty(
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName,
        packageName: String
    ): String {
        val enumSpecBuilder = TypeSpec.enumBuilder(enumName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("_value", valueType)
                    .build()
            )
            .addProperty(PropertySpec.builder("_value", valueType).initializer("_value").build())

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
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        return enumName
    }

    private fun generateObjectEnum(
        propertySchema: ObjectSchema,
        enumName: String,
        enumArray: MutableSet<JsonValue>,
        packageName: String
    ): String {
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
                FunSpec.builder("from${adjustedPropertyName.capitalize()}")
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

        val enumSpec = enumSpecBuilder.build()
        ClassGenerator.generateNewClass(enumSpec, enumName, packageName)

        return enumName
    }

    private fun buildToValueFunc(valueType: TypeName): FunSpec {
        return FunSpec.builder("toValue")
            .addAnnotation(JacksonJsonValue::class)
            .returns(valueType)
            .addCode("return _value")
            .build()
    }

    private fun buildFromValueFunc(enumClassName: ClassName, valueType: TypeName): FunSpec {
        return FunSpec.builder("fromValue")
            .addAnnotation(JsonCreator::class)
            .returns(enumClassName.copy(nullable = true))
            .addParameter("v", valueType.copy(nullable = true))
            .addCode("return entries.firstOrNull { it._value == v }")
            .build()
    }

    private fun buildFromNameFunc(enumClassName: ClassName): FunSpec {
        return FunSpec.builder("fromName")
            .returns(enumClassName.copy(nullable = true))
            .addParameter("name", String::class.asTypeName().copy(nullable = true))
            .addCode("return entries.firstOrNull { it.name == name }")
            .build()
    }

    private fun addCustomEnumConstants(
        enumSpecBuilder: TypeSpec.Builder,
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName
    ) {
        enumValues.forEach {
            enumSpecBuilder.addEnumConstant(
                asEnumConstantName(enumName, it.toString()),
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter(
                        if (valueType == STRING) "%S" else "%L",
                        it
                    )
                    .build()
            )
        }
    }
}
