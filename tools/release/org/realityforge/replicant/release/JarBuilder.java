package org.realityforge.replicant.release;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.jspecify.annotations.Nullable;

public final class JarBuilder {
    private static final long STABLE_TIME = 0L;

    private JarBuilder() {}

    public static void main(final String[] args) throws Exception {
        if (args.length == 0 || !"merge".equals(args[0])) {
            throw new IllegalArgumentException("Usage: merge --output <jar> [--main-class <class>] --input <jar>...");
        }

        final var inputs = new ArrayList<Path>();
        final var resources = new ArrayList<Resource>();
        Path output = null;
        String mainClass = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--output":
                    output = Path.of(args[++i]);
                    break;
                case "--main-class":
                    mainClass = args[++i];
                    break;
                case "--input":
                    inputs.add(Path.of(args[++i]));
                    break;
                case "--resource":
                    resources.add(parseResource(args[++i]));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (output == null) {
            throw new IllegalArgumentException("Missing --output");
        }
        merge(output, mainClass, inputs, resources);
    }

    private static Resource parseResource(final String arg) {
        final int index = arg.indexOf('=');
        if (index <= 0 || index == arg.length() - 1) {
            throw new IllegalArgumentException("Expected resource argument in the form <path>=<entry>");
        }
        return new Resource(Path.of(arg.substring(0, index)), arg.substring(index + 1));
    }

    private static void merge(
            final Path output,
            @Nullable final String mainClass,
            final List<Path> inputs,
            final List<Resource> resources)
            throws IOException {
        final var entries = new LinkedHashMap<String, byte[]>();
        final var services = new TreeMap<String, LinkedHashSet<String>>();
        final var plexusComponents = new ArrayList<String>();

        for (final Path input : inputs) {
            mergeJar(input, entries, services, plexusComponents);
        }
        for (final Resource resource : resources) {
            addEntry(entries, resource.entryName(), Files.readAllBytes(resource.path()), false);
        }

        Files.createDirectories(output.toAbsolutePath().getParent());
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(output), manifest(mainClass))) {
            writeEntries(out, entries, services, plexusComponents);
        }
    }

    private static Manifest manifest(@Nullable final String mainClass) {
        final var manifest = new Manifest();
        final var attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (mainClass != null && !mainClass.isBlank()) {
            attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        }
        return manifest;
    }

    private static void mergeJar(
            final Path input,
            final Map<String, byte[]> entries,
            final Map<String, LinkedHashSet<String>> services,
            final List<String> plexusComponents)
            throws IOException {
        try (JarFile jar = new JarFile(input.toFile())) {
            final var jarEntries = Collections.list(jar.entries());
            jarEntries.sort(Comparator.comparing(ZipEntry::getName));
            for (final JarEntry entry : jarEntries) {
                final String name = entry.getName();
                if (entry.isDirectory() || shouldSkip(name)) {
                    continue;
                }
                final byte[] content = read(jar, entry);
                if ("META-INF/plexus/components.xml".equals(name)) {
                    plexusComponents.add(extractPlexusComponents(content));
                } else if (isLineMergedMetadata(name)) {
                    mergeService(services, name, content);
                } else {
                    addEntry(entries, name, content, isLegalMetadata(name));
                }
            }
        }
    }

    private static boolean shouldSkip(final String name) {
        if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
            return true;
        }
        if (isModuleDescriptor(name)) {
            return true;
        }
        final var upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"));
    }

    private static boolean isModuleDescriptor(final String name) {
        return "module-info.class".equals(name)
                || (name.startsWith("META-INF/versions/") && name.endsWith("/module-info.class"));
    }

    private static boolean isLegalMetadata(final String name) {
        final var upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/LICENSE")
                || upper.startsWith("META-INF/NOTICE")
                || upper.startsWith("META-INF/DEPENDENCIES");
    }

    private static boolean isLineMergedMetadata(final String name) {
        return name.startsWith("META-INF/services/") || name.startsWith("META-INF/sisu/");
    }

    private static byte[] read(final JarFile jar, final ZipEntry entry) throws IOException {
        try (InputStream input = jar.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static void mergeService(
            final Map<String, LinkedHashSet<String>> services, final String name, final byte[] content) {
        final var lines = services.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
        final var text = new String(content, StandardCharsets.UTF_8);
        for (final String line : text.split("\\R")) {
            final String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
    }

    private static String extractPlexusComponents(final byte[] content) throws IOException {
        final var text = new String(content, StandardCharsets.UTF_8);
        final int start = text.indexOf("<components>");
        final int end = text.lastIndexOf("</components>");
        if (start < 0 || end < start) {
            throw new IOException("Unable to merge malformed META-INF/plexus/components.xml");
        }
        return text.substring(start + "<components>".length(), end).trim();
    }

    private static void addEntry(
            final Map<String, byte[]> entries, final String name, final byte[] content, final boolean keepFirst)
            throws IOException {
        final byte[] existing = entries.get(name);
        if (existing == null) {
            entries.put(name, content);
        } else if (keepFirst || java.util.Arrays.equals(existing, content)) {
            return;
        } else {
            throw new IOException("Duplicate non-identical jar entry: " + name);
        }
    }

    private static void writeEntries(
            final JarOutputStream out,
            final Map<String, byte[]> entries,
            final Map<String, LinkedHashSet<String>> services,
            final List<String> plexusComponents)
            throws IOException {
        final var sortedEntries = new TreeMap<>(entries);
        for (final Map.Entry<String, byte[]> entry : sortedEntries.entrySet()) {
            writeEntry(out, entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, LinkedHashSet<String>> service : services.entrySet()) {
            final var content = new ByteArrayOutputStream();
            for (final String line : service.getValue()) {
                content.write(line.getBytes(StandardCharsets.UTF_8));
                content.write('\n');
            }
            writeEntry(out, service.getKey(), content.toByteArray());
        }
        if (!plexusComponents.isEmpty()) {
            final var content = new ByteArrayOutputStream();
            content.write("<component-set>\n  <components>\n".getBytes(StandardCharsets.UTF_8));
            for (final String component : plexusComponents) {
                content.write(component.getBytes(StandardCharsets.UTF_8));
                content.write('\n');
            }
            content.write("  </components>\n</component-set>\n".getBytes(StandardCharsets.UTF_8));
            writeEntry(out, "META-INF/plexus/components.xml", content.toByteArray());
        }
    }

    private static void writeEntry(final JarOutputStream out, final String name, final byte[] content)
            throws IOException {
        final var entry = new JarEntry(name);
        entry.setTime(STABLE_TIME);
        out.putNextEntry(entry);
        out.write(content);
        out.closeEntry();
    }

    private static final class Resource {
        private final Path _path;
        private final String _entryName;

        private Resource(final Path path, final String entryName) {
            _path = path;
            _entryName = entryName;
        }

        private Path path() {
            return _path;
        }

        private String entryName() {
            return _entryName;
        }
    }
}
