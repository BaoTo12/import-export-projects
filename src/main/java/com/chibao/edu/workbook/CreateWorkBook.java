package com.chibao.edu.workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

public class CreateWorkBook {
    public static void main(String[] args) {
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(new File("exmaple.xlsx"))) {
            xssfWorkbook.write(out);
            System.out.println("example.xlsx written successfully");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
