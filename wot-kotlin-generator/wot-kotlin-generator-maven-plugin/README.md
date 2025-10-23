# Eclipse Ditto™ - WoT (Web of Things) Tooling :: WoT Kotlin Generator :: Maven plugin

A tool for generating type-safe Kotlin data classes and DSLs from Web of Things (WoT) Thing models with path 
generation capabilities for Eclipse Ditto

**Project URL:** https://github.com/eclipse-ditto/ditto-wot-tooling/tree/main/wot-kotlin-generator

## Overview

The WoT Kotlin Generator Plugin transforms WoT Thing Models (JSON-LD format) into strongly-typed Kotlin classes, providing:

- **Type Safety**: Compile-time validation of WoT schema constraints
- **DSL Support**: Fluent builder patterns for easy object construction
- **Eclipse Ditto Integration**: Built on top of the Eclipse Ditto WoT API and Model
- **Flexible Configuration**: Multiple strategies for enum generation and class naming
- **Suspend Function Support**: Optional coroutine support for asynchronous DSL operations

## Prerequisites

- Java 21 or later
- Maven 3.9+ or Maven Wrapper
- Kotlin 2.0+ (for consuming projects)

## Installation

### Maven Dependencies

Add the plugin and common model dependency to your project's `pom.xml`:

```xml
<dependencies>
    <!-- Common model classes required by generated code -->
    <dependency>
        <groupId>org.eclipse.ditto</groupId>
        <artifactId>wot-kotlin-generator-common-models</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.eclipse.ditto</groupId>
            <artifactId>wot-kotlin-generator-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>codegen</goal>
                    </goals>
                    <configuration>
                        <thingModelUrl>https://example.org/thing-model.jsonld</thingModelUrl>
                        <packageName>org.example.generated</packageName>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Multi-Module Structure

The WoT Kotlin Generator consists of two Maven modules:

1. **`wot-kotlin-generator-common-models`**: Contains shared interfaces and base classes
2. **`wot-kotlin-generator-maven-plugin`**: The Maven plugin that generates code

**Important**: Consumer projects must include both dependencies:
- The **common-models** dependency provides the base classes that generated code extends
- The **plugin** dependency provides the code generation functionality

### Plugin Configuration Parameters

| Parameter | Type | Required | Default | Description                                                              |
|-----------|------|----------|---------|--------------------------------------------------------------------------|
| `thingModelUrl` | String | Yes | - | URL or path to the WoT Thing Model                                       |
| `packageName` | String | No | `org.eclipse.ditto.wot.kotlin.generator.model` | Target package for generated classes                                     |
| `outputDir` | String | No | `target/generated-sources` | Output directory for generated sources                                   |
| `enumGenerationStrategy` | String | No | `INLINE` | Strategy for generating enums (`INLINE` or `SEPARATE_CLASS`)             |
| `classNamingStrategy` | String | No | `COMPOUND_ALL` | Strategy for naming classes (`COMPOUND_ALL` or `ORIGINAL_THEN_COMPOUND`) |
| `generateSuspendDsl` | boolean | No | `false` | Whether DSL functions should be suspend functions                        |


## Configuration Strategies

### Enum Generation Strategy

The plugin supports two strategies for generating enums from WoT schema enums:

#### INLINE (Default)
Generates enums as nested classes within the classes that use them.

**Advantages:**
- Compact representation
- Reduced file count
- Efficient for simple enums

**Example:**
```kotlin
data class Thermostat(
    val status: ThermostatStatus
) {
    enum class ThermostatStatus {
        ONLINE, OFFLINE, ERROR
    }
}
```

#### SEPARATE_CLASS
Generates enums as standalone classes in separate files.

**Advantages:**
- Better IDE support
- Standalone usage
- Standard enum behavior
- Reusable across multiple classes

**Example:**
```kotlin
// ThermostatStatus.kt
enum class ThermostatStatus {
    ONLINE, OFFLINE, ERROR
}

