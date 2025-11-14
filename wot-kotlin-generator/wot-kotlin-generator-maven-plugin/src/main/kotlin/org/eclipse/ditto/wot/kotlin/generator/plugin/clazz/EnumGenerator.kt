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
package org.eclipse.ditto.wot.kotlin.generator.plugin.clazz

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.ditto.json.JsonValue
import org.eclipse.ditto.wot.kotlin.generator.common.model.enum.JsonEnum
import org.eclipse.ditto.wot.kotlin.generator.plugin.util.*
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.ObjectSchema
import org.eclipse.ditto.wot.model.SingleDataSchema
import kotlin.jvm.optionals.getOrNull

object EnumGenerator {

    private val classGenerator = ClassGenerator
    /**
     * Generates an enum from a schema.
     *
     * @param enumName The name for the enum
     * @param packageName The package for the enum
     * @param schema The schema containing enum values
     * @param parentClassName The parent class name for naming strategy
     * @return The generated enum name
     */
    fun generateEnum(
        propertySchema: SingleDataSchema?,
        propertyName: String?,
        enumArray: MutableSet<JsonValue>,
        schemaType: DataSchemaType,
        parentClassName: String? = null
    ): String {
        val enumName = asClassName(propertyName!!)
        val enumValues = asListOf(enumArray, schemaType)
        return when (schemaType) {
            DataSchemaType.INTEGER -> generateEnumWithProperty(
                enumName,
                enumValues,
                LONG
            )

            DataSchemaType.NUMBER  -> generateEnumWithProperty(
                enumName,
                enumValues,
                DOUBLE
            )

            DataSchemaType.STRING  -> generateStringEnum(enumName, enumValues)

            DataSchemaType.OBJECT  -> generateObjectEnum(propertySchema as ObjectSchema, enumName, enumArray)

            else                   -> throw IllegalArgumentException("Unsupported type for enum property $schemaType")
        }
    }

