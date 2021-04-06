package com.backbase.oss.boat.transformers;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public interface Transformer {

    default OpenAPI transform(OpenAPI openAPI) {
        return transform(openAPI, emptyMap());
    }

    default String getInput(Map<String, Object> options) {
        String input = (String) options.get("input");

        if (input == null) {
            throw new TransformerException(
                format("Transformer %s requested \"input\", but no such option has been provided",
                    getClass().getName()));
        }

        return input;
    }

    OpenAPI transform(OpenAPI openAPI, Map<String, Object> options);
}
