# Java Conventions

- Every maintained Java package must have a `package-info.java` marked with JSpecify `@NullMarked`. Use JSpecify
  nullness annotations unless an adjacent comment documents a processor compatibility workaround.
- Prefer `final` for bindings that are not reassigned. Use explicit types rather than `var` in client/shared
  production code and tests.
- Treat values whose declared type is in `replicant.messages` as JavaScript-native interop objects. Use a Java
  `assert`, rather than `Objects.requireNonNull`, before directly assigning, returning, or otherwise using such a
  value. Keep `Objects.requireNonNull` out of the `replicant.messages` package.
- Mark JVM-only client code with the package-local `replicant.GwtIncompatible` annotation, or
  `replicant.messages.GwtIncompatible` inside the messages package.
- Limit `javax.annotation` in maintained source to Java EE annotations and documented processor compatibility
  workarounds.
- Update Javadoc when changing a public API.
