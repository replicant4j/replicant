package org.realityforge.replicant.release;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.Nullable;

public final class DistBuilder {
    private static final String GROUP_PATH = "org/realityforge/replicant";
    private static final String DIST_ID = "replicant";
    private static final Map<String, List<String>> ARTIFACTS = Map.of(
            "replicant-client", List.of("jar", "sources", "javadoc", "pom"),
            "replicant-server", List.of("jar", "sources", "javadoc", "pom"));
    private static final long STABLE_TIME = 0L;

    private DistBuilder() {}

    public static void main(final String[] args) throws Exception {
        final var options = parse(args);
        final var version = Files.readString(resolve(options.versionFile())).trim();
        validateReleaseVersion(version);

        final var dist = outputDirectory(options);
        final var stagingRoot = dist.resolve(DIST_ID + "-" + version);
        final var zip = dist.resolve(DIST_ID + "-" + version + ".zip");

        deleteTree(stagingRoot);
        Files.deleteIfExists(zip);
        Files.createDirectories(dist);

        final var primaryFiles = stageArtifacts(options.artifacts(), stagingRoot, version);
        for (final Path file : primaryFiles) {
            final var signature = sign(options, file);
            writeChecksum(file, "MD5", ".md5");
            writeChecksum(file, "SHA-1", ".sha1");
            writeChecksum(signature, "MD5", ".md5");
            writeChecksum(signature, "SHA-1", ".sha1");
        }

        writeZip(stagingRoot, zip);
        System.out.println("Wrote " + stagingRoot);
        System.out.println("Wrote " + zip);
    }

    private static Options parse(final String[] args) {
        Path versionFile = null;
        Path outputDirectory = null;
        String gpgExecutable = "gpg";
        String gpgKeyId = env("GPG_USER");
        final String gpgPass = env("GPG_PASS");
        final var artifacts = new LinkedHashMap<String, Path>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--version-file":
                    versionFile = Path.of(args[++i]);
                    break;
                case "--artifact":
                    final var artifact = args[++i];
                    final int index = artifact.indexOf('=');
                    if (index <= 0 || index == artifact.length() - 1 || artifact.indexOf(':') <= 0) {
                        throw new IllegalArgumentException("Expected --artifact <artifact-id>:<kind>=<path>");
                    }
                    artifacts.put(artifact.substring(0, index), Path.of(artifact.substring(index + 1)));
                    break;
                case "--output-directory":
                    outputDirectory = Path.of(args[++i]);
                    break;
                case "--gpg-executable":
                    gpgExecutable = args[++i];
                    break;
                case "--gpg-key-id":
                    gpgKeyId = args[++i];
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (versionFile == null) {
            throw new IllegalArgumentException("Missing --version-file");
        }
        for (final Map.Entry<String, List<String>> artifact : ARTIFACTS.entrySet()) {
            for (final String kind : artifact.getValue()) {
                final String key = artifact.getKey() + ":" + kind;
                if (!artifacts.containsKey(key)) {
                    throw new IllegalArgumentException("Missing --artifact " + key + "=<path>");
                }
            }
        }
        return new Options(versionFile, outputDirectory, artifacts, gpgExecutable, gpgKeyId, gpgPass);
    }

    @Nullable
    private static String env(final String name) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static void validateReleaseVersion(final String version) {
        if (!version.matches("6\\.[0-9]+")) {
            throw new IllegalArgumentException("Release version must match 6.<number>: " + version);
        }
    }

    private static Path workspace() {
        final String workspace = System.getenv("BUILD_WORKSPACE_DIRECTORY");
        return Path.of(workspace == null ? "." : workspace).toAbsolutePath().normalize();
    }

    private static Path outputDirectory(final Options options) {
        final Path outputDirectory = options.outputDirectory();
        return (outputDirectory == null ? workspace().resolve("dist") : outputDirectory)
                .toAbsolutePath()
                .normalize();
    }

    private static List<Path> stageArtifacts(
            final Map<String, Path> artifacts, final Path stagingRoot, final String version) throws IOException {
        final var primaryFiles = new ArrayList<Path>();
        for (final Map.Entry<String, List<String>> artifact : ARTIFACTS.entrySet()) {
            final String artifactId = artifact.getKey();
            final Path repositoryDir =
                    stagingRoot.resolve(GROUP_PATH).resolve(artifactId).resolve(version);
            Files.createDirectories(repositoryDir);
            for (final String kind : artifact.getValue()) {
                primaryFiles.add(copy(
                        artifact(artifacts, artifactId + ":" + kind),
                        repositoryDir.resolve(filename(artifactId, kind, version))));
            }
        }
        return primaryFiles;
    }

