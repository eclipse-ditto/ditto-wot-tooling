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
package org.eclipse.ditto.wot.openapi.generator.features

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.eclipse.ditto.wot.model.DataSchemaType
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.SingleDataSchema
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.model.ObjectSchema as WotObjectSchema
import org.eclipse.ditto.wot.openapi.generator.Utils
import org.eclipse.ditto.wot.openapi.generator.Utils.DeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
import org.eclipse.ditto.wot.openapi.generator.Utils.asPropertyName
import org.eclipse.ditto.wot.openapi.generator.Utils.buildDeprecationDescription
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.Utils.extractDesiredEnabled
import org.eclipse.ditto.wot.openapi.generator.Utils.extractPropertyCategory
import org.eclipse.ditto.wot.openapi.generator.Utils.isPrimitive
import org.eclipse.ditto.wot.openapi.generator.Utils.markSchemaDeprecated
import org.eclipse.ditto.wot.openapi.generator.Utils.mergeWithDeprecationNotice
import org.eclipse.ditto.wot.openapi.generator.providers.ApiResponsesProvider
import org.eclipse.ditto.wot.openapi.generator.providers.ParametersProvider
import org.eclipse.ditto.wot.openapi.generator.providers.addApiResponse
import kotlin.jvm.optionals.getOrNull

object FeaturesPathsGenerator {

    private const val APPLICATION_JSON = "application/json"

    var apiResponsesProvider: ApiResponsesProvider = ApiResponsesProvider

    fun generateFeaturesPaths(featureName: String, featureModel: ThingModel, paths: Paths, openAPI: OpenAPI, submodelDeprecationNotice: DeprecationNotice? = null) {
        val featurePropertiesModels = featureModel.properties.getOrNull()
        val featureTitle = featureModel.title.getOrNull()?.toString() ?: featureName
        val submodelDeprecated = submodelDeprecationNotice?.deprecated == true
        paths.putAll(providePathItemsForFeature(featureName, featureTitle, submodelDeprecated, submodelDeprecationNotice))

        featurePropertiesModels?.entries
            ?.sortedBy { extractPropertyCategory(it.value) + it.key }
            ?.map {
                val dittoCategory = extractPropertyCategory(it.value)
                val relativePath = "${dittoCategory?.let { "$it/" } ?: ""}${it.key}"
                if (dittoCategory != null && !paths.containsKey("/{thingId}/features/$featureName/properties/$dittoCategory")) {
                    paths.addPathItem(
                        "/{thingId}/features/$featureName/properties/$dittoCategory",
                        providePathItemFeaturePropertiesCategory(featureName, featureTitle, dittoCategory, submodelDeprecationNotice)
                    )
                }
                paths.addPathItem("/{thingId}/features/$featureName/properties/$relativePath",
                    providePathItemFeatureProperty(featureName, featureTitle, it.key, it.key, relativePath, it.value, openAPI, submodelDeprecationNotice))

                // a readOnly property can never be desired, regardless of a ditto:desired declaration
                if (extractDesiredEnabled(it.value) && !it.value.isReadOnly) {
                    if (dittoCategory != null && !paths.containsKey("/{thingId}/features/$featureName/desiredProperties/$dittoCategory")) {
                        paths.addPathItem(
                            "/{thingId}/features/$featureName/desiredProperties/$dittoCategory",
                            providePathItemFeatureDesiredPropertiesCategory(featureName, featureTitle, dittoCategory, submodelDeprecationNotice)
                        )
                    }
                    paths.addPathItem("/{thingId}/features/$featureName/desiredProperties/$relativePath",
                        providePathItemFeatureDesiredProperty(featureName, featureTitle, it.key, it.key, relativePath, it.value, openAPI, submodelDeprecationNotice))
                }

                // recurse into nested sub-properties of object-typed properties (arbitrary depth);
                // nested ditto:category is not supported
                provideNestedPropertyPaths(featureName, featureTitle, relativePath, it.value, openAPI, paths, submodelDeprecationNotice)
            }
    }

