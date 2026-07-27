![logoimage](https://raw.githubusercontent.com/DominoKit/DominoKit.github.io/master/logo/128.png)

<a href="https://github.com/DominoKit/domino-auto/actions?query=workflow:%22Deploy%22"><img src="https://github.com/DominoKit/domino-auto/workflows/Deploy/badge.svg" alt="Deploy"></a>
![Sonatype Nexus (Snapshots)](https://img.shields.io/badge/Snapshot-HEAD--SNAPSHOT-orange)
<a href="https://github.com/DominoKit/domino-auto/releases/"><img src="https://img.shields.io/github/release/DominoKit/domino-auto?include_prereleases=&amp;sort=semver&amp;color=14c398" alt="GitHub release"></a>
<a href="https://discord.gg/35UG3FhfHq"><img src="https://img.shields.io/badge/Discord-Join_chat-14c398?logo=discord&amp;logoColor=white" alt="Discord - Join chat"></a>
<a href="https://matrix.to/#/#DominoKit_domino:gitter.im"><img src="https://img.shields.io/badge/Element-Join_chat-14c398?logo=element&amp;logoColor=white" alt="Element - Join chat"></a>
<a href="#license"><img src="https://img.shields.io/badge/License-_Apache_2.0-14c398" alt="License"></a>
![GWT3/J2CL compatible](https://img.shields.io/badge/GWT3/J2CL-compatible-brightgreen.svg)

# Domino Auto

Domino Auto is a lightweight, annotation-processor-based service loader for GWT/J2CL applications. It scans
`META-INF/services` at compile time and generates a strongly typed loader class per service interface, avoiding
runtime reflection.

## Modules

- `domino-auto-api`: Provides the `@DominoAuto` annotation and GWT module.
- `domino-auto-processor`: Annotation processor that generates service loaders.

## How it works

1. You declare service interfaces and their implementations.
2. You register implementations via `META-INF/services/<service-interface-FQN>` (standard Java ServiceLoader files).
3. The annotation processor scans `META-INF/services` at compile time.
4. For each included service interface, it generates `[ServiceInterface]_ServiceLoader` with a `load()` method that
   returns a `List` of instantiated implementations.

The processor only generates loaders for services in the include list and not in the exclude list.

## Requirements

- Java 11 (see `maven.compiler.release` in `pom.xml`)
- Maven build
- Compatible with GWT3/J2CL

## Installation

Add the API dependency:

```xml
<dependency>
    <groupId>org.dominokit</groupId>
    <artifactId>domino-auto-api</artifactId>
    <version>[version]</version>
</dependency>
```

Add the processor dependency (provided scope) or configure it via annotationProcessorPaths:

```xml
<dependency>
    <groupId>org.dominokit</groupId>
    <artifactId>domino-auto-processor</artifactId>
    <version>[version]</version>
    <scope>provided</scope>
</dependency>
```

Or as a processor path in the compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.dominokit</groupId>
                <artifactId>domino-auto-processor</artifactId>
                <version>[version]</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Configuration

You must specify the packages of service interfaces to include. The include list applies to the service interface
package, not implementation packages.

### Compiler arguments

```xml
<compilerArgs>
    <arg>-AdominoAutoInclude=com.example.services,org.example.more</arg>
    <arg>-AdominoAutoExclude=com.example.services.internal</arg>
</compilerArgs>
```

The processor resolves `dominoAutoInclude` and `dominoAutoExclude` in this order:
- JVM system properties, for example `-DdominoAutoInclude=com.example.services`
- Environment variables, using either `dominoAutoInclude` / `dominoAutoExclude` or `DOMINO_AUTO_INCLUDE` / `DOMINO_AUTO_EXCLUDE`
- Annotation processor options, for example `-AdominoAutoInclude=...`

### Annotation on a type or package

```java
@DominoAuto(include = {"com.example.services"}, exclude = {"com.example.services.internal"})
package com.example.services;

import org.dominokit.auto.DominoAuto;
```

Notes:
- If no include list is provided, no loaders are generated.
- Exclude entries override includes by package prefix.

## Usage

### 1) Define a service interface

```java
package com.example.services;

public interface SampleService {
  void init();
}
```

### 2) Implement it

```java
package com.example.services;

public class FooSampleServiceImpl implements SampleService {
  public void init() {
    // Do something here
  }
}

package com.example.services;

public class BarSampleServiceImpl implements SampleService {
  public void init() {
    // Do something here
  }
}
```

### 3) Register implementations

Create `META-INF/services/com.example.services.SampleService` and list implementations:

```
com.example.services.FooSampleServiceImpl
com.example.services.BarSampleServiceImpl
```

Tip: You can also use `com.google.auto.service.AutoService` on implementations to generate the service files.

### 4) Use the generated loader

The processor generates `SampleService_ServiceLoader`:

```java
public class SampleService_ServiceLoader {
  public static List<SampleService> load() {
    List<SampleService> services = new ArrayList<>();
    services.add(new FooSampleServiceImpl());
    services.add(new BarSampleServiceImpl());
    return services;
  }
}
```

Usage:

```java
SampleService_ServiceLoader.load()
    .forEach(SampleService::init);
```

## Generated code details

- Naming: `[ServiceInterfaceSimpleName]_ServiceLoader`
- Package: same as the service interface
- API: `public static List<ServiceInterface> load()`

## Troubleshooting

- No generated loaders: confirm `dominoAutoInclude` is set or `@DominoAuto` is present on a type or package.
- Missing implementations: ensure `META-INF/services/<service-interface-FQN>` is on the compile classpath.
- IDE builds: configure annotation processor arguments in the IDE or delegate build to Maven.

## License

Apache License, Version 2.0. See `LICENSE`.
