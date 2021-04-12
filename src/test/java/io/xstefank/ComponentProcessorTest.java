package io.xstefank;

import io.quarkus.test.junit.QuarkusTest;
import io.xstefank.model.json.Item;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@QuarkusTest
public class ComponentProcessorTest {

    public static final String ORG_GROUP_1 = "org.group1";
    public static final String ORG_GROUP_2 = "org.group2";
    public static final String VERSION_1 = "1.0-SNAPSHOT-20210412111742-ba78ff2";
    public static final String VERSION_2 = "2.2-SNAPSHOT-20210412111846-24053bb";
    public static final String VERSION_3 = "1.0-SNAPSHOT-20230412111742-ba78ff2";

    ComponentProcessor componentProcessor;

    @BeforeEach
    public void beforeEach() {
        componentProcessor = new ComponentProcessor();
    }

    @Test
    public void oneVersionPerComponentTest() {
        componentProcessor.process(List.of(Item.of(ORG_GROUP_1, VERSION_1),
            Item.of(ORG_GROUP_2, VERSION_2)));

        Assertions.assertEquals(Map.of(ORG_GROUP_1, VERSION_1, ORG_GROUP_2, VERSION_2),
            componentProcessor.getFinalVersions(), "Unambiguous version should not be changed.");
    }

    @Test
    public void oneComponentMultipleVersionsTest() {
        componentProcessor.process(List.of(Item.of(ORG_GROUP_1, VERSION_1),
            Item.of(ORG_GROUP_1, VERSION_2)));

        Assertions.assertEquals(Map.of(ORG_GROUP_1, VERSION_2), componentProcessor.getFinalVersions(), "Processing didn't pick the latest version.");
    }

    @Test
    public void multipleComponentsMultipleVersionsTest() {
        componentProcessor.process(List.of(Item.of(ORG_GROUP_1, VERSION_1),
            Item.of(ORG_GROUP_1, VERSION_2),
            Item.of(ORG_GROUP_2, VERSION_1),
            Item.of(ORG_GROUP_2, VERSION_2)));

        Assertions.assertEquals(Map.of(ORG_GROUP_1, VERSION_2, ORG_GROUP_2, VERSION_2),
            componentProcessor.getFinalVersions(), "Processing didn't pcik the latest versions");
    }

    @Test
    public void oneComponentMultipleVersionsDateTest() {
        componentProcessor.process(List.of(Item.of(ORG_GROUP_1, VERSION_1),
            Item.of(ORG_GROUP_1, VERSION_3)));

        Assertions.assertEquals(Map.of(ORG_GROUP_1, VERSION_3), componentProcessor.getFinalVersions(), "Processing didn't pick the latest version.");
    }

    @Test
    public void multipleComponentsMultipleVersionsDateTest() {
        componentProcessor.process(List.of(Item.of(ORG_GROUP_1, VERSION_1),
            Item.of(ORG_GROUP_1, VERSION_3),
            Item.of(ORG_GROUP_2, VERSION_1),
            Item.of(ORG_GROUP_2, VERSION_3)));

        Assertions.assertEquals(Map.of(ORG_GROUP_1, VERSION_3, ORG_GROUP_2, VERSION_3),
            componentProcessor.getFinalVersions(), "Processing didn't pcik the latest versions");
    }
}
