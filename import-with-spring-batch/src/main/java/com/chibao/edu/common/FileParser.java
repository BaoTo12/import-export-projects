package com.chibao.edu.common;

import java.io.IOException;

public interface FileParser {
    ParseResult parse(String filePath) throws IOException;
    boolean supports(String filename);
}