    /**
     * Generates an action enum from action names.
     *
     * @param className The class name for the enum
     * @param packageName The package for the enum
     * @param actionNames The list of action names
     * @return The generated enum name
     */
    fun generateActionEnum(
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
                asScreamingSnakeCase(it), TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%S", it)
                .build())
        }
        val enumClassName = ClassName(packageName, enumName)
        actionsEnumSpec.addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder("fromActionName")
                        .returns(enumClassName.copy(nullable = true))
                        .addParameter("actionName", STRING.copy(nullable = true))
                        .addCode("return·entries.firstOrNull·{·it.actionName·==·actionName·}")
                        .build()
                )
                .build()
        )

        ClassGenerator.generateNewClass(actionsEnumSpec.build(), enumName, packageName)
        return enumName
    }


    private fun generateStringEnum(enumName: String, enumValues: List<Any>): String {
        val firstEnumValue = enumValues.first().toString()
        val isSimpleEnum = asEnumConstantName(enumName, firstEnumValue) == firstEnumValue

        val enumSpecBuilder = TypeSpec.enumBuilder(enumName)

        if (isSimpleEnum) {
            enumValues.forEach {
                enumSpecBuilder.addEnumConstant(asEnumConstantName(enumName, it.toString()))
            }

            enumSpecBuilder.addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(
                        buildFromNameFunc(ClassName("", enumName))
                    )
                    .build()
            )
        } else {
            // Enum with custom values
            val valueType = STRING

            enumSpecBuilder.primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("_value", valueType)
                    .build()
            )
                .addProperty(PropertySpec.builder("_value", valueType).initializer("_value").build())
            addCustomEnumConstants(enumSpecBuilder, enumName, enumValues, valueType)

            enumSpecBuilder.addFunction(
                buildToValueFunc(valueType)
            )

            enumSpecBuilder.addType(
                TypeSpec.companionObjectBuilder().addFunction(
                    buildFromValueFunc(ClassName("", enumName), valueType)
                )
                    .build()
            )
        }

        val enumClassName = ClassName("", enumName)
        val pathFunSpec = ClassGenerator.createPathFunSpec(enumClassName.simpleName, false)

        ClassGenerator.mergeOrAddCompanionObject(enumSpecBuilder, pathFunSpec)
        val enumKey = EnumRegistry.registerEnum(enumSpecBuilder.build())

        return enumKey
    }

    private fun generateEnumWithProperty(
        enumName: String,
        enumValues: List<Any>,
        valueType: TypeName
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

        enumSpecBuilder.addFunction(
            buildToValueFunc(valueType)
        )

        enumSpecBuilder.addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    buildFromNameFunc(enumClassName)

                ).addFunction(
                    buildFromValueFunc(enumClassName, valueType)
                )
                .build()
        )
        val pathFunSpec = ClassGenerator.createPathFunSpec(enumClassName.simpleName, false)
        ClassGenerator.mergeOrAddCompanionObject(enumSpecBuilder, pathFunSpec)

        val enumKey = EnumRegistry.registerEnum(enumSpecBuilder.build())

        return enumKey
    }

    private fun generateObjectEnum(
        propertySchema: ObjectSchema,
        enumName: String,
        enumArray: MutableSet<JsonValue>
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

        // Add enum constants as sealed class objects
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

        // Add generic methods for retrieving enum by properties
        val companionBuilder = TypeSpec.companionObjectBuilder()
        propertySchema.properties.forEach { (propertyName, schema) ->
            val adjustedPropertyName = asPropertyName(propertyName)
            val typeName = asPrimitiveClassName(schema)
            companionBuilder.addFunction(
                FunSpec.builder("from${adjustedPropertyName.capitalize()}")
                    .returns(enumClassName.copy(nullable = true))
                    .addParameter(adjustedPropertyName, typeName.copy(nullable = true))
                    .addCode("return·INSTANCES.firstOrNull·{·it.$adjustedPropertyName·==·$adjustedPropertyName·}")
                    .build()
            )
        }

        // Add create method
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

        val conditions = propertySchema.properties.keys.joinToString("·&&·") {
            "it.${
                asPropertyName(it)
            }·==·${asPropertyName(it)}"
        }
        jsonCreatorBuilder.addCode("return·INSTANCES.firstOrNull·{·$conditions·}")

        companionBuilder.addFunction(jsonCreatorBuilder.build())
        companionBuilder.addProperty(
            PropertySpec.builder("INSTANCES", List::class.asClassName().parameterizedBy(enumClassName))
                .addModifiers(KModifier.PUBLIC)
                .delegate(
                    CodeBlock.of(
                        "lazy·{·listOf(${enumArray.joinToString(", ") { asValidEnumConstant(it.asObject().getValue("name").getOrNull().toString()) }})·}"
                    )
                )
                .build()
        )

        // Add HasPath interface and startPath method to the companion object
        val pathFunSpec = ClassGenerator.createPathFunSpec(enumClassName.simpleName, false)
        companionBuilder.addSuperinterface(ClassName(Const.COMMON_PACKAGE_PATH, "HasPath"))
        companionBuilder.addFunction(pathFunSpec)

        enumSpecBuilder.addType(companionBuilder.build())

        val enumSpec = enumSpecBuilder.build()
        val enumKey = EnumRegistry.registerEnum(enumSpec)
        return enumKey
    }


    private fun buildToValueFunc(valueType: TypeName): FunSpec {
        return FunSpec.builder("toValue")
            .addAnnotation(com.fasterxml.jackson.annotation.JsonValue::class)
            .returns(valueType)
            .addCode("return _value")
            .build()
    }

    private fun buildFromValueFunc(enumClassName: ClassName, valueType: TypeName): FunSpec {
        return FunSpec.builder("fromValue")
            .addAnnotation(JsonCreator::class)
            .returns(enumClassName.copy(nullable = true))
            .addParameter("v", valueType.copy(nullable = true))
            .addCode("return·entries.firstOrNull·{·it._value·==·v·}")
            .build()
    }


    private fun buildFromNameFunc(enumClassName: ClassName): FunSpec {
        return FunSpec.builder("fromName")
            .returns(enumClassName.copy(nullable = true))
            .addParameter("name", String::class.asTypeName().copy(nullable = true))
            .addCode("return·entries.firstOrNull·{·it.name·==·name·}")
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
                asEnumConstantName(enumName, it.toString()), TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter(if (valueType == STRING) "%S" else "%L", it)
                .build()
            )
        }
    }
}
