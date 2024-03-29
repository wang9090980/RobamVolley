package org.robam.xutils.bitmap.core;


public class BitmapSize {

    public static final BitmapSize ZERO = new BitmapSize(0, 0);

    private int width;
    private int height;

    public BitmapSize() {
    }

    public BitmapSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Scales down dimensions in <b>sampleSize</b> times. Returns new object.
     */
    public BitmapSize scaleDown(int sampleSize) {
        return new BitmapSize(width / sampleSize, height / sampleSize);
    }

    /**
     * Scales dimensions according to incoming scale. Returns new object.
     */
    public BitmapSize scale(float scale) {
        return new BitmapSize((int) (width * scale), (int) (height * scale));
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "_" + width + "*" + height;
    }
}
