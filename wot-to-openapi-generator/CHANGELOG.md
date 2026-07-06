# Changelog

All notable changes to the **WoT to OpenAPI generator** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-07-06

### Added

- Const value propagation for WoT-to-OpenAPI schema conversion: `const` values from the Thing Model
  are now carried over into the generated OpenAPI schemas.
- Reusable Ditto query parameters for live channel usage.

### Changed

- The Ditto error schema is now emitted only once and moved to the end of the schema list.
- Action output schema generation uses the action input as the naming context (`actioninput`
  instead of `actionoutput`), with a display-name fallback for feature and thing action path generation.
- Updated to Eclipse Ditto 3.9.3. As a consequence of Ditto 3.9.x compiling `ditto-wot-api` to Java 25
  bytecode, the build and runtime now require **JDK 25**. Kotlin 2.4.0, Ktor 3.5.1, kotlinx-coroutines 1.11.0,
  Swagger 2.2.52, Caffeine 3.2.4 and SLF4J 2.0.18.

### Fixed

- BigDecimal precision when wrapping `double` values is now preserved across all conversion sites.
- Generation no longer fails when a property description is missing.
- Stacktraces are no longer printed twice on generation errors.

## [1.1.0] - 2026-03-24

### Added

- OpenID Connect security scheme support.
- `deprecationNotice` support: WoT deprecation metadata is reflected in the generated OpenAPI spec.
- Deprecation support for `tm:submodel` links.

## [1.0.0] - 2026-01-22

### Added

- Initial release: generate OpenAPI 3.1.0 specifications describing Eclipse Ditto HTTP endpoints
  from WoT (Web of Things) Thing Models (JSON-LD).
- CLI entry point (`GeneratorStarter`) and library API (`OpenApiGenerator`).
- Thing-level and feature-level paths, actions and schemas, plus reusable OpenAPI components
  (responses, parameters, errors).
- WoT model fetching over HTTP via `DittoBasedWotLoader` / `ToolJsonDownloader`.
