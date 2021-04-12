package io.xstefank.model.json;

public class Item {

    public String group;
    public String name;
    public String version;

    public static Item of(String group, String version) {
        Item item = new Item();
        item.group = group;
        item.version = version;
        return item;
    }
}
