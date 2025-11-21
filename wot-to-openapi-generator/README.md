# Eclipse Ditto™ - WoT (Web of Things) Tooling :: WoT to OpenAPI Generator

[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.ditto/wot-to-openapi-generator?label=maven)](https://search.maven.org/search?q=g:org.eclipse.ditto%20AND%20a:wot-to-openapi-generator)


A Kotlin-based tool that converts WoT (Web of Things) Thing Models (JSON-LD format) into OpenAPI 3.1.0 specifications
describing Ditto HTTP endpoints.  
This generator enables seamless integration between IoT devices and REST APIs by automatically creating comprehensive
OpenAPI documentation from WoT schemas.

## Features

- **WoT to OpenAPI Conversion**: Converts Thing Models to OpenAPI 3.1.0 specifications describing Ditto HTTP endpoints

## Getting Started

### Prerequisites

- JDK 21 or higher
- Apache Maven 3.9.x or higher

### Installation

1. Clone the repository:

```bash
git clone https://github.com/eclipse-ditto/ditto-wot-tooling.git
cd ditto-wot-tooling/wot-to-openapi-generator
```

2. Build the project:

```bash
mvn clean install
```

### Usage

The generator can be used in several ways:

#### 1. From IDE (Recommended for Development)

Configure your IDE to run the `GeneratorStarter` class with the following arguments:

**Main Class:** `org.eclipse.ditto.wot.openapi.generator.GeneratorStarter`

**Program Arguments:** `<model-base-url> <model-name> <model-version> [ditto-base-url]`

**Example Configuration:**

```
Program Arguments: https://eclipse-ditto.github.io/ditto-examples/wot/models/ dimmable-colored-lamp 1.0.0 https://ditto.example.com
```

This configuration will load the WoT model from `https://eclipse-ditto.github.io/ditto-examples/wot/models/dimmable-colored-lamp-1.0.0.tm.jsonld` and generate
an OpenAPI specification for it, using `https://ditto.example.com` as the Ditto base URL.


#### 2. Command Line Interface

```bash
java -jar target/wot-to-openapi-generator-0-SNAPSHOT.jar <model-base-url> <model-name> <model-version> [ditto-base-url]
```

Parameters:

- `model-base-url`: Base URL for model loading
- `model-name`: Name of the WoT model (e.g., "dimmable-colored-lamp")
- `model-version`: Version of the WoT model (e.g., "1.0.0")
- `ditto-base-url` (optional): Base URL for the Ditto API (e.g., "https://ditto.example.com")

Example:

```bash
java -jar target/wot-to-openapi-generator-0-SNAPSHOT.jar https://eclipse-ditto.github.io/ditto-examples/wot/models/ dimmable-colored-lamp 1.0.0
```

#### 2. Quick Start with Example

1. **Build the project:**
```bash
mvn clean package
```

2. **Run with a sample WoT model:**
```bash
java -jar target/wot-to-openapi-generator-0-SNAPSHOT.jar https://eclipse-ditto.github.io/ditto-examples/wot/models/ dimmable-colored-lamp 1.0.0
```

3. **Check the generated output:**
The OpenAPI specifications will be generated in the `generated/` directory.

#### 3. Custom Configuration

The generator now accepts the Ditto base URL as a command-line parameter, making it easy to configure without modifying
code:

```bash
# Use your own Ditto deployment
java -jar target/wot-to-openapi-generator-0-SNAPSHOT.jar https://eclipse-ditto.github.io/ditto-examples/wot/models/ dimmable-colored-lamp 1.0.0 https://ditto.example.com
```

#### 4. Programmatic Usage

```kotlin
import org.eclipse.ditto.wot.openapi.generator.GeneratorStarter

fun main() {
    val args = arrayOf("https://eclipse-ditto.github.io/ditto-examples/wot/models/", "dimmable-colored-lamp", "1.0.0", "https://ditto.example.com")
    GeneratorStarter.run(args)
}
```

### Generated Output

The generator creates OpenAPI 3.1.0 YAML files in the `generated/` directory with the following structure:

- **Thing-level APIs**: Complete thing representation with all properties
- **Attribute APIs**: Individual attribute endpoints with proper schemas
- **Feature APIs**: Feature-specific endpoints with categorized properties
- **Action APIs**: Action invocation endpoints with input/output schemas
- **Enum Support**: Proper enum definitions for constrained values


### Troubleshooting

- **Java version error**: Ensure you have JDK 21+ installed
- **Maven build fails**: Check that Maven 3.9+ is installed
- **Model not found**: Verify the model name and version exist at the specified endpoint
- **Network issues**: Check if the WoT model endpoints are accessible


### Running with Different Models

To generate OpenAPI for different WoT models, simply change the program arguments:

**For Lamp Model:**

```
Program Arguments: https://your-models.com/repo lamp 0.2.0 https://ditto.mycompany.com 
```

**For Building Model:**

```
Program Arguments: https://your-models.com/repo building 1.0.0 https://ditto.mycompany.com 
```


**Note:** Replace `https://your-models.com/repo` with your actual WoT model endpoint URLs.  
E.g. if using the version `1.0.0` and model name `building`, the full URL would be `https://your-models.com/repo/building-1.0.0.tm.jsonld`.

## Architecture

The generator follows a modular architecture:

- **`GeneratorStarter`**: Main entry point and CLI handling
- **`WotLoader`**: Loads WoT models from URLs and manages generation
- **`OpenApiGenerator`**: Core generation interface
- **`AttributeSchemaResolver`**: Handles attribute schema generation
- **`FeatureSchemaResolver`**: Handles feature schema generation
- **`Utils`**: Common utilities for schema conversion


## WoT Model Requirements

Your WoT Thing models should:

- Be in JSON-LD format
- Follow the WoT specification
- Be accessible via HTTP/HTTPS endpoints
- Include proper schema definitions for properties, actions, and events

## Support

- **Issues**: Report bugs and feature requests
  on [GitHub Issues](https://github.com/eclipse-ditto/ditto-wot-tooling/issues)
- **Documentation**: Check the generated OpenAPI specs in the `generated/` directory
- **Community**: Join our discussions on GitHub
- **Eclipse Ditto**: Visit [eclipse.dev/ditto/](https://www.eclipse.dev/ditto/) for more information

## Acknowledgments

- Built on top of [Eclipse Ditto](https://www.eclipse.dev/ditto/) WoT libraries
- Uses [Swagger/OpenAPI](https://swagger.io/) for API specification generation
- Inspired by the Eclipse IoT ecosystem

