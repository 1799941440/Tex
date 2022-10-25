package com.wz.base;

public class SkillLayoutConfig {
    private int index;
    /**
     * 相对位置映射1 相同大小的按钮映射，不用计算映射点位
     * 等比放大拉伸映射2 宽高等比放大，计算映射
     */
    private int mapType;
    private int offsetX;
    private int offsetY;
    /**
     * {@link android.content.res.Configuration#ORIENTATION_PORTRAIT }
     */
    private int orientation;
    private int width;
    private int height;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getMapType() {
        return mapType;
    }

    public void setMapType(int mapType) {
        this.mapType = mapType;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
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
}
