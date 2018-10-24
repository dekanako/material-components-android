/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.google.android.material.shadow;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Shader;
import android.support.annotation.RestrictTo;
import android.support.v4.graphics.ColorUtils;

/**
 * A helper class to draw linear or radial shadows using gradient shaders.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ShadowRenderer {

  /** Gradient start color of 68 which evaluates to approximately 26% opacity. */
  private static final int COLOR_ALPHA_START = 0x44;
  /** Gradient start color of 20 which evaluates to approximately 8% opacity. */
  private static final int COLOR_ALPHA_MIDDLE = 0x14;

  private static final int COLOR_ALPHA_END = 0;

  private final Paint shadowPaint;
  private final Paint cornerShadowPaint;
  private final Paint edgeShadowPaint;

  private int shadowStartColor;
  private int shadowMiddleColor;
  private int shadowEndColor;

  private static final int[] edgeColors = new int[3];
  /** Start, middle of shadow, and end of shadow positions */
  private static final float[] edgePositions = new float[] {0f, .5f, 1f};

  private static final int[] cornerColors = new int[4];
  /** Start, beginning of corner, middle of shadow, and end of shadow positions */
  private static final float[] cornerPositions = new float[] {0f, 0f, .5f, 1f};

  private final Path scratch = new Path();

  public ShadowRenderer() {
    this(Color.BLACK);
  }

  public ShadowRenderer(int color) {
    setShadowColor(color);

    cornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    cornerShadowPaint.setStyle(Paint.Style.FILL);

    shadowPaint = new Paint();
    shadowPaint.setColor(shadowStartColor);

    edgeShadowPaint = new Paint(cornerShadowPaint);
    edgeShadowPaint.setAntiAlias(false);
  }

  public void setShadowColor(int color) {
    shadowStartColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_START);
    shadowMiddleColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_MIDDLE);
    shadowEndColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_END);
  }

  /** Draws an edge shadow on the canvas in the current bounds with the matrix transform applied. */
  public void drawEdgeShadow(Canvas canvas, Matrix transform, RectF bounds, int elevation) {
    bounds.bottom += elevation;
    bounds.offset(0, -elevation);

    edgeColors[0] = shadowEndColor;
    edgeColors[1] = shadowMiddleColor;
    edgeColors[2] = shadowStartColor;

    edgeShadowPaint.setShader(
        new LinearGradient(
            bounds.left,
            bounds.top,
            bounds.left,
            bounds.bottom,
            edgeColors,
            edgePositions,
            Shader.TileMode.CLAMP));

    // TODO: handle angled edges, or cut corners by rotating the matrix

    canvas.save();
    canvas.concat(transform);
    canvas.drawRect(bounds, edgeShadowPaint);
    canvas.restore();
  }

  /**
   * Draws a corner shadow on the canvas in the current bounds with the matrix transform applied.
   */
  public void drawCornerShadow(
      Canvas canvas,
      Matrix matrix,
      RectF bounds,
      int elevation,
      float startAngle,
      float sweepAngle) {

    boolean drawShadowInsideBounds = sweepAngle < 0;

    Path arcBounds = scratch;

    if (drawShadowInsideBounds) {
      cornerColors[0] = 0;
      cornerColors[1] = shadowEndColor;
      cornerColors[2] = shadowMiddleColor;
      cornerColors[3] = shadowStartColor;
    } else {
      // Calculate the arc bounds to prevent drawing shadow in the same part of the arc.
      arcBounds.rewind();
      arcBounds.moveTo(bounds.centerX(), bounds.centerY());
      arcBounds.arcTo(bounds, startAngle, sweepAngle);
      arcBounds.close();

      bounds.inset(-elevation, -elevation);
      cornerColors[0] = 0;
      cornerColors[1] = shadowStartColor;
      cornerColors[2] = shadowMiddleColor;
      cornerColors[3] = shadowEndColor;
    }

    float startRatio = 1f - (elevation / (bounds.width() / 2f));
    float midRatio = startRatio + ((1f - startRatio) / 2f);
    cornerPositions[1] = startRatio;
    cornerPositions[2] = midRatio;

    cornerShadowPaint.setShader(
        new RadialGradient(
            bounds.centerX(),
            bounds.centerY(),
            bounds.width() / 2,
            cornerColors,
            cornerPositions,
            Shader.TileMode.CLAMP));

    // TODO: handle oval bounds by scaling the canvas.

    canvas.save();
    canvas.concat(matrix);

    if (!drawShadowInsideBounds) {
      canvas.clipPath(arcBounds, Op.DIFFERENCE);
    }

    canvas.drawArc(bounds, startAngle, sweepAngle, true, cornerShadowPaint);
    canvas.restore();
  }

  public Paint getShadowPaint() {
    return shadowPaint;
  }
}
