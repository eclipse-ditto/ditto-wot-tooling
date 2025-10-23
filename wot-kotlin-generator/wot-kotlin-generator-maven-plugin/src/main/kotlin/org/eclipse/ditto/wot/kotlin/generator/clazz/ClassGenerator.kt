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
package org.eclipse.ditto.wot.kotlin.generator.clazz

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.json.JsonPointer
import org.eclipse.ditto.wot.kotlin.generator.config.GeneratorConfiguration
import org.eclipse.ditto.wot.kotlin.generator.dsl.DslGenerator
import org.eclipse.ditto.wot.kotlin.generator.link.LinkRelationType
import org.eclipse.ditto.wot.kotlin.generator.property.PropertyResolver
import org.eclipse.ditto.wot.kotlin.generator.property.PropertyRole
import org.eclipse.ditto.wot.kotlin.generator.property.ReferencePropertyResolver
import org.eclipse.ditto.wot.kotlin.generator.property.SchemaTypeResolver
import org.eclipse.ditto.wot.kotlin.generator.serialize.ExplicitNullAwareSerializer
import org.eclipse.ditto.wot.kotlin.generator.strategy.EnumGenerationStrategyFactory
import org.eclipse.ditto.wot.kotlin.generator.strategy.IEnumGenerationStrategy
import org.eclipse.ditto.wot.kotlin.generator.util.*
import org.eclipse.ditto.wot.kotlin.generator.util.Const.ATTRIBUTES_CLASS_NAME
import org.eclipse.ditto.wot.kotlin.generator.util.Const.COMMON_PACKAGE
import org.eclipse.ditto.wot.kotlin.generator.util.Const.EXISTING_ATTRIBUTES_PACKAGE
import org.eclipse.ditto.wot.kotlin.generator.util.Const.EXISTING_FEATURES_PACKAGE
import org.eclipse.ditto.wot.kotlin.generator.util.Const.FEATURES_CLASS_NAME
import org.eclipse.ditto.wot.model.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

/**
 * Main class generator for creating Kotlin classes from WoT Thing Models.
 *
 * This object is responsible for generating Kotlin data classes, enums, and interfaces
 * from WoT Thing Model schemas. It handles class naming strategies, enum generation
 * strategies, and integrates with the DSL generator for creating fluent APIs.
 *
 * The generator supports multiple configuration options including:
 * - Enum generation strategies (INLINE or SEPARATE_CLASS)
 * - Class naming strategies (COMPOUND_ALL or ORIGINAL_THEN_COMPOUND)
 * - DSL generation with optional suspend function support
 */
object ClassGenerator {
    private const val EXPLICITLY_SET_NULL_FIELD_SUFFIX = ExplicitNullAwareSerializer.EXPLICITLY_SET_NULL_FIELD_SUFFIX

    private val logger = LoggerFactory.getLogger(ClassGenerator::class.java)
    private val propertyResolver = PropertyResolver
    private val referencePropertyResolver = ReferencePropertyResolver
    private val dslGenerator = DslGenerator
    private val enumGenerator = EnumGenerator
    private val schemaTypeResolver = SchemaTypeResolver

    private var outputDir: String = ""
    private var enumGenerationStrategy: IEnumGenerationStrategy? = null
    private var config: GeneratorConfiguration? = null

    /**
     * Gets the current generator configuration.
     *
     * @return The current configuration or null if not set
     */
    fun getConfig(): GeneratorConfiguration? = config

    /**
     * Sets the output directory for generated files.
     *
     * @param dir The directory path where generated files will be written
     */
    fun setOutputDir(dir: String) {
        outputDir = dir
    }

    /**
     * Sets the enum generation strategy and stores the configuration.
     *
     * This method configures the enum generation strategy based on the provided configuration
     * and stores the configuration for use by other components like the DSL generator.
     *
     * @param config The generator configuration containing enum strategy and other settings
     */
    fun setEnumGenerationStrategy(config: GeneratorConfiguration) {
        val strategy = EnumGenerationStrategyFactory.createStrategy(config)
        enumGenerationStrategy = strategy
        logger.info("Set enum generation strategy: ${config.enumGenerationStrategy}")

        this.config = config
        dslGenerator.setConfiguration(config)
    }

    /**
     * Generates attribute classes from a WoT Thing Model.
     *
     * Creates Kotlin data classes for thing attributes based on the properties
     * defined in the WoT Thing Model.
     *
     * @param parentPackage The base package for generated attribute classes
     * @param thingModel The WoT Thing Model containing attribute definitions
     */
    fun generateAttributesClass(parentPackage: String, thingModel: ThingModel) {
        val attributesPackage = "$parentPackage.attributes"
        val thingProperties = propertyResolver.resolveProperties(
            thingModel.properties.get(), attributesPackage, PropertyRole.ATTRIBUTE
        )
        val linkAttributes = emptyMap<Pair<Property, PropertySpec>, String?>()
        val allAttributes = (thingProperties.map { Pair(it.key.first, it.key.second) }
            + linkAttributes.map { Pair(it.key.first, it.key.second) }).toList()
        generateAttributesClass(allAttributes, attributesPackage)
    }

    /**
     * Generates action interfaces from a WoT Thing Model.
     *
     * Creates Kotlin interfaces for thing actions based on the actions
     * defined in the WoT Thing Model.
     *
     * @param rootPackageName The base package for generated action interfaces
     * @param thingModel The WoT Thing Model containing action definitions
     */
    fun generateThingActions(rootPackageName: String, thingModel: ThingModel) {
        val thingClassName = asClassName(thingModel.title.get().toString())
        val thingWotActions = thingModel.actions.getOrNull()
        thingWotActions?.map { it.value }
            ?.let {
                generateActionInterface(
                    thingClassName,
                    "$rootPackageName.actions",
                    ClassName("$COMMON_PACKAGE.actions", "ThingAction"),
                    it
                )
                enumGenerator.generateActionEnum(
                    thingClassName,
                    "$rootPackageName.actions",
                    it.map { action -> action.actionName })
            }
    }

    /**
     * Generates feature classes from WoT links.
     *
     * Creates Kotlin classes for thing features based on the links
     * defined in the WoT Thing Model.
     *
     * @param parentPackage The base package for generated feature classes
     * @param links The WoT links containing feature definitions
     */
    suspend fun generateFeaturesClass(parentPackage: String, links: Iterable<BaseLink<*>>) {
        val features = resolveFeatures(links, "$parentPackage.features")
        val featuresPackage = "$parentPackage.features"
        generateFeaturesClassFromFeatures(FEATURES_CLASS_NAME, features, featuresPackage)
    }

    /**
     * Generates a new class and writes it to the output directory.
     *
     * Creates a Kotlin class file with the specified type specification, adds
     * necessary imports and functions, and writes it to the output directory.
     * Also registers the class in the class registry to prevent conflicts.
     *
     * @param typeSpec The type specification for the class
     * @param className The name of the class
     * @param packageName The package for the class
     * @param funSpecs Additional functions to add to the file
     * @param imports Additional imports to add to the file
     * @param originalName The original name for path generation
     */
    fun generateNewClass(
        typeSpec: TypeSpec,
        className: String,
        packageName: String,
        funSpecs: List<FunSpec> = emptyList(),
        imports: List<String> = emptyList(),
        originalName: String? = null
    ) {
        logger.debug("Generating class $className in package $packageName ...")
        val isPropertiesClass = typeSpec.superinterfaces.keys.any {
            (it as? ParameterizedTypeName)?.rawType == ClassName(Const.COMMON_PACKAGE_FEATURES, "Properties")
        }

        val pathFunSpec = createPathFunSpec(className, isPropertiesClass, originalName)

        val newTypeSpecBuilder = typeSpec.toBuilder()

        mergeOrAddCompanionObject(newTypeSpecBuilder, pathFunSpec)

        val fileSpecBuilder = createFileSpecBuilder(newTypeSpecBuilder, className, packageName, funSpecs, imports)

        fileSpecBuilder.build().writeTo(Path(outputDir))
    }

