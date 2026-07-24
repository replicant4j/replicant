package org.realityforge.replicant.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class JarBuilderTest {
    private JarBuilderTest() {}

    public static void main(final String[] args) throws Exception {
        final Path root = Files.createTempDirectory("jar-builder-test");
        try {
            testSkipsModuleDescriptors(root);
            testDuplicateRealEntryFails(root);
        } finally {
            deleteTree(root);
        }
    }

    private static void testSkipsModuleDescriptors(final Path root) throws Exception {
        final Path directory = Files.createDirectories(root.resolve("module-descriptors"));
        final Path first = directory.resolve("first.jar");
        final Path second = directory.resolve("second.jar");
        final Path output = directory.resolve("output.jar");

        writeJar(
                first,
                entries(
                        "module-info.class",
                        "first module",
                        "META-INF/versions/9/module-info.class",
                        "first versioned module",
                        "com/example/First.class",
                        "first"));
        writeJar(
                second,
                entries(
                        "module-info.class",
                        "second module",
                        "META-INF/versions/9/module-info.class",
                        "second versioned module",
                        "com/example/Second.class",
                        "second"));

        merge(output, first, second);

        try (JarFile jar = new JarFile(output.toFile())) {
            assertMissing(jar, "module-info.class");
            assertMissing(jar, "META-INF/versions/9/module-info.class");
            assertExists(jar, "com/example/First.class");
            assertExists(jar, "com/example/Second.class");
        }
    }

    private static void testDuplicateRealEntryFails(final Path root) throws Exception {
        final Path directory = Files.createDirectories(root.resolve("duplicate-real-entry"));
        final Path first = directory.resolve("first.jar");
        final Path second = directory.resolve("second.jar");
        final Path output = directory.resolve("output.jar");

        writeJar(first, entries("com/example/Duplicate.class", "first"));
        writeJar(second, entries("com/example/Duplicate.class", "second"));

        try {
            merge(output, first, second);
            throw new AssertionError("Expected duplicate real entry to fail");
        } catch (final IOException ioe) {
            assertContains(
                    String.valueOf(ioe.getMessage()),
                    "Duplicate non-identical jar entry: com/example/Duplicate.class",
                    "duplicate entry failure");
        }
    }

    private static void merge(final Path output, final Path... inputs) throws Exception {
        final var args = new String[3 + inputs.length * 2];
        args[0] = "merge";
        args[1] = "--output";
        args[2] = output.toString();
        int index = 3;
        for (final Path input : inputs) {
            args[index++] = "--input";
            args[index++] = input.toString();
        }
        JarBuilder.main(args);
    }

    private static Map<String, String> entries(final String... values) {
        final var entries = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) {
            entries.put(values[i], values[i + 1]);
        }
        return entries;
    }

    private static void writeJar(final Path path, final Map<String, String> entries) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                final var jarEntry = new JarEntry(entry.getKey());
                jarEntry.setTime(0L);
                out.putNextEntry(jarEntry);
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static void assertExists(final JarFile jar, final String name) {
        if (jar.getJarEntry(name) == null) {
            throw new AssertionError("Expected jar entry to exist: " + name);
        }
    }

    private static void assertMissing(final JarFile jar, final String name) {
        if (jar.getJarEntry(name) != null) {
            throw new AssertionError("Expected jar entry to be absent: " + name);
        }
    }

    private static void assertContains(final String actual, final String expected, final String message) {
        if (!actual.contains(expected)) {
            throw new AssertionError(message + "\nExpected to contain: " + expected + "\nActual:\n" + actual);
        }
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        for (final Path path : paths(root)) {
            Files.deleteIfExists(path);
        }
    }

    private static Iterable<Path> paths(final Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.sorted(Comparator.reverseOrder()).toList();
        }
    }
}
