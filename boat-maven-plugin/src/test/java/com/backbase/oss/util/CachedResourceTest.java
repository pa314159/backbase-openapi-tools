package com.backbase.oss.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class CachedResourceTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "oas-examples/petstore.yaml",
        "src/test/resources/oas-examples/petstore.yaml",
        "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore.yaml",
        "META-INF/maven/org.apache.maven/maven-core/pom.properties",
    })
    void run(String source) {
        CachedResource resource = new CachedResource(source);

        log.info("local: {}", resource.getFile());
        log.info("hash: {}", resource.getHash());

        assertThat(resource.getFile(), is(notNullValue()));
        assertThat(resource.getHash(), is(notNullValue()));
        assertThat(resource.getFile().exists(), is(true));
    }
}
