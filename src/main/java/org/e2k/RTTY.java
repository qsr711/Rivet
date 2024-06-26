// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.Color;
import javax.swing.JOptionPane;

public class RTTY extends FSK {
	
	private double baudRate=50;
	private int state=0;
	private double samplesPerSymbol;
	private double sampleRate;
	private Rivet theApp;
	public long sampleCount=0;
	private long symbolCounter=0;
	private CircularDataBuffer energyBuffer=new CircularDataBuffer();
	private int characterCount=0;
	private int highBin;
	private int lowBin;
	private boolean inChar7[]=new boolean[7];
	private boolean inChar8[]=new boolean[8];
	private final int MAXCHARLENGTH=100;
	private int bcount;
	private long missingCharCounter=0;
	private double adjBuffer[]=new double[2];
	private int adjCounter=0;
	private int shift=450;
	private double stopBits=1.5;
	private int previousbcount=0;
	private double symbolTotal;
	private double previousSymbolTotal;
	private double oldSymbolPercentage[]=new double[4];
	
	public RTTY (Rivet tapp)	{
		theApp=tapp;
		samplesPerSymbol=samplesPerSymbol(baudRate,8000);
	}
	
	public void setBaudRate(double br) {
		if (br!=this.baudRate) setState(0);
		this.baudRate=br;
		//Recalculate samples per symbol
		samplesPerSymbol=samplesPerSymbol(baudRate,sampleRate);
	}

	public double getBaudRate() {
		return baudRate;
	}

	// Set the objects decode state and the status bar
	public void setState(int state) {
		this.state=state;
		if (state==1) theApp.setStatusLabel("Sync Hunt");
		else if (state==2) theApp.setStatusLabel("Decoding Traffic");
	}

	public int getState() {
		return state;
	}
	