    /**
     * Creates a path function specification for companion objects.
     *
     * Generates a `startPath()` function that returns the path segment for this class.
     * This is used in companion objects that implement the [HasPath] interface.
     *
     * @param className The name of the class
     * @param isPropertyClass Whether this is a property class
     * @param originalName The original name to use for the path, or null to use the class name
     * @return A [FunSpec] representing the startPath function
     */
    fun createPathFunSpec(className: String, isPropertyClass: Boolean, originalName: String? = null): FunSpec {
        val name = originalName ?: className.replaceFirstChar { it.lowercase() }
        return FunSpec.builder("startPath")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", name)
            .build()
    }

    /**
     * Merges or adds a companion object with path functionality.
     *
     * If a companion object already exists, it adds the path function to it.
     * Otherwise, it creates a new companion object with the path function.
     *
     * @param typeSpecBuilder The type spec builder to modify
     * @param pathFunSpec The path function specification to add
     */
    fun mergeOrAddCompanionObject(typeSpecBuilder: TypeSpec.Builder, pathFunSpec: FunSpec) {
        val existingCompanion = typeSpecBuilder.build().typeSpecs.find { it.isCompanion }

        if (existingCompanion != null) {
            val hasHasPathInterface = existingCompanion.superinterfaces.keys.any {
                it.toString().contains("HasPath")
            }

            val hasStartPathMethod = existingCompanion.funSpecs.any { it.name == "startPath" }

            if (hasHasPathInterface && hasStartPathMethod) {
                return
            }

            val modifiedCompanion = existingCompanion.toBuilder()
            if (!hasHasPathInterface) {
                modifiedCompanion.addSuperinterface(ClassName(Const.COMMON_PACKAGE_PATH, "HasPath"))
            }
            if (!hasStartPathMethod) {
                modifiedCompanion.addFunction(pathFunSpec)
            }

            typeSpecBuilder.typeSpecs.remove(existingCompanion)
            typeSpecBuilder.addType(modifiedCompanion.build())
        } else {
            val companionObjectSpec = TypeSpec.companionObjectBuilder()
                .addSuperinterface(ClassName(Const.COMMON_PACKAGE_PATH, "HasPath"))
                .addFunction(pathFunSpec)
                .build()
            typeSpecBuilder.addType(companionObjectSpec)
        }
    }

    private fun createFileSpecBuilder(
        typeSpecBuilder: TypeSpec.Builder,
        className: String,
        packageName: String,
        funSpecs: List<FunSpec>,
        imports: List<String>
    ): FileSpec.Builder {
        val fileSpecBuilder = FileSpec.builder(packageName, className)
            .addType(typeSpecBuilder.build())
            .addImport(Const.COMMON_PACKAGE_PATH, "HasPath")

        val superclass = typeSpecBuilder.build().superclass
        logger.info("Class $className - superclass: $superclass")
        if (superclass is ParameterizedTypeName && superclass.rawType.simpleName == "MapObjectProperty") {
            fileSpecBuilder.addImport(Const.COMMON_PACKAGE_FEATURES, "MapObjectProperty")
            logger.info("Class $className - added MapObjectProperty import")
        }

        funSpecs.forEach { fileSpecBuilder.addFunction(it) }
        imports.forEach {
            val importPackageName = it.substringBeforeLast(".")
            val importClassName = it.substringAfterLast(".")
            if (importClassName.isNotEmpty()) {
                fileSpecBuilder.addImport(importPackageName, importClassName)
            }
        }

        return fileSpecBuilder
    }

    /**
     * Generates a primitive type alias.
     *
     * Creates a type alias for primitive types when needed for property mapping.
     *
     * @param primitiveType The primitive type to create an alias for
     * @param className The name for the type alias class
     * @param packageName The package for the type alias
     */
    fun generatePrimitiveTypeAlias(primitiveType: TypeName, className: String, packageName: String) {
        logger.debug("Class $className will be typealias to primitive $primitiveType.")
        val typeAliasSpec = TypeAliasSpec.builder(className, primitiveType).build()
        val file = FileSpec.builder(packageName, className).addTypeAlias(typeAliasSpec).build()
        file.writeTo(Path(outputDir))
    }

    /**
     * Checks for enum naming conflicts.
     *
     * Verifies that an enum with the given name doesn't already exist in the package.
     *
     * @param packageName The package to check
     * @param enumName The enum name to check for conflicts
     * @throws IllegalArgumentException if the enum already exists
     */
    fun checkForEnumConflict(packageName: String, enumName: String) {
        val enumFilePath = Paths.get(outputDir, packageName.replace(".", "/"), "$enumName.kt")
        require(!Files.exists(enumFilePath)) { "Enum $enumName already exists in package $packageName" }
    }

    /**
     * Generates a class from an object schema.
     *
     * Creates a Kotlin data class based on the provided object schema, applying
     * naming strategies and conflict resolution as needed.
     *
     * @param name The base name for the class
     * @param objectSchema The object schema to generate the class from
     * @param packageName The package for the generated class
     * @param role The role of the property (attribute, feature, etc.)
     * @param asObjectProperty Whether this should be generated as an object property
     * @param feature The feature name if this is part of a feature
     * @param parentClassName The parent class name for naming strategy
     * @param dittoCategory The Ditto category if applicable
     * @return The generated class type name
     */
    fun generateClassFrom(
        name: String,
        objectSchema: ObjectSchema,
        packageName: String,
        role: PropertyRole,
        asObjectProperty: Boolean = false,
        feature: String? = null,
        parentClassName: String? = null,
        dittoCategory: String? = null
    ): TypeName {
        val simpleClassName = if (config != null) {
            asClassNameWithStrategy(name, parentClassName, config!!.classNamingStrategy, emptySet())
        } else {
            asClassName(name)
        }
        val schemaJson = objectSchema.toJson()

        if (ClassRegistry.hasConflict(packageName, simpleClassName, schemaJson)) {
            val prefix = parentClassName?.let { asClassName(it) } ?: feature ?: "Shared"
            val conflictResolvedClassName = "${prefix}${asClassName(name)}"
            logger.warn("Class name conflict for '${asClassName(name)}' in package '$packageName'. Using new name '$conflictResolvedClassName'")
            val finalClassName = conflictResolvedClassName
            val resolvedObjectSchema = referencePropertyResolver.resolveReferenceProperties(objectSchema)
            logger.info("Generating class $finalClassName - asObjectProperty: $asObjectProperty - feature: $feature")
            if (asObjectProperty && feature != null) {
                generateFeaturePropertyWrapperClass(resolvedObjectSchema, name, packageName, feature, role, dittoCategory)
            } else {
                generateClassFromObjectSchema(name, resolvedObjectSchema, packageName, role, parentClassName)
            }
            return ClassName(packageName, finalClassName).copy(nullable = true)
        }

        val resolvedObjectSchema = referencePropertyResolver.resolveReferenceProperties(objectSchema)
        logger.info("Generating class $simpleClassName - asObjectProperty: $asObjectProperty - feature: $feature")
        if (asObjectProperty && feature != null) {
            generateFeaturePropertyWrapperClass(resolvedObjectSchema, name, packageName, feature, role, dittoCategory)
        } else {
            generateClassFromObjectSchema(name, resolvedObjectSchema, packageName, role, parentClassName)
        }
        return ClassName(packageName, simpleClassName).copy(nullable = true)
    }

