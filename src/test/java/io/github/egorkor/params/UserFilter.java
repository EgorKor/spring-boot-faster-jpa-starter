package io.github.egorkor.params;

import io.github.egorkor.model.User;
import ru.samgtu.packages.webutils.annotations.AllowedOperations;
import ru.samgtu.packages.webutils.annotations.FieldParamMapping;
import ru.samgtu.packages.webutils.annotations.ParamCountLimit;
import ru.samgtu.packages.webutils.queryparam.Filter;
import ru.samgtu.packages.webutils.queryparam.filterInternal.FilterOperation;

@ParamCountLimit(2)
public class UserFilter extends Filter<User> {
    //Маппится в orders.name
    @ParamCountLimit(1)
    @AllowedOperations({FilterOperation.CONTAINS, FilterOperation.NOT_CONTAINS, FilterOperation.LIKE})
    @FieldParamMapping(requestParamMapping = "orders_name", sqlMapping = "orders.name")
    private String orderNameLike;
    private Long id;
}
