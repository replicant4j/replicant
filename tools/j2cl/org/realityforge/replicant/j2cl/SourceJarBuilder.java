package org.realityforge.replicant.j2cl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class SourceJarBuilder {
    private static final String J2CL_ONLY_MARKER = "// J2CL_ONLY ";
    private static final String AKASHA_WINDOW_GLOBAL = "akasha/WindowGlobal.java";

    private SourceJarBuilder() {}

    public static void main(final String[] args) throws Exception {
        String input = "";
        String output = "";
        boolean activateJ2clOnly = false;
        boolean rewriteAkashaWindowGlobal = false;
        final var excludePrefixes = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input" -> input = requireValue(args, ++i, "--input");
                case "--output" -> output = requireValue(args, ++i, "--output");
                case "--activate-j2cl-only" -> activateJ2clOnly = true;
                case "--rewrite-akasha-window-global" -> rewriteAkashaWindowGlobal = true;
                case "--exclude-prefix" -> excludePrefixes.add(requireValue(args, ++i, "--exclude-prefix"));
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (input.isEmpty() || output.isEmpty()) {
            throw new IllegalArgumentException("--input and --output are required");
        }
        build(Path.of(input), Path.of(output), activateJ2clOnly, rewriteAkashaWindowGlobal, excludePrefixes);
    }

    static void build(
            final Path input,
            final Path output,
            final boolean activateJ2clOnly,
            final boolean rewriteAkashaWindowGlobal,
            final List<String> excludePrefixes)
            throws IOException {
        final var matchedPrefixes = new HashSet<String>();
        boolean activated = false;
        boolean rewroteAkashaWindowGlobal = false;
        try (JarFile jar = new JarFile(input.toFile());
                JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            final List<String> names = jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(JarEntry::getName)
                    .sorted()
                    .toList();
            for (final String name : names) {
                final String excludedBy = excludedBy(name, excludePrefixes);
                if (!excludedBy.isEmpty()) {
                    matchedPrefixes.add(excludedBy);
                    continue;
                }
                final JarEntry sourceEntry = jar.getJarEntry(name);
                byte[] content;
                try (var inputStream = jar.getInputStream(sourceEntry)) {
                    content = inputStream.readAllBytes();
                }
                if (activateJ2clOnly && name.endsWith(".java")) {
                    final var source = new String(content, StandardCharsets.UTF_8);
                    final var transformed = source.replace(J2CL_ONLY_MARKER, "");
                    if (!source.equals(transformed)) {
                        activated = true;
                        content = transformed.getBytes(StandardCharsets.UTF_8);
                    }
                }
                if (rewriteAkashaWindowGlobal && AKASHA_WINDOW_GLOBAL.equals(name)) {
                    final var source = new String(content, StandardCharsets.UTF_8);
                    final var withImport = source.replace(
                            "import jsinterop.annotations.JsMethod;\n",
                            "import jsinterop.annotations.JsMethod;\nimport jsinterop.annotations.JsPackage;\n");
                    final var transformed = withImport.replace(
                            "namespace = \"<window>\",\n    name = \"$wnd\"",
                            "namespace = JsPackage.GLOBAL,\n    name = \"window\"");
                    if (source.equals(withImport) || withImport.equals(transformed)) {
                        throw new IOException("Unexpected " + AKASHA_WINDOW_GLOBAL + " source shape in " + input);
                    }
                    rewroteAkashaWindowGlobal = true;
                    content = transformed.getBytes(StandardCharsets.UTF_8);
                }
                final var outputEntry = new JarEntry(name);
                outputEntry.setTime(0L);
                out.putNextEntry(outputEntry);
                out.write(content);
                out.closeEntry();
            }
        }
        if (activateJ2clOnly && !activated) {
            throw new IOException("No " + J2CL_ONLY_MARKER.trim() + " markers found in " + input);
        }
        if (rewriteAkashaWindowGlobal && !rewroteAkashaWindowGlobal) {
            throw new IOException(AKASHA_WINDOW_GLOBAL + " not found in " + input);
        }
        final Set<String> unmatchedPrefixes = new HashSet<>(excludePrefixes);
        unmatchedPrefixes.removeAll(matchedPrefixes);
        if (!unmatchedPrefixes.isEmpty()) {
            throw new IOException("No entries matched excluded prefixes " + unmatchedPrefixes + " in " + input);
        }
    }

    private static String excludedBy(final String name, final List<String> excludePrefixes) {
        for (final String prefix : excludePrefixes) {
            if (name.startsWith(prefix)) {
                return prefix;
            }
        }
        return "";
    }

    private static String requireValue(final String[] args, final int index, final String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index];
    }
}
