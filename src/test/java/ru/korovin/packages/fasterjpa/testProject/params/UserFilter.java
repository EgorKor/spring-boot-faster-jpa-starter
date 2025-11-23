package ru.korovin.packages.fasterjpa.testProject.params;

import ru.korovin.packages.fasterjpa.testProject.model.User;
import ru.korovin.packages.fasterjpa.annotations.AllowedOperations;
import ru.korovin.packages.fasterjpa.annotations.FieldParamMapping;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterOperation;

@ParamCountLimit(2)
public class UserFilter extends Filter<User> {
    //Маппится в orders.name
    @ParamCountLimit(1)
    @AllowedOperations({FilterOperation.CONTAINS, FilterOperation.NOT_CONTAINS, FilterOperation.LIKE})
    @FieldParamMapping(requestParamMapping = "orders_name", sqlMapping = "orders.name")
    private String orderNameLike;
    private Long id;
}
