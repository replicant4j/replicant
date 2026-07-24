package org.realityforge.replicant.j2cl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class SourceJarBuilderTest {
    private SourceJarBuilderTest() {}

    public static void main(final String[] args) throws Exception {
        final Path root = Files.createTempDirectory("j2cl-source-jar-builder-test");
        try {
            final Path input = root.resolve("input.jar");
            final Path first = root.resolve("first.jar");
            final Path second = root.resolve("second.jar");
            writeJar(
                    input,
                    Map.of(
                            "com/example/Example.java",
                            "package com.example;\n// J2CL_ONLY import jsinterop.annotations.JsMethod;\n",
                            "akasha/WindowGlobal.java",
                            """
                            package akasha;
                            import jsinterop.annotations.JsMethod;
                            @JsType(
                                isNative = true,
                                namespace = "<window>",
                                name = "$wnd"
                            )
                            public final class WindowGlobal {}
                            """,
                            "org/jspecify/annotations/Nullable.java",
                            "package org.jspecify.annotations;\n"));

            SourceJarBuilder.build(input, first, true, true, List.of("org/jspecify/annotations/"));
            SourceJarBuilder.build(input, second, true, true, List.of("org/jspecify/annotations/"));

            assertEquals(Files.readAllBytes(first), Files.readAllBytes(second), "deterministic output");
            try (JarFile jar = new JarFile(first.toFile())) {
                assertEquals(
                        "package com.example;\nimport jsinterop.annotations.JsMethod;\n",
                        read(jar, "com/example/Example.java"),
                        "activated source");
                assertEquals("""
                    package akasha;
                    import jsinterop.annotations.JsMethod;
                    import jsinterop.annotations.JsPackage;
                    @JsType(
                        isNative = true,
                        namespace = JsPackage.GLOBAL,
                        name = "window"
                    )
                    public final class WindowGlobal {}
                    """, read(jar, "akasha/WindowGlobal.java"), "Akasha window global");
                if (jar.getJarEntry("org/jspecify/annotations/Nullable.java") != null) {
                    throw new AssertionError("Excluded source remains in output");
                }
                if (jar.getJarEntry("com/example/Example.java").getTime() != 0L) {
                    throw new AssertionError("Output entry timestamp is not stable");
                }
            }
        } finally {
            deleteTree(root);
        }
    }

    private static void writeJar(final Path path, final Map<String, String> entries) throws IOException {
        try (var out = new JarOutputStream(Files.newOutputStream(path))) {
            for (final var entry : new LinkedHashMap<>(entries).entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static String read(final JarFile jar, final String name) throws IOException {
        try (var input = jar.getInputStream(jar.getJarEntry(name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertEquals(final byte[] expected, final byte[] actual, final String label) {
        if (!java.util.Arrays.equals(expected, actual)) {
            throw new AssertionError("Unexpected " + label);
        }
    }

    private static void assertEquals(final String expected, final String actual, final String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Unexpected " + label + "\nExpected: " + expected + "\nActual: " + actual);
        }
    }

    private static void deleteTree(final Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
