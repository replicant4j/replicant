package org.realityforge.replicant.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Temporary Buildr-to-Bazel publication parity test.
 *
 * <p>This test is deleted with the Buildr baseline after the Bazel cutover is established.
 */
public final class ReleaseArtifactsIntegrationTest {
    private static final String BASELINE_VERSION = "6.999";
    private static final String LEGACY_CLIENT_NULLNESS_DEPENDENCY = """
            <dependency>
              <groupId>org.realityforge.javax.annotation</groupId>
              <artifactId>javax.annotation</artifactId>
              <version>1.1.1</version>
            </dependency>
        """;
    private static final String JSPECIFY_CLIENT_DEPENDENCY = """
            <dependency>
              <groupId>org.jspecify</groupId>
              <artifactId>jspecify</artifactId>
              <version>1.0.0</version>
            </dependency>
        """;
    private static final String JSPECIFY_SERVER_DEPENDENCY = """
            <dependency>
              <groupId>org.jspecify</groupId>
              <artifactId>jspecify</artifactId>
              <version>1.0.0</version>
              <scope>provided</scope>
              <exclusions>
                <exclusion>
                  <groupId>*</groupId>
                  <artifactId>*</artifactId>
                </exclusion>
              </exclusions>
            </dependency>
        """;

    private ReleaseArtifactsIntegrationTest() {}

    public static void main(final String[] args) throws Exception {
        if (args.length != 15) {
            throw new IllegalArgumentException(
                    "Expected version, eight Maven artifacts, and six Buildr parity fixtures");
        }
        final String version = read(resolve(Path.of(args[0]))).trim();

        assertParityJar(resolve(Path.of(args[1])), resolve(Path.of(args[9])));
        assertParityJar(resolve(Path.of(args[2])), resolve(Path.of(args[10])));
        assertJavadocJar(resolve(Path.of(args[3])));
        assertPom(resolve(Path.of(args[4])), expectedClientPom(resolve(Path.of(args[11])), version));

        assertParityJar(resolve(Path.of(args[5])), resolve(Path.of(args[12])));
        assertParityJar(resolve(Path.of(args[6])), resolve(Path.of(args[13])));
        assertJavadocJar(resolve(Path.of(args[7])));
        assertPom(resolve(Path.of(args[8])), expectedServerPom(resolve(Path.of(args[14])), version));
    }

    private static void assertParityJar(final Path actualPath, final Path baselineEntriesPath) throws IOException {
        final Set<String> expected = normalizedBaselineEntries(baselineEntriesPath);
        final Set<String> actual = jarEntries(actualPath);
        final Set<String> baselinePackages = codePackages(expected);
        for (final String entry : actual) {
            if (isPackageInfo(entry) && baselinePackages.contains(parent(entry))) {
                expected.add(entry);
            }
        }
        if (!actual.equals(expected)) {
            final Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            final Set<String> unexpected = new LinkedHashSet<>(actual);
            unexpected.removeAll(expected);
            throw new AssertionError("Unexpected archive parity for " + actualPath + "\nMissing: " + missing
                    + "\nUnexpected: " + unexpected);
        }
        assertManifest(actualPath);
        assertNoForbiddenEntries(actualPath, actual);
    }

