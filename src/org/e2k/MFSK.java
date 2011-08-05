package org.e2k;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MFSK {
	private DoubleFFT_1D ft=new DoubleFFT_1D(1024);
	private double fft_percentage;
	
	// Return the number of samples per baud
	public double samplesPerSymbol (double dbaud,double sampleFreq)	{
		return (sampleFreq/dbaud);
	}
	
	// Test for a specific tone
	public boolean toneTest (int freq,int tone,int errorAllow)	{
	    if ((freq>(tone-errorAllow))&&(freq<(tone+errorAllow))) return true;
	     else return false;
	  }
	
	// Find the bin containing the hight value from an array of doubles
	private int findHighBin(double[]x)	{
		int a,highBin=-1;
		double highVal=-1,secondHigh=-1;
		for (a=0;a<(x.length/2);a++)	{
			if (x[a]>highVal)	{
				secondHigh=highVal;
				highVal=x[a];
				highBin=a;
			}
		}
		// Calculate the percentage difference between the highest and second highest bins
		fft_percentage=100-((secondHigh/highVal)*100.0);
		// Return the highest bin position
		return highBin;
	}
		
	// Given the real data in a double array return the largest frequency component
	private int getFFTFreq (double[]x,double sampleFreq,int correctionFactor)	{
		int bin=findHighBin(x);
		double len=x.length;
		double ret=(((sampleFreq/len)*bin)/2)-correctionFactor;
		return (int)ret;
	}
	
	// We have a problem since FFT sizes must be to a power of 2 but samples per symbol can be any value
	// So instead I am doing a FFT in the middle of the symbol
	public int symbolFreq (Boolean huntMode,CircularDataBuffer circBuf,WaveData waveData,int start,double samplePerSymbol)	{
		// There must be at least 1024 samples Per Symbol
		if (samplePerSymbol<1024) return -1;
		final int fftSIZE=1024;
		int fftStart=start+(((int)samplePerSymbol-fftSIZE)/2);
		double freq=doFFT(circBuf,waveData,fftStart,fftSIZE);
		// In hunt mode a single frequency must be 90% larger than any other frequency
		if ((huntMode==true)&&(fft_percentage<90.0)) return -1;
		else return (int)freq;
	}
	
	public int doFFT (CircularDataBuffer circBuf,WaveData waveData,int start,int length)	{
		// Get the data from the circular buffer
	    double datar[]=circBuf.extractDataDouble(start,length);
		ft.realForward(datar);
		int freq=getFFTFreq (datar,waveData.sampleRate,waveData.correctionFactor);  
		return freq;
	}

}
