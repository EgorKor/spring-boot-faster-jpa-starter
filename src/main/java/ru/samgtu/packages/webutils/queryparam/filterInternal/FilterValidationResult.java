package ru.samgtu.packages.webutils.queryparam.filterInternal;

import java.util.List;

public record FilterValidationResult(boolean isValid, String message, List<String> errors, List<FilterToken> tokens) {
}
