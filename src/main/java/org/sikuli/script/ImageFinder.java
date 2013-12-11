package org.sikuli.script;

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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;

public class ImageFinder extends Finder {

  private static String me = "ImageFinder";
  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "", me + ": " + message, args);
  }
  private boolean isImageFinder = true;
  protected boolean isImage = false;
  protected Region region = null;
  protected boolean isRegion = false;
  protected Screen screen = null;
  protected boolean isScreen = false;
  protected int offX, offY;
  protected long MaxTimePerScan;
  private Image bImage = null;
  protected Mat base = new Mat();
  private double waitingTime = Settings.AutoWaitTimeout;
  private int minChanges;
  private ImageFind firstFind = null;
  private boolean isReusable = false;
  protected boolean isMultiFinder = false;

  public ImageFinder() {
    init(null, null, null);
  }

  public ImageFinder(Image base) {
    init(base, null, null);
  }

  public ImageFinder(Screen scr) {
    init(null, scr, null);
  }

  public ImageFinder(Region reg) {
    init(null, null, reg);
  }

  protected ImageFinder(Mat base) {
    log(3, "init");
    reset();
    this.base = base;
    isImage = true;
    log(3, "search in: \n%s", base);
  }

  private void init(Image base, Screen scr, Region reg) {
    log(3, "init");
    if (base != null) {
      setImage(base);
    } else if (scr != null) {
      setScreen(scr);
    } else if (reg != null) {
      setRegion(reg);
    }
  }

  private void reset() {
    firstFind = null;
    isImage = false;
    isScreen = false;
    isRegion = false;
    screen = null;
    region = null;
    bImage = null;
    base = new Mat();
  }

  @Override
  public void destroy() {
    reset();
  }
  
  public void setIsMultiFinder() {
    base = new Mat();
    isMultiFinder = true;
  }

  public boolean setImage(Image base) {
    reset();
    if (base.isValid()) {
      bImage = base;
      this.base = createMat(base.get());
      isImage = true;
      log(3, "search in: \n%s", base.get());
    }
    return isImage;
  }

  public boolean isImage() {
    return isImage;
  }

  protected void setBase(BufferedImage bImg) {
    log(3, "search in: \n%s", bImg);
    base = createMat(bImg);
  }

  public boolean setScreen(Screen scr) {
    reset();
    if (scr != null) {
      screen = scr;
      isScreen = true;
      setScreenOrRegion(scr);
    }
    return isScreen;
  }

  public boolean setRegion(Region reg) {
    reset();
    if (reg != null) {
      region = reg;
      isRegion = true;
      setScreenOrRegion(reg);
    }
    return isRegion;
  }

  private void setScreenOrRegion(Object reg) {
    Region r = (Region) reg;
    MaxTimePerScan = (int) (1000.0 / r.getWaitScanRate());
    offX = r.x;
    offY = r.y;
    log(3, "search in: \n%s", r);
  }

  public void setFindTimeout(double t) {
    waitingTime = t;
  }

  public boolean isValid() {
    if (!isImage && !isScreen && !isRegion) {
      log(-1, "not yet initialized (not valid Image, Screen nor Region)");
      return false;
    }
    return true;
  }

  @Override
  public String find(Image img) {
    if (null == imageFind(img)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String find(String filenameOrText) {
    if (null == imageFind(filenameOrText)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String find(Pattern pat) {
    if (null == imageFind(pat)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String findText(String text) {
    log(-1, "findText: not yet implemented");
    return null;
  }

  public <PSI> ImageFind search(PSI probe, Object... args) {
    isReusable = true;
    return imageFind(probe, args);
  }

  protected <PSI> ImageFind findInner(PSI probe, double sim) {
    ImageFind newFind = new ImageFind();
    newFind.setIsInnerFind();
    newFind.setSimilarity(sim);
    if (!newFind.checkFind(this, probe)) {
      return null;
    }
    firstFind = newFind;
    if (newFind.isValid()) {
      return newFind.doFind();
    }
    return null;
  }

  private <PSI> ImageFind imageFind(PSI probe, Object... args) {
    Debug.enter(me + ": find: %s", probe);
    ImageFind newFind = new ImageFind();
    newFind.setFindTimeout(waitingTime);
    if (!newFind.checkFind(this, probe, args)) {
      return null;
    }
    if (newFind.isValid() && !isReusable && firstFind == null) {
      firstFind = newFind;
    }
    ImageFind imgFind = newFind.doFind();
    log(lvl, "find: success: %s", imgFind.get());
    return imgFind;
  }

  public <PSI> ImageFind searchAny(PSI probe, Object... args) {
    Debug.enter(me + ": findAny: %s", probe);
    ImageFind newFind = new ImageFind();
    newFind.setFinding(ImageFind.FINDING_ANY);
    isReusable = true;
    if (!newFind.checkFind(this, probe, args)) {
      return null;
    }
    if (newFind.isValid() && !isReusable && firstFind == null) {
      firstFind = newFind;
    }
    ImageFind imgFind = newFind.doFind();
    log(lvl, "find: success: %s", imgFind.get());
    return imgFind;
  }

  public <PSI> ImageFind searchSome(PSI probe, Object... args) {
    return searchSome(probe, ImageFind.SOME_COUNT, args);
  }

  public <PSI> ImageFind searchSome(PSI probe, int count, Object... args) {
    isReusable = true;
    return imageFindAll(probe, ImageFind.BEST_FIRST, count, args);
  }

  @Override
  public String findAll(Image img) {
    if (null == imageFindAll(img, ImageFind.BEST_FIRST, 0)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String findAll(String filenameOrText) {
    if (null == imageFindAll(filenameOrText, ImageFind.BEST_FIRST, 0)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String findAll(Pattern pat) {
    if (null == imageFindAll(pat, ImageFind.BEST_FIRST, 0)) {
      return null;
    } else {
      return "--fromImageFinder--";
    }
  }

  public <PSI> ImageFind searchAll(PSI probe, Object... args) {
    isReusable = true;
    return imageFindAll(probe, ImageFind.BEST_FIRST, 0, args);
  }

  public <PSI> ImageFind searchAll(PSI probe, int sorted, Object... args) {
    isReusable = true;
    return imageFindAll(probe, sorted, 0, args);
  }

  private <PSI> ImageFind imageFindAll(PSI probe, int sorted, int count, Object... args) {
    Debug.enter(me + ": findAny: %s", probe);
    ImageFind newFind = new ImageFind();
    newFind.setFinding(ImageFind.FINDING_ALL);
    newFind.setSorted(sorted);
    if (count > 0) {
      newFind.setCount(count);
    }
   if (!newFind.checkFind(this, probe, args)) {
      return null;
    }
    if (newFind.isValid() && !isReusable && firstFind == null) {
      firstFind = newFind;
    }
    ImageFind imgFind = newFind.doFind();
    log(lvl, "find: success: %s", imgFind.get());
    return imgFind;
   }

  public boolean hasChanges(Mat current) {
    Mat bg = new Mat();
    Mat cg = new Mat();
    Mat diff = new Mat();
    Mat tdiff = new Mat();

    Imgproc.cvtColor(base, bg, Imgproc.COLOR_BGR2GRAY);
    Imgproc.cvtColor(current, cg, Imgproc.COLOR_BGR2GRAY);
    Core.absdiff(bg, cg, diff);
    Imgproc.threshold(diff, tdiff, 5.0, 0.0, Imgproc.THRESH_TOZERO);
    if (Core.countNonZero(tdiff) <= 5) {
      return false;
    }
    return true;
  }

  public void setMinChanges(int min) {
    minChanges = min;
  }

  @Override
  public boolean hasNext() {
    if (null != firstFind) {
      return firstFind.hasNext();
    }
    return false;
  }

  @Override
  public Match next() {
    if (firstFind != null) {
      return firstFind.next();
    }
    return null;
  }

  @Override
  public void remove() {
  }

  public static Mat createMat(BufferedImage img) {
    if (img != null) {
      Debug timer = Debug.startTimer("Mat create\t (%d x %d) from \n%s",
              img.getWidth(), img.getHeight(), img);
      Mat mat_ref = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC4);
      timer.lap("init");
      byte[] data;
      BufferedImage cvImg;
      // createBufferedImage(int w, int h)
      ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
      int[] nBits = {8, 8, 8, 8};
      ColorModel cm = new ComponentColorModel(cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      SampleModel sm = cm.createCompatibleSampleModel(img.getWidth(), img.getHeight());
      DataBufferByte db = new DataBufferByte(img.getWidth() * img.getHeight() * 4);
      WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0, 0));
      cvImg = new BufferedImage(cm, r, false, null);
      timer.lap("empty");
      // convertBufferedImageToByteArray(cvImg)
      Graphics2D g = cvImg.createGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      timer.lap("created");
      data = ((DataBufferByte) cvImg.getRaster().getDataBuffer()).getData();
      mat_ref.put(0, 0, data);
      Mat mat = new Mat();
      timer.lap("filled");
      Imgproc.cvtColor(mat_ref, mat, Imgproc.COLOR_RGBA2BGR, 3);
      timer.end();
      return mat;
    } else {
      return null;
    }
  }

  protected static byte[] convertBufferedImageToByteArray(BufferedImage img) {
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

  protected static BufferedImage createBufferedImage(int w, int h) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    int[] nBits = {8, 8, 8, 8};
    ColorModel cm = new ComponentColorModel(cs, nBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
    SampleModel sm = cm.createCompatibleSampleModel(w, h);
    DataBufferByte db = new DataBufferByte(w * h * 4);
    WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0, 0));
    BufferedImage bm = new BufferedImage(cm, r, false, null);
    return bm;
  }
}
