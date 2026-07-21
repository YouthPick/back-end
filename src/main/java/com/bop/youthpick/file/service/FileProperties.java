package com.bop.youthpick.file.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "youthpick.file")
public record FileProperties(DataSize maxSize) {}
