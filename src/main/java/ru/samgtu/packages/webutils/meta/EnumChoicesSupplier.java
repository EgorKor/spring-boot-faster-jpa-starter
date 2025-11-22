package ru.samgtu.packages.webutils.meta;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class EnumChoicesSupplier implements ChoicesSupplier<Object> {
    private final List<Object> cachedEnumChoices;

    @Override
    public List<Object> getChoices(Map<String, String> context) {
        return cachedEnumChoices;
    }
}
