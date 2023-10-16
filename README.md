[![Build Status](https://jenkins.frengor.com/job/MappingsVersionResolver/badge/icon)](https://jenkins.frengor.com/job/MappingsVersionResolver/)
[![License](https://img.shields.io/badge/license-Apache--2.0-orange)](LICENSE)

# Mappings Version Resolver

Mappings Version Resolver is a maven plugin for resolving the mappings version used by the provided Spigot server (or fork).

It can be used to automatically have the correct mappings version inside the compiled plugin without having to manually update
it for every new minecraft version (or mapping change).

**Plugin documentation:** <https://frengor.com/maven-plugins/MappingsVersionResolver/>  
**Usage with maven:** <https://frengor.com/maven-plugins/MappingsVersionResolver/usage.html>  
**Example of automation with `templating-maven-plugin`:** <https://frengor.com/maven-plugins/MappingsVersionResolver/examples/example-templating.html>  

## Maven instructions

> Copied from [Usage](https://frengor.com/maven-plugins/MappingsVersionResolver/usage.html)

The plugin is published on the `fren_gor` repo:

```xml
<pluginRepositories>
    <pluginRepository>
        <id>fren_gor</id>
        <url>https://nexus.frengor.com/repository/public/</url>
    </pluginRepository>
</pluginRepositories>
```

The mappings version is resolved using the server artifact (or path to the server's file) provided in the `server` parameter.  
The resolved version is put into the property specified into the `outputProperty` parameter. If not specified, the default value is `resolvedMappingsVersion`.

```xml
<plugins>
    <plugin>
        <groupId>com.frengor</groupId>
        <artifactId>mappings-version-resolver-maven-plugin</artifactId>
        <version>1.0</version>
        <executions>
            <execution>
                <goals>
                    <goal>resolve-mappings-version</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <!-- Spigot artifact (or fork) -->
            <server>org.spigotmc:spigot:SPIGOT VERSION</server>

            <!-- Optional, the default value is resolvedMappingsVersion -->
            <outputProperty>resolvedMappingsVersion</outputProperty>
        </configuration>
    </plugin>
</plugins>
```

#### From the command line

The following command can be used to print the mappings version on the command line:

```shell
mvn com.frengor:mappings-version-resolver-maven-plugin:resolve-mappings-version -Dserver=org.spigotmc:spigot:SPIGOT-VERSION \
        help:evaluate -Dexpression=resolvedMappingsVersion -q -DforceStdout
```

#### Checking the mappings version

The string obtained using the Mappings Version Resolver plugin can be compared with the value returned by CraftMagicNumbers.getMappingsVersion():

```java
if (!CraftMagicNumbers.getMappingsVersion().equals(mappingsVersionString)) {
    throw new RuntimeException("Wrong mappings version found!");
}
```
