package org.gardner.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class serves two main purposes:
 *  - It abstracts away a lot of file manipulation code so that I can use it in a number of
 *    different places (like reading in a list of Integers from a file, or whatever).
 *  - It serves as an overridable interface between my code and the file system, allowing for the
 *    use of a fake file system during testing.  So when I'm trying to make my code testable, I
 *    tend to use this class instead of using java.io directly.
 *
 * It might make sense to split these two purposes out into separate classes, but that's not a big
 * deal to me right now, so they will stay as they are.  There's also some overlap - if this were
 * two classes, the one that does file manipulation would need to call the file system interface.
 */
public class FileUtil {

  // These logEvery methods fit here, for now, because I only ever use them when I'm parsing
  // through a really long file and want to see progress updates as I go.
  public void logEvery(int logFrequency, int current) {
    logEvery(logFrequency, current, Integer.toString(current));
  }

  public void logEvery(int logFrequency, int current, String toLog) {
    if (current % logFrequency == 0) System.out.println(toLog);
  }

  /**
   * Attempts to create the directory dirName, and exits if the directory already exists.
   */
  public void mkdirOrDie(String dirName) {
    if (!dirName.endsWith("/")) {
      dirName += "/";
    }
    if (new File(dirName).exists()) {
      System.out.println("Out directory already exists! Exiting...");
      System.exit(-1);
    }
    new File(dirName).mkdirs();
  }

  public List<String> listDirectoryContents(String filename) throws IOException {
    File[] contents = new File(filename).listFiles();
    List<String> list = Lists.newArrayList();
    if (contents != null) {
      for (File file : contents) {
        list.add(file.getName());
      }
    }
    return list;
  }

  public void touchFile(String filename) throws IOException {
    new File(filename).createNewFile();
  }

  public void deleteFile(String filename) throws IOException {
    new File(filename).delete();
  }

  public FileWriter getFileWriter(String filename) throws IOException {
    return getFileWriter(filename, false);
  }

  public FileWriter getFileWriter(String filename, boolean append) throws IOException {
    return new FileWriter(filename, append);
  }

  public void writeLinesToFile(String filename, List<String> lines) throws IOException {
    FileWriter writer = getFileWriter(filename);
    for (String line : lines) {
      writer.write(line);
      writer.write("\n");
    }
    writer.close();
  }

  public BufferedReader getBufferedReader(String filename) throws IOException {
    return new BufferedReader(new FileReader(filename));
  }

  public BufferedReader getBufferedReader(File file) throws IOException {
    return new BufferedReader(new FileReader(file));
  }

  /**
   * Calls new File(dirName).mkdirs().
   */
  public void mkdirs(String dirName) {
    new File(dirName).mkdirs();
  }

  public String addDirectorySeparatorIfNecessary(String dirName) {
    if (dirName.endsWith(File.separator)) return dirName;
    return dirName + File.separator;
  }

  public List<String> readLinesFromReader(BufferedReader reader) throws IOException {
    List<String> lines = Lists.newArrayList();
    String line;
    while ((line = reader.readLine()) != null) lines.add(line);
    reader.close();
    return lines;
  }

  public List<String> readLinesFromFile(String filename) throws IOException {
    return readLinesFromReader(getBufferedReader(filename));
  }

  public List<String> readLinesFromFile(File file) throws IOException {
    return readLinesFromReader(getBufferedReader(file));
  }

  public Map<String, String> readMapFromTsvFile(String filename) throws IOException {
    return readMapFromTsvFile(filename, false);
  }

  public Map<String, String> readMapFromTsvFile(String filename, boolean skipErrors) throws IOException {
    return readMapFromTsvReader(getBufferedReader(filename), skipErrors);
  }

  public Map<String, String> readMapFromTsvReader(BufferedReader reader, boolean skipErrors) throws IOException {
    Map<String, String> map = Maps.newHashMap();
    String line;
    while ((line = reader.readLine()) != null) {
      String[] fields = line.split("\t");
      if (fields.length != 2) {
        if (skipErrors) continue;
        System.out.println("Offending line: " + line);
        throw new RuntimeException(
            "readMapFromTsvReader called on file that didn't have two columns");
      }
      map.put(fields[0], fields[1]);
    }
    return map;
  }

  public Map<String, List<String>> readMapListFromTsvFile(String filename) throws IOException {
    return readMapListFromTsvReader(getBufferedReader(filename));
  }

  public Map<String, List<String>> readMapListFromTsvFile(String filename,
                                                          int keyIndex,
                                                          boolean overwrite,
                                                          LineFilter filter) throws IOException {
    return readMapListFromTsvReader(getBufferedReader(filename),
                                    keyIndex,
                                    overwrite,
                                    filter);
  }

  public Map<String, List<String>> readMapListFromTsvReader(BufferedReader reader) throws IOException {
    return readMapListFromTsvReader(reader, 0, false, null);
  }

  /**
   * Reads a tab-separated file and puts the contents into a map.
   *
   * We give a few options:
   * - You can set the index for the key to the map.  If the key is not zero, we only add the first
   *   column as a value to this map (and so setting overwrite to true in this case doesn't make a
   *   whole lot of sense - just use readMapFromTsv instead).
   * - If overwrite is true, we don't bother checking to see if the key is already in the map.
   *   This will speed up the processing if you know that your file only has one line per unique
   *   key.
   * - You can provide a LineFilter object that wlil be called with each line to determine if it
   *   should be skipped.
   */
  public Map<String, List<String>> readMapListFromTsvReader(BufferedReader reader,
                                                            int keyIndex,
                                                            boolean overwrite,
                                                            LineFilter filter) throws IOException {
    Map<String, List<String>> map = Maps.newHashMap();
    String line;
    while ((line = reader.readLine()) != null) {
      String[] fields = line.split("\t");
      if (filter != null && filter.filter(fields)) continue;
      String key = fields[keyIndex];
      List<String> list;
      if (overwrite) {
        list = Lists.newArrayList();
        map.put(key, list);
      } else {
        list = map.get(key);
        if (list == null) {
          list = Lists.newArrayList();
          map.put(key, list);
        }
      }
      if (keyIndex == 0) {
        for (int i = 1; i < fields.length; i++) {
          list.add(fields[i]);
        }
      } else {
        list.add(fields[0]);
      }
    }
    return map;
  }

  public List<Double> readDoubleListFromFile(String filename) throws IOException {
    List<Double> doubles = Lists.newArrayList();
    BufferedReader reader = getBufferedReader(filename);
    String line;
    while ((line = reader.readLine()) != null) {
      doubles.add(Double.parseDouble(line));
    }
    reader.close();
    return doubles;
  }

  public interface LineFilter {
    public boolean filter(String[] fields);
  }

  public boolean fileExists(String path) {
    return new File(path).exists();
  }

  /**
   * Copies the lines in reader to writer.  Does not close writer.
   */
  public void copyLines(BufferedReader reader, FileWriter writer) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      writer.write(line + "\n");
    }
  }

  public void blockOnFileDeletion(String filename) throws IOException {
    if (!new File(filename).exists()) return;
    System.out.println("Waiting for file " + filename + " to be deleted");
    WatchService watchService = FileSystems.getDefault().newWatchService();
    Path parent = Paths.get(filename).getParent();
    WatchKey watchKey = parent.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
    try {
      WatchKey key = watchService.take();
      for (WatchEvent<?> event : key.pollEvents()) {
        if (filename.endsWith(event.context().toString())) return;
      }
    } catch (InterruptedException e) { }
    return;
  }

  public void copy(String from, String to) throws IOException {
    Files.copy(new File(from).toPath(), new File(to).toPath());
  }
}
