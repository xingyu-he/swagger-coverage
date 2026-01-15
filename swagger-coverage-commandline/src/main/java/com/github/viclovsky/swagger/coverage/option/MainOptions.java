package com.github.viclovsky.swagger.coverage.option;

import com.beust.jcommander.Parameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public class MainOptions {

    @Parameter(
            names = {"-s", "--spec"},
            description = "Path to local or URL to remote swagger specification. Can be specified multiple times, or use comma-separated values (e.g., -s spec1.yaml,spec2.yaml or -s spec1.yaml -s spec2.yaml).",
            required = true,
            order = 0
    )
    private List<String> specPathStrings = new ArrayList<>();

    @Parameter(
            names = {"-i", "--input"},
            description = "Path to folder with generated files with coverage.",
            required = true,
            order = 1
    )
    private Path inputPath;

    @Parameter(
            names = {"-c", "--configuration"},
            description = "Path to file with report configuration.",
            order = 1
    )
    private Path configuration;

    @Parameter(
            names = "--help",
            description = "Print commandline help.",
            help = true,
            order = 5
    )
    private boolean help;

    public boolean isHelp() {
        return help;
    }

    public List<URI> getSpecPaths() {
        // Parse and expand comma-separated values
        List<URI> result = new ArrayList<>();
        for (String pathString : specPathStrings) {
            // Split by comma and trim whitespace
            String[] paths = pathString.split(",");
            for (String path : paths) {
                String trimmed = path.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        result.add(new URI(trimmed));
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("Invalid URI: " + trimmed, e);
                    }
                }
            }
        }
        return result;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Path getConfiguration() {
        return configuration;
    }
}
