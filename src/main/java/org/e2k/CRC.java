// Copyright (C) 2017 Daniel Ekmann
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.e2k;

public class CRC {
	private int width;
	private int polynomial;
	private int initialVal;
	private int finalXorVal;
	private boolean inputReflected;
	private boolean resultReflected;
	private int castMask;
	private int msbMask;
	private int[] crcTable;

	public CRC(int width, int polynomial, int initialVal, int finalXorVal, boolean inputReflected, boolean resultReflected) {
		this.width = width;
		this.polynomial = polynomial;
		this.initialVal = initialVal;
		this.finalXorVal = finalXorVal;
		this.inputReflected = inputReflected;
		this.resultReflected = resultReflected;

		castMask = (1 << width) - 1;
		msbMask = 1 << (width - 1);

		crcTable = new int[256];
		for (int divident = 0; divident < 256; ++divident) {
			int currByte = (divident << (width - 8)) & castMask;
			for (int bit = 0; bit < 8; ++bit) {
				if ((currByte & msbMask) != 0) {
					currByte <<= 1;
					currByte ^= polynomial;
				} else {
					currByte <<= 1;
				}
			}
			crcTable[divident] = currByte & castMask;
		}
	}

	public int compute(int[] bytes) {
		int crc = initialVal;
		for (int i = 0; i < bytes.length; ++i) {
			int currByte = bytes[i] & 0xff;
			if (inputReflected) {
				currByte = reflect8(currByte);
			}
			crc = (crc ^ (currByte << (width - 8))) & castMask;
			int pos = (crc >> (width - 8)) & 0xff;
			crc = (crc << 8) & castMask;
			crc = (crc ^ crcTable[pos]) & castMask;
		}
		if (resultReflected) {
			crc = reflectGeneric(crc, width);
		}
		return (crc ^ finalXorVal) & castMask;
	}

	private int reflect8(int val) {
		int resByte = 0;
		for (int i = 0; i < 8; ++i) {
			if ((val & (1 << i)) != 0) {
				resByte |= (1 << (7 - i)) & 0xff;
			}
		}
		return resByte;
	}

	private int reflectGeneric(int val, int width) {
		int resByte = 0;
		for (int i = 0; i < width; ++i) {
			if ((val & (1 << i)) != 0) {
				resByte |= 1 << ((width - 1) - i);
			}
		}
		return resByte;
	}
}
