package org.qbicc.plugin.native_;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public record YamlMemberInfo(String name, int offset, String type) {
    public YamlMemberInfo {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
    }
}
