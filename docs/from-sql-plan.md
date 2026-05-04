# Plan: `from-sql` command (JDBC reader)

## Core constraint: JDBC drivers cannot be in the fat jar

Fat jar is currently 1.7 MB. Bundling even one driver (PostgreSQL ~1.1 MB, H2 ~2.5 MB) bloats it.
More importantly: users need different drivers, and MySQL Connector/J is GPL — cannot redistribute in MIT project.

## Driver distribution: lazy download from Maven Central

On first use, `from-sql` parses the URL prefix, looks up Maven coordinates in a built-in registry,
downloads the JAR from Maven Central, verifies the SHA-1 checksum, and caches it in `~/.hosh/jdbc/`.
Subsequent runs use the cached JAR — no download.

Flow:
```
from-sql "jdbc:mysql://localhost/db?user=root&password=x" "SELECT * FROM t"
  → parse prefix "jdbc:mysql:"
  → check ~/.hosh/jdbc/ for mysql driver JAR
  → not found → look up registry → com.mysql:mysql-connector-j:9.3.0
  → GET https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.3.0/mysql-connector-j-9.3.0.jar
  → verify SHA-1
  → save to ~/.hosh/jdbc/mysql-connector-j-9.3.0.jar
  → load driver, connect, run query
  → next run: JAR cached, skip download
```

Download uses `java.net.http.HttpClient` (JDK 11+) — no new prod dependencies.

**Override:** check `State.getVariables()` for `JDBC_DRIVERS_DIR` to use a non-default cache dir.

**Version override:** hosh variable `JDBC_DRIVER_MYSQL=com.mysql:mysql-connector-j:8.0.33` pins a
specific version instead of the registry default.

**Manual JAR:** if a matching JAR already exists in `~/.hosh/jdbc/`, skip download entirely.

**Unknown prefix:** error — "unknown JDBC URL prefix; place driver JAR in ~/.hosh/jdbc/"

### Built-in registry

| URL prefix | Maven coordinates |
|---|---|
| `jdbc:postgresql:` | `org.postgresql:postgresql:42.7.3` |
| `jdbc:mysql:` | `com.mysql:mysql-connector-j:9.3.0` |
| `jdbc:mariadb:` | `org.mariadb.jdbc:mariadb-java-client:3.5.2` |
| `jdbc:sqlite:` | `org.xerial:sqlite-jdbc:3.45.3.0` |
| `jdbc:h2:` | `com.h2database:h2:2.3.232` |
| `jdbc:sqlserver:` | `com.microsoft.sqlserver:mssql-jdbc:12.10.0.jre11` |
| `jdbc:oracle:thin:` | `com.oracle.database.jdbc:ojdbc11:23.7.0.25.01` |

Oracle note: ojdbc is on Maven Central today; was historically behind a paywall.

## Multiple versions in cache

If `~/.hosh/jdbc/` contains multiple JARs matching the same vendor prefix (e.g. both
`mysql-connector-j-8.0.33.jar` and `mysql-connector-j-9.3.0.jar`), two options are under
consideration:

### Option A: Fail with helpful error (preferred)

```
error: multiple MySQL drivers found in ~/.hosh/jdbc/:
  mysql-connector-j-8.0.33.jar
  mysql-connector-j-9.3.0.jar
set JDBC_DRIVER_MYSQL=com.mysql:mysql-connector-j:9.3.0 to pin a version
```

Forces the user to decide explicitly. Avoids silent surprises — schema differences, SSL behavior,
and protocol changes between driver major versions make silent version selection dangerous.

### Option B: Use latest by version sort

Scan for matching JARs, pick highest version via semver comparison. Simple, but the user may not
know which version is actually running.

Option A is preferred for v1. Option B can be reconsidered if the explicit error proves too noisy
in practice.

## Why not `DriverManager.getConnection()`

`DriverManager` checks that the driver was loaded by the calling classloader or an ancestor.
A `URLClassLoader` child fails this check. Must use `driver.connect(url, props)` directly —
the JDBC spec allows this and bypasses the classloader restriction.

