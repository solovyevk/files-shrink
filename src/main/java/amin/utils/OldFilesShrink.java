package amin.utils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class OldFilesShrink {
  public static final Long GB_DENOMINATOR = 1024 * 1024 * 1024L;
  public static final Long MB_DENOMINATOR = 1024 * 1024L;
  public static final Long TRUNCATE_WHEN_AVAILABLE_SPACE_IS_LESS_THAN_DEFAULT = 20 * GB_DENOMINATOR;
  public static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
  final private String rootDirectoryPath;
  final private File rootDirectory;
  final private long truncateWhenAvailableSpaceIsLessThan;

  public OldFilesShrink(String rootDirectory, long truncateAtSize) {
    this.rootDirectoryPath = rootDirectory;
    this.truncateWhenAvailableSpaceIsLessThan = truncateAtSize;
    this.rootDirectory = new File(rootDirectory);
  }

  public long getRootFolderSize(List<File> allFiles, List<File> emptyDirectories) throws IOException {
    return getFolderSize(new File(rootDirectoryPath), allFiles, emptyDirectories);
  }

  private long getFolderSize(File folder, List<File> allFiles, List<File> emptyDirectories) {
    long length = 0;
    File[] files = folder.listFiles();
    assert files != null;
    for (final File file: files) {
      if (file.isFile()) {
        length += file.length();
        allFiles.add(file);
      } else {
        length += getFolderSize(file, allFiles, emptyDirectories);
        if (Objects.requireNonNull(file.listFiles()).length == 0) {
          emptyDirectories.add(file);
        }
      }
    }
    return length;
  }

  private void processFiles() throws IOException {
    final List<File> allFiles = new ArrayList<File>();
    final List<File> emptyDirectories = new ArrayList<File>();
    long totalCapacity = rootDirectory.getTotalSpace();
    long usablePartitionSpace = rootDirectory.getUsableSpace();
    long memoryToFree = this.truncateWhenAvailableSpaceIsLessThan - usablePartitionSpace;
    logInfo("=====================================================================================================");
    logInfo("Total  partition size : " + totalCapacity / GB_DENOMINATOR + " GB");
    logInfo("Usable Space : " + usablePartitionSpace / GB_DENOMINATOR + " GB");
    logInfo(String.format("Start truncate then available space is less than %d GB", this.truncateWhenAvailableSpaceIsLessThan / GB_DENOMINATOR));
    if (memoryToFree > 0) {
      long rootSize = getRootFolderSize(allFiles, emptyDirectories);
      logInfo(String.format("Total number of files in \"%s\" directory is %d", rootDirectoryPath, allFiles.size()));
      logInfo(String.format("Total number of empty directories in \"%s\" directory is %d", rootDirectoryPath, emptyDirectories.size()));
      logInfo(String.format("Size of directory \"%s\" is: %d GB", rootDirectoryPath, rootSize / MB_DENOMINATOR));
      logInfo(String.format("Need to release %d MB from %d GB in directory \"%s\"", memoryToFree / MB_DENOMINATOR, rootSize / GB_DENOMINATOR,
                            rootDirectoryPath));
      allFiles.sort(Comparator.comparingLong(File::lastModified));
      final List<File> filesToDelete = new ArrayList<File>();
      long releaseSpace = 0;
      for (final File file: allFiles) {
        filesToDelete.add(file);
        releaseSpace += file.length();
        if (releaseSpace >= memoryToFree) {
          break;
        }
      }
      logInfo(String.format("Start removing obsolete files from  \"%s\" directory", rootDirectoryPath));
      filesToDelete.parallelStream().forEach((file) -> {
        String filename = file.getName();
        if (!file.delete()) {
          logInfo("Can't delete file: " + filename);
        } else {
          logInfo("Delete file: " + filename);
        }
      });
      logInfo(String.format("Finish removing obsolete files from  \"%s\" directory, removed %d files", rootDirectoryPath,
                            filesToDelete.size()));
    } else {
      logInfo(String.format("No files need to be deleted from  \"%s\" directory", rootDirectoryPath));
    }
    if (emptyDirectories.size() > 0) {
      logInfo(String.format("Start removing empty directories from  \"%s\" directory", rootDirectoryPath));
      for (final File dir: emptyDirectories) {
        String directoryName = dir.getName();
        if (!dir.delete()) {
          logInfo("Can't delete directory: " + directoryName);
        } else {
          logInfo("Delete directory: \"" + directoryName + "\"");
        }
      }
      logInfo(String.format("Finish removing empty directories from  \"%s\", removed %d directory(s)", rootDirectoryPath,
                            emptyDirectories.size()));
    }
  }

  private static void log(String level, String message) {
    System.out.println(level + " " + LOG_DATE_FORMAT.format(new Date()) + " - " + message);
  }

  private static void logInfo(String message) {
    log("INFO", message);
  }

  private static void logWarn(String message) {
    log("WARN", message);
  }

  private static void logError(String message) {
    log("ERROR", message);
  }


  public static void main(String[] args) {
    if (args.length == 0 || args[0] == null || "".equals(args[0].trim())) {
      logError("No root directory passed as arguments. It is required");
      System.exit(-1);
    }
    String rootDir = args[0];
    long truncateWhenAvailableSpaceIsLessThanDefault = TRUNCATE_WHEN_AVAILABLE_SPACE_IS_LESS_THAN_DEFAULT;
    if (args.length == 1 || args[1] == null || "".equals(args[1].trim())) {
      logWarn(
          "No truncateWhenAvailableSpaceIsLessThan passed as arguments. Will use default size of " +
          TRUNCATE_WHEN_AVAILABLE_SPACE_IS_LESS_THAN_DEFAULT +
          " bytes");
    } else {
      String truncateWhenAvailableSpaceIsLessThanStr = args[1];
      try {
        truncateWhenAvailableSpaceIsLessThanDefault = Long.parseLong(truncateWhenAvailableSpaceIsLessThanStr) * GB_DENOMINATOR;
      } catch (NumberFormatException e) {
        logError(String.format("The truncateAtSize: \"%s\" argument is not a number, please use bytes numeric value ", truncateWhenAvailableSpaceIsLessThanStr));
        System.exit(-1);
      }
    }
    File root = new File(rootDir);
    try {
      if (root.exists() && root.isDirectory()) {
        OldFilesShrink fr = new OldFilesShrink(rootDir, truncateWhenAvailableSpaceIsLessThanDefault);
        fr.processFiles();
      } else {
        logError("The root directory parameter point to none existing directory. It should point to real directory");
        System.exit(-1);
      }
    } catch (IOException e) {
      logError("Error while processing files");
      e.printStackTrace();
    }
  }
}
