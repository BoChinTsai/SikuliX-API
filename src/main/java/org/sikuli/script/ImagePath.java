package org.sikuli.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sikuli.basics.Debug;
import org.sikuli.basics.FileManager;
import org.sikuli.basics.Settings;

public class ImagePath {
  
  private static String me = "ImagePath";
  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "", me + ": " + message, args);
  }
  
  public static class PathEntry {
    public URL pathURL;
    public String pathGiven;

    public PathEntry(String g, URL p) {
      pathGiven = g;
      pathURL = p;
    }
  } 
  
  private static List<PathEntry> imagePaths = Collections.synchronizedList(new ArrayList<PathEntry>());
  private static String BundlePath = null;

  static {
    imagePaths.add(null);
    BundlePath = Settings.BundlePath;
  }

  /**
   * get the list of path entries
   * @return
   */
  public static List<PathEntry> getPaths() {
    return imagePaths;
  }
  
  /**
   * print the list of path entries
   * @return
   */
  public static void printPaths() {
    log(0, "ImagePath has %d entries", imagePaths.size());
    log(lvl, "start of list ----------------------------");
    for (PathEntry path : imagePaths) {
      if (path == null) {
        log(lvl, "Path: NULL");
      } else {
        log(lvl, "Path: given: %s\nis: %s", path.pathGiven, path.pathURL.toString());
      }
    }
    log(lvl, "end of list ----------------------------");
  }
  
  public static URL find(String fname) {
    URL fURL = null;
    fname = FileManager.slashify(fname, false);
    if (new File(fname).isAbsolute()) {
      if (new File(fname).exists()) {
        fURL = FileManager.makeURL(fname);
      } else {
        log(-1, "FatalError: not locatable: " + fname);
      }
    } else {
      for (PathEntry path : getPaths()) {
        if (path == null) {
          continue;
        }
        if ("file".equals(path.pathURL.getProtocol())) {
          fURL = FileManager.makeURL(path.pathURL, fname);
          if (new File(fURL.getPath()).exists()) {
            break;
          }
        } else if ("jar".equals(path.pathURL.getProtocol())) {
          fURL = FileManager.getURLForContentFromURL(path.pathURL, fname);
          if (fURL != null) {
            break;
          }
        }
      }
      if (fURL == null) {
        log(-1, "not found on image path: " + fname);
        printPaths();
      }
    }
    return fURL;
  }
  
  public static BufferedReader open(String fname) {
    log(lvl, "open: " + fname);
    BufferedReader br = null;
    URL furl = find(fname);
    if (furl != null) {
      try {
        br = new BufferedReader(new InputStreamReader(furl.openStream()));
      } catch (Exception ex) {
        log(-1, "open: %s", ex.getMessage());
        return null;
      } 
      try {
        br.mark(10);
        if (br.read() < 0) {
          br.close();
          return null;
        }
        br.reset();
      } catch (Exception ex) {
        log(-1, "open: %s", ex.getMessage());
        try {
            br.close();
        } catch (Exception ex1) {
          log(-1, "open: %s", ex1.getMessage());
          return null;
        } 
        return null;
      } 
    }
    return br;
  }
  
  /**
   * Set the primary image path to the top folder level of a jar based on the given class name (must
   * be found on class path). When not running from a jar (e.g. running in some IDE) the path will be the
   * path to the compiled classes (for Maven based projects this is target/classes that contains all
   * stuff copied from src/main/resources automatically)<br />
   * this is the same as setJarImagePath(klassName, null)
   *
   * @param klassName fully qualified (canonical) class Name
   */
  public static boolean add(String mainPath) {
    return add(mainPath, null);
  }

  /**
   * Set the primary image path to the top folder level of a jar based on the given class name (must
   * be found on class path). When not running from a jar (e.g. running in some IDE) the path will be the
   * path to the compiled classes (for Maven based projects this is target/classes that contains all
   * stuff copied from src/main/resources automatically)<br />
   * this is the same as setJarImagePath(klassName, null)
   *
   * @param klassName fully qualified (canonical) class Name
   * @param altPath alternative image folder, when not running from jar (absolute path) 
   */
  public static boolean add(String mainPath, String altPath) {
    PathEntry path = makePathURL(mainPath, altPath);
    if (path != null) {
      log(lvl, "addImagePath: %s", path.pathURL);
      imagePaths.add(path);
      return true;
    } else {
      log(-1, "addImagePath: not valid: %s", mainPath);      
      return false;
    }
  }

  /**
   * add entry to end of list
   * 
   * @param pURL
   * @return
   */
  public static boolean add(URL pURL) {
    imagePaths.add(new PathEntry("__PATH_URL__", pURL));
    return true;
  }
  
  /**
   * remove entry with given path
   * 
   * @param path
   * @return true on success, false ozherwise
   */
  public static boolean remove(String path) {
    for (PathEntry p : imagePaths) {
      if (!p.pathGiven.equals(path)) {
        continue;
      }
      imagePaths.remove(p);
      return true;
    }
    return false;
  }
  
  /**
   * remove entry with given URL
   * 
   * @param pURL
   * @return true on success, false ozherwise
   */
  public static boolean remove(URL pURL) {
    for (PathEntry p : imagePaths) {
      if (!p.pathGiven.equals("__PATH_URL__")) {
        continue;
      }
      if (!p.pathURL.toString().equals(pURL.toString())) {
        continue;
      }
      imagePaths.remove(p);
      return true;
    }
    return false;
  }
  
  /**
   * empty list and add given path
   * 
   * @param path
   * @return true on success, false ozherwise
   */
  public static boolean reset(String path) {
    reset();
    return add(path);
  }
  
  /**
   * empty list and add given path
   * 
   * @return true
   */
  public static boolean reset() {
    log(lvl, "reset");
    imagePaths.clear();
    imagePaths.add(null);
    return true;
  }
  
  
	/**
	 * the given path is added to the list replacing the first entry and
	 * Settings.BundlePath is replaced as well
	 *
	 * @param bundlePath a file path string relative or absolute
   * @return true on success, false ozherwise
	 */
  public static boolean setBundlePath(String bundlePath) {
    if (bundlePath != null && !bundlePath.isEmpty()) {
      PathEntry path = makePathURL(bundlePath, null);
      if (path != null && "file".equals(path.pathURL.getProtocol())) {
        if (new File(new File(path.pathURL.getPath()).getAbsolutePath()).exists()) {
          imagePaths.set(0, path);
          Settings.BundlePath = path.pathURL.getPath();
          BundlePath = Settings.BundlePath;
          log(3, "new BundlePath: " + Settings.BundlePath);
          return true;
        }
      }
    }
    log(-1, "setBundlePath: Settings not changed: invalid BundlePath: " + bundlePath);
    return false;
  }

  
  /**
   * the resetting version of setBundlePath for IDE usage
   * @param bundlePath
   * @return
   */
  public static boolean resetBundlePath(String bundlePath) {
    reset();
    return setBundlePath(bundlePath);
  }

	/**
	 *
	 * @return the current bundle path or null if invalid
	 */
	public static String getBundlePath() {
    if (imagePaths.get(0) == null) {
      setBundlePath(Settings.BundlePath);
      return BundlePath;
    }
    String path = imagePaths.get(0).pathURL.getPath();
    if (Settings.BundlePath != null && path.equals(Settings.BundlePath)) {
      return BundlePath;
    } else {
      log(-1, "getBundlePath: Settings.BundlePath is invalid: returning working dir\n" +
              "Settings.BundlePath: %s\nImagePaths[0]: %s", 
              Settings.BundlePath, imagePaths.get(0).pathURL.getPath());
      return new File("").getAbsolutePath();
    }
	}
  
  private static PathEntry makePathURL(String mainPath, String altPath) {
    if (new File(mainPath).isAbsolute()) {
      if(new File(mainPath).exists()) {
        mainPath = FileManager.slashify(mainPath, true);
        return new PathEntry(mainPath, FileManager.makeURL(mainPath));
      } else {
        return null;
      }
    }
    Class cls = null;
    String klassName = null;;
    String subPath = "";
    URL pathURL = null;
    int n = mainPath.indexOf("/");
    if (n > 0) {
      klassName = mainPath.substring(0, n);
      if (n < mainPath.length() - 2) {
        subPath = mainPath.substring(n+1);
      }
    } else {
      klassName = mainPath;
    }
    try {
      cls = Class.forName(klassName);
    } catch (ClassNotFoundException ex) { }
    if (cls != null) {
      CodeSource codeSrc = cls.getProtectionDomain().getCodeSource();
      if (codeSrc != null && codeSrc.getLocation() != null) {
        URL jarURL = codeSrc.getLocation();
        if (jarURL.getPath().endsWith(".jar")) {
            pathURL = FileManager.makeURL(FileManager.slashify(jarURL.toString() + "!/" + subPath, true), "jar");
        } else {
          if (altPath == null) {
            altPath = jarURL.getPath();
          }
          if (new File(altPath, subPath).exists()) {
            pathURL = FileManager.makeURL(FileManager.slashify(new File(altPath, subPath).getAbsolutePath(), true));
          }
        }
      }
    } else {
      if (new File(mainPath).exists()) {
        pathURL = FileManager.makeURL(new File(mainPath).getAbsolutePath());
      }
    }
    if (pathURL != null) {
      return new PathEntry(mainPath, pathURL);
    } else {
      return null;
    }
  }
}
