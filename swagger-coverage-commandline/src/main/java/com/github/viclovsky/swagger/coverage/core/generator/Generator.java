package com.github.viclovsky.swagger.coverage.core.generator;

import com.github.viclovsky.swagger.coverage.CoverageOutputReader;
import com.github.viclovsky.swagger.coverage.FileSystemOutputReader;
import com.github.viclovsky.swagger.coverage.configuration.Configuration;
import com.github.viclovsky.swagger.coverage.configuration.ConfigurationBuilder;
import com.github.viclovsky.swagger.coverage.core.results.Results;
import com.github.viclovsky.swagger.coverage.core.results.builder.core.StatisticsBuilder;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private List<URI> specPaths = new ArrayList<>();
    private List<AuthorizationValue> specAuths;

    private Path inputPath;

    private Path configurationPath;

    private final OpenAPIParser parser = new OpenAPIParser();

    private List<StatisticsBuilder> statisticsBuilders = new ArrayList<>();

    public void run() {
        Configuration configuration = ConfigurationBuilder.build(configurationPath);
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        
        // Parse and merge multiple specs
        OpenAPI mergedSpec = null;
        for (URI specPath : specPaths) {
            LOGGER.info("Loading spec from: {}", specPath);
            SwaggerParseResult parsed = parser.readLocation(specPath.toString(), specAuths, parseOptions);
            parsed.getMessages().forEach(LOGGER::info);
            OpenAPI spec = parsed.getOpenAPI();
            
            if (spec == null) {
                LOGGER.warn("Failed to parse spec from: {}", specPath);
                continue;
            }
            
            if (mergedSpec == null) {
                mergedSpec = spec;
                LOGGER.info("Using spec from {} as base", specPath);
            } else {
                // Merge paths from this spec into the merged spec
                if (spec.getPaths() != null) {
                    if (mergedSpec.getPaths() == null) {
                        mergedSpec.setPaths(new Paths());
                    }
                    for (String path : spec.getPaths().keySet()) {
                        PathItem pathItem = spec.getPaths().get(path);
                        if (mergedSpec.getPaths().containsKey(path)) {
                            LOGGER.warn("Path {} already exists in merged spec, skipping from {}", path, specPath);
                        } else {
                            mergedSpec.getPaths().addPathItem(path, pathItem);
                            LOGGER.debug("Added path {} from {}", path, specPath);
                        }
                    }
                }
                LOGGER.info("Merged spec from {}, total paths: {}", specPath, 
                    mergedSpec.getPaths() != null ? mergedSpec.getPaths().size() : 0);
            }
        }
        
        if (mergedSpec == null) {
            throw new IllegalStateException("Failed to load any valid spec files");
        }

        LOGGER.info("Final merged spec has {} paths", 
            mergedSpec.getPaths() != null ? mergedSpec.getPaths().size() : 0);
        statisticsBuilders = configuration.getStatisticsBuilders(mergedSpec);

        CoverageOutputReader reader = new FileSystemOutputReader(getInputPath());
        reader.getOutputs().forEach(this::processFile);

        Results result = new Results();

        statisticsBuilders.stream().filter(StatisticsBuilder::isPreBuilder).forEach(
                statisticsBuilder -> statisticsBuilder.build(result, configuration));

        statisticsBuilders.stream().filter(StatisticsBuilder::isPostBuilder).forEach(
                statisticsBuilder -> statisticsBuilder.build(result, configuration));

        configuration.getConfiguredResultsWriters().forEach(writer -> writer.write(result));
    }

    public void processFile(Path path) {
        SwaggerParseResult parsed = parser.readLocation(path.toUri().toString(), null, null);
        parsed.getMessages().forEach(LOGGER::info);
        OpenAPI spec = parsed.getOpenAPI();
        statisticsBuilders.stream().filter(StatisticsBuilder::isPreBuilder).forEach(builder ->
                builder.add(path.toString()).add(spec));
    }

    public List<URI> getSpecPaths() {
        return specPaths;
    }

    public Generator setSpecPaths(List<URI> specPaths) {
        this.specPaths = specPaths;
        return this;
    }

    public List<AuthorizationValue> getSpecAuths() {
        return specAuths;
    }

    public Generator setSpecAuths(List<AuthorizationValue> specAuths) {
        this.specAuths = specAuths;
        return this;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Generator setInputPath(Path inputPath) {
        this.inputPath = inputPath;
        return this;
    }

    public Path getConfigurationPath() {
        return configurationPath;
    }

    public Generator setConfigurationPath(Path configurationPath) {
        this.configurationPath = configurationPath;
        return this;
    }
}
