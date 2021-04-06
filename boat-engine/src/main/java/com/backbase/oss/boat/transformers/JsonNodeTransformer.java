package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.SneakyThrows;

public abstract class JsonNodeTransformer implements Transformer {

    @Override
    @SneakyThrows
    public final OpenAPI transform(OpenAPI source, Map<String, Object> options) {
        final ObjectMapper mapper = Yaml.mapper();
        JsonNode tree = mapper.valueToTree(source);

        tree = transform(tree, options);

        return mapper.treeToValue(tree, OpenAPI.class);
    }

    protected abstract JsonNode transform(JsonNode tree, Map<String, Object> options);
}