    /**
     * Recursively exposes nested sub-properties of an object-typed property (or sub-property) as their own
     * `properties/...` GET path and, when `ditto:desired` is enabled for the nested schema, a `desiredProperties/...`
     * GET/PUT/PATCH path. Recurses to arbitrary depth for further nested objects. The parent's own whole-object
     * path is left completely unaffected. Nested `ditto:category` is not supported.
     */
    private fun provideNestedPropertyPaths(
        featureName: String,
        featureTitle: String,
        parentRelativePath: String,
        schema: SingleDataSchema,
        openAPI: OpenAPI,
        paths: Paths,
        submodelDeprecationNotice: DeprecationNotice?
    ) {
        if (schema.type.getOrNull() != DataSchemaType.OBJECT) {
            return
        }
        val objectSchema = if (schema is Property) schema.asObjectSchema() else schema as? WotObjectSchema ?: return
        objectSchema.properties.forEach { (key, nestedSchema) ->
            val relativePath = "$parentRelativePath/$key"
            val schemaRefNameSuffix = relativePath.replace("/", "_")
            paths.addPathItem(
                "/{thingId}/features/$featureName/properties/$relativePath",
                providePathItemFeatureProperty(featureName, featureTitle, key, schemaRefNameSuffix, relativePath, nestedSchema, openAPI, submodelDeprecationNotice)
            )

            // a readOnly nested property can never be desired, regardless of a ditto:desired declaration
            if (extractDesiredEnabled(nestedSchema) && !nestedSchema.isReadOnly) {
                paths.addPathItem(
                    "/{thingId}/features/$featureName/desiredProperties/$relativePath",
                    providePathItemFeatureDesiredProperty(featureName, featureTitle, key, schemaRefNameSuffix, relativePath, nestedSchema, openAPI, submodelDeprecationNotice)
                )
            }

            provideNestedPropertyPaths(featureName, featureTitle, relativePath, nestedSchema, openAPI, paths, submodelDeprecationNotice)
        }
    }

    /**
     * Detects whether [schema] (directly or transitively) contains a nested sub-property that is `ditto:desired`
     * enabled and writable (`readOnly=false`), whose *direct* containing object is explicitly `readOnly=true`.
     * When true, PUT/PATCH must be suppressed (GET-only) not just on that direct container but on every ancestor
     * above it too, since bulk-writing the ancestor would implicitly write the desired-only nested field.
     * The nested property's own `readOnly` value is never overridden by this check.
     */
    private fun hasSuppressingDescendant(schema: SingleDataSchema): Boolean {
        if (schema.type.getOrNull() != DataSchemaType.OBJECT) {
            return false
        }
        val objectSchema = if (schema is Property) schema.asObjectSchema() else schema as? WotObjectSchema ?: return false
        return objectSchema.properties.values.any { nested ->
            (schema.isReadOnly && extractDesiredEnabled(nested) && !nested.isReadOnly) || hasSuppressingDescendant(nested)
        }
    }

