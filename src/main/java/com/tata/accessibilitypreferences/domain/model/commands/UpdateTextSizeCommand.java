package com.tata.accessibilitypreferences.domain.model.commands;

import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;

public record UpdateTextSizeCommand(String userId, TextSizeLevel textSize) {}
