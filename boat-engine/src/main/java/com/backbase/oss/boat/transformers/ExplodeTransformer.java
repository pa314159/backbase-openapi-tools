package com.backbase.oss.boat.transformers;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.google.common.base.CaseFormat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("rawtypes")
@Slf4j
public class ExplodeTransformer implements Transformer {

    private static final Path EMPTY_PATH = Paths.get("");

    private static boolean isLocalRef(String ref) {
        return isNotBlank(ref) && !ref.startsWith("http");
    }

    private static Path safeParent(Path path) {
        return ofNullable(path.getParent()).orElse(EMPTY_PATH);
    }

    private static boolean isEmptyPath(Path path) {
        return isBlank(path.normalize().toString());
    }

    private static String toHyphen(String name) {
        return name.contains("-")
            ? name
            : CaseFormat.LOWER_CAMEL
                .to(CaseFormat.LOWER_HYPHEN, uncapitalize(name));
    }

    private static String toCamel(String name) {
        return name.contains("-") ? CaseFormat.LOWER_HYPHEN
            .to(CaseFormat.UPPER_CAMEL, name.toLowerCase())
            : capitalize(name);

    }

    @Getter
    private final Map<Path, Schema> files = new HashMap<>();
    private final Map<Path, Map<String, Schema>> components = new HashMap<>();
    private final Map<String, Schema> aliases = new HashMap<>();
    private final Path sourceFile;
    private final Path targetFile;
    private final Path targetName;
    private final Path targetPath;

    private final Path schemasPath;

    private final PropUtils pu = new PropUtils().lenient();

    @Getter
    private OpenAPI openAPI;
    private Map<Pattern, String> rename;

    public ExplodeTransformer(@NonNull Path sourceFile, @NonNull Path targetFile, @NonNull Path schemasPath) {
        if (schemasPath.isAbsolute()) {
            throw new IllegalArgumentException("Map<String, Schema> path cannot be absolute");
        }

        this.sourceFile = sourceFile;
        this.targetFile = targetFile.normalize();
        this.targetName = targetFile.getFileName();
        this.targetPath = safeParent(this.targetFile);
        this.schemasPath = schemasPath.normalize();
    }

    @SneakyThrows
    public void export(boolean clean) {
        if (clean && !isEmptyPath(this.targetPath) && Files.isDirectory(this.targetPath)) {
            FileUtils.cleanDirectory(this.targetPath.toFile());
        }

        SerializerUtils.write(this.targetFile, this.openAPI);

        this.files.forEach((file, schema) -> {
            SerializerUtils.write(this.targetPath.resolve(file), schema);
        });
        this.components.forEach((file, map) -> {
            SerializerUtils.copy(this.sourceFile.resolveSibling(file), this.targetPath.resolve(file));
        });
    }

    @SuppressWarnings("unused")
    @Override
    public void transform(@NonNull OpenAPI openAPI, Map<String, Object> options) {
        if (this.openAPI != null) {
            throw new IllegalStateException(format("Instances of %s are not reusable.", getClass().getSimpleName()));
        }

        this.openAPI = openAPI;
        this.rename = ((Map<Pattern, String>) options.getOrDefault("rename", emptyMap()));
        final Map<String, Schema> schemas = new PropUtils().create().get(openAPI, "components.schemas");

        transformSchemas(this.targetName, schemas);

        this.pu.ifPresent(openAPI, "paths", (io.swagger.v3.oas.models.Paths paths) -> {
            paths.forEach((path, item) -> {
                for (final HttpMethod method : HttpMethod.values()) {
                    this.pu.ifPresent(item, method.name().toLowerCase(),
                        (Operation op) -> transformOperation(method, path, op));
                }
            });
        });

        schemas.putAll(this.aliases);

        this.files.forEach((file, schema) -> updateRefs(file, schema));
    }

    private void transformSchemas(Path source, Map<String, Schema> schemas) {
        schemas.forEach((key, schema) -> {
            transformSchema(source, toHyphen(key), schema, rep -> schemas.put(key, rep));
        });
    }

