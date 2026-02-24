package ru.korovin.packages.fasterjpa.tests.params;

import org.junit.jupiter.api.Test;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.tokenizing.FilterToken;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.tokenizing.FilterTokenizer;

import java.util.List;

public class FilterExpressionCompilerTest {

    @Test
    public void testCompilation(){
        String expression =
                """
                concat(
                """ + // УГНП
                        """
                            to_char(federalEducationStandard.direction.consolidatedGroupOfDirections.code, 'FM09'), '.00.00',
                            ' ',
                        """ + // Код направления
                        """
                            to_char(federalEducationStandard.direction.consolidatedGroupOfDirections.code, 'FM09'),'.',
                            to_char(federalEducationStandard.direction.educationDegree.code, 'FM09'),'.',
                            to_char(federalEducationStandard.direction.code, 'FM09'),
                            ' ',
                        """ + // Название направления
                        """
                            federalEducationStandard.direction.name,
                            ' ',
                        """ + // Профили
                        """
                            educationProgramDirectionProfiles.directionProfile.name,
                            ' ',
                        """ +
                        // Год начала (Поиск не производится)
                        // Структурные подразделения
                        """
                            educationProgramDirectionProfiles.directionProfile.structureDepartment.name,
                            ' ',
                        """ +
                        // Версия (Поиск не производится)
                        // Статус (Поиск не производится)
                        // Идентификатор ОП
                        """
                            id)
                        """;
        FilterTokenizer tokenizer = new FilterTokenizer();
        List<FilterToken> tokens = tokenizer.tokenize(expression);
        System.out.println(tokens);
    }

}
