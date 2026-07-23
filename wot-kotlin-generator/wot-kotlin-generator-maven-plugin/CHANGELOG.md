# Changelog

All notable changes to the **WoT Kotlin Generator Maven plugin** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.2] - 2026-07-23

### Fixed

- Generated code now preserves the exact casing of WoT object property keys when
  (de)serializing. Action input/output data classes derived the Kotlin property name
  directly from the model key, so an UPPERCASE key such as `MONDAY` became a Kotlin
  property literally named `MONDAY`. Jackson's getter-name mangling turns `getMONDAY()`
  into the property name `monday`, which overrode the `@JsonProperty("MONDAY")` and
  produced the wrong (or duplicated) JSON key and broke deserialization. Identifiers are
  now generated as lower-camel-case via `asPropertyName`, while `@JsonProperty` continues
  to pin the wire name to the verbatim model key — so the emitted JSON is unchanged for
  already-valid keys and corrected for keys that previously mis-serialized.
- `ExplicitNullAwareSerializer` no longer lower-cases property names when a sibling DSL
  property is explicitly set to `null`. Its hand-rolled object writer now resolves the
  `@JsonProperty` name for every field it writes, matching Jackson's default serializer.

Note: generated Kotlin properties for non-lower-camel-case keys are renamed (e.g.
`input.MONDAY` becomes `input.monday`). This is a source-level change caught at compile
time on regeneration; the on-wire JSON is unaffected.

## [1.2.1] - 2026-07-16

### Fixed

- Annotation generation for Kotlin 2.4.0: `@JsonSetter` is now pinned to the constructor
  parameter use-site (`PARAM`). Kotlin 2.4.0 changed the default annotation use-site target to
  `param-property`, which additionally placed `@JsonSetter` on the private backing field. For
  `is`-prefixed booleans this caused Jackson to see two fields mapped to the same JSON name and
  fail with "Multiple fields representing property ...".

## [1.2.0] - 2026-07-06

### Added

- `deduplicateReferencedTypes` configuration option: referenced types that are structurally identical
  are generated once and reused instead of being duplicated per usage site.
- WoT metadata (titles, descriptions, etc.) is now emitted as KDoc on the generated Kotlin code.

### Changed

- Updated to Eclipse Ditto 3.9.3. Because Ditto 3.9.x compiles `ditto-wot-api` to Java 25 bytecode,
  the plugin now requires **JDK 25** — both to build it and to run it during a consumer's
  `generate-sources` phase. Also bumped Kotlin 2.4.0, Ktor 3.5.1, kotlinx-coroutines 1.11.0
  (aligned with Ktor 3.5.1), KotlinPoet 2.3.0 and SLF4J 2.0.18.
- Bumped `maven-plugin-plugin` to 3.15.2 so the mojo-descriptor scanner (ASM 9.9) can read the
  Java 25 bytecode of `ditto-wot-api`.

## [1.1.0] - 2026-03-24

### Added

- Generation of `DEFAULT_*` constants from WoT Thing Model default values.
- Submodel-only generation mode.
- `deprecationNotice` support: WoT deprecation metadata is reflected in the generated Kotlin code.
- Deprecation support for `tm:submodel` links (`@Deprecated` on the generated feature class and DSL).

### Fixed

- Enum type resolution now respects the configured `EnumGenerationStrategy`.
- Correct enum generation for action input/output properties.

## [1.0.0] - 2026-01-22

### Added

- Initial release: Maven plugin (`codegen` goal, bound to `generate-sources`) that generates Kotlin
  source from WoT (Web of Things) Thing Models (JSON-LD).
- Generated artifacts: data classes, fluent DSL builders, enums and type-safe path builders for
  Ditto RQL queries.
- Pluggable strategies for enum generation (`INLINE` / `SEPARATE_CLASS`) and class naming
  (`COMPOUND_ALL` / `ORIGINAL_THEN_COMPOUND`) via `GeneratorConfiguration`.
- Kotlin source emitted with KotlinPoet; runtime base types provided by the
  `wot-kotlin-generator-common-models` module.
