# Java Developer Setup

This guide is for getting BankFlow building locally on Windows.

## Install Java 21

Recommended:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Close and reopen PowerShell, then verify:

```powershell
java -version
javac -version
```

Expected: Java 21.

On this machine, the JDK was found at:

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
```

## Install Maven

Recommended:

```powershell
winget install Apache.Maven
```

Close and reopen PowerShell, then verify:

```powershell
mvn -version
```

## Manual PATH Fix

If PowerShell still cannot find Java:

1. Open Windows search.
2. Search `Edit the system environment variables`.
3. Open `Environment Variables`.
4. Add or update:

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x
```

5. Add this to `Path`:

```text
%JAVA_HOME%\bin
```

If PowerShell still cannot find Maven, add Maven's `bin` folder to `Path`:

```text
C:\Program Files\Apache\Maven\apache-maven-3.9.x\bin
```

Open a new PowerShell window after changing environment variables.

## BankFlow Verification Commands

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
docker build --progress=plain -t bankflow-api:local .
docker compose up --build
```

Then check:

```powershell
curl http://localhost:8080/actuator/health/readiness
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Testcontainers Note

BankFlow's PostgreSQL integration tests use Testcontainers. If Docker is not reachable from the JVM, those integration tests skip automatically and unit tests still run. To force full integration coverage, make sure Docker Desktop is running and this works:

```powershell
docker ps
```

## Maven Wrapper

BankFlow includes Maven Wrapper pinned to Maven 3.9.9. Use:

```powershell
.\mvnw.cmd test
```

To regenerate the wrapper after Maven is available:

```powershell
mvn -N wrapper:wrapper "-Dmaven=3.9.9"
```
