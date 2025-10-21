# Eclipse Ditto WoT Kotlin Generator

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg)](../LICENSE)

A tool for generating type-safe Kotlin data classes and DSLs from Web of Things (WoT) Thing models with path generation capabilities for Eclipse Ditto.

**Project URL:** https://github.com/eclipse-ditto/ditto-wot-tooling

## Overview

The WoT Kotlin Generator transforms WoT Thing Models (JSON-LD format) into strongly-typed Kotlin classes, providing:

- **Type Safety**: Compile-time validation of WoT schema constraints
- **DSL Support**: Fluent builder patterns for easy object construction
- **Eclipse Ditto Integration**: Built on top of the Eclipse Ditto WoT API and Model
- **Path Generation**: Type-safe path construction for RQL queries
- **Flexible Configuration**: Multiple strategies for enum generation and class naming
- **Suspend Function Support**: Optional coroutine support for asynchronous DSL operations

## Module Structure

This is a multi-module Maven project consisting of:

### 1. wot-kotlin-generator-common-models
Common model classes and base interfaces used by generated code:
- Path generation APIs
- DSL base classes
- Serialization/deserialization utilities
- Feature and property interfaces

**Consumers must include this module as a dependency** to use the generated code.

```xml
<dependency>
    <groupId>org.eclipse.ditto</groupId>
    <artifactId>wot-kotlin-generator-common-models</artifactId>
    <version>0-SNAPSHOT</version>
</dependency>
```

### 2. wot-kotlin-generator-plugin
Maven plugin for code generation:
- WoT Thing Model parsing
- Kotlin code generation
- Enum generation strategies
- Class naming strategies
- DSL generation

**Add this plugin to your build** to generate Kotlin code from WoT Thing Models.

```xml
<plugin>
    <groupId>org.eclipse.ditto</groupId>
    <artifactId>wot-kotlin-generator-plugin</artifactId>
    <version>0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>codegen</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Quick Start

See the [plugin README](wot-kotlin-generator-plugin/README.md) for detailed usage instructions, configuration options, and examples.

### Basic Usage

1. Add both the common-models dependency and the plugin to your `pom.xml`
2. Configure the plugin with your WoT Thing Model URL
3. Run: `mvn clean compile`
4. Use the generated Kotlin classes in your code

## Building from Source

```bash
cd wot-kotlin-generator
mvn clean install
```

This will build both modules:
1. `wot-kotlin-generator-common-models` (built first as a dependency)
2. `wot-kotlin-generator-plugin`

## Prerequisites

- Java 21 or later
- Maven 3.9+ or Maven Wrapper
- Kotlin 2.0+ (for consuming projects)

## Technology Stack

- **Kotlin**: 2.1.21
- **Eclipse Ditto**: 3.8.0-M4
- **KotlinPoet**: 2.2.0 (for code generation)
- **Jackson**: 2.19.2 (for JSON processing)
- **Ktor**: 3.2.0 (for HTTP client)
- **Maven Plugin API**: 3.9.11

## Key Features

### Type-Safe Path Generation
Generate compile-time validated paths for Eclipse Ditto queries:

```kotlin
val path = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .buildSearchProperty()
    .gt(20.0)
```

### Flexible Enum Generation
Choose between inline enums or separate class enums based on your needs.

### Multiple Naming Strategies
Configure how generated classes are named to match your project conventions.

### Suspend DSL Support
Generate coroutine-friendly DSL functions for asynchronous operations.

## Documentation

- [Plugin README](wot-kotlin-generator-plugin/README.md) - Comprehensive usage guide
- [Integration Summary](../INTEGRATION_SUMMARY.md) - Technical integration details
- [Eclipse Ditto Documentation](https://eclipse.dev/ditto/) - Eclipse Ditto platform docs

## Contributing

We welcome contributions! Please see the [Contributing Guide](../CONTRIBUTING.md) for details.

## License

This project is licensed under the [Eclipse Public License v2.0](../LICENSE).

## Support

- **Issues**: Report bugs via [GitHub Issues](https://github.com/eclipse-ditto/ditto-wot-tooling/issues)
- **Community**: Join the [Eclipse Ditto community](https://www.eclipse.org/ditto/community.html)
- **Discussions**: Use [GitHub Discussions](https://github.com/eclipse-ditto/ditto-wot-tooling/discussions)

## Related Projects

- [Eclipse Ditto](https://www.eclipse.org/ditto/) - IoT platform for device management
- [Web of Things (WoT)](https://www.w3.org/WoT/) - W3C Web of Things specification
- [Eclipse Ditto WoT API](https://github.com/eclipse-ditto/ditto) - WoT API implementation

