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
package org.eclipse.ditto.wot.openapi.generator.providers

import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter

object ParametersProvider {

    const val PATH_PARAM_THING_ID = "PathParamThingId"
    const val QUERY_PARAM_FIELDS = "QueryParamFields"
    const val QUERY_PARAM_CONDITION = "QueryParamCondition"

    private val parameters: Map<String, Parameter> = mapOf(
        PATH_PARAM_THING_ID to Parameter()
            .name("thingId")
            .description(
                """
                    The ID of a thing needs to follow the namespaced entity ID notation
                    (see [Ditto documentation on namespaced entity IDs](https://www.eclipse.dev/ditto/basic-namespaces-and-names.html#namespaced-id))
                """.trimIndent())
            .`in`("path")
            .required(true)
            .schema(StringSchema()),

        QUERY_PARAM_FIELDS to Parameter()
            .name("fields")
            .description(
                """
                    Contains a comma-separated list of fields to be included in the returned JSON, performing a
                    [partial request](https://eclipse.dev/ditto/httpapi-concepts.html#with-field-selector).

                    #### Selectable fields

                    Supports selecting arbitrary sub-fields  by using a comma-separated list:
                      * several properties paths can be passed as a comma-separated list of JSON pointers (RFC-6901)

                        For example:
                          * `?fields=model` would select only `model` value (if present)
                          * `?fields=model,make` would select `model` and `make` values (if present)

                    Supports selecting arbitrary sub-fields of objects by wrapping sub-fields inside parentheses `( )`:
                      * a comma-separated list of sub-fields (a sub-field is a JSON pointer (RFC-6901) separated with `/`) to select
                      * sub-selectors can be used to request only specific sub-fields by placing expressions in parentheses `( )` after a selected subfield

                        For example:
                         * `?fields=location(longitude,latitude)` would select the `longitude` and `latitude` value inside the `location` object

                    #### Examples

                    * `?fields=model,make,location(longitude,latitude)`
                    * `?fields=listOfAddresses/postal(city,street))`
                """.trimIndent()
            )
            .`in`("query")
            .required(false)
            .schema(StringSchema()),

        QUERY_PARAM_CONDITION to Parameter()
            .name("condition")
            .description(
                """
                    Defines that the [request should be done conditionally](https://eclipse.dev/ditto/basic-conditional-requests.html)
                    only be processed if the given condition is met.
                    The condition can be specified using [RQL](https://eclipse.dev/ditto/basic-rql.html) syntax.

                    The `condition` can be passed for both, `GET` verb (conditional request) and modifying verbs (conditional update).

                    #### Examples
                    E.g. if the temperature is not 23.9 update it to 23.9:
                    * `?condition=ne(features/temperature/properties/value,23.9)`

                       `body: 23.9`
                """.trimIndent()
            )
            .`in`("query")
            .required(false)
            .schema(StringSchema())
    )

    fun resolveParameter(paramKey: String) = paramKey to parameters[paramKey]
}