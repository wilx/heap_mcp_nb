package com.onpositive.analyzer.mcp;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildInfoTest {

    @Test
    void versionMatchesMavenProjectVersion() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        String pomVersion = document.getDocumentElement()
                .getElementsByTagName("version")
                .item(0)
                .getTextContent();

        assertEquals(pomVersion, BuildInfo.version());
    }
}
