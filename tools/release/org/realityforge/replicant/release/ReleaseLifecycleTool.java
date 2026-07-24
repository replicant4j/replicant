package org.realityforge.replicant.release;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class ReleaseLifecycleTool {
    private static final Pattern UNRELEASED_HEADING = Pattern.compile("(?m)^### Unreleased[ \\t]*(?:\\R|$)");
    private static final Pattern SECTION_HEADING = Pattern.compile("(?m)^### [^\\r\\n]*(?:\\R|$)");
    private static final Pattern RELEASE_HEADING =
            Pattern.compile("(?m)^### \\[v([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?\\][^\\r\\n]*(?:\\R|$)");
    private static final String REPOSITORY_URL = "https://github.com/replicant4j/replicant";

    private ReleaseLifecycleTool() {}

    public static void main(final String[] args) {
        final int exitCode = run(args, System.out, System.err, Clock.systemDefaultZone());
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(final String[] args, final PrintStream out, final PrintStream err, final Clock clock) {
        try {
            if (args.length == 0 || "--help".equals(args[0])) {
                printUsage(out);
                return 0;
            }

            switch (args[0]) {
                case "next-version":
                    out.println(nextVersion(readString(parsePathOption(args, "--changelog"))));
                    return 0;
                case "prepare":
                    prepare(parsePrepareOptions(args), clock);
                    return 0;
                case "finalize":
                    finalizeRelease(parseChangelogVersionOptions(args));
                    return 0;
                case "release-notes":
                    releaseNotes(parseReleaseNotesOptions(args));
                    return 0;
                default:
                    throw new IllegalArgumentException("Unknown command: " + args[0]);
            }
        } catch (final IllegalArgumentException | IOException e) {
            err.println(e.getMessage());
            return 1;
        }
    }

    private static void printUsage(final PrintStream out) {
        out.println("Usage:");
        out.println("  release_lifecycle --help");
        out.println("  release_lifecycle next-version --changelog PATH");
        out.println("  release_lifecycle prepare --changelog PATH --readme PATH --version VERSION"
                + " [--release-date YYYY-MM-DD]");
        out.println("  release_lifecycle finalize --changelog PATH --version VERSION");
        out.println("  release_lifecycle release-notes --changelog PATH --version VERSION --output PATH");
    }

    private static String nextVersion(final String changelog) {
        final Matcher matcher = RELEASE_HEADING.matcher(changelog);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to find release heading in changelog");
        }

        final String major = matcher.group(1);
        final String minor = matcher.group(2);
        final int nextMinor = Integer.parseInt(minor) + 1;
        final String nextMinorText = padLeft(Integer.toString(nextMinor), minor.length());
        final String patch = matcher.group(3);
        final String version = patch == null ? major + "." + nextMinorText : major + "." + nextMinorText + "." + patch;
        validateReleaseVersion(version);
        return version;
    }

    private static String padLeft(final String value, final int width) {
        if (value.length() >= width) {
            return value;
        }
        return "0".repeat(width - value.length()) + value;
    }

    private static void prepare(final PrepareOptions options, final Clock clock) throws IOException {
        validateReleaseVersion(options.version());
        final String requestedReleaseDate = options.releaseDate();
        final String releaseDate = requestedReleaseDate == null
                ? LocalDate.now(clock).toString()
                : parseReleaseDate(requestedReleaseDate).toString();

        final String changelog = readString(options.changelog());
        final String readme = readString(options.readme());
        final PreparedChangelog preparedChangelog = prepareChangelog(changelog, options.version(), releaseDate);
        final String updatedReadme = updateReadme(readme, preparedChangelog.previousVersion(), options.version());

        writeString(options.changelog(), preparedChangelog.content());
        writeString(options.readme(), updatedReadme);
    }

    private static LocalDate parseReleaseDate(final String releaseDate) {
        try {
            return LocalDate.parse(releaseDate);
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException("Release date must use YYYY-MM-DD: " + releaseDate);
        }
    }

    private static void validateReleaseVersion(final String version) {
        if (!version.matches("6\\.[0-9]+")) {
            throw new IllegalArgumentException("Version must match 6.<number>: " + version);
        }
    }

    private static PreparedChangelog prepareChangelog(
            final String changelog, final String version, final String releaseDate) {
        if (releaseHeading(version).matcher(changelog).find()) {
            throw new IllegalArgumentException("Release already exists in changelog: v" + version);
        }

        int unreleasedStart = -1;
        int unreleasedEnd = -1;
        int count = 0;
        final Matcher unreleasedMatcher = UNRELEASED_HEADING.matcher(changelog);
        while (unreleasedMatcher.find()) {
            count++;
            unreleasedStart = unreleasedMatcher.start();
            unreleasedEnd = unreleasedMatcher.end();
        }
        if (count == 0) {
            throw new IllegalArgumentException("Missing ### Unreleased section");
        }
        if (count > 1) {
            throw new IllegalArgumentException("Duplicate ### Unreleased sections");
        }

        final Matcher previousRelease = RELEASE_HEADING.matcher(changelog);
        if (!previousRelease.find(unreleasedEnd)) {
            throw new IllegalArgumentException("Unable to find previous release heading after ### Unreleased");
        }

        final String body =
                changelog.substring(unreleasedEnd, previousRelease.start()).strip();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("### Unreleased section is empty");
        }

        final String patch = previousRelease.group(3);
        final String previousVersion =
                previousRelease.group(1) + "." + previousRelease.group(2) + (patch == null ? "" : "." + patch);
        final String heading = "### [v" + version + "](" + REPOSITORY_URL + "/tree/v" + version + ") (" + releaseDate
                + ") · [Full Changelog](" + REPOSITORY_URL + "/compare/v" + previousVersion + "...v" + version + ")";
        final String content = changelog.substring(0, unreleasedStart) + heading + "\n\n"
                + "Changes in this release:\n\n" + body + "\n\n" + changelog.substring(previousRelease.start());
        return new PreparedChangelog(content, previousVersion);
    }

    private static String updateReadme(final String readme, final String previousVersion, final String version) {
        String updated = readme;
        updated = updated.replace("<version>" + previousVersion + "</version>", "<version>" + version + "</version>");
        updated = updated.replace("/" + previousVersion + "/", "/" + version + "/");
        updated = updated.replace("-" + previousVersion + "-", "-" + version + "-");
        if (updated.equals(readme)) {
            throw new IllegalArgumentException(
                    "README does not contain previous version patterns for " + previousVersion);
        }
        return updated;
    }

    private static void finalizeRelease(final ChangelogVersionOptions options) throws IOException {
        validateReleaseVersion(options.version());
        final String changelog = readString(options.changelog());
        final Matcher firstSection = SECTION_HEADING.matcher(changelog);
        if (firstSection.find() && isUnreleasedHeading(firstSection.group())) {
            return;
        }

        final Matcher release = releaseHeading(options.version()).matcher(changelog);
        if (!release.find()) {
            throw new IllegalArgumentException("Unable to find release heading for v" + options.version());
        }

        writeString(
                options.changelog(),
                changelog.substring(0, release.start()) + "### Unreleased\n\n" + changelog.substring(release.start()));
    }

    private static boolean isUnreleasedHeading(final String heading) {
        return "### Unreleased".equals(heading.strip());
    }

    private static void releaseNotes(final ReleaseNotesOptions options) throws IOException {
        validateReleaseVersion(options.version());
        final String changelog = readString(options.changelog());
        final Matcher release = releaseHeading(options.version()).matcher(changelog);
        if (!release.find()) {
            throw new IllegalArgumentException("Unable to find release heading for v" + options.version());
        }

        final Matcher nextRelease = RELEASE_HEADING.matcher(changelog);
        final int end = nextRelease.find(release.end()) ? nextRelease.start() : changelog.length();
        writeString(options.output(), changelog.substring(release.end(), end).strip() + "\n");
    }

    private static Pattern releaseHeading(final String version) {
        return Pattern.compile("(?m)^### \\[v" + Pattern.quote(version) + "\\][^\\r\\n]*(?:\\R|$)");
    }

    private static Path parsePathOption(final String[] args, final String name) {
        Path result = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--changelog":
                    if (!"--changelog".equals(name)) {
                        throw new IllegalArgumentException("Unexpected option: " + args[i]);
                    }
                    result = Path.of(requireValue(args, ++i, "--changelog"));
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected option: " + args[i]);
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("Missing " + name);
        }
        return result;
    }

    private static PrepareOptions parsePrepareOptions(final String[] args) {
        Path changelog = null;
        Path readme = null;
        String version = null;
        String releaseDate = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--changelog":
                    changelog = Path.of(requireValue(args, ++i, "--changelog"));
                    break;
                case "--readme":
                    readme = Path.of(requireValue(args, ++i, "--readme"));
                    break;
                case "--version":
                    version = requireValue(args, ++i, "--version");
                    break;
                case "--release-date":
                    releaseDate = requireValue(args, ++i, "--release-date");
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected option: " + args[i]);
            }
        }
        if (changelog == null) {
            throw new IllegalArgumentException("Missing --changelog");
        }
        if (readme == null) {
            throw new IllegalArgumentException("Missing --readme");
        }
        if (version == null) {
            throw new IllegalArgumentException("Missing --version");
        }
        return new PrepareOptions(changelog, readme, version, releaseDate);
    }

    private static ChangelogVersionOptions parseChangelogVersionOptions(final String[] args) {
        Path changelog = null;
        String version = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--changelog":
                    changelog = Path.of(requireValue(args, ++i, "--changelog"));
                    break;
                case "--version":
                    version = requireValue(args, ++i, "--version");
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected option: " + args[i]);
            }
        }
        if (changelog == null) {
            throw new IllegalArgumentException("Missing --changelog");
        }
        if (version == null) {
            throw new IllegalArgumentException("Missing --version");
        }
        return new ChangelogVersionOptions(changelog, version);
    }

    private static ReleaseNotesOptions parseReleaseNotesOptions(final String[] args) {
        Path changelog = null;
        String version = null;
        Path output = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--changelog":
                    changelog = Path.of(requireValue(args, ++i, "--changelog"));
                    break;
                case "--version":
                    version = requireValue(args, ++i, "--version");
                    break;
                case "--output":
                    output = Path.of(requireValue(args, ++i, "--output"));
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected option: " + args[i]);
            }
        }
        if (changelog == null) {
            throw new IllegalArgumentException("Missing --changelog");
        }
        if (version == null) {
            throw new IllegalArgumentException("Missing --version");
        }
        if (output == null) {
            throw new IllegalArgumentException("Missing --output");
        }
        return new ReleaseNotesOptions(changelog, version, output);
    }

    private static String requireValue(final String[] args, final int index, final String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static String readString(final Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void writeString(final Path path, final String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static final class PrepareOptions {
        private final Path _changelog;
        private final Path _readme;
        private final String _version;

        @Nullable
        private final String _releaseDate;

        private PrepareOptions(
                final Path changelog, final Path readme, final String version, @Nullable final String releaseDate) {
            _changelog = changelog;
            _readme = readme;
            _version = version;
            _releaseDate = releaseDate;
        }

        private Path changelog() {
            return _changelog;
        }

        private Path readme() {
            return _readme;
        }

        private String version() {
            return _version;
        }

        @Nullable
        private String releaseDate() {
            return _releaseDate;
        }
    }

    private static class ChangelogVersionOptions {
        private final Path _changelog;
        private final String _version;

        private ChangelogVersionOptions(final Path changelog, final String version) {
            _changelog = changelog;
            _version = version;
        }

        final Path changelog() {
            return _changelog;
        }

        final String version() {
            return _version;
        }
    }

    private static final class ReleaseNotesOptions extends ChangelogVersionOptions {
        private final Path _output;

        private ReleaseNotesOptions(final Path changelog, final String version, final Path output) {
            super(changelog, version);
            _output = output;
        }

        private Path output() {
            return _output;
        }
    }

    private static final class PreparedChangelog {
        private final String _content;
        private final String _previousVersion;

        private PreparedChangelog(final String content, final String previousVersion) {
            _content = content;
            _previousVersion = previousVersion;
        }

        private String content() {
            return _content;
        }

        private String previousVersion() {
            return _previousVersion;
        }
    }
}
