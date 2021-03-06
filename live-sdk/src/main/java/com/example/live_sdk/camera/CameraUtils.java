package com.example.live_sdk.camera;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;

import com.example.live_sdk.blacklist.BlackListHelper;
import com.example.live_sdk.camera.exception.CameraDisabledException;
import com.example.live_sdk.camera.exception.CameraNotSupportException;
import com.example.live_sdk.camera.exception.NoCameraException;
import com.example.live_sdk.configuration.CameraConfiguration;
import com.example.live_sdk.constant.SopCastConstant;
import com.example.live_sdk.utils.SopCastLog;

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

    /**
     * 初始化摄像头配置
     *
     * @param camera
     * @param cameraData
     * @param isTouchMode
     * @param configuration
     * @throws CameraNotSupportException
     */
    public static void initCameraParams(Camera camera, CameraData cameraData, boolean isTouchMode,
                                        CameraConfiguration configuration) throws CameraNotSupportException {
        boolean isLandscape = (configuration.orientation != CameraConfiguration.Orientation.PORTRAIT);
        int cameraWidth = Math.max(configuration.height, configuration.width);
        int cameraHeight = Math.min(configuration.height, configuration.width);
        Camera.Parameters parameters = camera.getParameters();
        setPreviewFormat(camera, parameters);
        setpreviewFps(camera, configuration.fps, parameters);
        setPreviewSize(camera, cameraData, cameraWidth, cameraHeight, parameters);
        cameraData.hasLight = supportFlash(camera);
        setOrientation(cameraData, isLandscape, camera);
        setFocusMode(camera, cameraData, isTouchMode);
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
     * 设置摄像头预览帧率
     *
     * @param camera
     * @param fps
     * @param parameters
     */
    private static void setpreviewFps(Camera camera, int fps, Camera.Parameters parameters) {

        if (BlackListHelper.deviceInFpsBlacklisted()) {
            SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set the camera fps 15");
            fps = 15;
        }

        try {
            parameters.setPreviewFrameRate(fps);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int[] range = adaptPreviewFps(fps, parameters.getSupportedPreviewFpsRange());
        try {
            parameters.setPreviewFpsRange(range[0], range[1]);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static int[] adaptPreviewFps(int expectedFps, List<int[]> fpsRanges) {
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    /**
     * 设置预览尺寸
     *
     * @param camera
     * @param cameraData
     * @param width
     * @param height
     * @param parameters
     * @throws CameraNotSupportException
     */
    public static void setPreviewSize(Camera camera, CameraData cameraData, int width, int height,
                                      Camera.Parameters parameters) throws CameraNotSupportException {
        Camera.Size size = getOptimalPreviewSize(camera, width, height);
        ;
        if (size == null) {
            throw new CameraNotSupportException();
        } else {
            cameraData.cameraWidth = size.width;
            cameraData.cameraHeight = size.height;
        }
        //设置预览大小
        SopCastLog.d(SopCastConstant.TAG, "Camera Width: " + size.width + "    Height: " + size.height);
        try {
            parameters.setPreviewSize(cameraData.cameraWidth, cameraData.cameraHeight);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Camera.Size getOptimalPreviewSize(Camera camera, int width, int height) {
        Camera.Size optimalSize = null;
        double minHeightDiff = Double.MAX_VALUE;
        double minWidthDiff = Double.MAX_VALUE;
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null)
            return null;
        //找到宽度差距最小的
        for (Camera.Size size : sizes) {
            if (Math.abs(size.width - width) < minWidthDiff) {
                minWidthDiff = Math.abs(size.width - width);
            }
        }
        //在宽度差距最小的里面，找到高度差距最小的
        for (Camera.Size size : sizes) {
            if (Math.abs(size.width - width) == minWidthDiff) {
                if (Math.abs(size.height - height) < minHeightDiff) {
                    optimalSize = size;
                    minHeightDiff = Math.abs(size.height - height);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 是否支持闪光灯
     *
     * @param camera
     * @return
     */
    public static boolean supportFlash(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes == null) {
            return false;
        }
        for (String flashMode : flashModes) {
            if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置屏幕方向
     *
     * @param cameraData
     * @param isLandscape
     * @param camera
     */
    private static void setOrientation(CameraData cameraData, boolean isLandscape, Camera camera) {
        int orientation = getDisplayOrientation(cameraData.cameraID);
        if (isLandscape) {
            orientation = orientation - 90;
        }
        camera.setDisplayOrientation(orientation);
    }

    public static int getDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation + 360) % 360;
        }
        return result;
    }

    /**
     * 设置对焦模式
     *
     * @param camera
     * @param cameraData
     * @param isTouchMode
     */
    private static void setFocusMode(Camera camera, CameraData cameraData, boolean isTouchMode) {
        cameraData.supportTouchFocus = supportTouchFocus(camera);
        if (!cameraData.supportTouchFocus) {
            setAutoFocusMode(camera);
        } else {
            if (!isTouchMode) {
                cameraData.touchFocusMode = false;
                setAutoFocusMode(camera);
            } else {
                cameraData.touchFocusMode = true;
            }
        }
    }

    public static boolean supportTouchFocus(Camera camera) {
        if (camera != null) {
            return (camera.getParameters().getMaxNumFocusAreas() != 0);
        }
        return false;
    }

    public static void setAutoFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setTouchFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
