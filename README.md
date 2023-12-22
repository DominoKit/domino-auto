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

<annotationProcessorPaths>
    <path>
        <groupId>org.dominokit</groupId>
        <artifactId>domino-auto-processor</artifactId>
        <version>[version]</version>
    </path>
</annotationProcessorPaths>
```

### Usage

For a service to be utilized by this tool, it must adhere to the `DominoAutoService` marker interface and possess a
default constructor without arguments. Additionally, these services need to be declared as a `DominoAutoService` service
within the project's `META-INF`. When a service meets these criteria, you can use the `@DominoAuto` annotation on a
class or interface to specify the desired service interface. The processor then automatically generates a class that
aggregates all instances of the specified service.

### Example

Lets define the desired service interface

```java
public interface SampleService extends DominoAutoService {
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

Create a file named `org.dominokit.auto.DominoAutoService` under the following path
`project root folder -> src -> main -> resources -> META-INF -> services`

In the file list the services with the full qualified class name

```java
com.dominokit.samples.FooSampleServiceImpl
com.dominokit.samples.BarSampleServiceImpl
```

Now in the client module from where you need to load the services, you create a class or interface and annotate it with `@DominoAuto`

```java
@DominoAuto(SampleService.class)
public interface SampleServiceAuto {
}
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
SampleServiceAuto_ServiceLoader.load()
              .forEach(SampleService::init);
```









