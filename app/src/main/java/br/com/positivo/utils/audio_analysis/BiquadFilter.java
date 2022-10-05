package br.com.positivo.utils.audio_analysis;

/**
 * Implements a biquad digital filter.
 * 
 *  Created by Nigel Redmon on 11/24/12
 *  EarLevel Engineering: earlevel.com
 *  Copyright 2012 Nigel Redmon
 *
 *  For a complete explanation of the CBiquadFilter code:
 *  http://www.earlevel.com/main/2012/11/26/biquad-c-source-code/
 *
 *  License:
 *
 *  This source code is provided as is, without warranty.
 *  You may copy and distribute verbatim copies of this document.
 *  You may modify and use this source code to create binary code
 *  for your own purposes, free or commercial.
 */
public final class BiquadFilter
{
    public enum FilterType {
        bq_type_lowpass,
        bq_type_highpass,
        bq_type_bandpass,
        bq_type_notch,
        bq_type_peak,
        bq_type_lowshelf,
        bq_type_highshelf
    }

    final FilterType type;
    final double a0, a1, a2, b1, b2;
    final double Fc, Q, peakGain;
    double z1 = 0.0, z2 = 0.0;

    BiquadFilter(FilterType type, double Fc, double Q, double peakGainDB)
    {
        this.peakGain = peakGainDB;
        this.Fc = Fc;
        this.Q = Q;
        this.type = type;

        final double norm;
        final double V = Math.pow(10, Math.abs(peakGain) / 20.0);
        final double K = Math.tan(Math.PI * Fc);
        switch (this.type)
        {
            case bq_type_lowpass:
                norm = 1.0 / (1.0 + K / Q + K * K);
                a0 = K * K * norm;
                a1 = 2.0 * a0;
                a2 = a0;
                b1 = 2.0 * (K * K - 1.0) * norm;
                b2 = (1.0 - K / Q + K * K) * norm;
                break;

            case bq_type_highpass:
                norm = 1.0 / (1.0 + K / Q + K * K);
                a0 = 1.0 * norm;
                a1 = -2.0 * a0;
                a2 = a0;
                b1 = 2.0 * (K * K - 1.0) * norm;
                b2 = (1.0 - K / Q + K * K) * norm;
                break;

            case bq_type_bandpass:
                norm = 1.0 / (1.0 + K / Q + K * K);
                a0 = K / Q * norm;
                a1 = 0;
                a2 = -a0;
                b1 = 2.0 * (K * K - 1.0) * norm;
                b2 = (1.0 - K / Q + K * K) * norm;
                break;

            case bq_type_notch:
                norm = 1.0 / (1.0 + K / Q + K * K);
                a0 = (1.0 + K * K) * norm;
                a1 = 2.0 * (K * K - 1.0) * norm;
                a2 = a0;
                b1 = a1;
                b2 = (1.0 - K / Q + K * K) * norm;
                break;

            case bq_type_peak:
                if (peakGain >= 0) {    // boost
                    norm = 1.0 / (1.0 + 1.0/Q * K + K * K);
                    a0 = (1.0 + V/Q * K + K * K) * norm;
                    a1 = 2.0 * (K * K - 1.0) * norm;
                    a2 = (1.0 - V/Q * K + K * K) * norm;
                    b1 = a1;
                    b2 = (1.0 - 1.0/Q * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1.0 / (1.0 + V/Q * K + K * K);
                    a0 = (1.0 + 1.0/Q * K + K * K) * norm;
                    a1 = 2.0 * (K * K - 1.0) * norm;
                    a2 = (1.0 - 1.0/Q * K + K * K) * norm;
                    b1 = a1;
                    b2 = (1.0 - V/Q * K + K * K) * norm;
                }
                break;
            case bq_type_lowshelf:
                if (peakGain >= 0) {    // boost
                    norm = 1.0 / (1.0 + Math.sqrt(2.0) * K + K * K);
                    a0 = (1.0 + Math.sqrt(2.0 * V) * K + V * K * K) * norm;
                    a1 = 2.0 * (V * K * K - 1.0) * norm;
                    a2 = (1.0 - Math.sqrt(2.0 * V) * K + V * K * K) * norm;
                    b1 = 2.0 * (K * K - 1.0) * norm;
                    b2 = (1.0 - Math.sqrt(2.0) * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1.0 / (1.0 + Math.sqrt(2.0 * V) * K + V * K * K);
                    a0 = (1.0 + Math.sqrt(2.0) * K + K * K) * norm;
                    a1 = 2.0 * (K * K - 1.0) * norm;
                    a2 = (1.0 - Math.sqrt(2.0) * K + K * K) * norm;
                    b1 = 2.0 * (V * K * K - 1.0) * norm;
                    b2 = (1.0 - Math.sqrt(2.0 * V) * K + V * K * K) * norm;
                }
                break;
            case bq_type_highshelf:
                if (peakGain >= 0) {    // boost
                    norm = 1.0 / (1.0 + Math.sqrt(2.0) * K + K * K);
                    a0 = (V + Math.sqrt(2 * V) * K + K * K) * norm;
                    a1 = 2 * (K * K - V) * norm;
                    a2 = (V - Math.sqrt(2 * V) * K + K * K) * norm;
                    b1 = 2 * (K * K - 1.0) * norm;
                    b2 = (1.0 - Math.sqrt(2.0) * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1.0 / (V + Math.sqrt(2.0 * V) * K + K * K);
                    a0 = (1.0 + Math.sqrt(2.0) * K + K * K) * norm;
                    a1 = 2 * (K * K - 1.0) * norm;
                    a2 = (1.0 - Math.sqrt(2.0) * K + K * K) * norm;
                    b1 = 2 * (K * K - V) * norm;
                    b2 = (V - Math.sqrt(2.0 * V) * K + K * K) * norm;
                }
                break;
            default:
                a0 = 0; a1 = 0; a2 = 0; b1 = 0; b2 = 0;
                break;
        }
    }

    public double process(double in)
    {
        final double out = in * a0 + z1;
        z1 = in * a1 + z2 - b1 * out;
        z2 = in * a2 - b2 * out;
        return out;
    }
}
