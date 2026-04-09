# libprune-maven-plugin

[![CI](https://github.com/IngoSchriek/libprune-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/IngoSchriek/libprune-maven-plugin/actions/workflows/ci.yml)
[![Maven](https://img.shields.io/badge/maven-plugin-blue.svg)](https://maven.apache.org/)
Maven plugin that iteratively restores deleted Java classes until compilation succeeds. The safety net for [cross-ref-scanner](https://github.com/IngoSchriek/cross-ref-scanner).

## The Problem

After removing unused classes from a shared library (e.g. with `cross-ref-scanner`), some deleted classes may still be needed internally for compilation. Manually figuring out which ones to restore is tedious -- each restored class may reveal new missing dependencies.

**libprune-maven-plugin** automates this: it runs `mvn compile`, parses `javac` errors, restores the missing classes from git, and repeats until the build passes.

## Installation

Add to your project's `pom.xml`:

```xml
<plugin>
    <groupId>io.github.ingoschriek</groupId>
    <artifactId>libprune-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</plugin>
```

Or install locally:

```bash
git clone https://github.com/IngoSchriek/libprune-maven-plugin.git
cd libprune-maven-plugin
mvn install
```

## Usage

```bash
# After deleting unused classes with cross-ref-scanner:
mvn libprune:recover
```

### Configuration

```xml
<plugin>
    <groupId>io.github.ingoschriek</groupId>
    <artifactId>libprune-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <maxIterations>30</maxIterations>        <!-- default: 30 -->
        <timeoutSeconds>300</timeoutSeconds>     <!-- default: 300 -->
        <compileArgs>                            <!-- extra args for mvn compile -->
            <arg>-T</arg>
            <arg>1C</arg>
        </compileArgs>
    </configuration>
</plugin>
```

Or via command line:

```bash
mvn libprune:recover -Dlibprune.maxIterations=50 -Dlibprune.timeoutSeconds=600
```

## How It Works

```
Iteration 1:
  mvn compile
    -> ERROR: symbol: class Foo, package com.example.util does not exist
    -> git restore Foo.java
    -> git restore com/example/util/*.java (all files in package)

Iteration 2:
  mvn compile
    -> ERROR: symbol: variable Bar (static reference to deleted class)
    -> git restore Bar.java

Iteration 3:
  mvn compile
    -> BUILD SUCCESS

Result: 3 classes restored, 47 remained deleted
```

### Supported Error Patterns

| javac Error | Action |
|---|---|
| `symbol: class Foo` | Restore `Foo.java` by class name |
| `symbol: variable Foo` | Restore `Foo.java` (static reference) |
| `symbol: static Foo` | Restore `Foo.java` |
| `package com.x.y does not exist` | Restore all deleted `.java` files in that package |

## Full Workflow

```
cross-ref-scanner (Python)            libprune-maven-plugin (Java)
        |                                       |
        |  1. Find unused classes               |
        |  2. Delete them (--delete)            |
        |                                       |
        +-------------------------------------->|
                                                |
                                   3. mvn libprune:recover
                                   4. Compile -> parse -> restore -> repeat
                                   5. Build passes with minimal set restored
```

## Architecture

```
io.github.ingoschriek.libprune
  |
  +-- RecoverMojo.java              Main @Mojo - orchestrates the loop
  |
  +-- compiler/
  |     CompileRunner.java           Runs mvn compile as subprocess
  |     CompileResult.java           Model: success + output
  |
  +-- parser/
  |     ErrorParser.java             Interface for parsing compiler errors
  |     JavacErrorParser.java        Javac implementation (regex-based)
  |     MissingSymbol.java           Model: name + kind (CLASS/PACKAGE)
  |
  +-- git/
        GitRestorer.java             Manages deleted files and git restore
```

## Development

```bash
git clone https://github.com/IngoSchriek/libprune-maven-plugin.git
cd libprune-maven-plugin
mvn clean verify
```

