# Micrometer Timer Sample Companion

Warning icon on a Micrometer `Timer.Sample sample = Timer.start(...)`
whose declaring method never calls `.stop(...)` on that sample
anywhere — Micrometer's own reference docs show the sample usage as
exactly two steps, start then stop, with no mention of a try/finally
guard. A sample that's started but never stopped never records its
timing, and per a real upstream Micrometer issue, can leak in
long-task-timer bookkeeping if this happens repeatedly.

## Why it exists

`Timer.Sample sample = Timer.start(registry);` compiles fine and
starts the clock — forget the matching `sample.stop(registry.timer(...))`
anywhere in the method (an early return, a refactor that dropped the
line, a copy-pasted method that never wired it up) and the timing for
that operation is silently never recorded, with no error, no warning,
nothing in the logs.

## Why built this way

- **100% static text/PSI analysis** — matches method/variable names by
  simple text, so it works whether the real Micrometer jar is on the
  classpath or not. Java and Kotlin.

## v0.1 scope — stated honestly, not exhaustively

Flags only the fully-unambiguous case: `.stop(...)` is never called on
the variable anywhere in the method, not even inside a branch. If
`.stop(...)` is called on any path (even one that doesn't cover every
branch, e.g. only inside an `if`), this is **not** flagged — true
control-flow/exception-path analysis is out of scope for a v0.1 static
scanner, and a false "still leaks on the exception path" positive is
worse than staying silent on partial coverage. Matches by simple
method name (`start`/`stop`), not real type resolution — an unrelated
`start()`/`stop()` pair on some other type named `sample` is a
possible (rare) false positive.

## Usage

Open any Java/Kotlin file using Micrometer. A `Timer.Sample` that's
started but never stopped anywhere in its method shows a warning icon.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
