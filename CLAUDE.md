# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring AI Extensions is a modular extension of Spring AI that provides integrations with Alibaba Cloud services and 40+ external tools/platforms. The project builds on Spring AI concepts like `ChatModel`, `ToolCallback`, `VectorStore`, `ChatMemory`, `DocumentReader`, etc.

**Key Technologies:**
- Spring Boot 4.1.x, Spring AI 2.0.x
- Maven multi-module project (JDK 17+)
- DashScope SDK for Alibaba Cloud AI models

## Spring AI 2.0 Baseline

This repository is being upgraded against the Spring AI 2.0 line. The root `pom.xml` is the source of truth for the active baseline:

- `spring-ai.version`: Spring AI 2.0 snapshot or milestone used by this branch
- `spring-boot.version`: Spring Boot 4.x baseline
- `mcp.sdk.version`: MCP Java SDK 2.x baseline
- `revision`: extension module version for the current upgrade branch

The repository declares the Spring AI 2.0 snapshot/milestone repositories in `pom.xml`. Do not commit personal Maven settings, absolute local repository paths, or machine-specific `.mvn/maven.config` files. If dependency resolution fails because a local or corporate Maven mirror uses `mirrorOf=*`, fix that in the user's Maven `settings.xml` by excluding `spring-snapshots`, `central-portal-snapshots`, and `spring-milestones` from the mirror.

## Build Commands

This project uses Maven Daemon (mvnd) for faster builds. Common commands via `make`:

```bash
# Build the entire project (skip tests)
make build
# Equivalent: mvnd -B package -DskipTests=true

# Run all tests
make test
# Equivalent: mvnd test

# Run a single test class
mvnd test -Dtest=ClassName

# Format code (Spring Java Format)
make format-fix
# Equivalent: mvnd spring-javaformat:apply

# Check code format
make format-check

# Apply Spotless formatting
make spotless-apply

# Run Checkstyle
make checkstyle-check

# Install CI tools (markdownlint, license-eye, codespell, etc.)
make tools

# Run all linters
make lint
```

## Module Architecture

The project follows a **two-tier pattern** for most modules:
1. **Implementation module** - Contains the actual code
2. **Auto-configuration module** - Contains `@AutoConfiguration` classes
3. **Spring Boot Starter** - Thin module that wires implementation + auto-config

### Module Categories

- **`models/dashscope`** - DashScope AI model implementations (Chat, Image, Audio, Embedding)
- **`tool-calls/`** - 40+ tool integrations (search, translation, maps, weather, etc.)
- **`memory/`** - ChatMemory implementations (Redis, MongoDB, JDBC, Memcached, etc.)
- **`vector-stores/`** - VectorStore implementations (AnalyticDB, OceanBase, TableStore, etc.)
- **`document-readers/`** - DocumentReader implementations (GitHub, GitLab, Notion, etc.)
- **`document-parsers/`** - DocumentParser implementations (PDF, Markdown, Tika, etc.)
- **`mcp/`** - Model Context Protocol (common, registry, router, distributed, gateway)
- **`rag/`** - RAG components (Hybrid search, HyDE)
- **`auto-configurations/`** - Auto-configuration classes
- **`spring-boot-starters/`** - Spring Boot starter modules
- **`observation/`** - ARMS observability integration

### Key Pattern: Tool Calling Modules

When adding a new tool calling module, follow the conventions in `tool-calls/README.md`:

1. **Naming**: `spring-ai-alibaba-starter-tool-calling-${name}`
2. **Package**: `com.alibaba.cloud.ai.toolcalling.${name}`
3. **Classes**:
   - `${name}AutoConfiguration` - Auto-configuration with `@Configuration`
   - `${name}Properties` - Extends `CommonToolCallProperties`
   - `${name}Service` - Tool function implementation
   - `${name}Constants` - Constants including `TOOL_NAME`
4. **Dependencies**: Use `JsonParseTool`, `RestClientTool`, or `WebClientTool` from `spring-ai-alibaba-starter-tool-calling-common`
5. **Bean Declaration**: Use `@Bean(name = ${Constants.TOOL_NAME})` with `@Description` annotation

Example from WeatherService:
```java
@Configuration
@ConditionalOnClass(WeatherService.class)
@EnableConfigurationProperties(WeatherProperties.class)
@ConditionalOnProperty(prefix = WeatherConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class WeatherAutoConfiguration {
    @Bean(name = WeatherConstants.TOOL_NAME)
    @ConditionalOnMissingBean
    @Description("Use api.weather to get weather information.")
    public WeatherService getWeatherService(WeatherProperties properties, JsonParseTool jsonParseTool) {
        return new WeatherService(WebClientTool.builder(jsonParseTool, properties).build(), jsonParseTool);
    }
}
```

## Testing Guidelines

- Each module should have unit tests in `src/test/java`
- Tests requiring API keys should use: `@EnabledIfEnvironmentVariable(named = XXXConstants.API_KEY_ENV, matches = ".+")`
- Run single module tests: `mvnd test -pl module-path`

## Code Style

- **Indentation**: 4 spaces (Java), 2 spaces (XML/YAML/JSON)
- **Line length**: 120 characters max
- **Braces**: Always use braces for control statements (`if`, `for`)
- **Imports**: Use wildcard imports sparingly
- **Apache License header**: Required on all Java files (enforced by license-eye)
- **Spotless**: Applies automatically during build, removes unused imports

## Adding a New Module

1. Add module to root `pom.xml` `<modules>` section
2. Create module directory with standard Maven structure
3. Parent POM: `<parent>` references root `spring-ai-alibaba-extensions`
4. Follow naming conventions for your module type
5. Write unit tests
6. Run `make build` and `make test`

## Important Files

- `pom.xml` - Root POM with all modules and dependency management
- `Makefile` - Wrapper for tools/make/*.mk build targets
- `tools/make/` - Makefile fragments for build/test/lint
- `.editorconfig` - Editor configuration (4 spaces, 120 char line length)
- `spring-ai-alibaba-extensions-bom/` - Bill of Materials for dependency management
