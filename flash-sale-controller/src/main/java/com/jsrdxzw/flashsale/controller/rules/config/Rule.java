package com.jsrdxzw.flashsale.controller.rules.config;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Rule {
    private boolean enable = true;
    private String path;
    private int windowPeriod;
    private int windowSize;
}
