/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.fmradio;
/**
 * This class define FM native interface, will description FM native interface
 */
public class FmNative {
    static {
        System.loadLibrary("fmjni");
    }
    /**
     * Open FM device, call before power up
     *
     * @return (true,success; false, failed)
     */
    static public native boolean openDev();
    /**
     * Close FM device, call after power down
     *
     * @return (true, success; false, failed)
     */
    static public native boolean closeDev();
    /**
     * power up FM with frequency use long antenna
     *
     * @param frequency frequency(50KHZ, 87.55; 100KHZ, 87.5)
     *
     * @return (true, success; false, failed)
     */
    static public native boolean powerUp(float frequency);
    /**
     * Power down FM
     *
     * @param type (0, FMRadio; 1, FMTransimitter)
     *
     * @return (true, success; false, failed)
     */
    static public native boolean powerDown(int type);
    /**
     * tune to frequency
     *
     * @param frequency frequency(50KHZ, 87.55; 100KHZ, 87.5)
     *
     * @return (true, success; false, failed)
     */
    static public native boolean tune(float frequency);
    /**
     * seek with frequency in direction
     *
     * @param frequency frequency(50KHZ, 87.55; 100KHZ, 87.5)
     * @param isUp (true, next station; false previous station)
     *
     * @return frequency(float)
     */
    static public native float seek(float frequency, boolean isUp);
    /**
     * Auto scan(from 87.50-108.00)
     *
     * @return The scan station array(short)
     */
    static public native short[] autoScan();
    /**
     * Stop scan, also can stop seek, other public native when scan should call stop
     * scan first, else will execute wait auto scan finish
     *
     * @return (true, can stop scan process; false, can't stop scan process)
     */
    static public native boolean stopScan();
    /**
     * Open or close rds fuction
     *
     * @param rdson The rdson (true, open; false, close)
     *
     * @return rdsset
     */
    static public native int setRds(boolean rdson);
    /**
     * Read rds events
     *
     * @return rds event type
     */
    static public native short readRds();
    /**
     * Get program service(program name)
     *
     * @return The program name
     */
    static public native byte[] getPs();
    /**
     * Get radio text, RDS standard does not support Chinese character
     *
     * @return The LRT (Last Radio Text) bytes
     */
    static public native byte[] getLrText();
    /**
     * Active alterpublic native frequencies
     *
     * @return The frequency(float)
     */
    static public native short activeAf();
    /**
     * Mute or unmute FM voice
     *
     * @param mute (true, mute; false, unmute)
     *
     * @return (true, success; false, failed)
     */
    static public native int setMute(boolean mute);
    /**
     * Inquiry if RDS is support in driver
     *
     * @return (1, support; 0, NOT support; -1, error)
     */
    static public native int isRdsSupport();
    /**
     * Switch antenna
     *
     * @param antenna antenna (0, long antenna, 1 short antenna)
     *
     * @return (0, success; 1 failed; 2 not support)
     */
    static public native int switchAntenna(int antenna);

    // FM EM start
    /**
     * get rssi from hardware(use for engineer mode)
     *
     * @return rssi value
     */
    static public native int readRssi();

    /**
     * Inquiry if fm stereo mono(true, stereo; false mono)
     *
     * @return (true, stereo; false, mono)
     */
    static public native boolean stereoMono();

    /**
     * Force set to stero/mono mode
     *
     * @param isMono
     *            (true, mono; false, stereo)
     * @return (true, success; false, failed)
     */
    static public native boolean setStereoMono(boolean isMono);

    /**
     * Read cap array of short antenna
     *
     * @return cap array value
     */
    static public native short readCapArray();

    /**
     * read rds bler
     *
     * @return rds bler value
     */
    static public native short readRdsBler();

    /**
     * send variables to public native, and get some variables return.
     * @param val send to native
     * @return get value from native
     */
    static public native short[] emcmd(short[] val);

    /**
     * set RSSI, desense RSSI, mute gain soft
     * @param index flag which will execute
     * (0:rssi threshold,1:desense rssi threshold,2: SGM threshold)
     * @param value send to native
     * @return execute ok or not
     */
    static public native boolean emsetth(int index, int value);

    /**
     * get hardware version
     *
     * @return hardware version information array(0, ChipId; 1, EcoVersion; 2, PatchVersion; 3,
     *         DSPVersion)
     */
    static public native int[] getHardwareVersion();
}