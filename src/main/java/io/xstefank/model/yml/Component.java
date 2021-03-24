package io.xstefank.model.yml;

public class Component {

    public final String groupId;
    public final String version;

    private Component(String groupId, String version) {
        this.groupId = groupId;
        this.version = version;
    }

    public static Component of(String groupId, String version) {
        return new Component(groupId, version);
    }
}