    private void transformSchemas(Path source, String id, List<Schema> schemas) {
        for (int ix = 0, sz = schemas.size(); ix < sz; ix++) {
            final int at = ix;

            transformSchema(source, id + Integer.toString(ix), schemas.get(ix), rep -> {
                schemas.remove(at);
                schemas.add(at, rep);
            });
        }
    }

    private void transformSchema(Path source, String id, Schema schema, Consumer<Schema> action) {
        if (isLocalRef(schema.get$ref())) {
            if (!schema.get$ref().contains("#/")) {
                transformSchema(source.resolveSibling(schema.get$ref()), id);
            }
        } else if (isBlank(schema.get$ref())) {
            this.pu
                .ifPresent(schema, "properties", (Map<String, Schema> scms) -> {
                    transformSchemas(extract(source, id, schema, action), scms);
                })
                .ifPresent(schema, "enum", unused -> {
                    extract(source, id, schema, action);
                })
                .ifPresent(schema, "items", (Schema scm) -> {
                    final ArraySchema as = (ArraySchema) schema;

                    transformSchema(source, toHyphen(id) + "-items", scm, as::setItems);
                })
                .ifPresent(schema, "allOf", (List<Schema> scms) -> {
                    transformSchemas(source, "all-off", scms);
                })
                .ifPresent(schema, "anyOf", (List<Schema> scms) -> {
                    transformSchemas(source, "all-off", scms);
                })
                .ifPresent(schema, "oneOf", (List<Schema> scms) -> {
                    transformSchemas(source, "all-off", scms);
                });
        }
    }

    @SneakyThrows
    private void transformSchema(Path source, String id) {
        final Path file = this.sourceFile.resolveSibling(source).normalize();
        final Schema schema = Yaml.mapper().readValue(file.toFile(), Schema.class);

        transformSchema(source, id, schema, scm -> {});
    }

    private void transformOperation(HttpMethod method, String uri, Operation op) {
        this.pu
            .ifPresent(op, "requestBody.content", (Content content) -> {
                transformContent(method, uri, op, content, "request");
            })
            .ifPresent(op, "responses", (ApiResponses responses) -> {
                responses.forEach((status, resp) -> {
                    final HttpStatus st = toHttpStatus(status);

                    if (st != null) {
                        if (st.is2xxSuccessful()) {
                            transformContent(method, uri, op, resp.getContent(), "response");
                        } else {
                            transformContent(method, uri, op, resp.getContent(), "response",
                                Integer.toString(st.value()));
                        }
                    } else {
                        transformContent(method, uri, op, resp.getContent(), "response", status);
                    }
                });
            });
    }

    private void transformContent(HttpMethod method, String uri, Operation op, Content content, String... suffixes) {
        content.forEach((name, media) -> {
            this.pu.ifPresent(media, "schema", (Schema schema) -> {
                final String id = buildTypeName(method, uri, op, suffixes);

                transformSchema(this.targetName, id, schema, media::setSchema);
            });
        });
    }

    private Path extract(Path source, String id, Schema schema, Consumer<Schema> action) {
        final String alias = aliasOf(source, id, schema);
        final Path file = fileOf(source, id, schema);

        log.debug("source = {}, alias = {}, file = {}", source, alias, file);

        if (isBlank(schema.getTitle())) {
            schema.setTitle(alias);
        }

        this.aliases.put(alias, new Schema<>().$ref(file.toString()));
        this.files.put(file, schema);

        action.accept(new Schema<>().$ref("#/components/schemas/" + alias));

        return file;
    }

    private void updateRefs(Path source, Schema schema) {
        if (isLocalRef(schema.get$ref())) {
            updateRef(source, schema);
        } else if (isBlank(schema.get$ref())) {
            this.pu
                .ifPresent(schema, "properties", (Map<String, Schema> m) -> updateRefs(source, m.values()))
                .ifPresent(schema, "items", (Schema scm) -> updateRefs(source, scm))
                .ifPresent(schema, "allOf", (Collection all) -> updateRefs(source, all))
                .ifPresent(schema, "anyOf", (Collection all) -> updateRefs(source, all))
                .ifPresent(schema, "oneOf", (Collection all) -> updateRefs(source, all));
        }
    }

