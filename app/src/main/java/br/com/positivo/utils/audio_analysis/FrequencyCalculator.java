package br.com.positivo.utils.audio_analysis;

import br.com.positivo.utils.Complex;
import br.com.positivo.utils.FFT;

/**
 * Find the two most powerful frequencies of a sample array using FFT.
 */
public class FrequencyCalculator
{
    /**
     * Run a FFT on the samples and return information about the most two powerful frequencies found.
     * @param bins The number of bins for the FFT (power of two).
     * @param samples The samples to be analyzed.
     * @param samplesOffset The start offset of the samples array to begin the FFT.
     * @param samplesCount The number of items in the samples array.
     * @param sampleRate The sample rate in Hz to allow the frequency calculation.
     * @return The first row of vector contain the powerful frequency found at position 0 and the absolute value of FFT at position 1. The second row is the same, but for the second most powerful frequency.
     */
	public static float [][] calculate(final int bins, final short[] samples, final int samplesOffset, final int samplesCount, final int sampleRate)
    {
		Complex[] fft = new Complex[bins];
        int fftIdx = 0;
		for (int sampleIdx = samplesOffset; fftIdx < bins && sampleIdx < samplesCount; sampleIdx++, fftIdx++)
            fft[fftIdx] = new Complex(samples[sampleIdx], 0.0);

        final int fftVectorLen = fft.length;
        for (; fftIdx < fftVectorLen; fftIdx++)
            fft[fftIdx] = new Complex(0, 0);

        // calculate FFT
        fft = FFT.fft(fft);

        // find the powerful bins
        float powerfulTwoFrequencies[][] = { { 0, 0 }, {0, 0 }};

        final int firstFftHalf = bins / 2;
		for (int i = 1; i < firstFftHalf; i++)
        {
            final float abs = (float)fft[i].abs();
			if (abs > powerfulTwoFrequencies[0][1])
            {
                powerfulTwoFrequencies[1][0] = powerfulTwoFrequencies[0][0];
                powerfulTwoFrequencies[1][1] = powerfulTwoFrequencies[0][1];

                powerfulTwoFrequencies[0][0] = i;
                powerfulTwoFrequencies[0][1] = abs;
            }
		}

        // to calculate the THD, we use the first, second and third harmonics.
        // we considerer the harmonic power the powerful value around the harmonic bin (-1,0,+1)
        /*double THD = 0;
        for (int harmonic = 1; harmonic <= 3; harmonic++)
        {
            int harmonicBin = (int)powerfulTwoFrequencies[0][0] * (harmonic + 1);
            double harmonicPower = -10000.0;
            for (int bin = harmonicBin - 1; bin <= harmonicBin + 1; bin++)
            {
                if (fft[bin].abs() > harmonicPower)
                    harmonicPower = fft[bin].abs();
            }

            THD += harmonicPower;
        }
        THD /= fft[(int)powerfulTwoFrequencies[0][0]].abs();
        THD *= 100.0;*/

        powerfulTwoFrequencies[0][0] = (powerfulTwoFrequencies[0][0] * sampleRate) / (bins);
        powerfulTwoFrequencies[1][0] = (powerfulTwoFrequencies[1][0] * sampleRate) / (bins);
        return powerfulTwoFrequencies;
	}
}