    /**
     * Generates DSL function specifications for attributes.
     *
     * @param attributesName The name for the attributes DSL function
     * @param attributesPackage The package for the attributes
     * @return A [FunSpec] representing the attributes DSL function
     */
    fun generateAttributesDslFunSpec(attributesName: String, attributesPackage: String) =
        dslGenerator.generateAttributesOrFeaturesDslFunSpec("attributes", attributesName, attributesPackage)

    /**
     * Generates DSL function specifications for features.
     *
     * @param featuresName The name for the features DSL function
     * @param featuresPackage The package for the features
     * @return A [FunSpec] representing the features DSL function
     */
    fun generateFeaturesDslFunSpec(featuresName: String, featuresPackage: String) =
        dslGenerator.generateAttributesOrFeaturesDslFunSpec("features", featuresName, featuresPackage)


    private suspend fun resolveFeatures(
        links: Iterable<BaseLink<*>>,
        featuresPackageName: String
    ): List<Pair<PropertySpec, String>> {
        return links.filter { LinkRelationType.isSubmodel(it) }.map {
            val featureModel = org.eclipse.ditto.wot.kotlin.generator.ThingModelGenerator.loadModel(it.href.toString())
            val originalFeatureName = getLinkInstanceName(it)
            val propertyName = asPropertyName(originalFeatureName)
            val packageName = "${featuresPackageName}.${asPackageName(originalFeatureName)}"
            val role = PropertyRole.FEATURE
            val linkProperties = propertyResolver.resolveProperties(
                featureModel.properties.getOrNull(), "$packageName.properties", PropertyRole.nextLevel(role),
                propertyName
            )

            val allProps2Category = linkProperties

            generateCategoryMarkerClasses(
                allProps2Category.values.filterNotNull(), featuresPackageName
            )

            if (allProps2Category.isNotEmpty()) {
                generateFeaturePropertiesClass(
                    asClassName(propertyName),
                    allProps2Category,
                    packageName,
                    featuresPackageName,
                    originalFeatureName
                )
            }

            val wotProperties = featureModel.properties.getOrNull()
            val wotActions = featureModel.actions.getOrNull()
            generateFeatureClassFromProperties(
                originalFeatureName, asClassName(propertyName), packageName, wotProperties, wotActions
            )
            Pair(
                PropertySpec.builder(
                    propertyName, ClassName(packageName, asClassName(propertyName)).copy(nullable = true)
                ).mutable(true).initializer("null").build(),
                originalFeatureName
            )
        }
    }


    private fun generateClassFromObjectSchema(
        propertyName: String,
        objectSchema: ObjectSchema,
        packageName: String,
        role: PropertyRole,
        parentClassName: String? = null
    ) {
        var className = if (config != null) {
            asClassNameWithStrategy(propertyName, parentClassName, config!!.classNamingStrategy, emptySet())
        } else {
            asClassName(propertyName)
        }
        val schemaJson = objectSchema.toJson()

        val targetObjectSchema = extractObjectSchemaForClassGeneration(schemaJson)
        if (targetObjectSchema == null) {
            logger.warn("No valid object schema found for $className, skipping class generation. Schema: $schemaJson")
            return
        }
        val schemaToUse = targetObjectSchema.toJson()

        if (ClassRegistry.hasConflict(packageName, className, schemaToUse)) {
            if (parentClassName != null) {
                className = "${asClassName(parentClassName)}${asClassName(propertyName)}"
            } else {
                className = "Shared${asClassName(propertyName)}"
            }
            logger.warn("Class name conflict for '${asClassName(propertyName)}' in package '$packageName'. Using new name '$className'")
        }
        ClassRegistry.registerGeneratedClass(packageName, className, schemaToUse)

        val isMapLike = targetObjectSchema.isPatternPropertiesSchema() || targetObjectSchema.isAdditionalPropertiesSchema()
        logger.info("Class $className - isMapLike: $isMapLike, schema: ${targetObjectSchema.toJson()}")
        val superClass = if (isMapLike) {
            val singleItemName = "${propertyName}Item"
            val singleItemNameAsClass = asClassName(singleItemName)
            val singleItemNameClass = ClassName(packageName, singleItemNameAsClass)
            var mapValueType: TypeName = singleItemNameClass

            val valueSchemaJson = when {
                targetObjectSchema.isPatternPropertiesSchema() -> {
                    val patternPropsValue = targetObjectSchema.toJson().getValue(JsonPointer.of("patternProperties"))
                    val patternProps = if (patternPropsValue.isPresent && patternPropsValue.get().isObject) patternPropsValue.get().asObject() else null
                    val firstPattern = patternProps?.keys?.firstOrNull()
                    if (firstPattern != null) {
                        val patternField = patternProps.getField(firstPattern)
                        if (patternField.isPresent && patternField.get().value.isObject) patternField.get().value.asObject() else null
                    } else null
                }
                targetObjectSchema.isAdditionalPropertiesSchema() -> {
                    val addPropsValue = targetObjectSchema.toJson().getValue(JsonPointer.of("additionalProperties"))
                    if (addPropsValue.isPresent && addPropsValue.get().isObject) addPropsValue.get().asObject() else null
                }
                else -> null
            }

            if (valueSchemaJson != null) {
                val valueType = valueSchemaJson.getValue(JsonPointer.of("type"))
                val valueTypeString = if (valueType.isPresent) valueType.get().toString().trim('"') else null
                if (valueTypeString == "array") {
                    val itemsValue = valueSchemaJson.getValue(JsonPointer.of("items"))
                    val itemsJson = if (itemsValue.isPresent && itemsValue.get().isObject) itemsValue.get().asObject() else null
                    if (itemsJson != null) {
                        generateClassFromObjectSchema(singleItemName, ObjectSchema.fromJson(itemsJson), packageName, role, propertyName)
                        mapValueType = MUTABLE_LIST.parameterizedBy(ClassName(packageName, asClassName(singleItemName)))
                    }
                } else if (valueTypeString == "object") {
                    generateClassFromObjectSchema(singleItemName, ObjectSchema.fromJson(valueSchemaJson), packageName, role, propertyName)
                    mapValueType = singleItemNameClass
                } else if (valueTypeString != null) {
                    val kotlinType = schemaTypeResolver.resolveSchemaType(org.eclipse.ditto.wot.model.SingleDataSchema.fromJson(valueSchemaJson), packageName, role, singleItemName)
                    val nonNullableType = kotlinType.copy(nullable = false)
                    generatePrimitiveTypeAlias(nonNullableType, singleItemNameAsClass, packageName)
                    mapValueType = nonNullableType
                }
            }
            ClassName(EXISTING_FEATURES_PACKAGE, "MapObjectProperty").parameterizedBy(mapValueType)
        } else {
            null
        }

        val propertiesJson = targetObjectSchema.toJson().getValue(JsonPointer.of("properties")).getOrNull()?.asObject()
        if (propertiesJson == null || propertiesJson.isEmpty()) {
            logger.warn("No properties found for class $className, schema: ${targetObjectSchema.toJson()}")
        }
        val fields = if (propertiesJson != null) {
            val schemaMap = mutableMapOf<String, SingleDataSchema>()
            for (entry in propertiesJson) {
                val key = entry.key.toString()
                val value = entry.value
                logger.info("Property for $className: $key -> $value")
                schemaMap[key] = SingleDataSchema.fromJson(value.asObject())
            }
            propertyResolver.convertFieldsToPropertySpecs(
                schemaMap,
                packageName,
                role,
                className
            )
        } else {
            emptyList()
        }

        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())

