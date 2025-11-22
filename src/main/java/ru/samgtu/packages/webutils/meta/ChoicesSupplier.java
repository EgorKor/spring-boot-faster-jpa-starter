package ru.samgtu.packages.webutils.meta;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ChoicesSupplier<T> {
    List<T> getChoices(Map<String, String> context);
}
