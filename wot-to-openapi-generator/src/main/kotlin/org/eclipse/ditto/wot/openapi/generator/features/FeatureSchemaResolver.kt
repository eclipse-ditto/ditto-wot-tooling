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
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import org.eclipse.ditto.wot.model.ArraySchema
import org.eclipse.ditto.wot.model.Properties
import org.eclipse.ditto.wot.model.Property
import org.eclipse.ditto.wot.model.ThingModel
import org.eclipse.ditto.wot.openapi.generator.Utils.asOpenApiSchema
import org.eclipse.ditto.wot.openapi.generator.Utils.asPropertyName
import org.eclipse.ditto.wot.openapi.generator.Utils.extractPropertyCategory
import kotlin.jvm.optionals.getOrNull
import org.eclipse.ditto.wot.model.ObjectSchema as WotObjectSchema

object FeatureSchemaResolver {

    fun resolveCompleteFeaturesSchema(featureModels: List<Pair<String, ThingModel>>, openAPI: OpenAPI): Schema<*>? {
        val featuresSchema = ObjectSchema()
        featureModels.sortedBy { it.first }.forEach { (featureName, _) ->
            featuresSchema.addProperty(featureName, Schema<Any>().apply{
                `$ref`("feature_$featureName")
            })
        }
        return featuresSchema
    }

    fun resolveFeatureSchema(featureName: String, featureModel: ThingModel, openAPI: OpenAPI): Schema<*>? {
        val featureSchema = featureModel.properties.getOrNull()?.let {
            val propertiesSchemaRefName = "${asPropertyName(featureName)}_properties"
            val propertiesSchema = createPropertiesSchema(it, featureName, openAPI)
            openAPI.schema(propertiesSchemaRefName, propertiesSchema)
            val propertiesSchemaRef = Schema<Any>().`$ref`("#/components/schemas/$propertiesSchemaRefName")
            ObjectSchema()
                .addProperty("properties", propertiesSchemaRef)
                .addProperty("desiredProperties", propertiesSchemaRef)
                    as ObjectSchema
        }

        featureSchema?.title = featureModel.title.get().toString()
        featureSchema?.description = featureModel.description.get().toString()
        return featureSchema
    }

    private fun createPropertiesSchema(properties: Properties, featureName: String, openAPI: OpenAPI): ObjectSchema {
        val propertiesSchema = ObjectSchema()

        val groupedByCategories = properties.entries
            .filter { extractPropertyCategory(it.value) != null }
            .groupBy { extractPropertyCategory(it.value) }

        groupedByCategories.forEach { (category, entry) ->
            val schemaRefName = "${asPropertyName(featureName)}_${category}_properties"
            openAPI.schema(
                schemaRefName,
                createPropertyCategorySchema(entry.associate { it.key to it.value }, featureName, openAPI)
            )
            val schemaRef = Schema<Any>().`$ref`("#/components/schemas/$schemaRefName")
            propertiesSchema.addProperty(category, schemaRef)
        }

        properties.filter { extractPropertyCategory(it.value) == null }
            .forEach { (name, property) ->
                if (property is WotObjectSchema || property is ArraySchema) {
                    val schemaRefName = "feature_${asPropertyName(featureName)}_property_${asPropertyName(name)}"
                    openAPI.schema(schemaRefName, asOpenApiSchema(property, featureName, "property", openAPI))
                    val schemaRef = Schema<Any>().`$ref`("#/components/schemas/$schemaRefName")
                    propertiesSchema.addProperty(name, schemaRef)
                } else {
                    propertiesSchema.addProperty(name, asOpenApiSchema(property, featureName, "property", openAPI))
                }
            }
        return propertiesSchema
    }

    private fun createPropertyCategorySchema(
        categoryProperties: Map<String, Property>,
        featureName: String,
        openAPI: OpenAPI
    ): ObjectSchema? {
        val categorySchema = ObjectSchema()
        categoryProperties.forEach { (name, property) ->
            categorySchema.addProperty(name, asOpenApiSchema(property, featureName, "property", openAPI))
        }
        return categorySchema
    }

}