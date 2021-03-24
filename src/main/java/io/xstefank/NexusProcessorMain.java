package io.xstefank;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.xstefank.model.json.Components;
import io.xstefank.model.yml.Component;
import io.xstefank.model.yml.ComponentVersions;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@QuarkusMain
@CommandLine.Command(name = "nexus-component-processor", mixinStandardHelpOptions = true, version = "1.0.0")
public class NexusProcessorMain implements Callable<Integer>, QuarkusApplication {

    private final Logger logger = Logger.getLogger(NexusProcessorMain.class);
    private final Pattern versionDatePattern = Pattern.compile(".*-(\\d+)$");

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Option(names = {"-l", "--nexus-url"}, description = "The URL of the nexus to query", required = true)
    URI nexusUrl;

    @CommandLine.Option(names = {"-r", "--repository"}, description = "The name of the nexus repository containing the components", required = true)
    String repository;

    @CommandLine.Option(names = {"-c", "--credentials"}, description = "The 'username:password' for the nexus", required = true)
    String credentials;

    @CommandLine.Option(names = {"-o", "--output"}, description = "The output file override")
    String output;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

    @Override
    public Integer call() throws IOException {
        logger.tracef("Processing components from %s for the repository %s", nexusUrl, repository);

        if (credentials == null) {
            logger.info("No credentials provided. Assuming they are not needed.");
        }

        Client client = null;

        try {
            client = ClientBuilder.newClient();
            Response response;

            final Map<String, String> finalVersions = new HashMap<>();
            Components components = new Components();

            do {
                WebTarget target = client.target(nexusUrl)
                    .path("/service/rest/v1/components")
                    .queryParam("repository", repository);

                if (components.continuationToken != null) {
                    target = target.queryParam("continuationToken", components.continuationToken);
                }

                Invocation.Builder request = target.request()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

                if (credentials != null) {
                    request.header(HttpHeaders.AUTHORIZATION,
                        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
                }

                response = request.get();

                if (response.getStatus() != 200) {
                    System.err.println("Invalid status returned from nexus " + response.getStatus());
                    return 1;
                }

                components = response.readEntity(Components.class);

                components.items.forEach(item -> {
                    if (!finalVersions.containsKey(item.group) || compareDates(item.version, finalVersions.get(item.group)) > 0) {
                        finalVersions.put(item.group, item.version);
                    }
                });

            } while (components.continuationToken != null);

            ComponentVersions outputComponentVersions = new ComponentVersions();

            finalVersions.forEach((groupId, version) -> {
                outputComponentVersions.components.add(Component.of(groupId, version));
            });

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            File resultFile = Paths.get(output == null ? "component-versions.yml" : output).toFile();
            System.out.println("Writing the output to " + resultFile.getCanonicalPath());
            mapper.writer(new DefaultPrettyPrinter()).writeValue(resultFile, outputComponentVersions);

        } finally {
            if (client != null) {
                client.close();
            }
        }

        return 0;
    }

    private int compareDates(String version1, String version2) {
        Matcher matcher1 = versionDatePattern.matcher(version1);
        Matcher matcher2 = versionDatePattern.matcher(version2);
        matcher1.matches();
        matcher2.matches();

        return Long.valueOf(matcher1.group(1)).compareTo(Long.valueOf(matcher2.group(1)));
    }
}
