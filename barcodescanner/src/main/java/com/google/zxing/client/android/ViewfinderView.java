/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import barcodescanner.xservices.nl.barcodescanner.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 20L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int laserColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;

  private Resources resources;
  private int scan_line_Width , scan_line_Height;
  public boolean hasLasertin = false;
  public int laserTop;
  private Bitmap scan_line_Bitmap;
  private Rect scan_line_SrcRect;
  private Rect scan_line_DesRect;
  private Context context;
  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    laserColor = resources.getColor(R.color.viewfinder_laser);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;

    scan_line_Bitmap = BitmapFactory.decodeStream(getResources().openRawResource(R.drawable.scan_line));
    scan_line_Width = scan_line_Bitmap.getWidth();
    scan_line_Height = scan_line_Bitmap.getHeight();
    scan_line_SrcRect = new Rect(0, 0, scan_line_Width, scan_line_Height);
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {

    if (cameraManager == null) {
      return; // not ready yet, early draw before done configuring
    }
    Rect frame = cameraManager.getFramingRect();
    Rect previewFrame = cameraManager.getFramingRectInPreview();    
    if (frame == null || previewFrame == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    drawOuterRect(frame,canvas);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      //canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {
      drawlaser(frame,paint,canvas);
      // Draw a red "laser scanner" line through the middle to show decoding is active
      /*paint.setColor(laserColor);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
      int middle = frame.height() / 2 + frame.top;
      canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);*/
      
      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();

      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;
      if (currentPossible.isEmpty()) {
        lastPossibleResultPoints = null;
      } else {
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = currentPossible;
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              POINT_SIZE, paint);
          }
        }
      }
      if (currentLast != null) {
        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
        paint.setColor(resultPointColor);
        synchronized (currentLast) {
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              radius, paint);
          }
        }
      }

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
  }

  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }

  public void drawOuterRect(Rect frame,Canvas canvas){
    Paint panit = new Paint(Paint.ANTI_ALIAS_FLAG);
    panit.setColor(Color.parseColor("#ffffffff"));
    float width1 = 3.0f;
    panit.setStrokeWidth(width1);
    canvas.drawLine(frame.left, frame.top+width1/2, frame.right, frame.top+width1/2, panit);
    canvas.drawLine(frame.right-width1/2, frame.top, frame.right-width1/2, frame.bottom, panit);
    canvas.drawLine(frame.left, frame.bottom-width1/2, frame.right, frame.bottom-width1/2, panit);
    canvas.drawLine(frame.left+width1/2, frame.top, frame.left+width1/2, frame.bottom, panit);

    float width = 60.0f;
    float width2 = 18.0f;
    panit.setStrokeWidth(width2);
    canvas.drawLine(frame.left, frame.top+width2/2, frame.left+width, frame.top+width2/2, panit);
    canvas.drawLine(frame.right-width, frame.top+width2/2, frame.right, frame.top+width2/2, panit);
    canvas.drawLine(frame.left+width2/2, frame.top, frame.left+width2/2,frame.top+width,panit);
    canvas.drawLine(frame.left+width2/2, frame.bottom-width, frame.left+width2/2,frame.bottom,panit);
    canvas.drawLine(frame.left, frame.bottom-width2/2, frame.left+width,frame.bottom-width2/2,panit);
    canvas.drawLine(frame.right-width, frame.bottom-width2/2, frame.right,frame.bottom-width2/2,panit);
    canvas.drawLine(frame.right-width2/2, frame.bottom-width, frame.right-width2/2,frame.bottom,panit);
    canvas.drawLine(frame.right-width2/2, frame.top, frame.right-width2/2,frame.top+width,panit);

  }

  public void drawlaser(Rect frame,Paint paint,Canvas canvas){

    if(!this.hasLasertin){
      laserTop = frame.top+1;
      this.hasLasertin = true;
    }
    scan_line_DesRect = new Rect(frame.left + 15, laserTop, frame.right - 15, laserTop + 6);

    canvas.drawBitmap(scan_line_Bitmap, scan_line_SrcRect, scan_line_DesRect, paint);

    //canvas.drawRect(frame.left + 2, laserTop, frame.right - 1, laserTop + 3, paint);
    //int f = laserSpeed(laserTop,frame);
    //Log.d("yw",f+"");
    laserTop += laserSpeed(laserTop,frame);
    if(laserTop > frame.bottom) laserTop = frame.top;
  }

  public int laserSpeed(int laserTop,Rect frame){
    int height = frame.bottom - frame.top;
    int half = (frame.bottom + frame.top)/2;
    if(laserTop>=frame.top && laserTop<=half){
      float tan = 15.0f/(half-frame.top);
      return (int) Math.ceil( tan*(laserTop - frame.top))+1;
    }else{
      float tan = 15.0f/(frame.bottom - half);
      return (int)Math.ceil(tan*(frame.bottom - laserTop))+1;
    }
  }

}
