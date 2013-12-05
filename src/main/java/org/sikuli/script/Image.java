/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added RaiMan 2013
 */
package org.sikuli.script;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.sikuli.basics.Debug;
import org.sikuli.basics.FileManager;
import org.sikuli.basics.Settings;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
//import org.sikuli.natives.Mat;
import org.sikuli.natives.Vision;

/**
 * This class hides the complexity behind image names given as string.<br />
 * Its companion is ImagePath that maintains a list of places, where images are stored.<br />
 * Another companion is ImageGroup allowing to to look at images in a folder as a group.<br />
 * An Image object:<br />
 * - has a name, either given or taken from the basename without ending.<br />
 * - keeps its in memory buffered image in a configurable cache avoiding reload from source<br />
 * - remembers, where it was found the last time searched<br />
 * - can be sourced from the filesystem, from jars, from the web and from other in memory images <br />
 * - it will have features for basic image manipulation <br />
 * - it contains the stuff to communicate with the underlying OpenCV based search engine <br />
 *
 * This class maintains<br />
 * - a list of all images loaded with there source reference and a ref to the image object<br />
 * - a list of all images currently storing their in memory buffered image (managed as a cache)<br />
 *
 */
public class Image {
  
  static {
    FileManager.loadLibrary("opencv_java247");
  }

