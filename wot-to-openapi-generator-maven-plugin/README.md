# WoT to OpenAPI Generator

[![License](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)

A Kotlin-based tool that converts WoT (Web of Things) Thing Models (JSON-LD format) into OpenAPI 3.1.0 specifications. This generator enables seamless integration between IoT devices and REST APIs by automatically creating comprehensive OpenAPI documentation from WoT schemas.

## Features

- **WoT to OpenAPI Conversion**: Converts Thing Models to OpenAPI 3.1.0 specifications

## Getting Started

### Prerequisites

- JDK 21 or higher
- Apache Maven 3.9.x or higher

### Installation

1. Clone the repository:
```bash
git clone https://github.com/eclipse-ditto/ditto-wot-tooling.git
cd ditto-wot-tooling
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

**Program Arguments:** `<model-version> <model-name> <ditto-base-url> [base-url]`

**Example Configuration:**
```
Program Arguments: 0 smart-radiator-thermostat https://ditto.example.com https://your-models.com/smartheating
```

**Working Directory:** `/path/to/wot-to-openapi-generator`

#### 2. Command Line Interface

```bash
java -jar target/wot-to-openapi-generator-1.0.0.jar <model-version> <model-name> <ditto-base-url> [base-url]
```

Parameters:
- `model-version`: Version of the WoT model (e.g., "4.2.2")
- `model-name`: Name of the WoT model (e.g., "smart-radiator-thermostat")
- `ditto-base-url`: Base URL for the Ditto API (e.g., "https://ditto.example.com")
- `base-url` (optional): Custom base URL for model loading

Example:
```bash
java -jar target/wot-to-openapi-generator-1.0.0.jar 0 smart-radiator-thermostat dev https://your-models.com/smartheating
```

#### 2. Quick Start with Example

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run with a sample WoT model:**
   ```bash
   java -jar target/wot-to-openapi-generator-1.0.0.jar 0 smart-radiator-thermostat https://ditto.example.com https://your-models.com/smartheating
   ```

3. **Check the generated output:**
   The OpenAPI specifications will be generated in the `generated/` directory.



#### 3. Custom Configuration

The generator now accepts the Ditto base URL as a command-line parameter, making it easy to configure without modifying code:

```bash
# Use your own Ditto deployment
java -jar target/wot-to-openapi-generator-1.0.0.jar 1.0.0 my-thing https://ditto.mycompany.com

# Or configure via Maven plugin (recommended for production use)
```

#### 4. Programmatic Usage

```kotlin
import org.eclipse.ditto.wot.openapi.generator.GeneratorStarter

fun main() {
    val args = arrayOf("0.1.0", "lamp", "https://ditto.example.com", "https://your-models.com/things")
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

## How to Run

### Method 1: IDE Configuration (Recommended for Development)

1. **Open the project in your IDE** (IntelliJ IDEA, Eclipse, VS Code, etc.)

2. **Configure Run Configuration:**
   - **Main Class:** `org.eclipse.ditto.wot.openapi.generator.GeneratorStarter`
   - **Program Arguments:** `0 lamp dev https://your-models.com/things`
   - **Working Directory:** `/path/to/wot-to-openapi-generator`
   - **Java SDK:** Java 21

3. **Run the configuration** - the generator will execute and create OpenAPI files in the `generated/` directory

### Method 2: Command Line

#### Step-by-Step Instructions

1. **Prerequisites Check:**
   ```bash
   java -version  # Should show Java 21 or higher
   mvn -version   # Should show Maven 3.9.x or higher
   ```

2. **Clone and Build:**
   ```bash
   git clone https://github.com/eclipse-ditto/ditto-wot-tooling.git
   cd wot-to-openapi-generator
   mvn clean package
   ```

3. **Run the Generator:**
   ```bash
   # Basic usage
   java -jar target/wot-to-openapi-generator-1.0.0.jar <version> <model-name> <ditto-base-url>
   
   # Example with real parameters
   java -jar target/wot-to-openapi-generator-1.0.0.jar 0 smart-radiator-thermostat dev
   
   # With custom base URL
   java -jar target/wot-to-openapi-generator-1.0.0.jar 0 smart-radiator-thermostat dev https://your-models.com
   ```

4. **Check Output:**
   ```bash
   ls -la generated/
   # You should see generated OpenAPI YAML files
   ```

### Troubleshooting

- **Java version error**: Ensure you have JDK 21+ installed
- **Maven build fails**: Check that Maven 3.9+ is installed
- **Model not found**: Verify the model name and version exist at the specified endpoint
- **Network issues**: Check if the WoT model endpoints are accessible

### Customization

- **Environment URLs**: Edit `src/main/kotlin/org/eclipse/ditto/wot/openapi/generator/Environment.kt`
- **Ditto endpoints**: Update the `provideDittoUrl()` method in `WotLoader.kt`
- **Output format**: Modify the OpenAPI generation logic in the generator classes

### Running with Different Models

To generate OpenAPI for different WoT models, simply change the program arguments:

**For Room Model:**
```
Program Arguments: 0 room dev https://your-models.com/room
```

**For Building Model:**
```
Program Arguments: 0 building dev https://your-models.com/building
```

**For Custom Model:**
```
Program Arguments: <version> <model-name> <ditto-base-url> <base-url>
```

**Note:** Replace `https://your-models.com` with your actual WoT model endpoint URLs.



## Example
                "TOILET",
                "KITCHEN",
                "LIVING_ROOM",
                "BEDROOM",
                "OFFICE"
              ]
            }
          }
        }
      }
    }
  }
}
```

### Generated OpenAPI Schema
```yaml
components:
  schemas:
    attribute_location_room_type:
      type: string
      enum:
        - HALLWAY_AND_STAIRCASE
        - CORRIDOR
        - BATHROOM
        - TOILET
        - KITCHEN
        - LIVING_ROOM
        - BEDROOM
        - OFFICE
      description: The usage type of the room
      title: Room Usage Type
