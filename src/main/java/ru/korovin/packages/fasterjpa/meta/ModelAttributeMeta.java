package ru.korovin.packages.fasterjpa.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ModelAttributeMeta {
    //определяется по полю сущности
    private String name;
    //задаётся в виде альяса в аннотации @MetaAttribute
    private String verboseName;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private boolean isRelation;
    //определяется по типу поля если isRelation = true
    private String relatedModel;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private boolean required;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private String placeholder;
    //определяется в зависимости от типа поля
    private String type;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private boolean immutable;
    //определяется по аннотациям из пакета jakarta.validation
    private List<Validator> validators;
    private Map<String, String> context;
    //порядковый номер в списке атрибутов мета-модели
    @JsonIgnore
    private int order;
    //определяется по аннотации @Choices
    private List<Object> choices;


    //поставщик выбора
    @JsonIgnore
    private ChoicesSupplier<Object> choicesSupplier;

    @JsonIgnore
    public ModelAttributeMeta getWithChoices() {
        List<Object> choices = null;
        if (choicesSupplier != null) {
            choices = choicesSupplier.getChoices(context);
        }
        return ModelAttributeMeta.builder()
                .name(name)
                .verboseName(verboseName)
                .isRelation(isRelation)
                .relatedModel(relatedModel)
                .order(order)
                .immutable(immutable)
                .required(required)
                .placeholder(placeholder)
                .type(type)
                .validators(validators)
                .choicesSupplier(choicesSupplier)
                .context(context)
                .choices(choices)
                .build();
    }

    public String toString() {
        return "ModelAttributeMeta(name=" + this.getName()
                + "\n\tverboseName=" + this.getVerboseName()
                + "\n\tisRelation=" + this.isRelation()
                + "\n\trelatedModel=" + this.getRelatedModel()
                + "\n\trequired=" + this.isRequired()
                + "\n\tplaceholder=" + this.getPlaceholder()
                + "\n\ttype=" + this.getType()
                + "\n\tvalidators=" + this.getValidators()
                + "\n\tchoices=" + this.getChoices() + ")";
    }
}
