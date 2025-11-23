package ru.korovin.packages.fasterjpa.queryparam.factories;

import ru.korovin.packages.fasterjpa.queryparam.Pagination;

public class Paginations {
    public static final int ALL_CONTENT_SIZE = -1;
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;


    public static Pagination unpaged() {
        return ofSize(ALL_CONTENT_SIZE);
    }

    public static Pagination of(int page, int size) {
        Pagination pagination = new Pagination();
        pagination.setSize(size);
        pagination.setPage(page);
        return pagination;
    }

    public static Pagination ofSize(int size){
        Pagination pagination = new Pagination();
        pagination.setSize(size);
        return pagination;
    }
}
