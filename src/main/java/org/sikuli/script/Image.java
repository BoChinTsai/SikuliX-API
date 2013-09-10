package org.sikuli.script;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
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
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.sikuli.basics.Debug;
import org.sikuli.basics.ImageLocator;
import org.sikuli.basics.Settings;
import org.sikuli.basics.proxies.Mat;
import org.sikuli.basics.proxies.Vision;

public class Image {

  private static String me = "Image";
  private static String mem = "";
  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "", me + ": " + message, args);
  }
  
  private static List<Image> images = Collections.synchronizedList(new ArrayList<Image>());
  private static Map<String, Image> imageFiles = Collections.synchronizedMap(new HashMap<String, Image>());
  private static List<Image> imagePaths = Collections.synchronizedList(new ArrayList<Image>());
  private static int KB = 1024;
  private static int MB = KB * KB;
  private static int maxMemory = 64 * MB;
  private static int currentMemory;
  private static String jarPath = null;
  private static boolean imagesInJar = false;
  private static ClassLoader cl;
  private static CodeSource codeSrc;
  public final static String isBImg = "__BufferedImage__";
  private String imageName;
  private boolean imageIsText = false;
  private String filepath = null;
  private BufferedImage bimg = null;
  private long bsize;
  private int bwidth;
  private int bheight;

  /**
   * Create Image from url, relative or absolute filename
   *
   * @param fname
   */
  public Image(String fname) {
    imageName = fname;
    if (jarPath == null) {
      filepath = ImageLocator.getPath(imageName);
    } else {
      if (imagesInJar) {
        filepath = "__FROM_JAR__";
      } else {
        File f = new File(jarPath, imageName);
        if (f.exists()) {
          filepath = f.getAbsolutePath();
        } else {
          filepath = ImageLocator.getPath(imageName);
        }
      }
    }
    loadImage();
  }

  private BufferedImage loadImage() {
    if (filepath != null) {
      imageFiles.put(imageName, this);
      log(lvl, "added to image list: %s with location: %s", imageName, filepath);
      try {
        if (imagesInJar) {
          bimg = ImageIO.read(cl.getResource(imageName));
        } else {
          bimg = ImageIO.read(new File(filepath));
        }
      } catch (Exception e) {
        log(-1, "FatalError: image could not be loaded from " + filepath);
        SikuliX.endFatal(1);
      }
      bwidth = bimg.getWidth();
      bheight = bimg.getHeight();
      bsize = bimg.getData().getDataBuffer().getSize();
      currentMemory += bsize;
      Image first;
      while (images.size() > 0 && currentMemory > maxMemory) {
        first = images.remove(0);
        currentMemory -= first.bsize;
        first.bimg = null;
      }
      images.add(this);
      log(lvl, "loaded image %s (%d KB of %d MB (%d / %d %%))", imageName, (int) (bsize / KB),
              (int) (maxMemory / MB), images.size(), (int) (100 * currentMemory / maxMemory));
    }
    return bimg;
  }
  
  public static Image getImageFromCache(String imgName) {
    return imageFiles.get(imgName);
  }
  
  public static Image createImage(String imgName) {
    Image img = Image.getImageFromCache(imgName);
    if (img == null) {
      img = new Image(imgName);
    }
    if (!img.isValid()) {
      if (Settings.OcrTextSearch) {
        img.setIsText(true);
      } else {
        log(-1, "Image not valid, but TextSearch is switched off!");
      }
    }
    return img;
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
  public static void setJarImagePath(String klassName) {
    setJarImagePath(klassName, null);
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
  public static void setJarImagePath(String klassName, String altPath) {
    Class cls = null;
    try {
      cls = Class.forName(klassName);
    } catch (ClassNotFoundException ex) {
      log(-1, "setJarImagePath: FatalError: %s class not found");
      SikuliX.endFatal(1);
    }
    cl = cls.getClassLoader();
    codeSrc = cls.getProtectionDomain().getCodeSource();
    if (codeSrc != null && codeSrc.getLocation() != null) {
      URL jarURL = codeSrc.getLocation();
      jarPath = jarURL.getPath();
      if (jarPath.endsWith(".jar")) {
        imagesInJar = true;
      } else {
        if (altPath != null) {
          jarPath = altPath;
        }
      }
      log(lvl, "setJarImagePath from %s as %s", klassName, jarPath);
    } else {
      log(-1, "setJarImagePath: jar not found for " + klassName);
    }
  }

  public Image(BufferedImage img) {
    imageName = isBImg;
    filepath = isBImg;
    bimg = img;
  }

  /**
   * check wether image is available
   *
   * @return true if located or is a buffered image
   */
  public boolean isValid() {
    return filepath != null;
  }

  public boolean isText() {
    return imageIsText;
  }

  public void setIsText(boolean isText) {
    imageIsText = isText;
  }

  /**
   * Get the image's absolute filename or null if in memory only
   *
   * @return
   */
  public String getFilename() {
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
   * return the image's BufferedImage
   *
   * @return
   */
  public BufferedImage getImage() {
    if (bimg != null) {
      log(lvl, "getImage: %s taken from cache", imageName);
      return bimg;
    } else {
      return loadImage();
    }
  }
  
  public Dimension getSize() {
    return new Dimension(bwidth, bheight);
  }

  /**
   * return an OpenCV Mat version from the BufferedImage
   *
   * @return
   */
  public Mat getMat() {
    return convertBufferedImageToMat(getImage());
  }

  //<editor-fold defaultstate="collapsed" desc="create an OpenCV Mat from a BufferedImage">
  private static BufferedImage createBufferedImage(int w, int h) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    int[] nBits = {8, 8, 8, 8};
    ColorModel cm = new ComponentColorModel(cs, nBits,
            true, false,
            Transparency.TRANSLUCENT,
            DataBuffer.TYPE_BYTE);
    SampleModel sm = cm.createCompatibleSampleModel(w, h);
    DataBufferByte db = new DataBufferByte(w * h * 4); //4 channels buffer
    WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0, 0));
    BufferedImage bm = new BufferedImage(cm, r, false, null);
    return bm;
  }

  private static byte[] convertBufferedImageToByteArray(BufferedImage img) {
    if (img != null) {
      BufferedImage cvImg = createBufferedImage(img.getWidth(), img.getHeight());
      Graphics2D g = cvImg.createGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      return ((DataBufferByte) cvImg.getRaster().getDataBuffer()).getData();
    } else {
      return null;
    }
  }

  private static Mat convertBufferedImageToMat(BufferedImage img) {
    if (img != null) {
      byte[] data = convertBufferedImageToByteArray(img);
      return Vision.createMat(img.getHeight(), img.getWidth(), data);
    } else {
      return null;
    }
  }
  //</editor-fold>
}