    private static Set<String> normalizedBaselineEntries(final Path path) throws IOException {
        final Set<String> entries = new LinkedHashSet<>();
        for (final String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.endsWith("/") && !line.endsWith("/BUILD.bazel") && !"BUILD.bazel".equals(line)) {
                entries.add(line.startsWith("java/replicant/Arez_") ? line.substring("java/".length()) : line);
            }
        }
        entries.add("META-INF/MANIFEST.MF");
        // JDK 17 emitted an enum-switch helper that JDK 25 no longer needs when targeting Java 17.
        entries.remove("replicant/Converger$1.class");
        return entries;
    }

    private static Set<String> codePackages(final Set<String> entries) {
        final Set<String> packages = new LinkedHashSet<>();
        for (final String entry : entries) {
            if (entry.endsWith(".java") || entry.endsWith(".class")) {
                packages.add(parent(entry));
            }
        }
        return packages;
    }

    private static String parent(final String entry) {
        final int index = entry.lastIndexOf('/');
        return index < 0 ? "" : entry.substring(0, index);
    }

    private static boolean isPackageInfo(final String entry) {
        return entry.endsWith("/package-info.java") || entry.endsWith("/package-info.class");
    }

    private static Set<String> jarEntries(final Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            final Set<String> entries = new LinkedHashSet<>();
            Collections.list(jar.entries()).stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName())
                    .forEach(entries::add);
            return entries;
        }
    }

    private static void assertJavadocJar(final Path path) throws IOException {
        final Set<String> entries = jarEntries(path);
        if (!entries.contains("index.html")) {
            throw new AssertionError("Missing index.html from Javadoc jar: " + path);
        }
        assertNoForbiddenEntries(path, entries);
    }

    private static void assertManifest(final Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            final Attributes attributes = jar.getManifest().getMainAttributes();
            if (!"1.0".equals(attributes.getValue(Attributes.Name.MANIFEST_VERSION))) {
                throw new AssertionError("Unexpected manifest version in " + path);
            }
            if (attributes.size() != 1) {
                throw new AssertionError("Unexpected manifest attributes in " + path + ": " + attributes);
            }
        }
    }

    private static void assertNoForbiddenEntries(final Path path, final Set<String> entries) {
        for (final String entry : entries) {
            if (entry.endsWith("/BUILD.bazel") || "BUILD.bazel".equals(entry)) {
                throw new AssertionError("Published jar contains BUILD.bazel: " + path + ": " + entry);
            }
            if (entry.contains("/src/test/") || entry.endsWith("Test.class") || entry.endsWith("Test.java")) {
                throw new AssertionError("Published jar contains test content: " + path + ": " + entry);
            }
        }
    }

    private static String expectedClientPom(final Path baseline, final String version) throws IOException {
        final String pom = versionedBaselinePom(baseline, version);
        if (!pom.contains(LEGACY_CLIENT_NULLNESS_DEPENDENCY)) {
            throw new AssertionError("Client baseline POM does not contain the legacy nullness dependency");
        }
        return pom.replace(LEGACY_CLIENT_NULLNESS_DEPENDENCY, JSPECIFY_CLIENT_DEPENDENCY);
    }

    private static String expectedServerPom(final Path baseline, final String version) throws IOException {
        final String pom = versionedBaselinePom(baseline, version);
        final String dependencies = "  <dependencies>\n";
        if (!pom.contains(dependencies)) {
            throw new AssertionError("Server baseline POM does not contain a dependencies element");
        }
        return pom.replace(dependencies, dependencies + JSPECIFY_SERVER_DEPENDENCY);
    }

    private static String versionedBaselinePom(final Path baseline, final String version) throws IOException {
        return read(baseline)
                .replace("<version>" + BASELINE_VERSION + "</version>", "<version>" + version + "</version>");
    }

    private static void assertPom(final Path path, final String expected) throws IOException {
        final String actual = read(path);
        if (!actual.equals(expected)) {
            throw new AssertionError(
                    "Unexpected POM parity for " + path + "\nExpected:\n" + expected + "\nActual:\n" + actual);
        }
    }

    private static String read(final Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static Path resolve(final Path path) {
        if (Files.exists(path)) {
            return path.toAbsolutePath().normalize();
        }
        for (final String env : List.of("RUNFILES_DIR", "JAVA_RUNFILES", "TEST_SRCDIR")) {
            final String root = System.getenv(env);
            if (root != null) {
                final Path candidate = Path.of(root).resolve(path);
                if (Files.exists(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
                final Path mainCandidate = Path.of(root).resolve("_main").resolve(path);
                if (Files.exists(mainCandidate)) {
                    return mainCandidate.toAbsolutePath().normalize();
                }
            }
        }
        throw new IllegalArgumentException("File does not exist: " + path);
    }
}