        superClass?.let {
            typeSpecBuilder.superclass(it)
        }
        addProperties(null, fields, typeSpecBuilder)

        val updatedFields = addInlineEnumsToTypeSpec(fields, typeSpecBuilder, packageName)
        val fieldDslFunSpecs = createObjectFieldDslFunSpecs(
            updatedFields.map { it.second }, targetObjectSchema, packageName, propertyName, role
        )
        fieldDslFunSpecs.forEach { typeSpecBuilder.addFunction(it) }

        val typeSpec = typeSpecBuilder.build()
        generateNewClass(
            typeSpec,
            className,
            packageName,
            listOf(dslGenerator.generateFeatureDslFunSpec(className, packageName, true)),
            originalName = propertyName
        )
    }

    private fun collectFields(
        objectSchema: ObjectSchema, packageName: String, propertyName: String?, role: PropertyRole
    ): MutableList<Pair<String, PropertySpec>> {
        val objectSchemaJson = objectSchema.toJson()
        val fields = propertyResolver.convertFieldsToPropertySpecs(objectSchema.properties, packageName, role, propertyName)
            .toMutableList()
        if (objectSchema.isPatternPropertiesSchema()) {
            logger.debug("Found object schema with patternProperties: ${objectSchema.toJson()}")
            generatePatternPropertiesClass(propertyName, objectSchema, packageName, role)
        }

        if (objectSchema.isAdditionalPropertiesSchema()) {
            logger.debug("Found object schema with additionalProperties: ${objectSchema.toJson()}")
            generateAdditionalPropertiesClass(propertyName, objectSchema, packageName, role)
        }

        if (objectSchemaJson.getValue(JsonPointer.of("oneOf")).isEmpty.not()) {
            logger.debug("Found object schema with oneOf properties: ${objectSchema.toJson()}")
            fields += propertyResolver.resolveOneOfProperties(objectSchema, packageName, role)
        }

        return fields
    }

    private fun addInlineEnumsToTypeSpec(
        fields: List<Pair<String, PropertySpec>>,
        typeSpecBuilder: TypeSpec.Builder,
        packageName: String
    ): List<Pair<String, PropertySpec>> {
        logger.info("addInlineEnumsToTypeSpec called with enumGenerationStrategy: ${enumGenerationStrategy?.javaClass?.simpleName ?: "null"}")

        val shouldAddInline = when {
            enumGenerationStrategy != null -> {
                when (enumGenerationStrategy!!::class.simpleName) {
                    "InlineEnumGenerationStrategy" -> true
                    "SeparateClassEnumGenerationStrategy" -> false
                    else -> true // Default to inline for backward compatibility
                }
            }
            else -> true // Default to inline for backward compatibility
        }

        if (!shouldAddInline) {
            logger.info("Using separate class enum strategy - not adding enums inline")
            return fields
        }

        logger.info("Using inline enum strategy - adding enums to class")
        val addedEnumNames = mutableSetOf<String>()

        val updatedFields = fields.map { field ->
            var currentField = field.second

            val parameterizedTypeName = currentField.type as? ParameterizedTypeName
            if (parameterizedTypeName != null) {
                parameterizedTypeName.typeArguments.forEach { typeArg ->
                    val className = typeArg as? ClassName
                    if (className != null) {
                        val enumSpec = EnumRegistry.getEnumByName(className.simpleName)
                        if (enumSpec != null && addedEnumNames.add(enumSpec.name!!)) {
                            val hasExistingType = ClassRegistry.hasClass(packageName, className.simpleName) ||
                                                typeSpecBuilder.build().typeSpecs.any { it.name == className.simpleName }

                            if (!hasExistingType) {
                                val nestedEnumSpec = enumSpec.toBuilder().build()
                                typeSpecBuilder.addType(nestedEnumSpec)

                                val newTypeArguments = parameterizedTypeName.typeArguments.map { arg ->
                                    val argClassName = arg as? ClassName
                                    if (argClassName != null && argClassName.simpleName == className.simpleName) {
                                        ClassName("", className.simpleName).copy(nullable = arg.isNullable)
                                    } else {
                                        arg
                                    }
                                }
                                val newType = parameterizedTypeName.rawType.parameterizedBy(newTypeArguments)
                                    .copy(nullable = currentField.type.isNullable)
                                currentField = currentField.toBuilder(currentField.name, newType).build()
                            }
                        }
                    }
                }
            }

            val typeClass = currentField.type as? ClassName
            if (typeClass != null) {
                val enumSpec = EnumRegistry.getEnumByName(typeClass.simpleName)
                if (enumSpec != null && addedEnumNames.add(enumSpec.name!!)) {
                    val hasExistingType = ClassRegistry.hasClass(packageName, typeClass.simpleName) ||
                                        typeSpecBuilder.build().typeSpecs.any { it.name == typeClass.simpleName }

                    if (!hasExistingType) {
                        val nestedEnumSpec = enumSpec.toBuilder().build()
                        typeSpecBuilder.addType(nestedEnumSpec)

                        val newType = ClassName("", typeClass.simpleName).copy(nullable = typeClass.isNullable)
                        currentField = currentField.toBuilder(currentField.name, newType).build()
                    }
                }
            }

            field.first to currentField
        }

        if (updatedFields != fields) {
            logger.info("Updated fields with inline enums: ${updatedFields.size} fields")
        }

        return updatedFields
    }

    private fun generatePatternPropertiesClass(
        propertyName: String?,
        objectSchema: ObjectSchema,
        packageName: String,
        role: PropertyRole
    ) {
        objectSchema.toJson()
            .getValue(JsonPointer.of("patternProperties"))
            .getOrNull()?.takeIf { it.isObject }?.asObject()?.let { patternProperties ->
                val title = objectSchema.title.getOrNull()?.toString()
                generateClassFromPatternProperties(patternProperties, packageName, propertyName, title, role)
            }
    }

    /**
     * Fields with additional properties are resolved as properties of type Map<String, SomeNewType?>
     */
    private fun generateAdditionalPropertiesClass(
        propertyName: String?,
        objectSchema: ObjectSchema,
        packageName: String,
        role: PropertyRole
    ) {
        objectSchema.toJson()
            .getValue(JsonPointer.of("additionalProperties"))
            .getOrNull()?.takeIf { it.isObject }?.asObject()?.let { additionalProperties ->
                val title = objectSchema.title.get().toString()

                val propertyNameSingular = "${propertyName ?: title}Item"
                generateClassFromObjectSchema(
                    propertyNameSingular,
                    referencePropertyResolver.resolveReferenceProperties(ObjectSchema.fromJson(additionalProperties)),
                    packageName,
                    role,
                    parentClassName = propertyName
                )
            }
    }

    private fun generateClassFromPatternProperties(
        patternProperties: JsonObject,
        packageName: String,
        propertyName: String?,
        title: String?,
        role: PropertyRole
    ): TypeName {
        val propertyItemName = "${propertyName ?: title}Item"
        val patternPropertyClassName = asClassName(propertyItemName)
        logger.info("[GEN] generateClassFromPatternProperties: propertyItemName=$propertyItemName, packageName=$packageName")
        logger.debug("patternProperties: $patternProperties")
        patternProperties.keys.firstOrNull()?.let {
            val patternProperty = patternProperties.getField(it).getOrNull()
            if (patternProperty?.value is JsonObject) {
                val patternPropertyObject = referencePropertyResolver.resolveReferenceProperties(
                    ObjectSchema.fromJson(patternProperty.value as JsonObject)
                )
                logger.info("[GEN] Calling generateClassFromObjectSchema for $propertyItemName in $packageName")
                generateClassFromObjectSchema(propertyItemName, patternPropertyObject, packageName, role, propertyName)
                logger.info("[GEN] Finished generateClassFromObjectSchema for $propertyItemName in $packageName")
                return ClassName(packageName, patternPropertyClassName)
            } else {
                throw IllegalArgumentException("Pattern property is not Object: $patternProperties")
            }
        } ?: run {
            throw IllegalArgumentException("Unsupported patternProperties: $patternProperties")
        }
    }

    private fun generateFeaturesClassFromFeatures(className: String, properties: List<Pair<PropertySpec, String>>,
                                                  packageName: String) {
        val featureDslFunsSpecs = properties.map {
            val propClassName = it.first.type as ClassName
            dslGenerator.generateFeatureDslFunSpec(propClassName.simpleName, propClassName.packageName)
        }

        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName(Const.COMMON_PACKAGE_FEATURES, "Features"))
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())
            .addAnnotation(buildJsonIgnoreAnnotationSpec())

        if (properties.isNotEmpty()) {
            typeSpecBuilder
                .addModifiers(KModifier.DATA)
                .primaryConstructor(buildPrimaryConstructor(properties.map { it.first }))
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.first.name}", it.first.type, KModifier.PRIVATE)
                        .addAnnotation(buildJsonSetterAnnotationSpec(it.second))
                        .mutable(true)
                        .initializer("_${it.first.name}")
                        .build()
                })
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.first.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class,
                                         KModifier.PRIVATE)
                        .mutable(true)
                        .initializer("false")
                        .build()
                })
                .addProperties(properties.map {
                    val builder = it.first.toBuilder()
                    if (!hasJacksonAnnotation(it.first)) {
                        builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.second))
                    }
                    builder
                        .setter(provideExplicitSettoNullSetter(it.first))
                        .mutable(true)
                        .initializer("_${it.first.name}")
                        .build()
                })
        }

        val typeSpec = typeSpecBuilder.addFunctions(featureDslFunsSpecs)
            .build()

        generateNewClass(
            typeSpec,
            className,
            packageName,
            listOf(dslGenerator.generateFeatureDslFunSpec(className, packageName, true))
        )
    }

    private fun generateFeatureClassFromProperties(
        originalFeatureName: String,
        featureClassName: String,
        packageName: String,
        wotProperties: Properties?,
        wotActions: Actions?
    ) {
        val featurePropertiesClassName = featureClassName + "Properties"
        val featureClass = ClassName(packageName, featureClassName)
        val featurePropertiesClass = if (wotProperties.isNullOrEmpty()) {
            NOTHING
        } else {
            ClassName(packageName, featurePropertiesClassName)
        }
        val superClass = ClassName(EXISTING_FEATURES_PACKAGE, "Feature")
            .parameterizedBy(featureClass, featurePropertiesClass)
        val featureClassFuns = createFeatureClassFunSpecs(originalFeatureName, featurePropertiesClass)

        val companionObject = createFeatureCompanionObject(originalFeatureName)

        wotActions?.map { it.value }
            ?.let {
                generateActionInterface(
                    featureClassName,
                    "$packageName.actions", ClassName(EXISTING_FEATURES_PACKAGE, "FeatureAction"),
                    it
                )
                enumGenerator.generateActionEnum(
                    featureClassName,
                    "$packageName.actions",
                    it.map { action -> action.actionName })
            }

        val typeSpecBuilder = TypeSpec.classBuilder(featureClassName)
            .superclass(superClass)
            .addSuperclassConstructorParameter("FEATURE_NAME")
            .addType(companionObject)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())

        if (featurePropertiesClass == NOTHING) {
            typeSpecBuilder.addAnnotation(
                AnnotationSpec.builder(ClassName("com.fasterxml.jackson.annotation", "JsonIgnoreProperties"))
                    .addMember("value = [\"properties\", \"desiredProperties\"]")
                    .build()
            )
        }

        featureClassFuns.forEach { typeSpecBuilder.addFunction(it) }

        generateNewClass(
            typeSpecBuilder.build(),
            featureClassName,
            packageName,
            listOf(dslGenerator.generateFeatureDslFunSpec(featureClassName, packageName, true)),
            originalName = originalFeatureName
        )
    }

    private fun generateActionInterface(
        featureClassName: String,
        packageName: String,
        actionMarkerInterfaceClassName: ClassName,
        actions: List<Action>
    ) {
        actions.forEach { action ->
            val actionClassAlias = asClassName(action.actionName)
            val interfaceSpec = TypeSpec.interfaceBuilder(actionClassAlias)
                .addSuperinterface(actionMarkerInterfaceClassName)
            val inputType = action.input.getOrNull()?.let {
                when {
                    it.type.isPresent && isPrimitive(it.type.getOrNull()) -> {
                        asPrimitiveClassName(it)
                    }

                    it is ObjectSchema                                    -> {
                        generateDataClassForAction("${actionClassAlias}Input", it, packageName)
                        ClassName(packageName, "${actionClassAlias}Input")
                    }

                    it is ArraySchema                                     -> {
                        schemaTypeResolver.resolveArraySchemaType(
                            it,
                            packageName,
                            PropertyRole.OTHER,
                            "${action.actionName}Input"
                        )
                    }

                    else                                                  -> {
                        UNIT.copy(nullable = true)
                    }
                }
            }

            val outputType = action.output.getOrNull()?.let {
                when {
                    it.type.isPresent && isPrimitive(it.type.getOrNull()) -> {
                        asPrimitiveClassName(it)
                    }

                    it is ObjectSchema                                    -> {
                        generateDataClassForAction("${actionClassAlias}Output", it, packageName)
                        ClassName(packageName, "${actionClassAlias}Output")
                    }

                    it is ArraySchema                                     -> {
                        schemaTypeResolver.resolveArraySchemaType(
                            it,
                            packageName,
                            PropertyRole.OTHER,
                            "${action.actionName}Output"
                        )
                    }

                    else                                                  -> UNIT.copy(nullable = true)
                }
            }

            val functionBuilder = FunSpec.builder(action.actionName)
                .addModifiers(KModifier.ABSTRACT)
                .addModifiers(KModifier.SUSPEND)

            inputType?.let {
                functionBuilder.addParameter("input", it)
            }

            functionBuilder.addParameter(
                ParameterSpec.builder("additionalInputs", ANY.copy(nullable = true))
                    .addModifiers(KModifier.VARARG)
                    .build()
            )

            functionBuilder.returns(outputType ?: UNIT)

            interfaceSpec
                .addFunction(functionBuilder.build())

            val file = FileSpec.builder(packageName, actionClassAlias)
                .addType(interfaceSpec.build())
                .build()
            file.writeTo(Path(outputDir))
        }
    }

    private fun generateDataClassForAction(className: String, objectSchema: ObjectSchema, packageName: String) {
        val typeSpecBuilder = TypeSpec.classBuilder(className).addModifiers(KModifier.DATA)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())
        val constructorSpec = FunSpec.constructorBuilder()
        val requiredFields = objectSchema.required.orEmpty().toSet()

        objectSchema.properties.forEach { (property, schema) ->
            val name = asClassProperty(property)
            val isRequired = property in requiredFields
            val kotlinType = when (schema.type.get()) {
                DataSchemaType.OBJECT -> {
                    val nestedClassName = "${asClassName(className)}${asClassName(name)}"
                    generateDataClassForAction(nestedClassName, schema as ObjectSchema, packageName)
                    ClassName(packageName, nestedClassName).copy(nullable = !isRequired)
                }

                DataSchemaType.ARRAY  -> {
                    val itemSchema = ((schema as ArraySchema).items.getOrNull()!! as SingleDataSchema)
                    if (itemSchema.type.get() == DataSchemaType.OBJECT) {
                        val nestedClassName = "${asClassName(className)}${asClassName(name)}Item"
                        generateDataClassForAction(nestedClassName, itemSchema as ObjectSchema, packageName)
                        LIST.parameterizedBy(ClassName(packageName, nestedClassName)).copy(nullable = !isRequired)
                    } else {
                        LIST.parameterizedBy(asPrimitiveClassName(itemSchema)).copy(nullable = !isRequired)
                    }
                }

                else                  -> asPrimitiveClassName(schema, !isRequired)
            }

            val paramSpec = ParameterSpec.builder(name, kotlinType)
            if (!isRequired) {
                paramSpec.defaultValue("null")
            }
            constructorSpec.addParameter(paramSpec.build())
            typeSpecBuilder.addProperty(
                PropertySpec.builder(name, kotlinType)
                    .addAnnotation(buildJsonPropertyAnnotationSpec(property))
                    .initializer(name)
                    .build()
            )
        }

        typeSpecBuilder.primaryConstructor(constructorSpec.build())
        val file = FileSpec.builder(packageName, className)
            .addType(typeSpecBuilder.build())
            .build()
        file.writeTo(Path(outputDir))
    }

    private fun createFeatureCompanionObject(featureName: String): TypeSpec {
        val companionObjectBuilder = TypeSpec.companionObjectBuilder()
        companionObjectBuilder
            .addProperty(
                PropertySpec.builder("FEATURE_NAME", String::class, KModifier.CONST)
                    .initializer("%S", featureName)
                    .build()
            )
            .build()
        return companionObjectBuilder.build()
    }

    private fun createFeatureClassFunSpecs(featureName: String, featurePropertiesClass: ClassName): List<FunSpec> {
        val returnStatement = if (featurePropertiesClass == NOTHING) {
            "throw·UnsupportedOperationException(\"No·properties·were·defined·for·feature·<${featureName}>\")"
        } else {
            "return ${featurePropertiesClass.simpleName}()"
        }

        val createPropertiesFun = FunSpec.builder("createProperties")
            .returns(featurePropertiesClass)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement(returnStatement)
            .build()

        val createDesiredPropertiesFun = FunSpec.builder("createDesiredProperties")
            .returns(featurePropertiesClass)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement(returnStatement)
            .build()

        return listOf(createPropertiesFun, createDesiredPropertiesFun)
    }

    private fun generateCategoryMarkerClasses(categories: Collection<String>, featuresPackageName: String) {
        if (categories.isNotEmpty()) {
            categories.forEach { category ->
                val file = FileSpec.builder(featuresPackageName, "${asClassName(category)}Category")
                    .addType(TypeSpec.interfaceBuilder("${asClassName(category)}Category")
                                 .addSuperinterface(ClassName(Const.COMMON_PACKAGE_FEATURES, "DittoCategory"))
                                 .build())
                    .build()
                file.writeTo(Path(outputDir))
            }
        }
    }

    private fun generateFeaturePropertiesClass(
        className: String,
        property2CategoryMap: Map<Pair<Property, PropertySpec>, String?>,
        packageName: String,
        featuresPackageName: String,
        originalFeatureName: String
    ) {
        val featurePropertiesClassName = className + "Properties"
        val featureClass = ClassName(packageName, className)
        val featurePropertiesClass = ClassName(packageName, featurePropertiesClassName)
        val superInterface = ClassName(EXISTING_FEATURES_PACKAGE, "Properties")
            .parameterizedBy(featurePropertiesClass, featureClass)

        val propertyPackage = "$packageName.properties"

        val propertiesWithoutCategory = property2CategoryMap.filter { it.value == null }.map { it.key }
            .distinctBy { it.second.name }
        val propertiesWithoutCategoryCreatorFunctions = if (propertiesWithoutCategory.isNotEmpty()) {
            dslGenerator.generatePropertyFunSpecs(propertiesWithoutCategory, propertyPackage)
        } else {
            emptyList()
        }

        val categorizedProperties = (property2CategoryMap.filterValues { it != null } as Map<Pair<Property, PropertySpec>, String>)
            .entries
            .groupBy { it.value }
            .map { (cat, entries) -> cat to entries.map { e -> e.key } }

        val categoryTypes: List<TypeSpec>
        val categoryProperties: List<PropertySpec>
        val categoryCreatorFunctions: List<FunSpec>
        if (categorizedProperties.isNotEmpty()) {
            categoryTypes = categorizedProperties.map { (category, properties) ->
                val pathFunSpec = createPathFunSpec(asClassName(category), false, category)

                val companionObjectSpec = TypeSpec.companionObjectBuilder()
                    .addSuperinterface(ClassName(Const.COMMON_PACKAGE_PATH, "HasPath"))
                    .addFunction(pathFunSpec)
                    .build()
                val categoryTypeBuilder = TypeSpec.classBuilder(asClassName(category))

                categoryTypeBuilder
                    .addModifiers(KModifier.DATA)
                    .addAnnotation(buildDittoJsonDslAnnotationSpec())
                    .addAnnotation(buildJsonIncludeAnnotationSpec())
                    .addSuperinterface(ClassName(featuresPackageName, "${asClassName(category)}Category"))
                    .addType(companionObjectSpec)
                    .primaryConstructor(buildPrimaryConstructor(properties.map { it.second }))
                    .addProperties(properties.map {
                        PropertySpec.builder("_${it.second.name}", it.second.type, KModifier.PRIVATE)
                            .addAnnotation(buildJsonSetterAnnotationSpec(it.first.propertyName))
                            .mutable(true)
                            .initializer("_${it.second.name}")
                            .build()
                    })
                    .addProperties(properties.map {
                        PropertySpec.builder("_${it.second.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class,
                                             KModifier.PRIVATE)
                            .mutable(true)
                            .initializer("false")
                            .build()
                    })
                    .addProperties(properties.map {
                        val builder = it.second.toBuilder()
                        if (!hasJacksonAnnotation(it.second)) {
                            builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.first.propertyName))
                        }
                        builder
                            .setter(provideExplicitSettoNullSetter(it.second))
                            .mutable(true)
                            .initializer("_${it.second.name}")
                            .build()
                    })
                    .addFunctions(dslGenerator.generatePropertyFunSpecs(properties, propertyPackage))

                addInlineEnumsToTypeSpec(
                    properties.map { it.first.propertyName to it.second }.toMutableList(), categoryTypeBuilder, propertyPackage
                )

                categoryTypeBuilder.build()
            }
            categoryProperties = categorizedProperties.map { (category, _) ->
                PropertySpec.builder(
                    category,
                    ClassName.bestGuess(asClassName(category)).copy(nullable = true)
                )
                    .mutable(true)
                    .initializer("null")
                    .build()
            }
            categoryCreatorFunctions = categorizedProperties.map { (category, _) ->
                val categoryAsClass = asClassName(category)
                val categoryClass = ClassName.bestGuess(asClassName(category))
                FunSpec.builder(category)
                    .returns(categoryClass)
                    .addParameter("block", LambdaTypeName.get(receiver = categoryClass, returnType = UNIT))
                    .addStatement("val·$category·=·$categoryAsClass()")
                    .addStatement("$category.block()")
                    .addStatement("this.$category·=·$category")
                    .addStatement("return·$category")
                    .build()
            }
        } else {
            categoryTypes = emptyList()
            categoryProperties = emptyList()
            categoryCreatorFunctions = emptyList()
        }

        val typeSpecBuilder = TypeSpec.classBuilder(featurePropertiesClassName)
            .addModifiers(KModifier.DATA)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())
            .addSuperinterface(superInterface)
            .primaryConstructor(
                buildPrimaryConstructor(propertiesWithoutCategory.map { it.second } + categoryProperties))
            .addProperties(propertiesWithoutCategory.map { it.second }.map {
                PropertySpec.builder("_${it.name}", it.type, KModifier.PRIVATE)
                    .addAnnotation(buildJsonSetterAnnotationSpec(it.name))
                    .mutable(true)
                    .initializer("_${it.name}")
                    .build()
            })
            .addProperties(propertiesWithoutCategory.map { it.second }.map {
                PropertySpec.builder("_${it.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("false")
                    .build()
            })
            .addProperties(propertiesWithoutCategory.map { it.second }.map {
                val builder = it.toBuilder()
                if (!hasJacksonAnnotation(it)) {
                    builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.name))
                }
                builder
                    .setter(provideExplicitSettoNullSetter(it))
                    .mutable(true)
                    .initializer("_${it.name}")
                    .build()
            })
            .addProperties(categoryProperties.map {
                PropertySpec.builder("_${it.name}", it.type, KModifier.PRIVATE)
                    .addAnnotation(buildJsonSetterAnnotationSpec(it.name))
                    .mutable(true)
                    .initializer("_${it.name}")
                    .build()
            })
            .addProperties(categoryProperties.map {
                PropertySpec.builder("_${it.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("false")
                    .build()
            })
            .addProperties(categoryProperties.map {
                val builder = it.toBuilder()
                if (!hasJacksonAnnotation(it)) {
                    builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.name))
                }
                builder
                    .setter(provideExplicitSettoNullSetter(it))
                    .mutable(true)
                    .initializer("_${it.name}")
                    .build()
            })
            .addFunctions(propertiesWithoutCategoryCreatorFunctions)
            .addFunctions(categoryCreatorFunctions)
            .addTypes(categoryTypes)

        addInlineEnumsToTypeSpec(
            (
                propertiesWithoutCategory.map { it.first.propertyName to it.second } +
                    categoryProperties.map { it.name to it }
             ).toMutableList(),
            typeSpecBuilder,
            packageName
        )

        generateNewClass(
            typeSpecBuilder.build(),
            featurePropertiesClassName,
            packageName,
            listOf(dslGenerator.generateFeatureDslFunSpec(featurePropertiesClassName, packageName, true)),
            originalName = originalFeatureName
        )
    }

    private fun provideExplicitSettoNullSetter(propertySpec: PropertySpec) =
        FunSpec.setterBuilder()
            .addParameter("value", propertySpec.type)
            .addCode("if·(value·==·null)·{\n")
            .addCode("··_${propertySpec.name}_explicitly_set_null·=·true\n")
            .addCode("}\n")
            .addCode("_${propertySpec.name}·=·value\n")
            .addCode("field·=·value\n")
            .build()

    private fun buildPrimaryConstructor(properties: List<PropertySpec>): FunSpec {
        val primaryConstructorBuilder = FunSpec.constructorBuilder()
        properties.forEach {
            primaryConstructorBuilder.addParameter(
                ParameterSpec.builder("_${it.name}", it.type)
                    .defaultValue("null")
                    .build()
            )
        }
        return primaryConstructorBuilder.build()
    }

    private fun generateAttributesClass(properties: List<Pair<Property, PropertySpec>>, packageName: String) {
        val className = ATTRIBUTES_CLASS_NAME
        val superInterface = ClassName(EXISTING_ATTRIBUTES_PACKAGE, "Attributes")
        val attributesDslFunsSpecs = properties
            .filter { it.first.isObjectSchema }
            .map { it.second }
            .filter {
                when (val type = it.type) {
                    is ClassName             -> type.packageName.startsWith("java")
                        .not() && type.packageName.startsWith("kotlin")
                        .not()

                    is ParameterizedTypeName -> type.rawType.packageName.startsWith("java")
                        .not() && type.rawType.packageName.startsWith("kotlin").not()

                    else                     -> false
                }
            }
            .map {
                val propClassName = when (val type = it.type) {
                    is ClassName             -> type
                    is ParameterizedTypeName -> type.rawType
                    else                     -> throw IllegalArgumentException(
                        "Unsupported type for property: ${it.type}")
                }
                dslGenerator.generateFeatureDslFunSpec(propClassName.simpleName, propClassName.packageName)
            }

        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(superInterface)
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())

        if (properties.isNotEmpty()) {
            typeSpecBuilder
                .addModifiers(KModifier.DATA)
                .primaryConstructor(buildPrimaryConstructor(properties.map { it.second }))
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.second.name}", it.second.type, KModifier.PRIVATE)
                        .addAnnotation(buildJsonSetterAnnotationSpec(it.first.propertyName))
                        .mutable(true)
                        .initializer("_${it.second.name}")
                        .build()
                })
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.second.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class,
                                         KModifier.PRIVATE)
                        .mutable(true)
                        .initializer("false")
                        .build()
                })
                .addProperties(properties.map {
                    val builder = it.second.toBuilder()
                    if (!hasJacksonAnnotation(it.second)) {
                        builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.first.propertyName))
                    }
                    builder
                        .setter(provideExplicitSettoNullSetter(it.second))
                        .mutable(true)
                        .initializer("_${it.second.name}")
                        .build()
                })
        }

        addInlineEnumsToTypeSpec(
            properties.map { it.first.propertyName to it.second }.toMutableList(),
            typeSpecBuilder,
            packageName
        )

        val typeSpec = typeSpecBuilder
            .addAnnotation(buildJsonIgnoreAnnotationSpec())
            .addFunctions(attributesDslFunsSpecs)
            .build()
        generateNewClass(
            typeSpec,
            className,
            packageName,
            listOf(dslGenerator.generateFeatureDslFunSpec(className, packageName, true))
        )
    }

    private fun createPropertyCompanionObject(featureName: String?, propertyName: String) =
        TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec.builder("PROPERTY_NAME", String::class, KModifier.CONST)
                    .initializer("%S", asPropertyName(propertyName))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("HEALTH_CONTRIBUTOR_KEY", String::class, KModifier.CONST)
                    .initializer("%S", "$featureName-${asPropertyName(propertyName)}")
                    .build()
            )
            .build()

    private fun generateFeaturePropertyWrapperClass(
        objectSchema: ObjectSchema,
        propertyName: String,
        propertyPackage: String,
        feature: String?,
        role: PropertyRole,
        dittoCategory: String? = null
    ) {
        val className = asClassName(propertyName)

        val isMapLike = objectSchema.isPatternPropertiesSchema() || objectSchema.isAdditionalPropertiesSchema()
        val superClass = if (isMapLike) {
            val singleItemName = "${propertyName}Item"
            val singleItemNameAsClass = asClassName(singleItemName)
            val singleItemNameClass = ClassName(propertyPackage, singleItemNameAsClass)
            var mapValueType: TypeName = singleItemNameClass

            val valueSchemaJson = when {
                objectSchema.isPatternPropertiesSchema() -> {
                    val patternPropsValue = objectSchema.toJson().getValue(JsonPointer.of("patternProperties"))
                    val patternProps = if (patternPropsValue.isPresent && patternPropsValue.get().isObject) patternPropsValue.get().asObject() else null
                    val firstPattern = patternProps?.keys?.firstOrNull()
                    if (firstPattern != null) {
                        val patternField = patternProps.getField(firstPattern)
                        if (patternField.isPresent && patternField.get().value.isObject) patternField.get().value.asObject() else null
                    } else null
                }
                objectSchema.isAdditionalPropertiesSchema() -> {
                    val addPropsValue = objectSchema.toJson().getValue(JsonPointer.of("additionalProperties"))
                    if (addPropsValue.isPresent && addPropsValue.get().isObject) addPropsValue.get().asObject() else null
                }
                else -> null
            }

            if (valueSchemaJson != null) {
                val valueType = valueSchemaJson.getValue(JsonPointer.of("type"))
                val valueTypeString = if (valueType.isPresent) valueType.get().toString().trim('"') else null
                if (valueTypeString == "array") {
                    val itemsValue = valueSchemaJson.getValue(JsonPointer.of("items"))
                    val itemsJson = if (itemsValue.isPresent && itemsValue.get().isObject) itemsValue.get().asObject() else null
                    if (itemsJson != null) {
                        generateClassFromObjectSchema(singleItemName, ObjectSchema.fromJson(itemsJson), propertyPackage, role, propertyName)
                        mapValueType = MUTABLE_LIST.parameterizedBy(ClassName(propertyPackage, asClassName(singleItemName)))
                    }
                } else if (valueTypeString == "object") {
                    generateClassFromObjectSchema(singleItemName, ObjectSchema.fromJson(valueSchemaJson), propertyPackage, role, propertyName)
                    mapValueType = singleItemNameClass
                } else if (valueTypeString != null) {
                    val kotlinType = schemaTypeResolver.resolveSchemaType(org.eclipse.ditto.wot.model.SingleDataSchema.fromJson(valueSchemaJson), propertyPackage, role, singleItemName)
                    generatePrimitiveTypeAlias(kotlinType, singleItemNameAsClass, propertyPackage)
                    mapValueType = kotlinType
                }
            }
            ClassName(EXISTING_FEATURES_PACKAGE, "MapObjectProperty").parameterizedBy(mapValueType)
        } else {
            null
        }

        val fields = collectFields(objectSchema, propertyPackage, propertyName, role)

        val fieldDslFunSpecs = createObjectFieldDslFunSpecs(
            fields.map { it.second }, objectSchema, propertyPackage, propertyName, role
        )

        val typeSpecBuilder = TypeSpec.classBuilder(asClassName(propertyName))
            .addAnnotation(buildDittoJsonDslAnnotationSpec())
            .addAnnotation(buildJsonIncludeAnnotationSpec())

        superClass?.let {
            typeSpecBuilder.superclass(it)
        }

        addProperties(superClass, fields, typeSpecBuilder)

        if (!isMapLike) {
            typeSpecBuilder.primaryConstructor(buildPrimaryConstructor(fields.map { it.second }))
        }
        fieldDslFunSpecs.forEach { typeSpecBuilder.addFunction(it) }
        typeSpecBuilder.addType(createPropertyCompanionObject(feature, propertyName))

        addInlineEnumsToTypeSpec(fields, typeSpecBuilder, propertyPackage)

        generateNewClass(
            typeSpecBuilder.build(),
            className,
            propertyPackage,
            listOf(dslGenerator.generateFeatureDslFunSpec(className, propertyPackage, true)),
            originalName = propertyName
        )
    }

    private suspend fun resolveExtendingLinkProperties(
        links: Iterable<BaseLink<*>>, parentPackage: String, role: PropertyRole, feature: String? = null
    ): Map<Pair<Property, PropertySpec>, String?> {
        return links.filter { LinkRelationType.isExtends(it) }.map {
            val linkThingModel = org.eclipse.ditto.wot.kotlin.generator.ThingModelGenerator.loadModel(it.href.toString())
            val linkProperties = propertyResolver.resolveProperties(
                linkThingModel.properties.get(), parentPackage, role, feature
            )
            val subLinks = linkThingModel.links.getOrNull() ?: emptyList<BaseLink<*>>()
            val subLinkProperties = resolveExtendingLinkProperties(subLinks, parentPackage, role, feature)
            linkProperties + subLinkProperties
        }.flatMap { it.entries }.associate { it.key to it.value }
    }

    private fun addProperties(
        superClass: ParameterizedTypeName?,
        properties: List<Pair<String, PropertySpec>>,
        typeSpecBuilder: TypeSpec.Builder
    ) {
        if (superClass?.rawType == ClassName(EXISTING_FEATURES_PACKAGE, "MapObjectProperty") && properties.isNotEmpty()) {
            typeSpecBuilder.addProperties(properties.map { it.second })
        } else if (properties.isNotEmpty()) {
            typeSpecBuilder
                .addModifiers(KModifier.DATA)
                .primaryConstructor(buildPrimaryConstructor(properties.map { it.second }))
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.second.name}", it.second.type, KModifier.PRIVATE)
                        .addAnnotation(buildJsonSetterAnnotationSpec(it.first))
                        .mutable(true)
                        .initializer("_${it.second.name}")
                        .build()
                })
                .addProperties(properties.map {
                    PropertySpec.builder("_${it.second.name}$EXPLICITLY_SET_NULL_FIELD_SUFFIX", Boolean::class,
                                         KModifier.PRIVATE)
                        .mutable(true)
                        .initializer("false")
                        .build()
                })
                .addProperties(properties.map {
                    val builder = it.second.toBuilder()
                    if (!hasJacksonAnnotation(it.second)) {
                        builder.addAnnotation(buildJsonPropertyAnnotationSpec(it.first))
                    }
                    builder
                        .setter(provideExplicitSettoNullSetter(it.second))
                        .mutable(true)
                        .initializer("_${it.second.name}")
                        .build()
                })
        }
    }

    private fun createObjectFieldDslFunSpecs(
        fields: List<PropertySpec>,
        objectSchema: ObjectSchema,
        packageName: String,
        propertyName: String,
        role: PropertyRole
    ): List<FunSpec> {
        val fieldsDslFunSpecs =
            fields.filter { field ->
                objectSchema.properties.entries
                    .any { (key, value) ->
                        asPropertyName(key) == field.name && value.enum.isEmpty() && value.type.getOrNull() == DataSchemaType.OBJECT
                    }
            }
                .map { dslGenerator.generateObjectFieldDslFunSpec(it.name, packageName, it.type.copy(nullable = false)) }.toMutableList()

        dslGenerator.generateAdditionalOrPatternPropertyDslFunSpec(
            propertyName,
            objectSchema,
            packageName,
            role
        )?.let { fieldsDslFunSpecs += it }
        return fieldsDslFunSpecs
    }
}
