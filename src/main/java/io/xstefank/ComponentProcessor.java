package io.xstefank;

import io.xstefank.model.json.Item;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ComponentProcessor {

    private final Pattern versionDatePattern = Pattern.compile(".*-(\\d+)-.*$");

    private final Map<String, String> finalVersions = new HashMap<>();

    public void process(List<Item> items) {
        items.forEach(item -> {
            if (!finalVersions.containsKey(item.group) || compareDates(item.version, finalVersions.get(item.group)) > 0) {
                finalVersions.put(item.group, item.version);
            }
        });
    }

    private int compareDates(String version1, String version2) {
        Matcher matcher1 = versionDatePattern.matcher(version1);
        Matcher matcher2 = versionDatePattern.matcher(version2);
        matcher1.matches();
        matcher2.matches();

        return Long.valueOf(matcher1.group(1)).compareTo(Long.valueOf(matcher2.group(1)));
    }

    public Map<String, String> getFinalVersions() {
        return Collections.unmodifiableMap(finalVersions);
    }
}