  private static String me = "Image";
  private static String mem = "";
  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "", me + ": " + message, args);
  }

  private static List<Image> images = Collections.synchronizedList(new ArrayList<Image>());
  private static List<Image> purgeList = Collections.synchronizedList(new ArrayList<Image>());
  private static Map<URL, Image> imageFiles = Collections.synchronizedMap(new HashMap<URL, Image>());
  private static int KB = 1024;
  private static int MB = KB * KB;
  private static int maxMemory = 64 * MB;
  private static int currentMemory;
  private static String imageFromJar = "__FROM_JAR__";
  private final static String isBImg = "__BufferedImage__";
  private String imageName = null;;
  private boolean imageIsText = false;
  private boolean imageIsAbsolute = false;
  private String filepath = null;
  private URL fileURL = null;
  private BufferedImage bimg = null;
  private long bsize;
  private int bwidth;
  private int bheight;
  private Rectangle lastSeen = null;

  @Override
  public String toString() {
    return (imageName != null ? imageName : "__UNKNOWN__");
  }

  public static Image create(String imgName) {
    Image img = get(imgName);
    if (img == null) {
      img = new Image(imgName);
    }
    return createImageValidate(img);
  }

  public static Image create(URL url) {
    Image img = get(url);
    if (img == null) {
      img = new Image(url);
    }
    return createImageValidate(img);
  }

  private static Image createImageValidate(Image img) {
    if (!img.isValid()) {
      if (Settings.OcrTextSearch) {
        img.setIsText(true);
      } else {
        log(-1, "Image not valid, but TextSearch is switched off!");
      }
    }
    return img;
  }

  protected static Image get(URL imgURL) {
    return imageFiles.get(imgURL);
  }

  protected static Image get(String fname) {
    if (!fname.endsWith(".png")) {
      fname += ".png";
    }
    URL fURL = ImagePath.find(fname);
    if (fURL != null) {
      return imageFiles.get(fURL);
    } else {
      return null;
    }
  }

  private Image() {
  }

  private Image(String fname) {
    init(fname);
  }

  private void init(String fname) {
    imageName = fname;
    if (!fname.endsWith(".png")) {
      fname += ".png";
    }
    fname = FileManager.slashify(fname, false);
    if (new File(fname).isAbsolute()) {
      if (new File(fname).exists()) {
        String bundlePath = ImagePath.getBundlePath();
        if (bundlePath != null && fname.startsWith(bundlePath)) {
          imageName = new File(fname).getName();
        } else {
          imageIsAbsolute = true;
        }
        filepath = fname;
        fileURL = FileManager.makeURL(fname);
       } else {
        log(-1, "FatalError: not locatable: " + fname);
      }
    } else {
      for (ImagePath.PathEntry path : ImagePath.getPaths()) {
        if (path == null) {
          continue;
        }
        if ("jar".equals(path.pathURL.getProtocol())) {
          fileURL = FileManager.getURLForContentFromURL(path.pathURL, fname);
          if (fileURL != null) {
            filepath = imageFromJar;
            imageName = getNameFromURL(fileURL);
            break;
          }
        } else if ("file".equals(path.pathURL.getProtocol())) {
          fileURL = FileManager.makeURL(path.pathURL, fname);
          if (new File(fileURL.getPath()).exists()) {
            filepath = fileURL.getPath();
            break;
          }
        }
      }
      if (filepath == null) {
        log(-1, "not found on image path: " + fname);
        ImagePath.printPaths();
      }
    }
    loadImage();
  }

  private Image(URL fURL) {
    if ("file".equals(fURL.getProtocol())) {
      init(fURL.getPath());
    } else if ("jar".equals(fURL.getProtocol())) {
      imageName = getNameFromURL(fURL);
      fileURL = fURL;
      filepath = imageFromJar;
      loadImage();
    } else if (fURL.getProtocol().startsWith("http")) {
      log(-1, "FatalError: Image from http(s) not supported: " + fURL);

    } else {
      log(-1, "FatalError: ImageURL not supported: " + fURL);
    }
    fileURL = fURL;
  }

  private static String getNameFromURL(URL fURL) {
//TODO add handling for http
    if ("jar".equals(fURL.getProtocol())) {
      int n = fURL.getPath().lastIndexOf(".jar!/");
      int k = fURL.getPath().substring(0, n).lastIndexOf("/");
      if (n > -1) {
        return "JAR:" + fURL.getPath().substring(k+1, n) + fURL.getPath().substring(n+5);
      }
    }
    return null;
  }

  private BufferedImage loadImage() {
    if (filepath != null) {
      try {
        bimg = ImageIO.read(this.fileURL);
      } catch (Exception e) {
        log(-1, "FatalError: image could not be loaded from " + filepath);
        return null;
      }
      if (imageName != null) {
        imageFiles.put(fileURL, this);
        log(lvl, "added to image list: %s \nwith URL: %s",
                imageName, fileURL);
        bwidth = bimg.getWidth();
        bheight = bimg.getHeight();
        bsize = bimg.getData().getDataBuffer().getSize();
        currentMemory += bsize;
        Image first;
        while (images.size() > 0 && currentMemory > maxMemory) {
          first = images.remove(0);
          currentMemory -= first.bsize;
        }
        images.add(this);
        log(lvl, "loaded %s (%d KB of %d MB (%d / %d %%) (%d))", imageName, (int) (bsize / KB),
                (int) (maxMemory / MB), images.size(), (int) (100 * currentMemory / maxMemory),
                (int) (currentMemory / KB));
      } else {
        log(-1, "ImageName invalid! not cached!");
      }
    }
    return bimg;
  }

  public static void purge(String bundlePath) {
    URL pathURL = FileManager.makeURL(bundlePath);
    if (!ImagePath.getPaths().get(0).pathURL.equals(pathURL)) {
      log(-1, "purge: not current bundlepath: " + pathURL);
      return;
    }
    int onPath = 0;
    for (ImagePath.PathEntry e : ImagePath.getPaths()) {
      if (e.pathURL.equals(pathURL)) {
        onPath += 1;
      }
    }
    if (onPath > 1 || imageFiles.size() == 0 ) {
      // also added as path entry by import, so we do not purge
      return;
    }
    String pathStr = pathURL.toString();
    URL imgURL;
    Image img;
    Image buf;
    log(lvl, "purge: " + pathStr);
    Iterator<Map.Entry<URL, Image>> it = imageFiles.entrySet().iterator();
    Map.Entry<URL, Image> entry;
    Iterator<Image> bit;
    purgeList.clear();

    while (it.hasNext()) {
      entry = it.next();
      imgURL = entry.getKey();
      if (imgURL.toString().startsWith(pathStr)) {
        log(lvl, "purge: entry: " + imgURL.toString());
        purgeList.add(entry.getValue());
        it.remove();

      }
    }
    if (purgeList.size() > 0) {
        bit  = images.iterator();
        while (bit.hasNext()) {
          buf = bit.next();
          if (purgeList.contains(buf)) {
            bit.remove();
            log(lvl, "purge: bimg: " + buf);
            currentMemory -= buf.bsize;
          }
        }
        log(lvl, "Max %d MB (%d / %d %%) (%d))", (int) (maxMemory / MB), images.size(),
                (int) (100 * currentMemory / maxMemory), (int) (currentMemory / KB));
    }
  }

  public Image(BufferedImage img) {
    this(img, null);
  }

  public Image(BufferedImage img, String name) {
    if (name == null) {
      imageName = isBImg;
    } else {
      imageName = name;
    }
    filepath = isBImg;
    bimg = img;
    bwidth = bimg.getWidth();
    bheight = bimg.getHeight();
  }

  public Image(ScreenImage img) {
    this(img.getImage(), null);
  }

  public Image(ScreenImage img, String name) {
    this(img.getImage(), name);
  }

  /**
   * check wether image is available
   *
   * @return true if located or is a buffered image
   */
  public boolean isValid() {
    return filepath != null;
  }

  /**
   *
   * @return true if image was given with absolute filepath
   */
  public boolean isAbsolute() {
    return imageIsAbsolute;
  }

  public boolean isText() {
    return imageIsText;
  }

  public void setIsText(boolean isText) {
    imageIsText = isText;
  }

  public URL getURL() {
    return fileURL;
  }

  /**
   * Get the image's absolute filename or null if jar, http or in memory only
   *
   * @return
   */
  public String getFilename() {
//    if (isBImg.equals(imageName)) {
//      return null;
//    }
    if (fileURL != null && !"file".equals(fileURL.getProtocol())) {
      return null;
    }
    return filepath;
  }

  /**
   * Get the image's absolute filename or null if in memory only
   *
   * @return
   */
  public String getName() {
    return imageName;
  }

  /**
   * return the image's BufferedImage (load it if not in cache)
   *
   * @return
   */
  public BufferedImage get() {
    if (bimg != null) {
      if (!filepath.equals(isBImg)) {
        log(lvl, "getImage from cache: %s\n%s", imageName, (fileURL == null ? filepath : fileURL));
      } else {
        log(lvl, "getImage inMemory: %s", imageName);
      }
      return bimg;
    } else {
      return loadImage();
    }
  }

  public Dimension getSize() {
    return new Dimension(bwidth, bheight);
  }

  public Rectangle getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Rectangle lastSeen) {
    this.lastSeen = lastSeen;
  }
  
  public Image subImage(int x, int y, int w, int h) {
    Image img = new Image(get().getSubimage(x, y, w, h));
    return img;
  }

  /**
   * return an OpenCV Mat version from the BufferedImage
   *
   * @return
   */
  protected org.sikuli.natives.Mat getMat() {
    return convertBufferedImageToMat(get());
  }

  protected static org.sikuli.natives.Mat convertBufferedImageToMat(BufferedImage img) {
    if (img != null) {
      byte[] data = ImageFinder.convertBufferedImageToByteArray(img);
      return Vision.createMat(img.getHeight(), img.getWidth(), data);
    } else {
      return null;
    }
  }
}