    private fun providePathItemFeaturePropertiesCategory(
        featureName: String,
        featureTitle: String,
        category: String,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {
        val deprecated = submodelDeprecationNotice?.deprecated == true
        val deprecationDescription = buildDeprecationDescription(submodelDeprecationNotice)
        return PathItem()
        .get(
            Operation()
                .also { if (deprecated) it.deprecated(true) }
                .summary("Retrieves all '$category' categorized properties of feature $featureTitle")
                .description(deprecationDescription)
                .tags(listOf("Feature: $featureTitle"))
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                .responses(
                    ApiResponses()
                        .addApiResponse(
                            "200", ApiResponse()
                                .description("Returns the full feature category '$category'")
                                .content(
                                    Content().addMediaType(
                                        APPLICATION_JSON, MediaType()
                                            .schema(Schema<Any>().apply {
                                                `$ref`("#/components/schemas/${asPropertyName(featureName)}_${category}_properties")
                                            })
                                    )
                                )
                        )
                        .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName/properties/$category"))
                )
        )
    }

    private fun providePathItemFeatureDesiredPropertiesCategory(
        featureName: String,
        featureTitle: String,
        category: String,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {
        val deprecated = submodelDeprecationNotice?.deprecated == true
        val deprecationDescription = buildDeprecationDescription(submodelDeprecationNotice)
        return PathItem()
        .get(
            Operation()
                .also { if (deprecated) it.deprecated(true) }
                .summary("Retrieves all '$category' categorized desired properties of feature $featureTitle")
                .description(deprecationDescription)
                .tags(listOf("Feature: $featureTitle"))
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                .responses(
                    ApiResponses()
                        .addApiResponse(
                            "200", ApiResponse()
                                .description("Returns the desired feature category '$category'")
                                .content(
                                    Content().addMediaType(
                                        APPLICATION_JSON, MediaType()
                                            .schema(Schema<Any>().apply {
                                                `$ref`("#/components/schemas/${asPropertyName(featureName)}_${category}_properties")
                                            })
                                    )
                                )
                        )
                        .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName/desiredProperties/$category"))
                )
        )
    }

    private fun providePathItemsForFeature(featureName: String, featureTitle: String, deprecated: Boolean = false, deprecationNotice: DeprecationNotice? = null): Map<String, PathItem> {
        val deprecationDescription = buildDeprecationDescription(deprecationNotice)
        return mapOf(
            "/{thingId}/features/$featureName" to PathItem()
                .get(
                    Operation()
                        .also { if (deprecated) it.deprecated(true) }
                        .summary("Retrieves the feature $featureTitle")
                        .description(deprecationDescription)
                        .tags(listOf("Feature: $featureTitle"))
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                        .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                        .responses(
                            ApiResponses()
                                .addApiResponse(
                                    "200", ApiResponse()
                                        .description("Returns the full feature")
                                        .content(
                                            Content().addMediaType(
                                                APPLICATION_JSON, MediaType()
                                                    .schema(Schema<Any>().apply {
                                                        `$ref`("feature_$featureName")
                                                    })
                                            )
                                        )
                                )
                                .addApiResponse(apiResponsesProvider.provide401ApiResponse("features/$featureName"))
                        )
                )
        )
    }

    private fun providePathItemFeatureProperty(
        featureName: String,
        featureTitle: String,
        displayName: String,
        schemaRefNameSuffix: String,
        relativePath: String,
        schema: SingleDataSchema,
        openAPI: OpenAPI,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {

        val schemaDeprecationNotice = extractDeprecationNotice(schema.toJson())
        val deprecationNotice = schemaDeprecationNotice ?: submodelDeprecationNotice
        val deprecated = deprecationNotice?.deprecated == true
        val description = mergeWithDeprecationNotice(schema.description.getOrNull()?.toString(), deprecationNotice)
        val responseSchema = provideSchema(schema, schemaRefNameSuffix, featureName, openAPI)
        if (deprecated) markSchemaDeprecated(responseSchema, openAPI)
        val path = "features/$featureName/properties/$relativePath"
        val pathItem = PathItem()
            .get(
                Operation()
                    .also { if (deprecated) it.deprecated(true) }
                    .summary("Retrieves the '${schema.title.getOrNull()?.toString()}' property")
                    .description(description)
                    .tags(listOf("Feature: $featureTitle"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The feature property '$displayName' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(responseSchema)
                                        )
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
                    )
            )
        // writes move to the desiredProperties path instead when ditto:desired is enabled for this property;
        // writes are also suppressed when a desired-writable nested property requires the parent to stay GET-only
        if (!schema.isReadOnly && !extractDesiredEnabled(schema) && !hasSuppressingDescendant(schema)) {
            pathItem
                .put(providePropertyWriteOperation("Replaces", "modified", displayName, schema, featureName, featureTitle, openAPI, deprecated, description, path, desired = false))
                .patch(providePropertyWriteOperation("Merges", "merged", displayName, schema, featureName, featureTitle, openAPI, deprecated, description, path, desired = false))
        }
        return pathItem
    }

    private fun providePathItemFeatureDesiredProperty(
        featureName: String,
        featureTitle: String,
        displayName: String,
        schemaRefNameSuffix: String,
        relativePath: String,
        schema: SingleDataSchema,
        openAPI: OpenAPI,
        submodelDeprecationNotice: DeprecationNotice? = null
    ): PathItem {

        val schemaDeprecationNotice = extractDeprecationNotice(schema.toJson())
        val deprecationNotice = schemaDeprecationNotice ?: submodelDeprecationNotice
        val deprecated = deprecationNotice?.deprecated == true
        val description = mergeWithDeprecationNotice(schema.description.getOrNull()?.toString(), deprecationNotice)
        val responseSchema = provideSchema(schema, schemaRefNameSuffix, featureName, openAPI)
        if (deprecated) markSchemaDeprecated(responseSchema, openAPI)
        val path = "features/$featureName/desiredProperties/$relativePath"
        return PathItem()
            .get(
                Operation()
                    .also { if (deprecated) it.deprecated(true) }
                    .summary("Retrieves the desired '${schema.title.getOrNull()?.toString()}' property")
                    .description(description)
                    .tags(listOf("Feature: $featureTitle"))
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.PATH_PARAM_THING_ID) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_FIELDS) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_CHANNEL) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_CONDITION) })
                    .addParametersItem(Parameter().apply { `$ref`(ParametersProvider.QUERY_PARAM_LIVE_CHANNEL_TIMEOUT_STRATEGY) })
                    .responses(
                        ApiResponses()
                            .addApiResponse(
                                "200", ApiResponse()
                                    .description("The desired feature property '$displayName' is returned")
                                    .content(
                                        Content().addMediaType(
                                            APPLICATION_JSON,
                                            MediaType().schema(responseSchema)
                                        )
                                    )
                            )
                            .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                            .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
                    )
            )
            .put(providePropertyWriteOperation("Replaces", "modified", displayName, schema, featureName, featureTitle, openAPI, deprecated, description, path, desired = true))
            .patch(providePropertyWriteOperation("Merges", "merged", displayName, schema, featureName, featureTitle, openAPI, deprecated, description, path, desired = true))
    }

    /**
     * Builds the PUT ("Replaces") or PATCH ("Merges") operation for a feature property, shared between the
     * regular `properties` path and the `desiredProperties` path (when `ditto:desired` is enabled).
     */
    private fun providePropertyWriteOperation(
        verb: String,
        successfullyWord: String,
        displayName: String,
        schema: SingleDataSchema,
        featureName: String,
        featureTitle: String,
        openAPI: OpenAPI,
        deprecated: Boolean,
        description: String?,
        path: String,
        desired: Boolean
    ): Operation {
        val titleLabel = schema.title.getOrNull()?.toString()
        val summary = if (desired) "$verb the desired '$titleLabel' property" else "$verb the '$titleLabel' property"
        val propertyLabel = if (desired) "desired feature property" else "feature property"
        return Operation()
            .also { if (deprecated) it.deprecated(true) }
            .summary(summary)
            .description(description)
            .tags(listOf("Feature: $featureTitle"))
            .responses(
                ApiResponses()
                    .addApiResponse(
                        "201", ApiResponse()
                            .description("The $propertyLabel '$displayName' was successfully created")
                            .content(
                                Content().addMediaType(
                                    APPLICATION_JSON,
                                    MediaType().schema(asOpenApiSchema(schema, featureName, "property", openAPI))
                                )
                            )
                    )
                    .addApiResponse(
                        "204", ApiResponse()
                            .description("The $propertyLabel '$displayName' was successfully $successfullyWord")
                    )
                    .addApiResponse(apiResponsesProvider.provide400ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide401ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide403ApiResponse(path))
                    .addApiResponse(apiResponsesProvider.provide404ApiResponse(path))
            )
    }

    private fun provideSchema(schema: SingleDataSchema, schemaRefNameSuffix: String, featureName: String, openAPI: OpenAPI) =
        if (isPrimitive(schema.type.getOrNull())) {
            asOpenApiSchema(schema, null, "property", openAPI)
        } else {
            Schema<Any>().apply { `$ref`("#/components/schemas/feature_${asPropertyName(featureName)}_property_${asPropertyName(schemaRefNameSuffix)}") }
        }

}
