package com.onpositive.analyzer.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultClassSkippedPredicateTest {

    private final ClassSkippedPredicate predicate = new DefaultClassSkippedPredicate();

    @Test
    void jdkClasses_skipped() {
        assertTrue(predicate.shouldSkip("java.lang.String"));
        assertTrue(predicate.shouldSkip("java.lang.Object"));
        assertTrue(predicate.shouldSkip("java.util.HashMap"));
        assertTrue(predicate.shouldSkip("java.util.ArrayList"));
        assertTrue(predicate.shouldSkip("java.io.File"));
        assertTrue(predicate.shouldSkip("java.nio.ByteBuffer"));
    }

    @Test
    void javaxClasses_skipped() {
        assertTrue(predicate.shouldSkip("javax.swing.JFrame"));
        assertTrue(predicate.shouldSkip("javax.xml.parsers.DocumentBuilder"));
    }

    @Test
    void sunJdkInternal_skipped() {
        assertTrue(predicate.shouldSkip("sun.misc.Unsafe"));
        assertTrue(predicate.shouldSkip("jdk.internal.reflect.Reflection"));
        assertTrue(predicate.shouldSkip("com.sun.management.GarbageCollectorMXBean"));
    }

    @Test
    void apacheLibrary_skipped() {
        assertTrue(predicate.shouldSkip("org.apache.commons.lang3.StringUtils"));
        assertTrue(predicate.shouldSkip("org.apache.tomcat.util.net.NioEndpoint"));
    }

    @Test
    void springFramework_skipped() {
        assertTrue(predicate.shouldSkip("org.springframework.boot.SpringApplication"));
        assertTrue(predicate.shouldSkip("org.springframework.beans.factory.BeanFactory"));
    }

    @Test
    void googleLibrary_skipped() {
        assertTrue(predicate.shouldSkip("com.google.common.collect.ImmutableList"));
        assertTrue(predicate.shouldSkip("com.google.gson.Gson"));
    }

    @Test
    void fasterXML_skipped() {
        assertTrue(predicate.shouldSkip("com.fasterxml.jackson.databind.ObjectMapper"));
    }

    @Test
    void hibernate_skipped() {
        assertTrue(predicate.shouldSkip("org.hibernate.SessionFactory"));
    }

    @Test
    void lombok_skipped() {
        assertTrue(predicate.shouldSkip("lombok.Data"));
        assertTrue(predicate.shouldSkip("org.projectlombok.Builder"));
    }

    @Test
    void loggingLibraries_skipped() {
        assertTrue(predicate.shouldSkip("org.slf4j.LoggerFactory"));
        assertTrue(predicate.shouldSkip("ch.qos.logback.classic.Logger"));
        assertTrue(predicate.shouldSkip("org.apache.logging.log4j.LogManager"));
    }

    @Test
    void testingLibraries_skipped() {
        assertTrue(predicate.shouldSkip("org.junit.jupiter.api.Test"));
        assertTrue(predicate.shouldSkip("org.testng.annotations.Test"));
        assertTrue(predicate.shouldSkip("org.mockito.Mockito"));
        assertTrue(predicate.shouldSkip("org.assertj.core.api.Assertions"));
    }

    @Test
    void applicationClasses_notSkipped() {
        assertFalse(predicate.shouldSkip("com.mycompany.service.UserService"));
        assertFalse(predicate.shouldSkip("org.myapp.model.Order"));
        assertFalse(predicate.shouldSkip("my.custom.package.Foo"));
        assertFalse(predicate.shouldSkip("app.controller.PaymentController"));
    }

    @Test
    void jdkProxyClasses_skipped() {
        assertTrue(predicate.shouldSkip("com.sun.proxy.$Proxy123"));
        assertTrue(predicate.shouldSkip("com.sun.proxy.$Proxy0"));
    }

    @Test
    void cglibProxyClasses_skipped() {
        assertTrue(predicate.shouldSkip("com.example.Foo$$EnhancerByCGLIB$$abc123"));
        assertTrue(predicate.shouldSkip("com.example.Bar$$FastClassByCGLIB$$def456"));
    }

    @Test
    void hibernateProxyClasses_skipped() {
        assertTrue(predicate.shouldSkip("com.example.Entity$HibernateProxy$xyz789"));
    }

    @Test
    void anonymousClasses_skipped() {
        assertTrue(predicate.shouldSkip("com.example.Outer$1"));
        assertTrue(predicate.shouldSkip("com.example.Outer$42"));
    }

    @Test
    void lambdaClasses_skipped() {
        assertTrue(predicate.shouldSkip("com.example.Service$$Lambda$1234/0x0000"));
    }

    @Test
    void netty_skipped() {
        assertTrue(predicate.shouldSkip("io.netty.channel.nio.NioEventLoopGroup"));
    }

    @Test
    void nullOrEmpty_skipped() {
        assertTrue(predicate.shouldSkip(null));
        assertTrue(predicate.shouldSkip(""));
    }

    @Test
    void customPackage_notOnSkipList_passes() {
        assertFalse(predicate.shouldSkip("com.acme.widget.WidgetService"));
        assertFalse(predicate.shouldSkip("net.mystartup.analytics.Tracker"));
        assertFalse(predicate.shouldSkip("com.github.example.UserRepo"));
    }
}
