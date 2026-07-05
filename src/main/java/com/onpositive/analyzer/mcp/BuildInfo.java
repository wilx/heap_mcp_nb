package com.onpositive.analyzer.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class BuildInfo {

    private static final String RESOURCE = "/com/onpositive/analyzer/mcp/version.properties";
    private static final String FALLBACK_VERSION = "0.0.0-dev";

    private BuildInfo() {
    }

    static String version() {
        String implementationVersion = BuildInfo.class.getPackage().getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isBlank()) {
            return implementationVersion;
        }

        Properties properties = new Properties();
        try (InputStream input = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                return FALLBACK_VERSION;
            }
            properties.load(input);
        } catch (IOException ex) {
            return FALLBACK_VERSION;
        }

        String version = properties.getProperty("version");
        if (version == null || version.isBlank() || version.startsWith("${")) {
            return FALLBACK_VERSION;
        }
        return version;
    }
}
