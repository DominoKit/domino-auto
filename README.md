![logoimage](https://raw.githubusercontent.com/DominoKit/DominoKit.github.io/master/logo/128.png)

<a href="https://github.com/DominoKit/domino-auto/actions?query=workflow:%22Deploy%22"><img src="https://github.com/DominoKit/domino-auto/workflows/Deploy/badge.svg" alt="Deploy"></a>
![Sonatype Nexus (Snapshots)](https://img.shields.io/badge/Snapshot-HEAD--SNAPSHOT-orange)
<a href="https://github.com/DominoKit/domino-auto/releases/"><img src="https://img.shields.io/github/release/DominoKit/domino-auto?include_prereleases=&amp;sort=semver&amp;color=14c398" alt="GitHub release"></a>
<a href="https://discord.gg/35UG3FhfHq"><img src="https://img.shields.io/badge/Discord-Join_chat-14c398?logo=discord&amp;logoColor=white" alt="Discord - Join chat"></a>
<a href="https://matrix.to/#/#DominoKit_domino:gitter.im"><img src="https://img.shields.io/badge/Element-Join_chat-14c398?logo=element&amp;logoColor=white" alt="Element - Join chat"></a>
<a href="#license"><img src="https://img.shields.io/badge/License-_Apache_2.0-14c398" alt="License"></a>
![GWT3/J2CL compatible](https://img.shields.io/badge/GWT3/J2CL-compatible-brightgreen.svg)

## Domino-auto
=====
Domino-auto is a simple and lightweight service loader for GWT/J2CL application that uses annotation processor to create
a loader class that load instances of a specific service class.

#### Dependencies

- The API dependency

```xml

<dependency>
    <groupId>org.dominokit</groupId>
    <artifactId>domino-auto-api</artifactId>
    <version>[version]</version>
</dependency>
```

- The Processor dependency

```xml

<dependency>
    <groupId>org.dominokit</groupId>
    <artifactId>domino-auto-processor</artifactId>
    <version>[version]</version>
    <scope>provided</scope>
</dependency>
```

Or as s processor path in the compiler plugin :

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <annotationProcessorPaths>
            <!-- path to your annotation processor -->
            <path>
                <groupId>org.dominokit</groupId>
                <artifactId>domino-auto-processor</artifactId>
                <version>[version]</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>

```

### Usage

Adding the dependencies should be all you need to start using the tool; it will automatically generate service loaders
for all services defined in the classpath.

The generated service loader class name will follow the convention `[Service name]_ServiceLoader`, and will provide a
single method `load` that returns a list of that service implementations.

The user needs to specify an include list of packages that will be included in the generation, the provided list
represent the package of the implemented service class not the implementations. the include list can be configured using a
compiler argument `dominoAutoInclude` or using `@DominoAuto` annotation on a type or package-info.
### Example

- Make sure the service package is added to the include parameter of the annotation :

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <annotationProcessorPaths>
            <!-- path to your annotation processor -->
            <path>
                <groupId>org.dominokit</groupId>
                <artifactId>domino-auto-processor</artifactId>
                <version>[version]</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-AdominoAutoInclude=com.dominokit.samples</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

> Using this method, make sure in case you are building from the ide to setup the compiler arguments in the ide or make
> the ide delegate the build to maven

or

```java
@DominoAuto(include = {"com.dominokit.samples"})
package com.dominokit.samples;

import org.dominokit.auto.DominoAuto;
```

Lets define the desired service interface

```java
package com.dominokit.samples;

public interface SampleService {
  void init();
}
```

The lets have different implementations

```java
package com.dominokit.samples;

public class FooSampleServiceImpl implements SampleService {
  public void init(){
    //Do something here
  }
}

//-------------

package com.dominokit.samples;

public class BarSampleServiceImpl implements SampleService {
  public void init(){
    //Do something here
  }
}
```
The project `META-INF` we register both implementations as services :

Create a file named `com.dominokit.samples.SampleService` under the following path
`project root folder -> src -> main -> resources -> META-INF -> services`

In the file list the services with the full qualified class name

```java
com.dominokit.samples.FooSampleServiceImpl
com.dominokit.samples.BarSampleServiceImpl
```

This will generate the following code :

```java
public class SampleServiceAuto_ServiceLoader {
  public static List<SampleService> load() {
    List<SampleService> services = new ArrayList();
    services.add(new FooSampleServiceImpl());
    services.add(new BarSampleServiceImpl());
    return services;
  }
}
```

Then we can use it like this :

```java
SampleService_ServiceLoader.load()
              .forEach(SampleService::init);
```









