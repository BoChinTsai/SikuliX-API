/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import org.sikuli.basics.Settings;
import java.awt.image.BufferedImage;

/**
 * to define a more complex search target<br />
 * - non-standard minimum similarity <br />
 * - click target other than center <br />
 * - image as in-memory image
 */
public class Pattern {
  private Image image = null;
  private float similarity = (float) Settings.MinSimilarity;
  private Location offset = new Location(0, 0);
  private int waitAfter = 0;

  /**
	 * creates empty Pattern object
	 * at least setFilename() or setBImage() must be used before
	 * the Pattern object is ready for anything
	 */
	public Pattern() {
  }

  /**
	 * create a new Pattern from another (attribs are copied)
	 *
	 * @param p
	 */
	public Pattern(Pattern p) {
    image = p.getImage();
    similarity = p.similarity;
    offset.x = p.offset.x;
    offset.y = p.offset.y;
  }

  /**
	 * create a Pattern with given image<br />
	 *
	 * @param imgpath
	 */
	public Pattern(Image img) {
    image = img;
  }

  /**
	 * create a Pattern based on an image file name<br />
	 *
	 * @param imgpath
	 */
	public Pattern(String imgpath) {
    image = Image.createImage(imgpath);
  }

  /**
	 * A Pattern from a BufferedImage
	 *
	 * @param bimg
	 */
	public Pattern(BufferedImage bimg) {
    image = new Image(bimg);
  }

  /**
	 * A Pattern from a ScreenImage
	 *
	 * @param simg
	 */
	public Pattern(ScreenImage simg) {
		image = new Image(simg.getImage());
	}
  
  /**
   * check wether the image is valid
   * 
   * @return true if image is useable
   */
  public boolean isValid() {
    return image.isValid();
  }

	/**
	 * sets the minimum Similarity to use with find
	 *
	 * @param sim
	 * @return the Pattern object itself
	 */
	public Pattern similar(float sim) {
    similarity = sim;
    return this;
  }

	/**
	 * sets the minimum Similarity to 0.99 which means exact match
	 *
	 * @return  the Pattern object itself
	 */
	public Pattern exact() {
    similarity = 0.99f;
    return this;
  }

  /**
	 *
	 * @return the current minimum similarity
	 */
	public float getSimilar() {
    return this.similarity;
  }

  /**
	 * set the offset from the match's center to be used with mouse actions
	 *
	 * @param dx
	 * @param dy
	 * @return the Pattern object itself
	 */
	public Pattern targetOffset(int dx, int dy) {
    offset.x = dx;
    offset.y = dy;
    return this;
  }

  /**
	 * set the offset from the match's center to be used with mouse actions
	 *
	 * @param loc
	 * @return the Pattern object itself
	 */
	public Pattern targetOffset(Location loc) {
    offset.x = loc.x;
    offset.y = loc.y;
    return this;
  }

  /**
	 *
	 * @return the current offset
	 */
	public Location getTargetOffset() {
    return offset;
  }

  /**
	 * set the Patterns image based on file name
	 *
	 * @param imgURL_
	 * @return the Pattern object itself
	 */
	public Pattern setFilename(String imgURL) {
    image = new Image(imgURL);
    return this;
  }

  /**
	 * the current image's absolute filepath
	 *
	 * @return might be null
	 */
	public String getFilename() {
    return image.getFilename();
  }

  /**
	 * check for a valid image file
	 *
	 * @return path or null
	 */
	public String checkFile() {
    return image.getFilename();
  }

  /**
	 * return the buffered image 
	 *
	 * @return might be null
	 */
	public BufferedImage getBImage() {
		return image.getImage();
  }

	/**
	 * sets the Pattern's buffered image
	 *
	 * @param bimg
	 * @return the Pattern object itself
	 */
	public Pattern setBImage(BufferedImage bimg) {
    image = new Image(bimg);
    return this;
	}

	/**
	 * sets the Pattern's image
	 *
	 * @param img
	 * @return the Pattern object itself
	 */
	public Pattern setImage(Image img) {
    image = img;
    return this;
	}
  
	/**
	 * get the Pattern's image
	 *
	 * @return 
	 */
	public Image getImage() {
    return image;
	}

  /**
   * set the seconds to wait, after this pattern is acted on
   * @param secs
   */
  public void setTimeAfter(int secs) {
    waitAfter = secs;
  }

  /**
   * get the seconds to wait, after this pattern is acted on
   */
  public int setTimeAfter() {
    return waitAfter;
  }

  @Override
  public String toString() {
    String ret = "P(" + image.getName() + ")";
    ret += " S: " + similarity;
    if (offset.x != 0 || offset.y != 0) {
      ret += " T: " + offset.x + "," + offset.y;
    }
    return ret;
  }
}
