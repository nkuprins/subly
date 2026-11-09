package com.app.subly.testutil;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// Use this to avoid repeatable deepCopy() on ObjectNode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonBody {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Map<String, Object> fields;

    public static JsonBody empty() {
        return new JsonBody(Map.of());
    }

    public JsonBody with(String field, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(fields);
        copy.put(field, value);
        return new JsonBody(copy);
    }

    public JsonBody without(String field) {
        Map<String, Object> copy = new LinkedHashMap<>(fields);
        copy.remove(field);
        return new JsonBody(copy);
    }

    public String json() {
        return JSON.writeValueAsString(fields);
    }
}
