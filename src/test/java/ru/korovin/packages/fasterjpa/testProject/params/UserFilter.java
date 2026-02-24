package ru.korovin.packages.fasterjpa.testProject.params;

import ru.korovin.packages.fasterjpa.testProject.model.User;
import ru.korovin.packages.fasterjpa.annotations.AllowedOperations;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation;

import java.util.function.Supplier;

@ParamCountLimit(2)
public class UserFilter extends Filter<User> {
    //Маппится в orders.name
    @ParamCountLimit(1)
    @AllowedOperations({FilterOperation.CONTAINS, FilterOperation.NOT_CONTAINS, FilterOperation.LIKE})
    private Supplier<String> orders_name = () -> "orders.name";
    private Long id;
}