	public boolean decode (CircularDataBuffer circBuf,WaveData waveData)	{

		// Just starting
		if (state==0)	{
			// Check the sample rate
			if (waveData.getSampleRate()!=8000.0 && waveData.getSampleRate()!=12000.0)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"WAV files containing\nRTTY recordings must have\nbeen recorded at a sample rate\nof 8 KHz or 12 Khz.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a mono recording
			if (waveData.getChannels()!=1)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\nmono WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			// Check this is a 16 bit WAV file
			if (waveData.getSampleSizeInBits()!=16)	{
				state=-1;
				JOptionPane.showMessageDialog(null,"Rivet can only process\n16 bit WAV files.","Rivet", JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
			setState(1);
			//Update sample rate and samples per second variables
			sampleRate= waveData.getSampleRate();
			samplesPerSymbol=samplesPerSymbol(baudRate,sampleRate);
			// sampleCount must start negative to account for the buffer gradually filling
			sampleCount=0-circBuf.retMax();
			symbolCounter=0;
			// Clear the energy buffer
			energyBuffer.setBufferCounter(0);
			// Clear the display side of things
			characterCount=0;
			lettersMode=true;
			return true;
		}
		
		// Hunt for the sync sequence
		if (state==1)	{
			String sRet;
			if (sampleCount>0)	{
				sRet=syncSequenceHunt(circBuf,waveData);
				if (sRet!=null)	{
					theApp.writeLine(sRet,Color.BLACK,theApp.italicFont);
					setState(2);
					energyBuffer.setBufferCounter(0);
					bcount=0;
					missingCharCounter=0;
					// Display on a newline from now on
					theApp.newLineWrite();
				}
			}
		}
				
		// Decode traffic
		if (state==2)	{
			// Only do this at the start of each symbol
			if (symbolCounter>=samplesPerSymbol)	{
				boolean cend=false;
				int ibit=rttyFreqHalf(circBuf,waveData,0);
				if (theApp.isInvertSignal()==true)	{
					if (ibit==0) ibit=1;
					else if (ibit==1) ibit=0;
				}
				// Is the bit stream being recorded ?
				if (theApp.isBitStreamOut()==true)	{
					if (ibit==1) theApp.bitStreamWrite("1");
					else if (ibit==0) theApp.bitStreamWrite("0");
					else if (ibit==2) theApp.bitStreamWrite("2");
					else if (ibit==3) theApp.bitStreamWrite("3");
				}
				
				
				// Shuffle the old stored percentage values
				oldSymbolPercentage[3]=oldSymbolPercentage[2];
				oldSymbolPercentage[2]=oldSymbolPercentage[1];
				oldSymbolPercentage[1]=oldSymbolPercentage[0];
				// Calculate the current percentage value
				if (symbolTotal<previousSymbolTotal) oldSymbolPercentage[0]=100.0-((symbolTotal/previousSymbolTotal)*100.0);
				else oldSymbolPercentage[0]=100.0-((previousSymbolTotal/symbolTotal)*100.0);
				double av=(oldSymbolPercentage[0]+oldSymbolPercentage[1]+oldSymbolPercentage[2]+oldSymbolPercentage[3])/4;
				// If the percentage different is over 40% and more than two characters are missing then the signal has been lost
				if ((av>40.0)&&(missingCharCounter>2)) setState(1);
					
				// Is this the end of a character
				if (((!theApp.isInvertSignal() && (stopBits==1.5)||(stopBits==2.5))&&(ibit==2)) || (theApp.isInvertSignal() && (stopBits==1.5)||(stopBits==2.5))&&(ibit==3))	{
					cend=true;
				}
				else if (stopBits==1)	{
					if (checkValid15()==true) cend=true;
				}
				else if (stopBits==2)	{
					if (checkValid20()==true) cend=true;
				}
				if (bcount<7) cend=false;
				if (cend==true)	{
					symbolCounter=(int)samplesPerSymbol/2;
					// If debugging display the character buffer in binary form + the number of bits since the last character and this baudot character
					if (theApp.isDebug()==true)	{
						theApp.writeLine((getCharBuffer()+" ("+Integer.toString(bcount)+")  "+getBaudotChar()),Color.BLACK,theApp.italicFont);
					}
					else	{
						// Display the character in the standard way
						String ch=getBaudotChar();
						// LF
						if (ch.equals(getBAUDOT_LETTERS(2)))	{
							characterCount=0;
							theApp.newLineWrite(); 
						}
						// CR
						else if (ch.equals(getBAUDOT_LETTERS(8)))	{
							characterCount=0; 
						}
						else	{
							theApp.writeChar(ch,Color.BLACK,theApp.boldMonospaceFont);
							characterCount++;
						}
					}
					// If more than 8 bits have gone still no character we have a problem
					if (bcount>8) missingCharCounter++;
					// If this character and the last character arrived on time clear the mising char counter
					if ((bcount<9)&&(previousbcount<9)) missingCharCounter=0;
					// Store this bcount before it is cleared
					previousbcount=bcount;
				    // Clear the bit counter
					bcount=0;
				}
				else	{
					addToCharBuffer(ibit);
					symbolCounter=adjAdjust();
				}
				// If the character count has reached MAXCHARLENGTH then display this line and write a newline to the screen
				if (characterCount>=MAXCHARLENGTH)	{
					characterCount=0;
					theApp.newLineWrite();
				}
			}
		}
		sampleCount++;
		symbolCounter++;
		return true;				
	}

	public int getShift() {
		return shift;
	}

	public void setShift(int shift) {
		this.shift = shift;
	}	
	
	
	// Find the frequency of a RTTY symbol
	// Currently the program only supports a sampling rate of 8000 KHz and 12000 Khz
	private int rttyFreq (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		// 8 and 12 KHz sampling
		if (waveData.getSampleRate()==8000.0 || waveData.getSampleRate()==12000.0)	{
			int freq=doRTTY_FFT(circBuf,waveData,pos,(int)samplesPerSymbol,baudRate);
			return freq;
		}
		return -1;
	}
	
	// Look for a sequence of 4 alternating tones with a certain shift
	private String syncSequenceHunt (CircularDataBuffer circBuf,WaveData waveData)	{
		int difference;
		// Get 4 symbols
		int freq1=rttyFreq(circBuf,waveData,0);
		int bin1=getFreqBin();
		// Check this first tone isn't just noise
		if (getPercentageOfTotal()<5.0) return null;
		int freq2=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*1);
		int bin2=getFreqBin();
		// Check we have a high low
		if (freq2>freq1) return null;
		// Check there is around shift (+25 and -25 Hz) of difference between the tones
		difference=freq1-freq2;
		if ((difference<(shift-25))||(difference>(shift+25))) return null;
		int freq3=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*2);
		// Don't waste time carrying on if freq1 isn't the same as freq3
		if (freq1!=freq3) return null;
		int freq4=rttyFreq(circBuf,waveData,(int)samplesPerSymbol*3);
		// Check 2 of the symbol frequencies are different
		if ((freq1!=freq3)||(freq2!=freq4)) return null;
		// Check that 2 of the symbol frequencies are the same
		if ((freq1==freq2)||(freq3==freq4)) return null;
		// Store the bin numbers
		if (freq1>freq2)	{
			highBin=bin1;
			lowBin=bin2;
		}
		else	{
			highBin=bin2;
			lowBin=bin1;
		}
		// If either the low bin or the high bin are zero there is a problem so return false
		if ((lowBin==0)||(highBin==0)) return null;
		String line=theApp.getTimeStamp()+" RTTY Sync Sequence Found";
		return line;
	}
	
