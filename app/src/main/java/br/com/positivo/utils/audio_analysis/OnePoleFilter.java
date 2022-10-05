package br.com.positivo.utils.audio_analysis;

/**
 * Implements a one pole digital filter (DC Filter).
 *
 *  Created by Nigel Redmon on 11/24/12
 *  EarLevel Engineering: earlevel.com
 *  Copyright 2012 Nigel Redmon
 *
 *  http://www.earlevel.com/main/2012/11/26/biquad-c-source-code/
 *
 *  License:
 *
 *  This source code is provided as is, without warranty.
 *  You may copy and distribute verbatim copies of this document.
 *  You may modify and use this source code to create binary code
 *  for your own purposes, free or commercial.
 */
public final class OnePoleFilter
{
    final double a0, b1;
    double z1 = 0.0;

    public OnePoleFilter(double Fc)
    {
        b1 = Math.exp(-2.0 * Math.PI * Fc);
        a0 = 1.0 - b1;
    }

    public double process(double in) {
        return z1 = in * a0 + z1 * b1;
    }
}
