package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import lombok.Getter;
import lombok.Setter;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

@SuppressWarnings("java:S3740")
@Getter
@Setter
public class ExtensionFilter extends JsonNodeTransformer {

    private List<String> remove = emptyList();

    @Override
    protected JsonNode transform(JsonNode tree, Map<String, Object> options) {
        List<String> extensions = new ArrayList<>(remove);

        ofNullable(options.get("remove"))
            .map(t -> (Collection<String>) t)
            .ifPresent(extensions::addAll);

        extensions.addAll(
            extensions.stream()
                .filter(s -> !s.startsWith("x-"))
                .map(s -> "x-" + s)
                .collect(toSet()));

        return extensions.size() > 0 && tree.isContainerNode()
            ? removeExtensions((ContainerNode) tree, extensions)
            : tree;
    }

    private JsonNode removeExtensions(ContainerNode node, Collection<String> remove) {
        if (node.isObject()) {
            ((ObjectNode) node).remove(remove);
        }

        stream(spliteratorUnknownSize(node.elements(), Spliterator.ORDERED), false)
            .filter(ContainerNode.class::isInstance)
            .map(ContainerNode.class::cast)
            .forEach(child -> removeExtensions(child, remove));

        return node;
    }
}