```

## Architecture

The generator follows a modular architecture:

- **`GeneratorStarter`**: Main entry point and CLI handling
- **`WotLoader`**: Loads WoT models from URLs and manages generation
- **`OpenApiGenerator`**: Core generation interface
- **`AttributeSchemaResolver`**: Handles attribute schema generation
- **`FeatureSchemaResolver`**: Handles feature schema generation
- **`Utils`**: Common utilities for schema conversion

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on how to submit pull requests, report issues, and contribute to the project.

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the Eclipse Public License 2.0 - see the [LICENSE](LICENSE) file for details.

## Configuration

### Configuration

The generator now accepts the Ditto base URL as a command-line parameter, making it easy to configure without modifying code:

```bash
# Basic usage with your Ditto deployment
java -jar target/wot-to-openapi-generator-1.0.0.jar 1.0.0 my-thing https://ditto.mycompany.com

# With custom model base URL
java -jar target/wot-to-openapi-generator-1.0.0.jar 1.0.0 my-thing https://ditto.mycompany.com https://models.mycompany.com
```

### Maven Plugin Configuration (Recommended)

For production use, configure the Ditto URL via Maven plugin configuration rather than command-line parameters:

```xml
<plugin>
    <groupId>org.eclipse.ditto</groupId>
    <artifactId>wot-to-openapi-generator</artifactId>
    <version>1.0.0</version>
    <configuration>
        <dittoBaseUrl>https://ditto.mycompany.com</dittoBaseUrl>
        <modelVersion>1.0.0</modelVersion>
        <modelName>my-thing</modelName>
    </configuration>
</plugin>
```

### WoT Model Requirements

Your WoT Thing models should:
- Be in JSON-LD format
- Follow the WoT specification
- Be accessible via HTTP/HTTPS endpoints
- Include proper schema definitions for properties, actions, and events

## Support

- **Issues**: Report bugs and feature requests on [GitHub Issues](https://github.com/eclipse-ditto/ditto-wot-tooling/issues)
- **Documentation**: Check the generated OpenAPI specs in the `generated/` directory
- **Community**: Join our discussions on GitHub
- **Eclipse Ditto**: Visit [eclipse-ditto.github.io](https://eclipse-ditto.github.io/) for more information

## Acknowledgments

- Built on top of [Eclipse Ditto](https://www.eclipse.dev/ditto/) WoT libraries
- Uses [Swagger/OpenAPI](https://swagger.io/) for API specification generation
- Inspired by the Eclipse IoT ecosystem

