package com.wz.base;

public class SkillLayoutConfig {
    private int index;
    /**
     * 相对位置映射1 相同大小的按钮映射，不用计算映射点位
     * 等比放大拉伸映射2 宽高等比放大，计算映射
     */
    private int mapType;
    private float offsetX;
    private float offsetY;
    /**
     * {@link android.content.res.Configuration#ORIENTATION_PORTRAIT }
     */
    private int orientation;
    private float width;
    private float height;

    public static SkillLayoutConfig getBlankShow() {
        SkillLayoutConfig skillLayoutConfig = new SkillLayoutConfig();
        skillLayoutConfig.setMapType(1);
        skillLayoutConfig.setOrientation(1);
        skillLayoutConfig.setWidth(100);
        skillLayoutConfig.setHeight(100);
        return skillLayoutConfig;
    }

    public static SkillLayoutConfig getBlankSave() {
        SkillLayoutConfig skillLayoutConfig = new SkillLayoutConfig();
        skillLayoutConfig.setMapType(1);
        skillLayoutConfig.setOrientation(1);
        return skillLayoutConfig;
    }

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

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
