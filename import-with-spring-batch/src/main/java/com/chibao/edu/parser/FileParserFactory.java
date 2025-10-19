package com.chibao.edu.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FileParserFactory {

    private final List<FileParser<?>> parsers;

    @SuppressWarnings("unchecked")
    public <T> FileParser<T> getParser(String fileExtension) {
        return (FileParser<T>) parsers.stream()
                .filter(parser -> parser.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No parser found for extension: " + fileExtension));
    }
}
