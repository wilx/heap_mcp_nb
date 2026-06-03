package com.onpositive.analyzer.search;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DefaultClassSkippedPredicate implements ClassSkippedPredicate {

    private static final List<String> SKIP_PREFIXES = List.of(
            "java.", "javax.", "sun.", "jdk.", "oracle.", "com.sun.", "netscape.",
            "org.apache.", "org.springframework.", "com.springsource.",
            "org.hibernate.", "org.jboss.", "org.wildfly.",
            "org.eclipse.", "com.google.", "com.fasterxml.",
            "io.netty.", "reactor.core.", "reactor.netty.",
            "org.eclipse.microprofile.", "io.micrometer.", "io.micronaut.",
            "io.quarkus.", "lombok.", "org.projectlombok.",
            "org.objectweb.asm.", "org.ow2.asm.", "net.sf.cglib.", "cglib.",
            "javassist.", "org.javassist.",
            "org.slf4j.", "ch.qos.logback.", "org.apache.logging.", "org.apache.log4j.",
            "org.junit.", "org.testng.", "org.mockito.", "org.assertj.",
            "org.hamcrest.", "org.jmock.", "org.easymock.",
            "com.thoughtworks.xstream.", "org.codehaus.", "com.esotericsoftware.",
            "org.bouncycastle.", "org.yaml.",
            "com.fasterxml.jackson.dataformat.yaml.",
            "org.glassfish.", "org.jvnet.",
            "io.undertow.", "rx.", "org.joda.",
            "org.apache.tomcat.", "org.apache.catalina.", "org.apache.coyote.",
            "org.eclipse.jetty."
    );

    private static final Set<Pattern> PROXY_PATTERNS = Set.of(
            Pattern.compile(".*\\$Proxy\\d*$"),
            Pattern.compile(".*\\$\\$EnhancerByCGLIB\\$\\$.*"),
            Pattern.compile(".*\\$\\$FastClassByCGLIB\\$\\$.*"),
            Pattern.compile(".*\\$\\$_javassist.*"),
            Pattern.compile(".*_\\$\\$_jvst.*"),
            Pattern.compile(".*_Stub$"),
            Pattern.compile(".*__Impl$"),
            Pattern.compile(".*\\$HibernateProxy\\$.*"),
            Pattern.compile("^\\$+[A-Za-z].*")
    );

    @Override
    public boolean shouldSkip(String fullyQualifiedClassName) {
        if (fullyQualifiedClassName == null || fullyQualifiedClassName.isEmpty()) {
            return true;
        }
        for (String prefix : SKIP_PREFIXES) {
            if (fullyQualifiedClassName.startsWith(prefix)) {
                return true;
            }
        }
        for (Pattern pattern : PROXY_PATTERNS) {
            if (pattern.matcher(fullyQualifiedClassName).matches()) {
                return true;
            }
        }
        if (fullyQualifiedClassName.contains("$Lambda")) {
            return true;
        }
        if (fullyQualifiedClassName.matches(".*\\$\\d+$")) {
            return true;
        }
        return false;
    }
}
