package com.backbase.oss.boat.transformers;

import static org.assertj.core.api.Assertions.assertThat;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ExplodeTransformerTest {

    @Parameterized.Parameters(name = "{0}")
    static public Object data() {
        return new Object[] {
            new Object[] {"DEST"},
            new Object[] {"DEST/schemas"},
            new Object[] {"DEST/shared/schemas"},
        };
    }

    @BeforeClass
    static public void cleanUp() {
        FileUtils.deleteQuietly(new File("target/explode"));
    }

    private final String targetPath;
    private final Path schemasPath;

    public ExplodeTransformerTest(String testName) {
        this.targetPath = testName.toLowerCase().replace('/', '-');
        this.schemasPath = Paths.get(testName.replaceFirst("^DEST/?", ""));
    }

    private ExplodeTransformer transform(String sample) throws OpenAPILoaderException {
        return transform(sample, emptyMap());
    }

    private ExplodeTransformer transform(String sample, Map<Pattern, String> rename) throws OpenAPILoaderException {
        final File file = new File(format("src/test/resources/openapi/explode/%s.yaml", sample));
        final OpenAPI openAPI = OpenAPILoader.load(file);
        final Path output = Paths.get(format("target/explode/%1$s/%2$s/%2$s-out.yaml", this.targetPath, sample));
        final ExplodeTransformer trn = new ExplodeTransformer(file.toPath(), output, this.schemasPath);

        trn.transform(openAPI, singletonMap("rename", rename));
        trn.export(true);

        return trn;
    }

    @Test
    public void simple() throws OpenAPILoaderException {
        final ExplodeTransformer trn =
            transform("simple", singletonMap(Pattern.compile("post-op-id-.+"), "bodies/$0"));
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(7));

        assertThat(schemas)
            .containsKeys("One")
            .containsKeys("Two")
            .containsKeys("Three")
            .containsKeys("Values")
            .containsKeys("PostOpIdRequest")
            .containsKeys("PostOpIdRequestComplex")
            .containsKeys("PostOpIdResult");

        assertThat(files)
            .containsKey(this.schemasPath.resolve("one.yaml"))
            .containsKey(this.schemasPath.resolve("two.yaml"))
            .containsKey(this.schemasPath.resolve("three.yaml"))
            .containsKey(this.schemasPath.resolve("bodies/post-op-id-request.yaml"))
            .containsKey(this.schemasPath.resolve("bodies/post-op-id-request/complex.yaml"))
            .containsKey(this.schemasPath.resolve("bodies/post-op-id-result.yaml"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));
    }

    @Test
    public void nestedWithRefs() throws OpenAPILoaderException {
        final ExplodeTransformer trn = transform("nested-with-refs");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(5));

        assertThat(schemas)
            .containsKeys("One")
            .containsKeys("OnePropTwo")
            .containsKeys("OnePropTwoArrayItems")
            .containsKeys("OnePropTwoEnumsItems")
            .containsKeys("Three");

        assertThat(files)
            .containsKey(this.schemasPath.resolve("one.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two/array-items.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two/enums-items.yaml"))
            .containsKey(this.schemasPath.resolve("three.yaml"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));
    }

    @Test
    public void nestedFully() throws OpenAPILoaderException {
        final ExplodeTransformer trn = transform("nested-fully");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(4));

        assertThat(schemas)
            .containsKeys("One")
            .containsKeys("OnePropTwo")
            .containsKeys("OnePropTwoPropThree")
            .containsKeys("OnePropTwoPropThreePropFour");

        assertThat(files)
            .containsKey(this.schemasPath.resolve("one.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two/prop-three.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two/prop-three/prop-four.yaml"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));
    }

    @Test
    public void nestedWithExtTypes() throws OpenAPILoaderException {
        final ExplodeTransformer trn = transform("nested-with-ext-types");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(3));


        assertThat(schemas)
            .containsKeys("One")
            .containsKeys("OnePropTwo")
            .containsKeys("Three");

        assertThat(files)
            .containsKey(this.schemasPath.resolve("one.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two.yaml"))
            .containsKey(this.schemasPath.resolve("three.yaml"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));
    }

    @Test
    public void nestedWithFiles() throws OpenAPILoaderException {
        final ExplodeTransformer trn = transform("nested-with-files");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(3));


        assertThat(schemas)
            .containsKeys("One")
            .containsKeys("OnePropTwo")
            .containsKeys("Three");

        assertThat(files)
            .containsKey(this.schemasPath.resolve("one.yaml"))
            .containsKey(this.schemasPath.resolve("one/prop-two.yaml"))
            .containsKey(this.schemasPath.resolve("three.yaml"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));
    }

    void assertRefsUpdated(Schema<?> s) {
        // TODO
    }


    @Test
    @Ignore
    public void clientApi() throws OpenAPILoaderException {
        realApi("client");
    }

    @Test
    @Ignore
    public void serviceApi() throws OpenAPILoaderException {
        realApi("service");
    }

    private void realApi(final String source) throws OpenAPILoaderException {
        final String file = format("src/test/resources/openapi/arrangement-manager-api-0.0.21/%s/openapi.yaml", source);
        final OpenAPI openAPI = OpenAPILoader.load(new File(file));
        final Path output = Paths.get(format("target/explode/%s.yaml", source));
        final ExplodeTransformer trn = new ExplodeTransformer(Paths.get(file), output, this.schemasPath);

        trn.transform(openAPI);
        trn.export(true);
    }
}


