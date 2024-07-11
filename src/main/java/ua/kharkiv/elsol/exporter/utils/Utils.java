package ua.kharkiv.elsol.exporter.utils;

public final class Utils {

  private Utils() {
  }

  public static String getFileExtension(String exportLink) {
    String[] splittedParts = exportLink.split("=");
    return splittedParts[splittedParts.length - 1];
  }

}
