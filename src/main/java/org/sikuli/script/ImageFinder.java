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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Size;
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
  private boolean isImage = false;
  private Region region = null;
  private boolean isRegion = false;
  private Screen screen = null;
  private boolean isScreen = false;
  private int offX, offY;
  private boolean isValid = false;
  private boolean isInnerFind = false;
  private Image bImage = null;
  private Mat base = new Mat();
  private Image pImage = null;
  private Mat probe = new Mat();
  private Mat result = new Mat();
  private boolean isPlainColor = false;
  private boolean isBlack = false;
  private int resizeMinDownSample = 12;
  private double resizeFactor;
  private float[] resizeLevels = new float[] {1f, 0.75f, 0.5f, 0.25f};
  private int resizeMaxLevel = resizeLevels.length - 1;
  private double resizeMinSim = 0.9;
  private double similarity = Settings.MinSimilarity;
  private double waitingTime = Settings.AutoWaitTimeout;
  private boolean shouldFail = true;
  private boolean shouldCheckLastSeen = Settings.CheckLastSeen;
  private static final boolean AS_EXISTS = false;
  private static final boolean SHOULD_FAIL = true;
  private int sorted;
  private static final int AS_ROWS = 0;
  private static final int AS_COLUMNS = 1;
  private static final int BEST_FIRST = 2;
  private int finding = -1;
  private static final int FINDING_ANY = 0;
  private static final int FINDING_SOME = 1;
  private static int SOME_COUNT = 5;
  private int count = 0;
  private static final int FINDING_All = 2;
  private static int ALL_MAX = 100;
  private int allMax = 0;
  private long lastFindTime = 0;
  private long lastSearchTime = 0;
  private boolean repeating;
  private long MaxTimePerScan;
  private static List<Match> matches = Collections.synchronizedList(new ArrayList<Match>());
  
  
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

  private ImageFinder(Mat base) {
    log(3, "init");
    reset();
    this.base = base;
    isImage = true;
    isValid = true;
    matches.clear();
    matches.add(0, null);
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
    matches.clear();
    matches.add(0, null);
  }
  
  private void reset() {
    isImage = false;
    isScreen = false;
    isRegion = false;
    screen = null;
    region = null;
    repeating = false;
    bImage = null;
    base = new Mat();
    pImage = null;
    probe = new Mat();
    result = new Mat();
    matches.clear();
    matches.add(0, null);
  }
  
  @Override
  public void destroy() {
    reset();
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
  
  private void setBase(BufferedImage bImg) {
    log(3, "search in: \n%s", bImg);
    base = createMat(bImg);
  }

  public boolean setScreen(Screen scr) {
    reset();
    if (scr != null) {
      screen = scr;
      isScreen = true;
      MaxTimePerScan = (int) (1000.0 / scr.getWaitScanRate());
      offX = scr.x;
      offY = scr.y;
      log(3, "search in: \n%s", scr);
    }
    return isScreen;
  }

  public boolean setRegion(Region reg) {
    reset();
    if (reg != null) {
      region = reg;
      isRegion = true;
      MaxTimePerScan = (int) (1000.0 / reg.getWaitScanRate());      
      offX = reg.x;
      offY = reg.y;
      log(3, "search in: \n%s", reg);      
    }
    return isRegion;
  }

  protected void setIsInnerFind() {
    isInnerFind = true;
  }
  
  public ImageFinder setSimilarity(double sim) {
    similarity = sim;
    return this;
  }

  private boolean checkFind(Object pprobe, Object... args) {
    if (!isImage && !isScreen && !isRegion) {
      log(-1, "not yet initialized (not valid Image, Screen nor Region)");
      return false;
    }
    isValid = false;
    shouldCheckLastSeen = Settings.CheckLastSeen;
    if (pprobe instanceof String) {
      pImage = Image.create((String) pprobe);
      if (pImage.isValid()) {
        isValid = true;
      }
    } else if (pprobe instanceof Image) {
      if (((Image) pprobe).isValid()) {
        isValid = true;
        pImage = (Image) pprobe;
      }
    } else if (pprobe instanceof Pattern) {
      if (((Pattern) pprobe).getImage().isValid()) {
        isValid = true;
        pImage = ((Pattern) pprobe).getImage();
        similarity = ((Pattern) pprobe).getSimilar();
      }
    } else if (pprobe instanceof Mat) {
      isValid = true;
      probe = (Mat) pprobe;
      waitingTime = 0.0;
      shouldCheckLastSeen = false;
    } else {
      log(-1, "find(... some, any, all): probe invalid (not Pattern, String nor valid Image)");
      return false;
    }
    if (probe.empty()) {
      probe = createMat(pImage.get());      
    }
    checkProbe();
    if ((isScreen || isRegion)) {
      if (args.length > 0) {
        if (args[0] instanceof Integer) {
           waitingTime = 0.0 + (Integer) args[0];
       } else if(args[0] instanceof Double) {
          waitingTime = (Double) args[0];
        } else if (args[0] instanceof Boolean) {
          shouldFail = (Boolean) args[0];
        }
      } else if (args.length > 0 && args[1] instanceof Boolean) {
        shouldFail = (Boolean) args[1];
      }
    }
    return isValid;
  }
  
  private void checkProbe() {
    MatOfDouble pMean = new MatOfDouble();
    MatOfDouble pStdDev = new MatOfDouble();
    Core.meanStdDev(probe, pMean, pStdDev);
    double min = 0.00001;
    isPlainColor = false;
    double sum = 0.0;
    double arr[] = pStdDev.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < min) {
      isPlainColor = true;
    }
    sum = 0.0;
    arr = pStdDev.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < min && isPlainColor) {
      isBlack = true;
    }
    resizeFactor = Math.min(((double) probe.width())/resizeMinDownSample, ((double) probe.height())/resizeMinDownSample);
    resizeFactor = Math.max(1.0, resizeFactor);
  }

  @Override
  public String find(Image img) {
    if (!imageFind(img)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }
  
  @Override
  public String find(String filenameOrText) {
    if (!imageFind(filenameOrText)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String find(Pattern pat) {
    if (!imageFind(pat)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }
  
  public <PSI> boolean find(PSI probe, Object... args) {
    return imageFind(probe, args);
  }
  
  private <PSI> boolean imageFind(PSI probe, Object... args) {
    Debug.enter(me + ": find: %s", probe);
    if (!checkFind(probe, args)) {
      return false;
    }
    boolean ret = doFind();   
    if (!isInnerFind) {
      log(lvl, "find: success: %s", matches.get(0));
    }                  
    return ret;
  }

  public <PSI> boolean findAny(PSI probe, Object... args) {
    Debug.enter(me + ": findAny: %s", probe);
    finding = FINDING_ANY;
    if (!checkFind(probe, args)) {
      return false;
    }
    if (doFind()) {
      return true;
    }
    return false;
  }

  public <PSI> boolean findSome(PSI probe, Object... args) {
    return findSome(probe, SOME_COUNT, args);
  }

  public <PSI> boolean findSome(PSI probe, int count, Object... args) {
    setFindAllMax(count);
    return findAll(probe, args);
  }

  @Override
  public String findAll(Image img) {
    if (!imageFindAll(img, BEST_FIRST)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }
  
  @Override
  public String findAll(String filenameOrText) {
    if (!imageFindAll(filenameOrText, BEST_FIRST)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }

  @Override
  public String findAll(Pattern pat) {
    if (!imageFindAll(pat, BEST_FIRST)) {
      return null;
    }
    else {
      return "--fromImageFinder--";
    }
  }
  
  public <PSI> boolean findAll(PSI probe, Object... args) {
    return imageFindAll(probe, BEST_FIRST, args);
  }

  public <PSI> boolean findAll(PSI probe, int sorted, Object... args) {
    return imageFindAll(probe, sorted, args);
  }

  private <PSI> boolean imageFindAll(PSI probe, int sorted, Object... args) {
    Debug.enter(me + ": findAll: %s", probe);
    if (!checkFind(probe, args)) {
      return false;
    }
    finding = FINDING_All;
    this.sorted = sorted;
    if (allMax == 0) {
      allMax = ALL_MAX;
    }
    if (doFind()) {
      return true;
    }
    return false;
  }
  
  public ImageFinder setFindAllMax(int max) {
    allMax = max;
    return this;
  } 

  @Override
  public boolean hasNext() {
    if (matches.size() > 0) {
      return matches.get(0) != null;
    }
    return false;
  }
  
  @Override
  public Match next() {
    Match m = null;
    if (matches.size() > 0) {
    m = matches.get(0);
    remove();
    }
    return m;
  }
  
  @Override
  public void remove() {
    if (matches.size() > 0) {
      matches.remove(0);
    }
  }
  
  public Match get() {
    return get(0);
  }
  
  public Match get(int n) {
    if (n < matches.size()) {
      return matches.get(n);
    }
    return null;
  }

  private Match add(Match m) {
    if (matches.add(m)) {
      return m;
    }
    return null;
  }
  
  private Match set(Match m) {
    if (matches.size() > 0) {
      matches.set(0, m);
    } else {
      matches.add(m);
    }
    return m;
  }
  
  public int getSize() {
    return matches.size();
  }
  
  private boolean doFind() {
    Debug.enter(me + ": doFind");
    boolean found =false;
    Core.MinMaxLocResult fres = null; 
    repeating = false;
    long begin = (new Date()).getTime();
    long lap;
    while (true) {
      lastFindTime = (new Date()).getTime();
      if (shouldCheckLastSeen && !repeating && !isImage && pImage.getLastSeen() != null) {
        ImageFinder f = new ImageFinder(new Region(pImage.getLastSeen()));
        f.setIsInnerFind();
        f.setSimilarity(0.99);
        f.find(probe);
        if (found = (f.hasNext())) {
          log(lvl, "checkLastSeen: success");
          set(f.next());
          get().setTimes(lastFindTime, lastSearchTime);
          if (pImage != null) {
            pImage.setLastSeen(get().getRect());
          }
          break;
        }
      }
      if (isRegion) {
        setBase(region.getScreen().capture(region).getImage());
      } else if (isScreen) {
        setBase(screen.capture().getImage());
      }
      if (isPlainColor) {
        if (isBlack) {
          
        } else {
          
        }
      } else {
        if (!isInnerFind && resizeFactor > 1.5) {
          fres = doFindDown(0, resizeFactor);
        }
      }
      if (fres == null) {
        if (!isInnerFind) {
          log(3, "not found with downsampling (%f) - trying original size", resizeFactor);
        }
        fres = doFindDown(0, 0.0);
        if(fres != null && fres.maxVal > similarity - 0.01) {
          set(new Match((int) fres.maxLoc.x + offX, (int) fres.maxLoc.y + offY, 
                  probe.width(), probe.height(), fres.maxVal, null, null));
        }
      } else {
        set(checkFound(fres));
      }
      lastFindTime = (new Date()).getTime() - lastFindTime;
      if (hasNext()) {
        get().setTimes(lastFindTime, lastSearchTime);
        if (pImage != null) {
          pImage.setLastSeen(get().getRect());
        }
        found = true;
        break;
      } else {
        if (isInnerFind || isImage) {
          break;
        }
        else {
          if (waitingTime < 0.001 || (lap = (new Date()).getTime() - begin) > waitingTime * 1000) {
            break;
          }
          if (MaxTimePerScan > lap) {
            try {
              Thread.sleep(MaxTimePerScan - lap);
            } catch (Exception ex) {
            }
          }
          repeating = true;
        }
      }
    }
    return found;
  }
  
  private Match checkFound(Core.MinMaxLocResult res) {
    Match match = null;
    ImageFinder f;
    Rect r = null;
    if (isImage) {
      int off = ((int) resizeFactor) + 1;
      r = getSubMatRect(base, (int) res.maxLoc.x, (int) res.maxLoc.y,
                            probe.width(), probe.height(), off);
      f = new ImageFinder(base.submat(r));
    } else {
      f = new ImageFinder((new Region((int) res.maxLoc.x + offX, (int) res.maxLoc.y + offY,
                            probe.width(), probe.height())).grow(((int) resizeFactor) + 1));
    }
    f.setIsInnerFind();
    f.setSimilarity(similarity);
    if (f.find(probe)) {
      log(lvl, "check after downsampling: success");
      match = f.next();
      if (isImage) {
        match.x += r.x;
        match.y += r.y;
      }
    }
    return match;
  }
  
  private static Rect getSubMatRect(Mat mat, int x, int y, int w, int h, int margin) {
    x = Math.max(0, x - margin);
    y = Math.max(0, y - margin);
    w = Math.min(w + 2 * margin, mat.width() - x);
    h = Math.min(h + 2 * margin, mat.height()- y);
    return new Rect(x, y, w, h);
  }
  
  private Core.MinMaxLocResult doFindDown(int level, double factor) {
    Debug.enter(me + ": doFindDown (%d - %f)", level, factor);
    Debug timer = Debug.startTimer("doFindDown");
    Mat res = new Mat();
    Mat b = new Mat();
    Mat p = new Mat();
    Core.MinMaxLocResult dres = null;
    double rfactor;
    if (factor > 0.0) {
      rfactor = factor * resizeLevels[level];
      Size sb = new Size(base.cols()/rfactor, base.rows()/factor);
      Size sp = new Size(probe.cols()/rfactor, probe.rows()/factor);
      Imgproc.resize(base, b, sb, 0, 0, Imgproc.INTER_AREA);
      Imgproc.resize(probe, p, sp, 0, 0, Imgproc.INTER_AREA);
      Imgproc.matchTemplate(b, p, res, Imgproc.TM_CCOEFF_NORMED);
      dres = Core.minMaxLoc(res);
    } else {
      Imgproc.matchTemplate(base, probe, res, Imgproc.TM_CCOEFF_NORMED);
      dres = Core.minMaxLoc(res);
      timer.end();
      return dres;
    }
    while (dres.maxVal < resizeMinSim) {
      if (level == resizeMaxLevel) {
        return null;
      }
      level++;
      dres = doFindDown(level, factor);
      if (dres == null) {
        return null;
      }
    }
    dres.maxLoc.x *= rfactor; 
    dres.maxLoc.y *= rfactor; 
    timer.end();
    return dres;
  }
  
  protected static Mat createMat(BufferedImage img) {
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
