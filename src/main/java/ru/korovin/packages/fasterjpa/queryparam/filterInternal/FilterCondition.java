package ru.korovin.packages.fasterjpa.queryparam.filterInternal;


public record FilterCondition(String property, FilterOperation operation, Object value) {

    @Override
    public String toString() {
        return "Filter('%s' %s %s)".formatted(property, operation.getOperation(), value);
    }

}