    private static String filename(final String artifactId, final String kind, final String version) {
        switch (kind) {
            case "jar":
                return artifactId + "-" + version + ".jar";
            case "sources":
            case "javadoc":
                return artifactId + "-" + version + "-" + kind + ".jar";
            case "pom":
                return artifactId + "-" + version + ".pom";
            default:
                throw new IllegalArgumentException("Unknown artifact kind: " + kind);
        }
    }

    private static Path artifact(final Map<String, Path> artifacts, final String kind) {
        final Path path = artifacts.get(kind);
        if (path == null) {
            throw new IllegalArgumentException("Missing --artifact " + kind + "=<path>");
        }
        return path;
    }

    private static Path copy(final Path source, final Path target) throws IOException {
        Files.copy(resolve(source), target);
        return target;
    }

    private static Path resolve(final Path path) {
        if (Files.exists(path)) {
            return path.toAbsolutePath().normalize();
        }
        for (final String env : List.of("RUNFILES_DIR", "JAVA_RUNFILES", "TEST_SRCDIR")) {
            final String root = System.getenv(env);
            if (root != null) {
                final var candidate = Path.of(root).resolve(path);
                if (Files.exists(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
                final var mainCandidate = Path.of(root).resolve("_main").resolve(path);
                if (Files.exists(mainCandidate)) {
                    return mainCandidate.toAbsolutePath().normalize();
                }
            }
        }
        throw new IllegalArgumentException("File does not exist: " + path);
    }

    private static Path sign(final Options options, final Path file) throws IOException, InterruptedException {
        final var signature = Path.of(file + ".asc");
        final var command = new ArrayList<String>();
        command.add(options.gpgExecutable());
        if (options.gpgKeyId() != null) {
            command.add("--local-user");
            command.add(options.gpgKeyId());
        }
        command.add("--armor");
        command.add("--output");
        command.add(signature.toString());
        if (options.gpgPass() != null) {
            command.add("--passphrase");
            command.add(options.gpgPass());
        }
        command.add("--detach-sig");
        command.add("--batch");
        command.add("--yes");
        command.add(file.toString());
        final var process = new ProcessBuilder(command).inheritIO().start();
        final int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("gpg exited with status " + exit);
        }
        if (!Files.isRegularFile(signature)) {
            throw new IOException("gpg did not create signature file " + signature);
        }
        return signature;
    }

    private static void writeChecksum(final Path file, final String algorithm, final String suffix)
            throws IOException, NoSuchAlgorithmException {
        Files.writeString(Path.of(file + suffix), digest(file, algorithm) + "\n", StandardCharsets.UTF_8);
    }

    private static String digest(final Path file, final String algorithm) throws IOException, NoSuchAlgorithmException {
        final var digest = MessageDigest.getInstance(algorithm);
        try (InputStream input = Files.newInputStream(file)) {
            final byte[] buffer = new byte[8192];
            while (true) {
                final int count = input.read(buffer);
                if (count < 0) {
                    break;
                }
                digest.update(buffer, 0, count);
            }
        }
        return toHex(digest.digest());
    }

    private static String toHex(final byte[] bytes) {
        final var sb = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static void writeZip(final Path root, final Path zip) throws IOException {
        final List<Path> files;
        try (var stream = Files.walk(root)) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        }
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (final Path file : files) {
                final var entry = new ZipEntry(root.relativize(file).toString().replace('\\', '/'));
                entry.setTime(STABLE_TIME);
                out.putNextEntry(entry);
                Files.copy(file, out);
                out.closeEntry();
            }
        }
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

    private static final class Options {
        private final Path _versionFile;

        @Nullable
        private final Path _outputDirectory;

        private final Map<String, Path> _artifacts;
        private final String _gpgExecutable;

        @Nullable
        private final String _gpgKeyId;

        @Nullable
        private final String _gpgPass;

        private Options(
                final Path versionFile,
                @Nullable final Path outputDirectory,
                final Map<String, Path> artifacts,
                final String gpgExecutable,
                @Nullable final String gpgKeyId,
                @Nullable final String gpgPass) {
            _versionFile = versionFile;
            _outputDirectory = outputDirectory;
            _artifacts = artifacts;
            _gpgExecutable = gpgExecutable;
            _gpgKeyId = gpgKeyId;
            _gpgPass = gpgPass;
        }

        private Path versionFile() {
            return _versionFile;
        }

        @Nullable
        private Path outputDirectory() {
            return _outputDirectory;
        }

        private Map<String, Path> artifacts() {
            return _artifacts;
        }

        private String gpgExecutable() {
            return _gpgExecutable;
        }

        @Nullable
        private String gpgKeyId() {
            return _gpgKeyId;
        }

        @Nullable
        private String gpgPass() {
            return _gpgPass;
        }
    }
}
