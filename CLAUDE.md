# hosh project guidance

## Testing

### Property-Based Testing (PBT)

This project uses [jqwik](https://jqwik.net/) for property-based tests.

**Known issue:** jqwik 1.9.3 targets JUnit Platform 1.x but the project uses JUnit 6 (Platform 6.x). `@Property` tests compile and are structurally correct but the jqwik engine does not execute them at runtime. Track https://github.com/jqwik-team/jqwik/issues for jqwik 2.x release targeting JUnit 6.

**Writing property tests:** use `@Property` + `@ForAll` for parameters, `@Provide` for custom arbitraries, `Assume.that(...)` for preconditions. Follow the existing patterns in `ValuesTest.java` (spi module).

**Key properties to test:**
- Comparator contract: reflexivity, antisymmetry, transitivity
- Merge commutativity: `a.merge(b) == b.merge(a)`
- Sort idempotence: `sort(sort(xs)) == sort(xs)`
- Partition: `take(n, xs) + drop(n, xs) == xs`

**Architecture rule:** `UnitTestsFitnessTest` allows `@Property` and `@Provide` annotations alongside the standard JUnit annotations (`@Test`, `@BeforeEach`, `@AfterEach`, `@ParameterizedTest`).
