package com.chibao.edu.workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OpenWorkBook {
    public static void main(String[] args) {
        File file = new File("example.xlsx");
        try (FileInputStream fileInputStream = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);) {
            if(file.isFile() && file.exists()) {
                System.out.println("example.xlsx file open successfully.");
            } else {
                System.out.println("Error to open openworkbook.xlsx file.");
            }
        } catch (IOException e) {
            System.out.println("Error to open openworkbook.xlsx file." + e.getMessage());
        }
    }
}
