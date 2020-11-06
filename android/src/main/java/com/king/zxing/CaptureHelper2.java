/*
 * Copyright (C) 2019 Jenly Yu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.king.zxing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.king.zxing.camera.CameraManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.FloatRange;

/**
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class CaptureHelper2 implements CaptureLifecycle, CaptureTouchEvent {

    public static final String TAG = CaptureHelper2.class.getSimpleName();

    private Activity activity;
    private CaptureHandler captureHandler;
    private OnCaptureListener onCaptureListener;
    private CameraManager cameraManager;

    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder.Callback callback;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,Object> decodeHints;
    private String characterSet;

    private boolean hasSurface;
    /**
     * 默认触控误差值
     */
    private static final int DEVIATION = 6;
    /**
     * 是否支持缩放（变焦），默认支持
     */
    private boolean isSupportZoom = true;
    private float oldDistance;

    /**
     * 是否支持自动缩放（变焦），默认支持
     */
    private boolean isSupportAutoZoom = true;

    /**
     * 是否支持垂直的条形码
     */
    private boolean isSupportVerticalCode;

    /**
     * 是否返回扫码原图
     */
    private boolean isReturnBitmap;
    private OnCaptureCallback onCaptureCallback;
    private OnCaptureFinishCallback onCaptureFinishCallback;
    private OnCaptureReadyCallback onCaptureReadyCallback;

    /**
     * 最多扫多少次
     * -1 为无限次
     */
    private int maxScan = -1;

    /**
     * 连扫中间停顿时间
     */
    private long delay = 300;

    /**
     * 自动连扫
     */
    private boolean autoRestart = false;

    public CaptureHelper2(Activity activity, SurfaceView surfaceView, ViewfinderView viewfinderView){
        this.activity = activity;
        this.viewfinderView = viewfinderView;
        this.surfaceView = surfaceView;
        surfaceHolder = surfaceView.getHolder();
        hasSurface = false;
    }

    @Override
    public void onCreate(){
        cameraManager = new CameraManager(activity);
        cameraManager.setFullScreenScan(false);
        cameraManager.setFramingRectRatio(0.9f);
        cameraManager.setFramingRectVerticalOffset(0);
        cameraManager.setFramingRectHorizontalOffset(0);

        // 设置解析区域
        int x = viewfinderView.getWidth();
        int y = viewfinderView.getHeight();
        cameraManager.setManualFramingRect(x, y);

        callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (holder == null) {
                    Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
                }
                if (!hasSurface) {
                    hasSurface = true;
                    initCamera(holder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { hasSurface = false; }
        };

        onCaptureListener = new OnCaptureListener() {
            @Override
            public void onHandleDecode(Result result, Bitmap barcode, float scaleFactor) {
                onResult(result);
            }
        };
    }

    @Override
    public void onResume(){
        surfaceHolder.addCallback(callback);
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(callback);
        }
    }

    @Override
    public void onPause(){
        if (captureHandler != null) {
            captureHandler.quitSynchronously();
            captureHandler = null;
        }
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(callback);
        }
    }

    @Override
    public void onDestroy(){
        if (captureHandler != null) {
            captureHandler.quitSynchronously();
            captureHandler = null;
        }
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(callback);
        }
    }

    /**
     * 支持缩放时，须在{@link Activity#onTouchEvent(MotionEvent)}调用此方法
     * @param event
     */
    public boolean onTouchEvent(MotionEvent event){
        if(isSupportZoom && cameraManager.isOpen()){
            Camera camera = cameraManager.getOpenCamera().getCamera();
            if(camera ==null){
                return false;
            }
            if(event.getPointerCount() > 1) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {//多点触控
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDistance = calcFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newDistance = calcFingerSpacing(event);

                        if (newDistance > oldDistance + DEVIATION) {//
                            handleZoom(true, camera);
                        } else if (newDistance < oldDistance - DEVIATION) {
                            handleZoom(false, camera);
                        }
                        oldDistance = newDistance;
                        break;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * 初始化Camera
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);

            Point sr = cameraManager.getScreenResolution();
            ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
            lp.height = sr.y;
            lp.width = sr.x;
            surfaceView.setLayoutParams(lp);

            if (captureHandler == null) {
                captureHandler = new CaptureHandler(activity,viewfinderView,onCaptureListener, decodeFormats, decodeHints, characterSet, cameraManager);
                captureHandler.setSupportVerticalCode(isSupportVerticalCode);
                captureHandler.setReturnBitmap(isReturnBitmap);
                captureHandler.setSupportAutoZoom(isSupportAutoZoom);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    /**
     * 处理变焦缩放
     * @param isZoomIn
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    /**
     * 聚焦
     * @param event
     * @param camera
     */
    @Deprecated
    private void focusOnTouch(MotionEvent event,Camera camera) {

        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();

        Rect focusRect = calcTapArea(event.getRawX(), event.getRawY(), 1f,previewSize);
        Rect meteringRect = calcTapArea(event.getRawX(), event.getRawY(), 1.5f,previewSize);
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 600));
            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 600));
            parameters.setMeteringAreas(meteringAreas);
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera1) {
                Camera.Parameters params1 = camera1.getParameters();
                params1.setFocusMode(currentFocusMode);
                camera1.setParameters(params1);
            }
        });
    }

    /**
     * 计算两指间距离
     * @param event
     * @return
     */
    private float calcFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算对焦区域
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private Rect calcTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 200;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) ((x / previewSize.width) * 2000 - 1000);
        int centerY = (int) ((y / previewSize.height) * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
    }

    /**
     * 根据范围限定值
     * @param x
     * @param min 范围最小值
     * @param max 范围最大值
     * @return
     */
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    /**
     * 重新启动扫码和解码器
     */
    public void restartPreviewAndDecode(){
        if(captureHandler!=null){
            captureHandler.restartPreviewAndDecode();
            if (onCaptureReadyCallback != null) {
                onCaptureReadyCallback.onReadyCallback();
            }
        }
    }

    /**
     * @param result 扫码结果
     */
    public void onResult(Result result){
        final String text = result.getText();
        if(onCaptureCallback!=null){
            onCaptureCallback.onResultCallback(text);
        }

        if (autoRestart) {
            if(--maxScan != 0 ){
                if (delay > 0) {
                    captureHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            restartPreviewAndDecode();
                        }
                    }, delay);
                } else {
                    restartPreviewAndDecode();
                }
                return;
            }
        } else {
            return;
        }

        if (onCaptureFinishCallback != null) {
            onCaptureFinishCallback.onFinishCallback();
        }
    }

    /**
     * 是否可以继续扫码
     */
    public void setReadyToScan() {
        restartPreviewAndDecode();
    }

    /**
     * 设置是否支持缩放
     * @param supportZoom
     * @return
     */
    public CaptureHelper2 supportZoom(boolean supportZoom) {
        isSupportZoom = supportZoom;
        return this;
    }

    /**
     * 设置支持的解码一/二维码格式，默认常规的码都支持
     * @param decodeFormats  可参见{@link DecodeFormatManager}
     * @return
     */
    public CaptureHelper2 decodeFormats(Collection<BarcodeFormat> decodeFormats) {
        this.decodeFormats = decodeFormats;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param decodeHints
     * @return
     */
    public CaptureHelper2 decodeHints(Map<DecodeHintType,Object> decodeHints) {
        this.decodeHints = decodeHints;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param key {@link DecodeHintType}
     * @param value {@link }
     * @return
     */
    public CaptureHelper2 decodeHint(DecodeHintType key, Object value){
        if(decodeHints == null){
            decodeHints = new EnumMap<>(DecodeHintType.class);
        }
        decodeHints.put(key,value);
        return this;
    }

    /**
     *  设置解码时编码字符集
     * @param characterSet
     * @return
     */
    public CaptureHelper2 characterSet(String characterSet) {
        this.characterSet = characterSet;
        return this;
    }

    /**
     * 设置是否支持扫垂直的条码
     * @param supportVerticalCode 默认为false，想要增强扫条码识别度时可使用，相应的会增加性能消耗。
     * @return
     */
    public CaptureHelper2 supportVerticalCode(boolean supportVerticalCode) {
        this.isSupportVerticalCode = supportVerticalCode;
        if(captureHandler!=null){
            captureHandler.setSupportVerticalCode(isSupportVerticalCode);
        }
        return this;
    }

    /**
     * 设置返回扫码原图
     * @param returnBitmap 默认为false，当返回true表示扫码就结果会返回扫码原图，相应的会增加性能消耗。
     * @return
     */
    public CaptureHelper2 returnBitmap(boolean returnBitmap) {
        isReturnBitmap = returnBitmap;
        if(captureHandler!=null){
            captureHandler.setReturnBitmap(isReturnBitmap);
        }
        return this;
    }


    /**
     * 设置是否支持自动缩放
     * @param supportAutoZoom
     * @return
     */
    public CaptureHelper2 supportAutoZoom(boolean supportAutoZoom) {
        isSupportAutoZoom = supportAutoZoom;
        if(captureHandler!=null){
            captureHandler.setSupportAutoZoom(isSupportAutoZoom);
        }
        return this;
    }

    /**
     * 设置扫码回调
     * @param callback
     * @return
     */
    public CaptureHelper2 setOnCaptureCallback(OnCaptureCallback callback) {
        this.onCaptureCallback = callback;
        return this;
    }

    /**
     * 设置扫码结束回调
     * @param callback
     * @return
     */
    public CaptureHelper2 setOnCaptureFinishCallback(OnCaptureFinishCallback callback) {
        this.onCaptureFinishCallback = callback;
        return this;
    }

    /**
     * 设置重启扫码回调
     * @param callback
     * @return
     */
    public CaptureHelper2 setOnCaptureReadyCallback (OnCaptureReadyCallback callback) {
        this.onCaptureReadyCallback = callback;
        return this;
    }

    /**
     * 设置最多扫码次数
     * @param maxScan
     * @return
     */
    public CaptureHelper2 setMaxScan(int maxScan) {
        this.maxScan = maxScan;
        return this;
    }

    public int getMaxScan() {
        return maxScan;
    }

    /**
     * 设置扫码停顿间隔
     * @param delay
     * @return
     */
    public CaptureHelper2 setDelay(long delay) {
        this.delay = delay;
        return this;
    }

    public long getDelay() {
        return delay;
    }

    /**
     * 自动连续扫码
     * @param autoRestart
     * @return
     */
    public CaptureHelper2 setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
        return this;
    }

    public boolean getAutoRestart() {
        return autoRestart;
    }

    /**
     * {@link CameraManager}
     * @return {@link #cameraManager}
     */
    public CameraManager getCameraManager() {
        return cameraManager;
    }
}