    private void updateRefs(Path source, Collection<Schema> schemas) {
        schemas.stream().forEach(scm -> updateRefs(source, scm));
    }

    private void updateRef(Path source, Schema schema) {
        final String $ref = schema.get$ref();
        final String link;
        int index;

        switch (index = $ref.indexOf("#/")) {
            default:
                final Path target = this.targetFile;

                if (index > 0) {
                    // TODO
                    // target = targetPath.relativize($ref.substring(0, index));
                }

                link = this.targetPath
                    .resolve(safeParent(source))
                    .normalize()
                    .relativize(target)
                    + $ref.substring(index);
                break;

            case -1:
                link = this.targetPath.resolve(this.schemasPath)
                    .resolve(source)
                    .relativize(this.targetPath.resolve($ref))
                    .toString();
                break;
        }

        schema.set$ref(link);

        log.debug("{}: relocated {} to {}", source, $ref, link);
    }

    private String aliasOf(Path source) {
        return ofNullable(this.files.get(source))
            .map(Schema::getTitle)
            .orElse("");
    }

    private String aliasOf(Path source, String name, Schema schema) {
        return ofNullable(schema.getTitle())
            .orElseGet(() -> aliasOf(source) + toCamel(toHyphen(name)));
    }

    private Path fileOf(Path source, String name, Schema schema) {
        final String baseType = aliasOf(source);
        final String thisType = ofNullable(schema.getTitle()).orElse(name);
        Path file;

        if (isNotEmpty(baseType) && !thisType.startsWith(baseType)) {
            final String fileName = source.getFileName().toString().replace(".yaml", "/"
                + toHyphen(thisType) + ".yaml");

            file = this.schemasPath.relativize(source.resolveSibling(fileName));
        } else {
            file = Paths.get(toHyphen(thisType) + ".yaml");
        }

        if (this.rename.size() > 0) {
            file = this.rename.entrySet().stream().reduce(file, (p, e) -> {
                final Matcher m = e.getKey().matcher(p.toString());

                return m.matches()
                    ? Paths.get(m.replaceAll(e.getValue()))
                    : p;
            }, (t, u) -> u);
        }

        return this.schemasPath.resolve(file).normalize();
    }

    private String buildTypeName(HttpMethod method, String uri, Operation op, String... suffixes) {
        if (isBlank(op.getOperationId())) {
            throw new RuntimeException(format("Cannot find operation id for %s %s", method, uri));
        }

        final List<String> elements = new ArrayList<>();

        elements.add(uri.replace('/', '-').replaceAll("^-|-$|\\{|\\}", ""));
        if (method != HttpMethod.GET) {
            elements.add(method.name().toLowerCase());
        }
        elements.add(toHyphen(op.getOperationId()));

        stream(suffixes).filter(StringUtils::isNotBlank).forEach(elements::add);

        final String type = toCamel(elements.stream()
            .filter(StringUtils::isNotBlank)
            .collect(joining("-")));

        return type;
    }

    private HttpStatus toHttpStatus(String status) {
        try {
            return HttpStatus.valueOf(Integer.parseInt(status));
        } catch (final IllegalArgumentException ex) {
        }
        try {
            return HttpStatus.valueOf(status);
        } catch (final IllegalArgumentException ex) {
        }

        // unparseable
        return null;
    }

    private Schema $ref(Path base, String type) {
        final Path rel = this.targetPath.resolve(safeParent(this.schemasPath.resolve(base)))
            .relativize(this.targetFile);

        final Schema s = new Schema<>()
            .$ref(rel + "#/components/schemas/" + type);

        // updatedRefs.add(scm);

        log.debug("added new type: {} to {}", type, base);

        return s;
    }
}