	// Add incoming data to the character buffer
	private void addToCharBuffer (int in)	{
		int a;
		// 7 bit buffer
		for (a=1;a<inChar7.length;a++)	{
			inChar7[a-1]=inChar7[a];
		}
		if (in==0) inChar7[6]=false;
		else inChar7[6]=true;
		// 8 bit buffer
		for (a=1;a<inChar8.length;a++)	{
			inChar8[a-1]=inChar8[a];
		}
		if (in==0) inChar8[7]=false;
		else inChar8[7]=true;
		// Increment the bit counter
		bcount++;
	}
	
	// Display the inChar buffer in binary when in debug mode
	private String getCharBuffer()	{
		StringBuilder lb=new StringBuilder();
		int a;
		// 1 or 1.5 stop bits
		if (stopBits<2)	{
			for (a=0;a<7;a++)	{
				if (inChar7[a]==true) lb.append("1");
				else lb.append("0");
				if ((a==0)||(a==5)) lb.append(" ");
			}
		}
		// 2 or more stop bits
		else	{
			for (a=0;a<8;a++)	{
				if (inChar8[a]==true) lb.append("1");
				else lb.append("0");
				if ((a==0)||(a==5)) lb.append(" ");
			}				
		}
		return lb.toString();
	}
	
	// Returns the baudot character in the character buffer
	private String getBaudotChar()	{
		int a=0;
		// 1 or 1.5 stop bits
		if (stopBits<2)	{
			if (inChar7[5]==true) a=16;
			if (inChar7[4]==true) a=a+8;
			if (inChar7[3]==true) a=a+4;
			if (inChar7[2]==true) a=a+2;
			if (inChar7[1]==true) a++;
		}
		// 2 or 2.5 stop bits
		else	{
			if (inChar8[5]==true) a=16;
			if (inChar8[4]==true) a=a+8;
			if (inChar8[3]==true) a=a+4;
			if (inChar8[2]==true) a=a+2;
			if (inChar8[1]==true) a++;
		}
		// Look out for figures or letters shift characters
		if (a==0)	{
			return "";
		}
		else if (a==27)	{
			lettersMode=false;
			return "";
		}
		else if (a==31)	{
			lettersMode=true;
			return "";
		}
		if (lettersMode==true) return getBAUDOT_LETTERS(a);
		else return getBAUDOT_NUMBERS(a);
	}
	
