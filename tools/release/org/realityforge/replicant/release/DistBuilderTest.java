package org.realityforge.replicant.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

public final class DistBuilderTest {
    private static final List<String> ARTIFACTS = List.of("replicant-client", "replicant-server");

    private DistBuilderTest() {}

    public static void main(final String[] args) throws Exception {
        if (args.length != 9) {
            throw new IllegalArgumentException("Expected version file and eight Maven artifact paths");
        }
        final Path root = Files.createTempDirectory("dist-builder-test");
        try {
            final Path fakeGpg = createFakeGpg(root);
            final Path dist = root.resolve("dist");
            final var distArgs = new ArrayList<String>();
            distArgs.add("--version-file");
            distArgs.add(args[0]);
            distArgs.add("--output-directory");
            distArgs.add(dist.toString());
            distArgs.add("--gpg-executable");
            distArgs.add(fakeGpg.toString());
            addArtifacts(distArgs, args);
            DistBuilder.main(distArgs.toArray(String[]::new));

            final String version = Files.readString(resolve(Path.of(args[0])), StandardCharsets.UTF_8)
                    .trim();
            final Path stagingRoot = dist.resolve("replicant-" + version);
            final Set<String> expected = expectedEntries(version);
            final Set<String> staged = relativeFiles(stagingRoot);
            if (!staged.equals(expected)) {
                throw new AssertionError(
                        "Unexpected staged distribution\nExpected: " + expected + "\nActual: " + staged);
            }
            assertNonEmpty(stagingRoot, staged);

            final Path zipPath = dist.resolve("replicant-" + version + ".zip");
            try (ZipFile zip = new ZipFile(zipPath.toFile())) {
                final var zipped = new LinkedHashSet<String>();
                zip.stream().filter(entry -> !entry.isDirectory()).forEach(entry -> {
                    zipped.add(entry.getName());
                    if (entry.getSize() <= 0) {
                        throw new AssertionError("Empty distribution entry: " + entry.getName());
                    }
                });
                if (!zipped.equals(expected)) {
                    throw new AssertionError(
                            "Unexpected ZIP distribution\nExpected: " + expected + "\nActual: " + zipped);
                }
            }
        } finally {
            deleteTree(root);
        }
    }

    private static void addArtifacts(final List<String> distArgs, final String[] args) {
        final String[] keys = {
            "replicant-client:jar",
            "replicant-client:sources",
            "replicant-client:javadoc",
            "replicant-client:pom",
            "replicant-server:jar",
            "replicant-server:sources",
            "replicant-server:javadoc",
            "replicant-server:pom"
        };
        for (int i = 0; i < keys.length; i++) {
            distArgs.add("--artifact");
            distArgs.add(keys[i] + "=" + args[i + 1]);
        }
    }

    private static Set<String> expectedEntries(final String version) {
        final var entries = new LinkedHashSet<String>();
        for (final String artifact : ARTIFACTS) {
            final var kinds = List.of("jar", "sources", "javadoc", "pom");
            for (final String kind : kinds) {
                final String filename;
                if ("jar".equals(kind)) {
                    filename = artifact + "-" + version + ".jar";
                } else if ("pom".equals(kind)) {
                    filename = artifact + "-" + version + ".pom";
                } else {
                    filename = artifact + "-" + version + "-" + kind + ".jar";
                }
                final String path = "org/realityforge/replicant/" + artifact + "/" + version + "/" + filename;
                for (final String suffix : List.of("", ".asc", ".md5", ".sha1", ".asc.md5", ".asc.sha1")) {
                    entries.add(path + suffix);
                }
            }
        }
        if (entries.size() != 48) {
            throw new AssertionError("Expected a 48-file distribution manifest, got " + entries.size());
        }
        return entries;
    }

    private static Set<String> relativeFiles(final Path root) throws IOException {
        final var files = new LinkedHashSet<String>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .sorted()
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .forEach(files::add);
        }
        return files;
    }

    private static void assertNonEmpty(final Path root, final Set<String> paths) throws IOException {
        for (final String path : paths) {
            if (Files.size(root.resolve(path)) <= 0) {
                throw new AssertionError("Empty staged distribution file: " + path);
            }
        }
    }

    private static Path createFakeGpg(final Path root) throws IOException {
        final Path script = root.resolve("fake-gpg");
        Files.writeString(script, """
            #!/bin/sh
            output=''
            while [ "$#" -gt 0 ]; do
              if [ "$1" = '--output' ]; then
                output="$2"
                shift 2
              else
                shift
              fi
            done
            printf '%s\\n' '-----BEGIN PGP SIGNATURE-----' 'fake' '-----END PGP SIGNATURE-----' > "$output"
            """, StandardCharsets.UTF_8);
        if (!script.toFile().setExecutable(true)) {
            throw new IOException("Unable to make fake gpg executable");
        }
        return script;
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

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        final List<Path> paths;
        try (var stream = Files.walk(root)) {
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        for (final Path path : paths) {
            Files.deleteIfExists(path);
        }
    }
}
