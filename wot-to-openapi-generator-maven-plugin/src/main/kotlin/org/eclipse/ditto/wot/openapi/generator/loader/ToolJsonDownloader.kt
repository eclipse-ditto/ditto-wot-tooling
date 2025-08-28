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
package main.kotlin.org.eclipse.ditto.wot.openapi.generator.loader

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import org.eclipse.ditto.json.JsonFactory
import org.eclipse.ditto.json.JsonObject
import org.eclipse.ditto.wot.api.provider.JsonDownloader
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

object ToolJsonDownloader : JsonDownloader {

    @OptIn(DelicateCoroutinesApi::class)
    override fun downloadJsonViaHttp(url: URL, executor: Executor): CompletionStage<JsonObject> {
        return GlobalScope.future { downloadInCoroutine(url) }
    }

    private suspend fun downloadInCoroutine(url: URL): JsonObject {
        val client = HttpClient(CIO)
        val response = client.get(url) {
            accept(ContentType.parse("application/tm+json"))
        }

        if (!response.status.isSuccess()) {
            throw IOException("Could not download ThingModel JSON from ${url}: ${response.status}")
        } else {
            return JsonFactory.readFrom(response.bodyAsText(StandardCharsets.UTF_8)).asObject()
        }
    }
}