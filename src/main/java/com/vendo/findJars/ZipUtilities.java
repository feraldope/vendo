//ZipUtilities.java

package com.vendo.findJars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtilities {
  public static final int DEFAULT_BUFFER_SIZE;

  static {
    DEFAULT_BUFFER_SIZE = 2048;
  }

  private ZipUtilities() {
  }

  public static void write(Map<String, byte[]> entries, OutputStream outputStream) {
    try {
      final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        final ZipEntry nextEntry = new ZipEntry(entry.getKey());
        writeEntry(zipOutputStream, entry, nextEntry);
      }
      finalizeZip(zipOutputStream);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void finalizeZip(ZipOutputStream zipOutputStream) throws IOException {
    zipOutputStream.flush();
    zipOutputStream.finish();
    zipOutputStream.close();
  }

  private static void writeEntry(ZipOutputStream zipOutputStream, Map.Entry<String, byte[]> entry, ZipEntry nextEntry)
      throws IOException {
    final byte[] contents = entry.getValue();
    nextEntry.setSize(contents.length);
    zipOutputStream.putNextEntry(nextEntry);
    zipOutputStream.write(contents, 0, contents.length);
    zipOutputStream.flush();
    zipOutputStream.closeEntry();
  }

  public static Map<String, byte[]> read(InputStream inputStream) {
    try {
      final ZipInputStream zipInputStream = new ZipInputStream(inputStream);
      final Map<String, byte[]> result = new HashMap<String, byte[]>();
      while (readEntry(zipInputStream, result));
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean readEntry(ZipInputStream zipInputStream, Map<String, byte[]> result) throws IOException {
    final ZipEntry entry = zipInputStream.getNextEntry();
    if (entry != null) {
      result.put(entry.getName(), readUnconditionally(new ByteArrayOutputStream(), zipInputStream));
      return true;
    }
    return false;
  }

  private static byte[] readUnconditionally(ByteArrayOutputStream buf, ZipInputStream zipInputStream) throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    int read = 0;
    while ((read = zipInputStream.read(buffer)) > 0) {
      buf.write(buffer, 0, read);
    }
    return buf.toByteArray();
  }
}