// Thermostat.kt
data class Thermostat(
    val status: ThermostatStatus
)
```

### Class Naming Strategy

The plugin supports two strategies for naming generated classes:

#### COMPOUND_ALL (Default)
Always combines parent and child names to create unique class names.

**Examples:**
- Parent: "smartheating", Child: "thermostat" → `SmartheatingThermostat`
- Parent: "room", Child: "attributes" → `RoomAttributes`
- Parent: "battery", Child: "properties" → `BatteryProperties`

**Advantages:**
- Guaranteed unique class names
- Clear hierarchy indication
- No naming conflicts

#### ORIGINAL_THEN_COMPOUND
Uses original schema titles when possible, falls back to compound naming for conflicts.

**Examples:**
- Schema title: "Thermostat" → `Thermostat` (if no conflict)
- Schema title: "Thermostat" → `SmartheatingThermostat` (if conflict exists)

**Advantages:**
- Shorter, more readable names when possible
- Maintains original schema naming intent
- Automatic conflict resolution

### DSL Configuration

#### Regular DSL (Default)
Generates standard Kotlin DSL functions:

```kotlin
fun thermostat(block: Thermostat.() -> Unit): Thermostat {
    val thermostat = Thermostat()
    thermostat.block()
    return thermostat
}

// Usage
val myThermostat = thermostat {
    status = ThermostatStatus.ONLINE
    temperature = 22.5
}
```

#### Suspend DSL
Generates suspend functions for coroutine support:

```kotlin
suspend fun thermostat(block: suspend Thermostat.() -> Unit): Thermostat {
    val thermostat = Thermostat()
    thermostat.block()
    return thermostat
}

// Usage
val myThermostat = thermostat {
    status = ThermostatStatus.ONLINE
    temperature = 22.5
}
```

## Usage Examples

### Basic Usage

```bash
mvn org.eclipse.ditto:wot-kotlin-generator-maven-plugin:codegen \
  -DthingModelUrl=https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld \
  -DpackageName=com.example.iot \
  -DoutputDir=target/generated-sources
```

### Advanced Configuration

#### Separate Class Enums with Compound Naming

```bash
mvn org.eclipse.ditto:wot-kotlin-generator-maven-plugin:codegen \
  -DthingModelUrl=https://example.org/thing-model.jsonld \
  -DpackageName=com.example.iot \
  -DoutputDir=target/generated-sources \
  -DenumGenerationStrategy=SEPARATE_CLASS \
  -DclassNamingStrategy=COMPOUND_ALL
```

#### Suspend DSL with Original Naming

```bash
mvn org.eclipse.ditto:wot-kotlin-generator-maven-plugin:codegen \
  -DthingModelUrl=https://example.org/thing-model.jsonld \
  -DpackageName=com.example.iot \
  -DoutputDir=target/generated-sources \
  -DclassNamingStrategy=ORIGINAL_THEN_COMPOUND \
  -DgenerateSuspendDsl=true
```

### Using Generated Code

```kotlin
import com.example.iot.floorLamp
import com.example.iot.features
import com.example.iot.lamp

// Create a thing using the generated DSL
val myThing = floorLamp {
    thingId = ThingId.of("org.example:my-lamp")
    attributes {
        location {
            room {
                id = "living-room"
                type = "living"
            }
        }
    }
    features {
        lamp {
            properties {
                status = LampStatus.ONLINE
                brightness = 80
                color = "#FFFFFF"
            }
        }
    }
}
```

## Generated Code Structure

The plugin generates the following structure:

```
generated-sources/
└── com/example/iot/
    ├── FloorLamp.kt                         # Main thing class
    ├── attributes/
    │   ├── Attributes.kt                    # Attributes interface
    │   ├── Location.kt                      # Location properties
    │   └── Room.kt                          # Room properties
    ├── features/
    │   ├── Features.kt                      # Features interface
    │   ├── lamp/
    │   │   ├── Lamp.kt                      # Lamp feature
    │   │   ├── LampProperties.kt            # Lamp properties
    │   │   └── LampStatus.kt                # Status enum (if SEPARATE_CLASS)
    │   └── battery/
    │       ├── Battery.kt                   # Battery feature
    │       └── BatteryProperties.kt         # Battery properties
    └── actions/
        └── ThingActions.kt                  # Actions interface
