package faang.school.paymentservice;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ServerPortConsistencyTest {

    /** The one port every artifact must agree on. */
    private static final int EXPECTED_PORT = 8082;

    @Test
    void applicationYaml_defaultServerPort_matchesStandard() throws IOException {
        // Arrange: locate the runtime config relative to this test class
        String yaml = readResource("application.yaml");

        // Act: extract the default value of the server.port placeholder
        int actual = extractFirstInt(yaml, "port\\s*:\\s*\\$\\{PAYMENT_SERVICE_PORT:(\\d+)\\}");

        // Assert: runtime default matches the standardized port
        assertThat(actual).isEqualTo(EXPECTED_PORT);
    }

    @Test
    void dockerfile_exposePort_matchesStandard() throws IOException {
        // Arrange: read the container image definition from the service root
        String dockerfile = readServiceFile("Dockerfile");

        // Act: extract the EXPOSE directive value
        int actual = extractFirstInt(dockerfile, "EXPOSE\\s+(\\d+)");

        // Assert: exposed port matches the standardized port
        assertThat(actual).isEqualTo(EXPECTED_PORT);
    }

    @Test
    void readme_documentedPort_matchesStandard() throws IOException {
        // Arrange: read the operator-facing run instructions
        String readme = readServiceFile("README.md");

        // Act: extract the host-mapped port from the docker run example
        int actual = extractFirstInt(readme, "docker run -p (\\d+):\\d+");

        // Assert: documented port matches the standardized port
        assertThat(actual).isEqualTo(EXPECTED_PORT);
    }

    private static String readResource(String name) throws IOException {
        try (var in = ServerPortConsistencyTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Classpath resource not found: " + name);
            }
            return new String(in.readAllBytes());
        }
    }

    private static String readServiceFile(String relativePath) throws IOException {
        // Resolve the service root from this class's compiled location so the
        // test is independent of Gradle's working directory. On Windows the URL
        // path carries a leading slash that Paths.get rejects, so strip it.
        String rawPath = ServerPortConsistencyTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        if (rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        Path classDir = Paths.get(rawPath);
        // The code-source location points at the compiled package directory.
        // Walk up until we land on the service root (the dir containing Dockerfile).
        Path resolved = classDir;
        while (resolved != null && !Files.exists(resolved.resolve("Dockerfile"))) {
            resolved = resolved.getParent();
        }
        if (resolved == null) {
            throw new IllegalStateException("Could not locate service root from " + classDir);
        }
        return Files.readString(resolved.resolve(relativePath));
    }

    private static int extractFirstInt(String content, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (!matcher.find()) {
            throw new AssertionError("Pattern not found: " + regex);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
