package com.backbase.oss.boat.transformers;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.models.media.Schema;

class SchemaBasket {
    private static final PropUtils PU = new PropUtils().lenient();

    private final Map<Integer, Schema> basket = new HashMap<>();

    Schema search(Schema schema) {
        return this.basket.merge(fingerprint(schema), schema, (s1, s2) -> s1 != null ? s1 : s2);
    }


    private int fingerprint(Schema schema) {
        final int hash = Arrays.hashCode(
            stream(new Object[] {
                PU.get(schema, "properties"),
                PU.get(schema, "enum"),
                PU.get(schema, "allOf"),
                PU.get(schema, "anyOf"),
                PU.get(schema, "oneOf"),
                ofNullable(schema.getProperties()).map(Map::values).map(this::fingerprint).orElse(0),
            }).toArray(Object[]::new));

        return hash;
    }

    private int fingerprint(Collection<Schema> schemas) {
        return Arrays.hashCode(schemas.stream()
            .map(this::fingerprint)
            .mapToInt(n -> n)
            .toArray());
    }
}