```

### Generated Class Types

- **Main Thing Class**: Contains the core thing definition with properties, actions, and events
- **Property Classes**: Type-safe interfaces for thing properties with proper validation
- **Feature Classes**: Interfaces for thing features with nested property structures
- **Enum Classes**: For enumerated values defined in the WoT model
- **DSL Functions**: Fluent builder functions for easy object construction
- **Action Classes**: Interfaces for thing actions with proper input/output schemas

## Path Generation and Type-Safe Queries

The generated code includes powerful path generation capabilities that enable type-safe construction of hierarchical paths and RQL (Resource Query Language) queries. This eliminates the risk of string-based errors and provides compile-time validation.

### Path Generation Benefits

- **Type Safety**: Compile-time validation prevents typos and invalid path segments
- **IDE Support**: Full autocomplete and refactoring support
- **Maintainability**: Paths automatically update when the underlying model changes
- **Performance**: Efficient path construction without string concatenation
- **Error Prevention**: Eliminates runtime errors from malformed path strings

### Path Construction Examples

#### Basic Path Building

```kotlin
// Type-safe path construction
val path = PathGenerator.from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .build()

// Result: "features/thermostat/properties/temperature"
```

### RQL Query Construction

The path generation system enables type-safe construction of complex RQL queries:

```kotlin
// Complex RQL query with type-safe paths
val query = and(
    // Check if battery feature exists
    pathBuilder().from(start = Thing::features)
        .add(Features::battery)
        .buildSearchProperty()
        .exists(),
    
    // Complex OR condition with temperature comparison
    or(
        pathBuilder().from(start = Thing::features)
            .add(Features::thermostat)
            .add(Thermostat::properties)
            .add(ThermostatProperties::temperature)
            .buildSearchProperty()
            .gt(25.0),
        
        pathBuilder().from(start = Thing::features)
            .add(Features::battery)
            .add(Battery::properties)
            .add(BatteryProperties::level)
            .buildSearchProperty()
            .lt(0.2)
    )
).toString()

// Generated RQL query:
// and(exists(features/battery),or(gt(features/thermostat/properties/temperature,25.0),lt(features/battery/properties/level,0.2)))
```

### Path Builder API

The generated code provides a fluent API for building paths:

#### Path Builder Methods

- **`from(start: KProperty1<*, *>)`**: Start path construction from a property
- **`add(property: KProperty1<*, *>)`**: Add a property segment to the path
- **`build()`**: Finalize the path construction
- **`buildSearchProperty()`**: Create a search property for RQL queries

#### Search Property Methods

- **`exists()`**: Check for property existence
- **`eq(value: Any)`**: Equality comparison
- **`ne(value: Any)`**: Not equal comparison
- **`gt(value: Any)`**: Greater than comparison
- **`ge(value: Any)`**: Greater than or equal comparison
- **`lt(value: Any)`**: Less than comparison
- **`le(value: Any)`**: Less than or equal comparison
- **`like(pattern: String)`**: Search with wildcard `?` for single character and `*` for multiple characters
- **`ilike(pattern: String)`**: Case insensitive search with wildcard `?` for single character and `*` for multiple characters
- **`in(values: Collection<*>)`**: In collection comparison

### Integration with Eclipse Ditto

The generated paths work seamlessly with Eclipse Ditto's APIs:

```kotlin
// Using generated paths with Ditto's search API
val searchQuery = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .buildSearchProperty()
    .gt(20.0)

