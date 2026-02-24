package ru.korovin.packages.fasterjpa.queryparam.filter_internal;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.parsing.tokenizing.FilterToken;

import java.util.List;

public record FilterValidationResult(boolean isValid, String message, List<String> errors, List<FilterToken> tokens) {
}
