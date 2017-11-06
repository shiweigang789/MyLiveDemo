package com.example.live_sdk.camera;

/**
 * Created by swg on 2017/11/6.
 */

public interface CameraListener {

    int CAMERA_NOT_SUPPORT = 1;
    int NO_CANERA = 2;
    int CAMERA_DISABLED = 3;
    int CAMERA_OPEN_FAILED = 4;

    void onOpenSuccess();
    void onOpenFail(int error);
    void onCameraChange();

}
