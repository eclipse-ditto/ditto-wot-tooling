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
 * Package for WoT model loading utilities.
 * 
 * This package contains components responsible for loading WoT Thing models
 * from various sources:
 * 
 * - Loading models from URLs
 * - Downloading model files
 * - Parsing WoT JSON-LD documents
 * - Handling model dependencies and references
 * 
 * Key components:
 * - [DittoBasedWotLoader]: Loads WoT models using Eclipse Ditto libraries
 * - [ToolJsonDownloader]: Downloads JSON files from URLs
 * 
 * These components provide the foundation for accessing and parsing
 * WoT Thing models that will be converted to OpenAPI specifications.
 */
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.loader
