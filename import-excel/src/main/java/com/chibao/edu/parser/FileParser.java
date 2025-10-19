package com.chibao.edu.parser;

import com.chibao.edu.common.ParseResult;

import java.io.IOException;

public interface FileParser {
    ParseResult parse(String filePath) throws IOException;
    boolean supports(String filename);
}