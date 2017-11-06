package com.example.live_sdk.camera;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;

import com.example.live_sdk.camera.exception.CameraDisabledException;
import com.example.live_sdk.camera.exception.CameraNotSupportException;
import com.example.live_sdk.camera.exception.NoCameraException;
import com.example.live_sdk.configuration.CameraConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by swg on 2017/11/6.
 */

public class CameraUtils {

    /**
     * 获取摄像头信息
     *
     * @param isBackFirst
     * @return
     */
    public static List<CameraData> getAllCamerasData(boolean isBackFirst) {
        ArrayList<CameraData> cameraDatas = new ArrayList<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_FRONT);
                if (isBackFirst) {
                    cameraDatas.add(cameraData);
                } else {
                    cameraDatas.add(0, cameraData);
                }
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_BACK);
                if (isBackFirst) {
                    cameraDatas.add(0, cameraData);
                } else {
                    cameraDatas.add(cameraData);
                }
            }
        }
        return cameraDatas;
    }

    public static void initCameraParams(Camera camera, CameraData cameraData, boolean isTouchMode,
                                        CameraConfiguration configuration) throws CameraNotSupportException {
        boolean isLandScape = (configuration.orientation != CameraConfiguration.Orientation.PORTRAIT);
        int cameraWidth = Math.max(configuration.height, configuration.width);
        int cameraHeight = Math.min(configuration.height, configuration.width);
        Camera.Parameters parameters = camera.getParameters();
        setPreviewFormat(camera, parameters);
    }

    /**
     * 设置预览回调的图片格式
     *
     * @param camera
     * @param parameters
     */
    private static void setPreviewFormat(Camera camera, Camera.Parameters parameters)
            throws CameraNotSupportException {
        try {
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        } catch (Exception e) {
            throw new CameraNotSupportException();
        }
    }

    /**
     * 检查摄像头是否可用
     *
     * @param context
     * @throws CameraDisabledException
     * @throws NoCameraException
     */
    public static void checkCameraService(Context context)
            throws CameraDisabledException, NoCameraException {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
        List<CameraData> cameraDatas = getAllCamerasData(false);
        if (cameraDatas.size() == 0) {
            throw new NoCameraException();
        }
    }


}
