# ge-mojo-inspection

This is an example Maven extension that demonstrates gathering information about a particular Mojo implementation and its classpath.  It adds a Gradle Enterprise build scan value for each project that contains the Mojo containing the files in the Mojo implementation classpath along with their associated MD5 hashes.

## Usage

First, install the extension in your local repository:

$ mvn install

Second, add the extension to the build you want to inspect in `.mvn/extensions.xml`:

```xml
<extensions>

    <extension>
        <groupId>com.gradle</groupId>
        <artifactId>ge-mojo-inspection</artifactId>
        <version>1.0-SNAPSHOT</version>
    </extension>

</extensions>
```
