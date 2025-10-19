package com.chibao.edu.parser;

import java.io.InputStream;
import java.util.List;

public interface FileParser<T> {
    List<T> parse(InputStream inputStream, Class<T> type);
    boolean supports(String fileExtension);
}
