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

/**
 * Package for OpenAPI component providers.
 * 
 * This package contains providers that supply common OpenAPI components
 * used across the generated specifications:
 * 
 * - Common parameters (path parameters, query parameters)
 * - Error response schemas
 * - API response templates
 * - Security schemes and requirements
 * 
 * Key components:
 * - [ParametersProvider]: Provides common OpenAPI parameters
 * - [ErrorProvider]: Provides error response schemas
 * - [ApiResponsesProvider]: Provides common API response templates
 * 
 * These providers ensure consistency across generated OpenAPI specifications
 * and provide reusable components that follow OpenAPI best practices.
 */
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.providers
