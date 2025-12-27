```java

List<Long> uniqueYears = queryManager.query(select(
        from(EducationProgram.class),
        sorting(),
        pagination()
));


static String educationProgram() {
    return EducationProgram$.ref();
}

```