// Execute search with Ditto client
val results = dittoClient.search(searchQuery.toString())
```

### Advanced Path Patterns

#### Nested Property Access

```kotlin
// Deep nested property access
val deepPath = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .build()
```

#### Array Property Access

```kotlin
// Array property access with index
val arrayPath = pathBuilder().from(start = Thing::features)
    .add(Features::sensors)
    .add(Sensors::properties)
    .add(SensorsProperties::readings)
    .buildSearchProperty()
    .at(0) // Access first element
    .gt(threshold)
```

### Error Prevention Examples

#### Before (String-based approach - error-prone)

```kotlin
val path1 = "features/thermostat/properties/temperature" // Typo: "thermostat" vs "thermostat"
val path2 = "features/thermostat/properties/status"      // No compile-time validation
val path3 = "features/battery/properties/level"          // Hard to maintain

// Runtime errors possible
val query = "features/thermostat/properties/temperature > 20" // String concatenation
```

#### After (Type-safe approach - error-free)

```kotlin
val path1 = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .build()

val path2 = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::status)
    .build()

// Compile-time validation and IDE support
val query = pathBuilder().from(start = Thing::features)
    .add(Features::thermostat)
    .add(Thermostat::properties)
    .add(ThermostatProperties::temperature)
    .buildSearchProperty()
    .gt(20.0)
    .toString()
```

### Performance Benefits

- **Efficient Construction**: Paths are built using property references, not string operations
- **Memory Optimization**: No intermediate string objects created during path building
- **Caching**: Generated paths can be cached and reused efficiently
- **Lazy Evaluation**: Paths are only converted to strings when needed

## WoT Thing Model Support

The plugin supports the WoT Thing Model specification including:

- **Properties**: Read/write properties with various data types
- **Actions**: Executable actions with input/output schemas
- **Events**: Event definitions with payload schemas
- **Links**: References to other WoT models

### Supported Data Types

- **Primitive Types**: `string`, `number`, `integer`, `boolean`
- **Complex Types**: `object`, `array`
- **Special Types**: `null`, `any`
- **Custom Types**: User-defined types via `$ref` references

## Integration with Eclipse Ditto

The generated code integrates seamlessly with Eclipse Ditto:

- **Thing Management**: Generated classes work with Ditto's thing management APIs
- **Message Handling**: Supports Ditto's message routing and processing
- **Search**: Generated classes work with Ditto's search capabilities

## Development

### Building from Source

```bash
git clone https://github.com/eclipse-ditto/ditto-wot-tooling.git
cd wot-kotlin-generator
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Local Development

```bash
mvn clean compile
mvn org.eclipse.ditto:wot-kotlin-generator-maven-plugin:codegen \
  -DthingModelUrl=https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld \
  -DpackageName=com.example.test \
  -DoutputDir=target/test-generated
```

## Support

### Getting Help

- **Documentation**: Check this README and the generated code documentation
- **Issues**: Report bugs and request features via [GitHub Issues](https://github.com/eclipse-ditto/ditto-wot-tooling/issues)
- **Community**: Join the [Eclipse Ditto community](https://www.eclipse.org/ditto/community.html)

### Reporting Issues

When reporting issues, please include:
- Plugin version and configuration
- WoT Thing Model (if applicable)
- Error messages and stack traces
- Steps to reproduce the issue
- Environment information (Java version, Maven version, OS)

## Related Projects

- [Eclipse Ditto](https://www.eclipse.org/ditto/) - IoT platform for device management
- [Eclipse Ditto WoT API](https://github.com/eclipse-ditto/ditto/tree/master/wot) - Web of Things API implementation
- [KotlinPoet](https://github.com/square/kotlinpoet) - Kotlin code generation library

## Project Status

- **Version**: 1.0.1-SNAPSHOT
- **Status**: Active Development
- **Compatibility**: Eclipse Ditto 3.8+, Kotlin 2.0+, Java 21+

## Acknowledgments

- [Eclipse Foundation](https://www.eclipse.org/) for hosting the project
- [Eclipse Ditto](https://www.eclipse.org/ditto/) team for the excellent WoT API
- [Kotlin](https://kotlinlang.org/) team for the amazing language
- All contributors and users of this plugin
