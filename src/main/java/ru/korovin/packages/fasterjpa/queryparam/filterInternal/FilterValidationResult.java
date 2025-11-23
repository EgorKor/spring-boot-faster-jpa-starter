package ru.korovin.packages.fasterjpa.queryparam.filterInternal;

import ru.korovin.packages.fasterjpa.queryparam.filterInternal.parsing.tokenizing.FilterToken;

import java.util.List;

public record FilterValidationResult(boolean isValid, String message, List<String> errors, List<FilterToken> tokens) {
}
