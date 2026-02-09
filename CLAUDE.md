# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eclipse Ditto WoT (Web of Things) Tooling — a multi-module Maven project providing:
- **wot-to-openapi-generator**: Converts WoT Thing Models into OpenAPI 3.1.0 specs for Eclipse Ditto HTTP endpoints (standalone CLI + library)
- **wot-kotlin-generator**: Generates type-safe Kotlin data classes, DSLs, and path builders from WoT Thing Models (Maven plugin + common models library)

## Build Commands

```bash
# Full build (what CI runs)
mvn verify --batch-mode --errors

# Quick build (skip tests)
mvn package -DskipTests

# Build with Javadoc (as CI does)
mvn verify javadoc:jar source:jar -DcreateJavadoc=true

# Run all tests
mvn test

# Run a single test class
mvn test -pl wot-kotlin-generator/wot-kotlin-generator-maven-plugin -Dtest=GeneratedModelAndDslRegressionTest

# Validate license headers
mvn license:check
```

**Requirements:** JDK 21 (Temurin), Maven 3.9+

## Module Structure

```
ditto-wot-tooling (root pom, groupId: org.eclipse.ditto)
├── wot-to-openapi-generator/          # Fat JAR CLI tool
│   └── Main: GeneratorStarter.kt      # Entry point for CLI usage
└── wot-kotlin-generator/              # Parent for code generation modules
    ├── wot-kotlin-generator-common-models/   # Runtime library for generated code
    │   └── Base types: Thing, Feature, Property, Action, path builders, DSL support
    └── wot-kotlin-generator-maven-plugin/    # Maven plugin that generates Kotlin code
        └── Mojo: WotKotlinCodegenMojo.kt     # Plugin entry point (phase: generate-sources)
```

## Architecture

### wot-to-openapi-generator
- **Entry point:** `GeneratorStarter.kt` — CLI that accepts TM URL + output dir
- **Core:** `OpenApiGenerator` interface / `OpenApiGeneratorImpl` — orchestrates OpenAPI spec generation
- **Loader:** Fetches WoT Thing Models via HTTP (Ktor client) or file, with Caffeine caching
- **Path generators:** Separate generators for thing-level, attributes, features, and actions (`thing/`, `features/` packages)
- **Providers:** Error responses, parameters, and common response schemas (`providers/` package)
- Output: OpenAPI YAML files in `generated/` directory

### wot-kotlin-generator-maven-plugin
- **Entry point:** `GeneratorStarter.kt` — orchestrates code generation
- **ThingModelGenerator:** Processes WoT Thing Model JSON, resolves references
- **PropertyResolver / SchemaTypeResolver:** Resolves WoT property schemas into Kotlin types
- **Code generation:** Uses KotlinPoet to emit type-safe data classes, DSL builders, enums, and path classes
- **Configuration strategies:**
  - `EnumGenerationStrategy`: `INLINE` (nested) vs `SEPARATE_CLASS` (top-level)
  - `ClassNamingStrategy`: `COMPOUND_ALL` vs `ORIGINAL_THEN_COMPOUND`
  - DSL options: regular and/or suspend function variants

### wot-kotlin-generator-common-models
- Runtime dependency for generated code — provides base interfaces and path builder infrastructure
- Key abstractions: `Thing`, `Feature`, `Property`, `Action`, `Attributes`, `Features`
- Type-safe path system: `PathGenerator`, `Path`, `Segment`, and ~20 specialized path types
- `DittoRql` for constructing RQL filter queries
- Custom Jackson serializers for null-aware JSON handling

## Key Technology Stack

- **Kotlin 2.2.x** with JDK 21 target
- **KotlinPoet 2.2.x** for Kotlin source code generation
- **Eclipse Ditto WoT API 3.8.x** for WoT Thing Model parsing
- **Ktor Client 3.2.x** for HTTP operations
- **Swagger SDK 2.2.x** for OpenAPI model construction
- **Jackson 2.20.x** for JSON serialization
- **JUnit Jupiter 5** + Kotlin Test for testing

## Conventions

- **License:** All source files (`.kt`, `.java`, `.scala`, `.sh`, `pom.xml`) must have EPL-2.0 headers. Validated by `mycila/license-maven-plugin` during `validate` phase. Header template: `legal/headers/license-header.txt`
- **Formatting:** 4-space indentation, 120-char max line length, LF line endings (see `.editorconfig`)
- **Versioning:** Uses `${revision}` property-based versioning (currently `0-SNAPSHOT`); flattened for releases via `flatten-maven-plugin`
- **Source layout:** `src/main/kotlin` and `src/test/kotlin` following standard Maven Kotlin conventions
