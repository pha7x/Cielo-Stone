package br.com.positivo.utils.audio_analysis;

/**
 * Analyses audio samples calculating the RMS level and THD+N values.
 * @Author Leandro G. B. Becker
 */
public final class AudioSamplesAnalyzer
{
    final double[] _RMS;
    final double[] _THDN;

    public double RMS(int channel) { return _RMS[channel]; }
    public double THDN(int channel) { return _THDN[channel]; }

    public AudioSamplesAnalyzer(final short [] pcm16Samples,
                                final int channels,
                                final int samplesPerSecond,
                                final double frequencyToFilterForTHDN
                                )
    {
        final double[] noiseRMS = new double[channels];
        final double[] fundamentalOnlyRMS = new double[channels];
        _RMS  = new double[channels];
        _THDN = new double[channels];

        final BiquadFilter[]  notchFilters = new BiquadFilter[channels];
        final BiquadFilter[]  bandPassFilters = new BiquadFilter[channels];
        final OnePoleFilter[] DCFilters  = new OnePoleFilter[channels];

        for (int channel = 0; channel < channels; channel++)
        {
            _RMS[channel] = 0.0;
            fundamentalOnlyRMS[channel] = 0.0;
            noiseRMS[channel] = 0.0;
            notchFilters[channel] = new BiquadFilter(BiquadFilter.FilterType.bq_type_notch, frequencyToFilterForTHDN /  (double)samplesPerSecond, 0.9, 30.0);
            bandPassFilters[channel] = new BiquadFilter(BiquadFilter.FilterType.bq_type_bandpass, frequencyToFilterForTHDN /  (double)samplesPerSecond, 30.0, 30.0);
            DCFilters[channel] = new OnePoleFilter(5.0 / (double)samplesPerSecond);
        }

        // calculates the RMS value for each channel and the THD+N
        int filteredSavedSamplesNumber = 0, rmsValidSamples = 0;
        final int pcmSamplesLength = pcm16Samples.length;
        for (int i = 400, filteredSamplesNumber = 0; i < pcmSamplesLength; i += channels, filteredSamplesNumber++)
        {
            for (int channel = 0; channel < channels; channel++)
            {
                final double val = (double)pcm16Samples[i + channel] / (double)Short.MAX_VALUE; // normalizes the value to be in range -1 ... + 1
                _RMS[channel] += val * val;
                rmsValidSamples++;

                if (frequencyToFilterForTHDN > 0)
                {
                    // filter out the desired frequency and calculate the remaining data RMS
                    double noise = val;
                    //noise -= pDcFilters[channel].process(val);
                    noise =  notchFilters[channel].process(noise);

                    double fundamental = val;
                    //fundamental -= DCFilters[channel].process(val);
                    fundamental =  bandPassFilters[channel].process(fundamental);

                    if (filteredSamplesNumber > 399 * channels) // skip the filter ripple
                    {
                        noiseRMS[channel] += noise * noise;
                        fundamentalOnlyRMS[channel] += fundamental * fundamental;
                        filteredSavedSamplesNumber++;
                    }
                }
            }
        }

        // finishes the RMS calculation for the signal and filtered signal to calc the THD+N
        for (int channel = 0; channel < channels; channel++)
        {
            _RMS[channel] = Math.sqrt(_RMS[channel] / (double) (rmsValidSamples / channels));
            noiseRMS[channel] = Math.sqrt(noiseRMS[channel] / (double) (filteredSavedSamplesNumber / channels));
            fundamentalOnlyRMS[channel] = Math.sqrt(fundamentalOnlyRMS[channel] / (double) (filteredSavedSamplesNumber / channels));
            _THDN[channel] = noiseRMS[channel] / fundamentalOnlyRMS[channel];
        }
    }
}
