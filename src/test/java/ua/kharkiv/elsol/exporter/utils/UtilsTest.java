package ua.kharkiv.elsol.exporter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UtilsTest {

  @Test
  public void shouldReturnCorrectExtension() {
    var link = "https://docs.google.com/feeds/download/documents/export/Export?id=assd&exportFormat=docx";
    String fileExtension = Utils.getFileExtension(link);
    assertEquals("docx", fileExtension);
  }

}
