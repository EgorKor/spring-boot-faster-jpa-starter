package ru.samgtu.packages.webutils.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ModelMetaFilter {
    private String filter;
    private String verboseName;
    private List<Object> values;

    @JsonIgnore
    private Map<String, String> context;
    @JsonIgnore
    private ChoicesSupplier<Object> choicesSupplier;

    @JsonIgnore
    public ModelMetaFilter getFilterWithChoices() {
        return ModelMetaFilter.builder()
                .filter(filter)
                .verboseName(verboseName)
                .choicesSupplier(choicesSupplier)
                .values(choicesSupplier.getChoices(context))
                .context(context)
                .build();
    }
}
