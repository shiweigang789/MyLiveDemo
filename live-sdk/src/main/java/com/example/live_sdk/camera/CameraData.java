package com.example.live_sdk.camera;

/**
 * Created by swg on 2017/11/6.
 */

public class CameraData {

    public static final int FACING_FRONT = 1;
    public static final int FACING_BACK = 2;

    /**
     * camera的id
     */
    public int cameraID;
    /**
     * 区分前后摄像头
     */
    public int cameraFacing;
    /**
     * camera的宽度
     */
    public int cameraWidth;
    /**
     * camera的高度
     */
    public int cameraHeight;

    public boolean hasLight;
    public int orientation;
    public boolean supportTouchFocus;
    public boolean touchFocusMode;

    public CameraData(int id, int facing, int width, int height){
        cameraID = id;
        cameraFacing = facing;
        cameraWidth = width;
        cameraHeight = height;
    }

    public CameraData(int id, int facing){
        cameraID = id;
        cameraFacing = facing;
    }

}
