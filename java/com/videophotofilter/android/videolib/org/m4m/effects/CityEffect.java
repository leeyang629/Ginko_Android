/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.videophotofilter.android.videolib.org.m4m.effects;

import com.videophotofilter.android.videolib.org.m4m.android.graphics.VideoEffect;
import com.videophotofilter.android.videolib.org.m4m.domain.graphics.IEglUtil;

public class CityEffect extends VideoEffect {
    public CityEffect(int angle, IEglUtil eglUtil) {
        super(angle, eglUtil);
        setFragmentShader(getFragmentShader());
    }

    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  vec4 color = texture2D(sTexture, vTextureCoord);\n" +
                "  float colorR = color.r;\n" +
                "  float colorG = color.g * 0.85;\n" +
                "  float colorB = color.b * 0.2;\n" +
                "  gl_FragColor = vec4(colorR, colorG, colorB, 0.4);\n" +
                "}\n";
    }
}