## Command API

```
from-sql <jdbc-url> <sql-query>
```

Examples:

```
from-sql "jdbc:postgresql://localhost/mydb?user=alice&password=s3cr3t" "SELECT id, name FROM users"
from-sql "jdbc:sqlite:/tmp/data.db" "SELECT * FROM log WHERE ts > '2026-01-01'"
from-sql "jdbc:h2:mem:test" "SELECT 1 AS n"
```

Credentials live in the URL — consistent with every JDBC CLI tool. Users use hosh variables to
avoid plaintext in scripts:

```
from-sql $DB_URL "SELECT * FROM users"
```

## Module: `modules/jdbc`

**Prod deps:** zero — only JDK JDBC (`java.sql`) and `java.net.http` for downloads.

**Test deps:** H2 database (`com.h2database:h2`, EPL-2.0/MPL-2.0) as `test` scope. Runs fully
in-memory — no external infrastructure needed for tests.

`module-info.java`:

```java
module hosh.modules.jdbc {
    requires hosh.spi;
    requires java.sql;
    requires java.net.http;
}
```

## Driver loading sequence in `run()`

```
1. resolve driversDir:
   - check state.getVariables().get(VariableName.of("JDBC_DRIVERS_DIR"))
   - fallback: Path.of(System.getProperty("user.home"), ".hosh", "jdbc")
2. parse URL prefix → look up registry
3. if unknown prefix → err "unknown JDBC URL prefix; place driver JAR in <driversDir>"
4. scan driversDir for JARs matching vendor
5. if multiple found → err with list + pin instruction (Option A)
6. if none found → download from Maven Central + verify SHA-1 → save to driversDir
7. URLClassLoader driverLoader = new URLClassLoader(new URL[]{jarPath}, currentClassLoader)
8. ServiceLoader.load(Driver.class, driverLoader) → find driver accepting url
9. driver.connect(url, new Properties()) → Connection
10. conn.prepareStatement(query).executeQuery() → ResultSet
11. ResultSetMetaData → Keys, rows → Records
```

## ResultSet → Records type mapping

| SQL type | hosh Value |
|---|---|
| `VARCHAR`, `CHAR`, `CLOB` | `Values.ofText()` |
| `INTEGER`, `SMALLINT`, `TINYINT` | `Values.ofNumeric()` |
| `BIGINT` | `Values.ofNumeric()` |
| `FLOAT`, `REAL`, `DOUBLE` | `Values.ofText(Double.toString())` |
| `NUMERIC`, `DECIMAL` | `Values.ofText(bd.toPlainString())` |
| `BOOLEAN`, `BIT` | `Values.ofText("true"/"false")` |
| `DATE`, `TIMESTAMP` | `Values.ofInstant()` |
| `BINARY`, `VARBINARY`, `BLOB` | `Values.ofBytes()` |
| `NULL` / `rs.wasNull()` | `Values.none()` |

## Tests

**Unit tests** (Mockito, no DB): missing args, too many args, unknown URL prefix, multiple JARs
in cache, download failure (mocked HTTP).

**Integration tests** (H2 in test scope): full round-trip — create in-memory H2 DB, insert rows,
`from-sql` reads them, verify Records match. Tests all type mappings.

## Open questions

1. **`to-sql`?** Insert records pipeline into a table. Needs `PreparedStatement` with dynamic
   column binding. Plan for later.

2. **Connection per query vs pool?** `from-sql` is a one-shot CLI command. Single connection,
   open/close per invocation is correct.

3. **Credentials security:** URL approach means credentials appear in `ps aux` and hosh history.
   Future: support password from a hosh variable or `~/.hosh/jdbc.properties`. Out of scope for v1.

4. **Streaming vs collect-all?** `ResultSet.next()` is lazy — stream directly to `out.send()`
   without buffering. Correct for large result sets.
