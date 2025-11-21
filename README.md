# Eclipse Ditto™ - WoT (Web of Things) Tooling

[![Join the chat at https://gitter.im/eclipse/ditto](https://badges.gitter.im/eclipse/ditto.svg)](https://gitter.im/eclipse/ditto?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://github.com/eclipse-ditto/ditto-wot-tooling/actions/workflows/maven.yml/badge.svg)](https://github.com/eclipse-ditto/ditto-wot-tooling/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)

### WoT to OpenAPI Generator

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.ditto/wot-to-openapi-generator?label=maven)](https://search.maven.org/search?q=g:org.eclipse.ditto%20AND%20a:wot-to-openapi-generator)

The [WoT to OpenAPI Generator](wot-to-openapi-generator) is a Kotlin-based tool that converts WoT (Web of Things) 
Thing Models (JSON-LD format) into Eclipse Ditto OpenAPI 3.1.0 specifications.

It is available as command line interface (CLI) to be executed with `java -jar`, but can also be used as a 
library in your own applications.

### WoT Kotlin Generator Maven plugin

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.ditto/wot-kotlin-generator-maven-plugin?label=maven)](https://search.maven.org/search?q=g:org.eclipse.ditto%20AND%20a:wot-kotlin-generator-maven-plugin)

The [WoT Kotlin Generator Maven plugin](wot-kotlin-generator) is a Maven plugin that generates Kotlin code 
(e.g. data classes) based on WoT (Web of Things) Thing Models (JSON-LD format) it downloads via HTTP.

The generated code can be used as type-safe representation of WoT Thing Models in your own Kotlin applications.

