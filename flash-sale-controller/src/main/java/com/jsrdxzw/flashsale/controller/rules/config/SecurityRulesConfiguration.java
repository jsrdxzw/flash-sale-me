package com.jsrdxzw.flashsale.controller.rules.config;

import com.jsrdxzw.flashsale.config.YamlPropertySourceFactory;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@Data
@Accessors(chain = true)
@PropertySource(value = "classpath:security-rules.yml", factory = YamlPropertySourceFactory.class)
@Component
@ConfigurationProperties(prefix = "rules")
public class SecurityRulesConfiguration {
    private boolean enable;

    private Rule ipRule;
    private PathRule pathRule;
    private Rule accountRule;

    public Rule getIpRule() {
        if (ipRule == null || !ipRule.isEnable() || !this.isEnable()) {
            return new Rule().setEnable(false);
        }
        return new Rule()
                .setEnable(ipRule.isEnable())
                .setWindowPeriod(ipRule.getWindowPeriod())
                .setWindowSize(ipRule.getWindowSize());
    }

    public Rule getPathRule(String path) {
        if (!StringUtils.hasText(path) || pathRule == null || CollectionUtils.isEmpty(pathRule.getUrlPaths())) {
            return new Rule().setEnable(false);
        }
        if (!this.isEnable() || !pathRule.isEnable()) {
            return new Rule().setEnable(false);
        }
        for (Rule rule : pathRule.getUrlPaths()) {
            if (!StringUtils.hasText(rule.getPath())) {
                continue;
            }
            if (isPathMatchTemplate(rule.getPath(), path)) {
                return new Rule()
                        .setEnable(rule.isEnable())
                        .setPath(rule.getPath())
                        .setWindowPeriod(rule.getWindowPeriod() == 0 ? pathRule.getWindowPeriod() : rule.getWindowPeriod())
                        .setWindowSize(rule.getWindowSize() == 0 ? pathRule.getWindowSize() : rule.getWindowSize());
            }
        }
        return new Rule().setEnable(false);
    }

    public Rule getAccountRule() {
        if (accountRule == null || !accountRule.isEnable() || !this.isEnable()) {
            return new Rule().setEnable(false);
        }
        return new Rule().setEnable(accountRule.isEnable());
    }

    private boolean isPathMatchTemplate(String uriTemplate, String path) {
        PathPatternParser parser = new PathPatternParser();
        parser.setMatchOptionalTrailingSeparator(true);
        PathPattern pathPattern = parser.parse(uriTemplate);
        PathContainer pathContainer = toPathContainer(path);
        return pathPattern.matches(pathContainer);
    }

    private PathContainer toPathContainer(String path) {
        if (path == null) {
            return null;
        }
        return PathContainer.parsePath(path);
    }
}
