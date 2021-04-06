package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class Dereference extends JsonNodeTransformer {

    @Override
    protected JsonNode transform(JsonNode tree, Map<String, Object> options) {
        return tree;
    }
}
