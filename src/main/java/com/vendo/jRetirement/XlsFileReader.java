//XlsFileReader.java - https://poi.apache.org/apidocs/5.0/

package com.vendo.jRetirement;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.nio.file.Path;
import java.util.*;

public class XlsFileReader {
    ///////////////////////////////////////////////////////////////////////////
    public XlsFileReader() {
    }

    ///////////////////////////////////////////////////////////////////////////
    public List<DetailedHoldingsData> readDetailedHoldingsDataFromXlsFile(Path detailedHoldingsPath) {
        List<DetailedHoldingsData> records = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(detailedHoldingsPath.toFile(), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            int firstDataRow = sheet.getFirstRowNum() + 2; //skip two rows at top
            int lastDataRow = sheet.getLastRowNum() - 1; //skip one row at bottom

            for (Row row : sheet) {
                int rowNumInFile = row.getRowNum();
                if (rowNumInFile < firstDataRow || rowNumInFile > lastDataRow) {
                    continue;
                }

                int cellIndex = 0;
                String symbol = row.getCell(cellIndex++).getStringCellValue();
                String description = row.getCell(cellIndex++).getStringCellValue();
                String account = row.getCell(cellIndex++).getStringCellValue();
                String investmentType = row.getCell(cellIndex++).getStringCellValue();
                String morningstarCategory = row.getCell(cellIndex++).getStringCellValue();
                String stockStyleCategory = row.getCell(cellIndex++).getStringCellValue();
                String bondStyleCategory = row.getCell(cellIndex++).getStringCellValue();

                DetailedHoldingsData record = new DetailedHoldingsData(rowNumInFile, symbol, description, account, investmentType, morningstarCategory, stockStyleCategory, bondStyleCategory);
                records.add(record);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return records;
    }
}

/* keep for now
    ///////////////////////////////////////////////////////////////////////////
    public void readDetailedHoldingsDebug(Path detailedHoldingsPath) {
        try (Workbook workbook = WorkbookFactory.create(detailedHoldingsPath.toFile())) {

            // Get the first sheet (0-indexed)
            Sheet sheet = workbook.getSheetAt(0);

            // Iterate over all rows
            for (Row row : sheet) {
                // Iterate over all cells in the row
                for (Cell cell : row) {

                    // Evaluate cell type to extract data properly
                    switch (cell.getCellType()) {
                        case STRING:
                            System.out.print("STRING:" + cell.getStringCellValue() + "\t");
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                System.out.print("DATE:" + cell.getDateCellValue() + "\t");
                            } else {
                                System.out.print("NUMERIC:" + cell.getNumericCellValue() + "\t");
                            }
                            break;
                        case BOOLEAN:
                            System.out.print("BOOLEAN:" + cell.getBooleanCellValue() + "\t");
                            break;
                        case FORMULA:
                            System.out.print("FORMULA:" + cell.getCellFormula() + "\t");
                            break;
                        default:
                            System.out.print("[UNKNOWN]\t");
                    }
                }
                System.out.println(); // Move to next row
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        int bh = 1;
    }
}
*/