	// Check if this a valid Baudot character this a start and a stop
	private boolean checkValid15()	{
		if ((inChar7[0]==false)&&(inChar7[6]==true)&&(bcount>=7)) return true;
		else return false;
	}
	
	// Look for 2 stop bits
	private boolean checkValid20()	{
		if ((inChar8[0]==false)&&(inChar8[6]==true)&&(inChar8[7]==true)&&(bcount>=8)) return true;
		else return false;
	}
	
	// Add a comparator output to a circular buffer of values
	private void addToAdjBuffer (double in)	{
		adjBuffer[adjCounter]=in;
		adjCounter++;
		if (adjCounter==adjBuffer.length) adjCounter=0;
	}
	
	// Return the average of the circular buffer
	private double adjAverage()	{
		int a;
		double total=0.0;
		for (a=0;a<adjBuffer.length;a++)	{
			total=total+adjBuffer[a];
		}
		return (total/adjBuffer.length);
	}
	
	// Get the average value and return an adjustment value
	private int adjAdjust()	{
		double av=adjAverage();
		double r=Math.abs(av)/5;
		if (av<0) r=0-r;
		//theApp.debugDump(Double.toString(av)+","+Double.toString(r));
		//r=0;
		return (int)r;
	}	

	
	// The "normal" way of determining the frequency of a RTTY symbol
	// is to do two FFTs of the first and last halves of the symbol
	// that allows us to use the data for the early/late gate and to detect a half bit (which is used as a stop bit)
	private int rttyFreqHalf (CircularDataBuffer circBuf,WaveData waveData,int pos)	{
		int v=0;
		int sp=(int)samplesPerSymbol/2;
		// First half
		double early[]=doRTTYHalfSymbolBinRequest(baudRate,circBuf,pos,sp,lowBin,highBin);
		// Last half
		double late[]=doRTTYHalfSymbolBinRequest(baudRate,circBuf,(pos+sp),sp,lowBin,highBin);
		// Store the previous symbol energy total
		previousSymbolTotal=symbolTotal;
		symbolTotal=early[0]+late[0]+early[1]+late[1];
		// Determine the symbol value
		int high1,high2;
		if (early[0]>early[1]) high1=0;
		else high1=1;
		if (late[0]>late[1]) high2=0;
		else high2=1;		
		// Both the same
		if (high1==high2)	{
			if (high1==0) v=1;
			else v=0;
		}
		else	{
			// 1.5 stop bits
			if (stopBits==1.5)	{
				// Test if this really could be a half bit
				if (checkValid15()==true)	{
					// Is this a stop bit
					if (high2>high1) v=2;
					else v=3;
					// No this must be a normal full bit
					//else if ((early[0]+late[0])>(early[1]+late[1])) v=1;
					//else v=0;
				}
			}
			// 2.5 stop bits
			else if (stopBits==2.5)	{
				if (checkValid20()==true)	{
					// Is this a stop bit
					if (high2>high1) v=2;
					else v=3;
					// No this must be a normal full bit
					//else if ((early[0]+late[0])>(early[1]+late[1])) v=1;
					//else v=0;
				}
			}
			else	{
				// If there isn't a vaid baudot character in the buffer this can't be a half bit and must be a full bit
				if ((early[0]+late[0])>(early[1]+late[1])) v=1;
				else v=0;
			}
			
		}
		// Early/Late gate code
		// was <2
		if (v<2)	{
			double lowTotal=early[0]+late[0];
			double highTotal=early[1]+late[1];
			if (lowTotal>highTotal) addToAdjBuffer(getPercentageDifference(early[0],late[0]));
			else addToAdjBuffer(getPercentageDifference(early[1],late[1]));
			
			//theApp.debugDump(Double.toString(early[0])+","+Double.toString(late[0])+","+Double.toString(early[1])+","+Double.toString(late[1]));
			
		}
		
	return v;
	}

	public double getStopBits() {
		return stopBits;
	}

	public void setStopBits(double stopBits) {
		this.stopBits = stopBits;
	}
	
}
