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

import java.awt.*;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

public class DisplayFrame extends JFrame implements ActionListener {
	
	private JMenuBar menuBar=new JMenuBar();
	private Rivet theApp;
	public static final long serialVersionUID=1;
	private JStatusBar statusBar=new JStatusBar();
	public JScrollBar vscrollbar=new JScrollBar(JScrollBar.VERTICAL,0,1,0,6000);
	private JMenu triggersMenu=new JMenu("Triggers");
	private JMenuItem exit_item,wavLoad_item,save_to_file,about_item,help_item,debug_item,soundcard_item,reset_item,copy_item,bitstream_item;
	private JMenuItem XPA_10_item,XPA_20_item,XPA2_item,CROWD36_item,experimental_item,CIS3650_item,FSK200500_item,CCIR493_item,GW_item,RTTY_item;
	private JMenuItem FSK2001000_item,CROWD36_sync_item,invert_item,save_settings_item,sample_item,e2k_item,twitter_item;
	private JMenuItem freeChannelMarkerGW_item,RTTYOptions_item,FSK_item,AddEditTrigger_item,credits_item,system_info_item;
	private JMenuItem ClearDisplay_item,DisplayBad_item,DisplayUTC_item,UDXF_item,CIS360Options_item;
	private JMenuItem F06a_item, F06aEncoding_item, priyom_item;
	private List<JMenuItem> trigger_items=new ArrayList<JMenuItem>();
	private JMenu audioDevicesMenu;
	private static ArrayList<AudioMixer> devices;
	
	// Constructor
	public DisplayFrame(String title,Rivet theApp) {
		setTitle(title);
		this.theApp=theApp;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.WHITE);
		// Read in the trigger.xml file
		try	{
			theApp.readTriggerSettings();
		}
		catch (Exception e)	{
			String err=e.toString();
			// Can't find the default settings file //
			System.out.println("\nInformative : Unable to read the file trigger.xml "+err);
		}		
		// Read in the default settings file
		try	{
			theApp.readDefaultSettings();
			// Update the soundcard input setting
			boolean cin=theApp.issoundCardInputTemp();
			theApp.setSoundCardInput(cin);
		}
		catch (Exception e)	{
			String err=e.toString();
			// Can't find the default settings file //
			System.out.println("\nInformative : Unable to read the file rivet_settings.xml "+err);
		}
		// Menu setup
		setJMenuBar(menuBar);
		// Main
		JMenu mainMenu=new JMenu("Main");
		mainMenu.add(copy_item=new JMenuItem("Copy All to the Clipboard"));
		copy_item.addActionListener(this);
		mainMenu.add(wavLoad_item=new JMenuItem("Load a WAV File"));		
		wavLoad_item.addActionListener(this);
		mainMenu.add(reset_item=new JMenuItem("Reset Decoding State"));
		reset_item.addActionListener(this);
		mainMenu.add(save_settings_item=new JMenuItem("Save the Current Settings"));
		save_settings_item.addActionListener(this);
		mainMenu.add(save_to_file=new JRadioButtonMenuItem("Save to File",theApp.getLogging()));
		save_to_file.addActionListener(this);
		mainMenu.add(bitstream_item=new JRadioButtonMenuItem("Save Bit Stream to File",theApp.isBitStreamOut()));
		bitstream_item.addActionListener(this);		
		mainMenu.add(soundcard_item=new JRadioButtonMenuItem("Soundcard Input",theApp.isSoundCardInput()));
		soundcard_item.addActionListener(this);
		mainMenu.add(exit_item=new JMenuItem("Exit"));		
		exit_item.addActionListener(this);
		menuBar.add(mainMenu);
		// Audio 
		JMenu audioMenu=new JMenu("Audio");
		audioDevicesMenu=buildAudioDevices();
		audioMenu.add(audioDevicesMenu);
		audioDevicesMenu.updateUI();
		menuBar.add(audioMenu);
		// Modes
		JMenu modeMenu=new JMenu("Modes");
		modeMenu.add(RTTY_item=new JRadioButtonMenuItem(theApp.MODENAMES[10],theApp.isRTTY()));
		RTTY_item.addActionListener(this);
		modeMenu.add(CCIR493_item=new JRadioButtonMenuItem(theApp.MODENAMES[7],theApp.isCCIR493()));
		CCIR493_item.addActionListener(this);
		modeMenu.add(CIS3650_item=new JRadioButtonMenuItem(theApp.MODENAMES[5],theApp.isCIS3650()));
		CIS3650_item.addActionListener(this);
		modeMenu.add(CROWD36_item=new JRadioButtonMenuItem(theApp.MODENAMES[0],theApp.isCROWD36()));
		CROWD36_item.addActionListener(this);
		modeMenu.add(FSK200500_item=new JRadioButtonMenuItem(theApp.MODENAMES[6],theApp.isFSK200500()));
		FSK200500_item.addActionListener(this);
		modeMenu.add(FSK2001000_item=new JRadioButtonMenuItem(theApp.MODENAMES[8],theApp.isFSK2001000()));
		FSK2001000_item.addActionListener(this);
		modeMenu.add(F06a_item=new JRadioButtonMenuItem(theApp.MODENAMES[12],theApp.isF06a()));
		F06a_item.addActionListener(this);
		modeMenu.add(FSK_item=new JRadioButtonMenuItem(theApp.MODENAMES[11],theApp.isFSK()));
		FSK_item.addActionListener(this);	
		modeMenu.add(GW_item=new JRadioButtonMenuItem(theApp.MODENAMES[9],theApp.isGW()));
		GW_item.addActionListener(this);
		modeMenu.add(XPA_10_item=new JRadioButtonMenuItem(theApp.MODENAMES[1],theApp.isXPA_10()));
		XPA_10_item.addActionListener(this);
		modeMenu.add(XPA_20_item=new JRadioButtonMenuItem(theApp.MODENAMES[3],theApp.isXPA_20()));
		XPA_20_item.addActionListener(this);
		modeMenu.add(XPA2_item=new JRadioButtonMenuItem(theApp.MODENAMES[2],theApp.isXPA2()));
		XPA2_item.addActionListener(this);
		modeMenu.addSeparator();
		modeMenu.add(experimental_item=new JRadioButtonMenuItem(theApp.MODENAMES[4],theApp.isExperimental()));
		experimental_item.addActionListener(this);
		menuBar.add(modeMenu);
		// Options
		JMenu optionsMenu=new JMenu("Options");
		optionsMenu.add(RTTYOptions_item=new JMenuItem("Baudot & FSK Options"));		
		RTTYOptions_item.addActionListener(this);
		optionsMenu.add(CIS360Options_item=new JMenuItem("CIS36-50 Options"));		
		CIS360Options_item.addActionListener(this);
		optionsMenu.add(debug_item=new JRadioButtonMenuItem("Debug Mode",theApp.isDebug()));		
		debug_item.addActionListener(this);
		optionsMenu.add(invert_item=new JRadioButtonMenuItem("Invert",theApp.isInvertSignal()));
		invert_item.addActionListener(this);
		optionsMenu.add(CROWD36_sync_item=new JMenuItem("Set the CROWD36 Sync High Tone"));
		CROWD36_sync_item.addActionListener(this);
		optionsMenu.add(F06aEncoding_item=new JRadioButtonMenuItem("F06a ASCII parsing",theApp.isF06aASCII()));
		F06aEncoding_item.addActionListener(this);
		menuBar.add(optionsMenu);
		// Triggers
		updateTriggerMenuItems();
		menuBar.add(triggersMenu);
		// View
		JMenu viewMenu=new JMenu("View");
		viewMenu.add(ClearDisplay_item=new JMenuItem("Clear Display"));	
		ClearDisplay_item.addActionListener(this);
		viewMenu.add(DisplayBad_item=new JRadioButtonMenuItem("Display Possible Bad Data",theApp.isDisplayBadPackets()));
		DisplayBad_item.addActionListener(this);
		viewMenu.add(DisplayUTC_item=new JRadioButtonMenuItem("Display UTC Time",theApp.isLogInUTC()));
		DisplayUTC_item.addActionListener(this);
		viewMenu.add(freeChannelMarkerGW_item=new JRadioButtonMenuItem("View GW Free Channel Markers",theApp.isViewGWChannelMarkers()));
		freeChannelMarkerGW_item.addActionListener(this);
		menuBar.add(viewMenu);
		// Help
		JMenu helpMenu=new JMenu("Help");
		helpMenu.add(about_item=new JMenuItem("About"));		
		about_item.addActionListener(this);
		helpMenu.add(credits_item=new JMenuItem("Credits"));		
		credits_item.addActionListener(this);
		helpMenu.add(sample_item=new JMenuItem("Download the latest version of Rivet or sound sample files"));		
		sample_item.addActionListener(this);
		helpMenu.add(e2k_item=new JMenuItem("Enigma2000"));
		e2k_item.addActionListener(this);
		helpMenu.add(twitter_item=new JMenuItem("Follow Rivet original author on Twitter"));		
		twitter_item.addActionListener(this);
		helpMenu.add(help_item=new JMenuItem("Help"));		
		help_item.addActionListener(this);
		helpMenu.add(system_info_item=new JMenuItem("System Information"));	
		system_info_item.addActionListener(this);
		helpMenu.add(UDXF_item=new JMenuItem("UDXF"));		
		UDXF_item.addActionListener(this);
		helpMenu.add(priyom_item=new JMenuItem("priyom.org"));
		priyom_item.addActionListener(this);
		menuBar.add(helpMenu);
		// Add the vertical scroll bar
		add(vscrollbar,BorderLayout.EAST);
		// Add a listener for this
		vscrollbar.addAdjustmentListener(new MyAdjustmentListener());
		// Add a mouse event listener to the vertical scroll bar
		vscrollbar.addMouseWheelListener(new MouseAdjustmentListener());
		// Add a mouse wheel event listener to the main screen
		this.addMouseWheelListener(new MouseAdjustmentListener());
		
		// Setup the status bar
		getContentPane().add(statusBar, java.awt.BorderLayout.SOUTH);
		statusBar.setLoggingStatus("Not Logging");
		statusBar.setStatusLabel("Idle");
		statusBar.setApp(theApp);
		
		// Update the menus
		menuItemUpdate();
		// Update the status bar
		statusBarUpdate();
		}

	
	// Handle messages from the scrollbars
	class MyAdjustmentListener implements AdjustmentListener  {
		public void adjustmentValueChanged(AdjustmentEvent e) {
			// Vertical scrollbar
			if (e.getSource()==vscrollbar) {
				theApp.vertical_scrollbar_value=e.getValue();
				// Is this a user scroll operation
				if (vscrollbar.getValueIsAdjusting()==true)	{
					// Record the time that this occurred
					theApp.setLastUserScroll(System.currentTimeMillis()/1000);
					// Turn off auto scroll
					theApp.setAutoScroll(false);
				}
				// Redraw
				repaint();   
			}
		}
	 }
	
	// Handle all mouse adjustment events
	class MouseAdjustmentListener implements MouseWheelListener	{
		// Handle any mousewheel events
		public void mouseWheelMoved(MouseWheelEvent me) {
			int notches=me.getWheelRotation();
			int vc=vscrollbar.getValue();
			final int ADJUST=4;
			// Down 
			if (notches>0)	{
				vc=vc+ADJUST;
				vscrollbar.setValue(vc);
			}
			// Up
			else if (notches<0)	{
				if (vc>ADJUST) vc=vc-ADJUST;
				vscrollbar.setValue(vc);
			}
			else return;
			// Record the user has done this
			// Record the time that this occurred
			theApp.setLastUserScroll(System.currentTimeMillis()/1000);
			// Turn off auto scroll for now
			theApp.setAutoScroll(false);
		}
	}
	
	// Handle all menu events
	public void actionPerformed (ActionEvent event) {
		String event_name=event.getActionCommand();	
		// Copy all
		if (event_name=="Copy All to the Clipboard")	{
			String contents=theApp.getAllText();
			setClipboard(contents);
		}
		// About
		if (event_name=="About")	{
			String line=theApp.program_version+"\r\n"+"ianwraith@gmail.com\r\nfor the Enigma2000 & UDXF groups.";
			JOptionPane.showMessageDialog(null,line,"Rivet", JOptionPane.INFORMATION_MESSAGE);
		}
		// Enigma2000
		if (event_name=="Enigma2000")	{
			BareBonesBrowserLaunch.openURL("http://www.signalshed.com");
		}
		// UDXF
		if (event_name=="UDXF")	{
			BareBonesBrowserLaunch.openURL("https://groups.io/g/UDXF");
		}
		// Help
		if (event_name=="Help") {
			BareBonesBrowserLaunch.openURL("https://github.com/IanWraith/Rivet/wiki/Introduction");
		}
		// Sound Samples
		if (event_name=="Download the latest version of Rivet or sound sample files")	{
			BareBonesBrowserLaunch.openURL("http://www.signalshed.com/rivet/");
		}
		// Twitter
		if (event_name=="Follow Rivet original author on Twitter")	{
			BareBonesBrowserLaunch.openURL("https://twitter.com/IanWraith");
		}
		//priyom.org
		if (event_name=="priyom.org"){
			BareBonesBrowserLaunch.openURL("https://priyom.org/");
		}
		// Debug mode
		if (event_name=="Debug Mode")	{
			if (theApp.isDebug()==true) theApp.setDebug(false);
			else theApp.setDebug(true);
		}
		// Run through all the mode names
		for (int a=0;a<theApp.MODENAMES.length;a++)	{
			if (event_name==theApp.MODENAMES[a]) theApp.setSystem(a);
		}
		// Load a WAV file
		if (event_name=="Load a WAV File")	{
			String fileName=loadDialogBox();
			if (fileName!=null) theApp.loadWAVfile(fileName);
		}
		// Save to File
		if (event_name=="Save to File")	{		
			if (theApp.getLogging()==false)	{
				if (saveDialogBox()==false)	{
					menuItemUpdate();
					return;
				}
				theApp.setLogging(true);
				statusBar.setLoggingStatus("Logging");
			}
			 else	{
				 closeLogFile();
			 }
		}	
		
		// Bit Stream Out
		if (event_name=="Save Bit Stream to File")	{
			if (theApp.isBitStreamOut()==false)	{
				if (saveBitStreamDialogBox()==false)	{
					menuItemUpdate();
					return;
				}
				theApp.setBitStreamOut(true);	
				theApp.clearBitStreamCountOut();
			}
			else	{
				closeBitStreamFile();
			}
		}
		
		// Soundcard Input
		if (event_name=="Soundcard Input")	{
			if (theApp.isSoundCardInput()==true) theApp.setSoundCardInput(false);
			else theApp.setSoundCardInput(true);
		}
		// Reset the decoder state
		if (event_name=="Reset Decoding State")	{
			theApp.resetDecoderState();
		}
		// Set the CROWD36 sync tone
		if (event_name=="Set the CROWD36 Sync High Tone")	{
			theApp.getCROWD36SyncHighTone();
		}
		// Baudot Options
		if (event_name=="Baudot & FSK Options")	{
			theApp.setRTTYOptions();
		}
		// CIS36-50 Options
		if (event_name=="CIS36-50 Options")	{
			theApp.setBEEOptions();
		}
		// Invert the input signal
		if (event_name=="Invert")	{
			if (theApp.isInvertSignal()==true) theApp.setInvertSignal(false);
			else theApp.setInvertSignal(true);
		}
		// Set F06a ASCII
		if (event_name=="F06a ASCII parsing") {
			if (theApp.isF06aASCII()) theApp.setF06aASCII(false);
			else theApp.setF06aASCII(true);
		}
		// Save Settings
		if (event_name=="Save the Current Settings")	{
			theApp.saveSettings();
		}
		// System Information
		if (event_name=="System Information")	{
			theApp.displaySystemInfo();
		}
		// Clear Display
		if (event_name=="Clear Display")	{
			theApp.clearScreen();
		}
		// Display possible bad data
		if (event_name=="Display Possible Bad Data")	{
			if (theApp.isDisplayBadPackets()==true) theApp.setDisplayBadPackets(false);
			else theApp.setDisplayBadPackets(true);
		}
		// View GW Free Channel Markers
		if (event_name=="View GW Free Channel Markers")	{
			if (theApp.isViewGWChannelMarkers()==true) theApp.setViewGWChannelMarkers(false);
			else theApp.setViewGWChannelMarkers(true);
		}
		// Show UTC Time
		if (event_name=="Display UTC Time")	{
			if (theApp.isLogInUTC()==true) theApp.setLogInUTC(false);
			else theApp.setLogInUTC(true);
		}
		// Exit 
		if (event_name=="Exit") {
			// If logging then close the log file
			if (theApp.getLogging()==true) closeLogFile();
			// Stop the program //
			System.exit(0);	
		}
		// Has the user clicked on a Trigger ?
		// Get details of all the triggers
		List<Trigger> trigList=theApp.getListTriggers();
		// Compare the event name with each triggers description
		int a;
		for (a=0;a<trigList.size();a++)	{
			if (event_name.equals(trigList.get(a).getTriggerDescription()+trigList.get(a).getTypeDescription()))	{
				// Change the active status of the trigger
				if (trigList.get(a).isActive()==true) trigList.get(a).setActive(false);
				else trigList.get(a).setActive(true);
				theApp.setListTriggers(trigList);
			}
		}	
		// Add,Edit or Delete a Trigger
		if (event_name=="Add,Edit or Delete a Trigger")	{
			DialogTriggerModify();
		}
		// Change mixer
		if (event_name.equalsIgnoreCase("mixer")){
			changeMixer(((JRadioButtonMenuItem)event.getSource()).getText());
		}
		// Credits 
		if (event_name=="Credits")	{
			StringBuilder sb=new StringBuilder();
			sb.append("Thanks to ..");
			sb.append("\r\nAlan W for his help with the GW MMSI decoding");
			JOptionPane.showMessageDialog(null,sb.toString(),"Rivet", JOptionPane.INFORMATION_MESSAGE);
		}
		
		menuItemUpdate();
		statusBarUpdate();
	}
	
	public void menuItemUpdate()	{
		save_to_file.setSelected(theApp.getLogging());
		CROWD36_item.setSelected(theApp.isCROWD36());
		XPA_10_item.setSelected(theApp.isXPA_10());
		XPA_20_item.setSelected(theApp.isXPA_20());
		XPA2_item.setSelected(theApp.isXPA2());
		CIS3650_item.setSelected(theApp.isCIS3650());
		CCIR493_item.setSelected(theApp.isCCIR493());
		GW_item.setSelected(theApp.isGW());
		experimental_item.setSelected(theApp.isExperimental());
		FSK_item.setSelected(theApp.isFSK());
		FSK200500_item.setSelected(theApp.isFSK200500());
		FSK2001000_item.setSelected(theApp.isFSK2001000());
		F06a_item.setSelected(theApp.isF06a());
		debug_item.setSelected(theApp.isDebug());
		soundcard_item.setSelected(theApp.isSoundCardInput());
		invert_item.setSelected(theApp.isInvertSignal());
		bitstream_item.setSelected(theApp.isBitStreamOut());
		freeChannelMarkerGW_item.setSelected(theApp.isViewGWChannelMarkers());
		DisplayBad_item.setSelected(theApp.isDisplayBadPackets());
		DisplayUTC_item.setSelected(theApp.isLogInUTC());
		RTTY_item.setSelected(theApp.isRTTY());
		F06aEncoding_item.setSelected(theApp.isF06aASCII());
		// Triggers
		List<Trigger> trigList=theApp.getListTriggers();
		int a;
		for (a=0;a<trigList.size();a++)	{
			trigger_items.get(a).setSelected(trigList.get(a).isActive());
		}
				// Audio sources
		MenuElement[] devs=audioDevicesMenu.getSubElements();
		if (devs.length>0){
				for (MenuElement m : devs[0].getSubElements()){
					if (((JRadioButtonMenuItem)m).getText().equals(theApp.inputThread.getMixerName())){
						((JRadioButtonMenuItem)m).setSelected(true);
						break;
					}
				}
			}
	}
	
	// Display a dialog box so the user can select a WAV file they wish to process
	public String loadDialogBox ()	{
		String file_name;
		// Bring up a dialog box that allows the user to select the name
		// of the WAV file to be loaded
		JFileChooser fc=new JFileChooser();
		// The dialog box title //
		fc.setDialogTitle("Select a WAV file to load");
		// Start in current directory
		fc.setCurrentDirectory(new File("."));
		// Don't all types of file to be selected //
		fc.setAcceptAllFileFilterUsed(false);
		// Only show .wav files //
		fc.setFileFilter(new WAVfileFilter());
		// Show open dialog; this method does not return until the
		// dialog is closed
		int returnval=fc.showOpenDialog(this);
		// If the user has selected cancel then quit
		if (returnval==JFileChooser.CANCEL_OPTION) return null;
		// Get the file name an path of the selected file
		file_name=fc.getSelectedFile().getPath();
		return file_name;
	}
	
	private void statusBarUpdate()	{
		statusBar.setModeLabel(theApp.MODENAMES[theApp.getSystem()]);
		// Update the soundcard input slider
		statusBar.setSoundCardInput(theApp.getSoundCardLevel());
		theApp.setSoundCardLevel(theApp.getSoundCardLevel());
	}
	
	public void progressBarUpdate (int v)	{
		statusBar.setVolumeBar(v);
	}
	
	public void setStatusLabel (String st)	{
		statusBar.setStatusLabel(st);
	}

	public void setModeLabel(String st){
		statusBar.setModeLabel(st);
		menuItemUpdate();
	}
	
	// Close the log file
	public void closeLogFile()	{
		 theApp.setLogging(false);
		 statusBar.setLoggingStatus("Not Logging");
		 try 	{
			 // If GW monitoring display a list of MMSIs logged
			 if (theApp.isGW()==true) theApp.fileWriteLine(theApp.getShipList());
			 // Close the file
			 theApp.file.flush();
			 theApp.file.close();
		 }
		 catch (Exception e)	{
			 JOptionPane.showMessageDialog(null,"Error closing Log file","Rivet", JOptionPane.INFORMATION_MESSAGE);
		 }
	}
	
	// Close the Bit Stream file
	public void closeBitStreamFile()	{
		 theApp.setBitStreamOut(false);
		 try 	{
			 // Close the file
			 theApp.bitStreamFile.flush();
			 theApp.bitStreamFile.close();
		 }
		 catch (Exception e)	{
			 JOptionPane.showMessageDialog(null,"Error closing the Bit Stream file","Rivet", JOptionPane.INFORMATION_MESSAGE);
		 }
	}
	
	// Display a dialog box so the user can select a location and name for a log file
	public boolean saveDialogBox ()	{
		if (theApp.getLogging()==true) return false;
		String file_name;
		// Bring up a dialog box that allows the user to select the name
		// of the saved file
		JFileChooser fc=new JFileChooser();
		// The dialog box title //
		fc.setDialogTitle("Select the log file name");
		// Start in current directory
		fc.setCurrentDirectory(new File("."));
		// Don't all types of file to be selected //
		fc.setAcceptAllFileFilterUsed(false);
		// Only show .txt files //
		fc.setFileFilter(new TextFileFilter());
		// Show save dialog; this method does not return until the
		// dialog is closed
		int returnval=fc.showSaveDialog(this);
		// If the user has selected cancel then quit
		if (returnval==JFileChooser.CANCEL_OPTION) return false;
		// Get the file name an path of the selected file
		file_name=fc.getSelectedFile().getPath();
		// Does the file name end in .txt ? //
		// If not then automatically add a .txt ending //
		int last_index=file_name.lastIndexOf(".txt");
		if (last_index!=(file_name.length()-4)) file_name=file_name + ".txt";
		// Create a file with this name //
		File tfile=new File(file_name);
		// If the file exists ask the user if they want to overwrite it
		if (tfile.exists()) {
			int response = JOptionPane.showConfirmDialog(null,
					"Overwrite existing file?", "Confirm Overwrite",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) return false;
		}
		// Open the file
		try {
			theApp.file=new FileWriter(tfile);
			// Write the program version as the first line of the log
			String fline=theApp.program_version+"\r\n";
			theApp.file.write(fline);
			
		} catch (Exception e) {
			System.out.println("\nError opening the logging file");
			return false;
		}
		theApp.setLogging(true);
		return true;
	}
	
	// This sets the clipboard with a string passed to it
	public void setClipboard(String str) {
	    StringSelection ss=new StringSelection(str);
	    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}
	
	// Display a dialog box so the user can select a location and name for a bit stream file
	public boolean saveBitStreamDialogBox ()	{
		if (theApp.isBitStreamOut()==true) return false;
		String file_name;
		// Bring up a dialog box that allows the user to select the name
		// of the saved file
		JFileChooser fc=new JFileChooser();
		// The dialog box title //
		fc.setDialogTitle("Select the bit stream output file name");
		// Start in current directory
		fc.setCurrentDirectory(new File("."));
		// Don't all types of file to be selected //
		fc.setAcceptAllFileFilterUsed(false);
		// Only show .txt files //
		fc.setFileFilter(new BitStreamFileFilter());
		// Show save dialog this method does not return until the dialog is closed
		int returnval=fc.showSaveDialog(this);
		// If the user has selected cancel then quit
		if (returnval==JFileChooser.CANCEL_OPTION) return false;
		// Get the file name an path of the selected file
		file_name=fc.getSelectedFile().getPath();
		// Does the file name end in .bsf ? //
		// If not then automatically add a .bsf ending //
		int last_index=file_name.lastIndexOf(".bsf");
		if (last_index!=(file_name.length()-4)) file_name=file_name + ".bsf";
		// Create a file with this name //
		File tfile=new File(file_name);
		// If the file exists ask the user if they want to overwrite it
		if (tfile.exists()) {
			int response = JOptionPane.showConfirmDialog(null,
					"Overwrite existing file?", "Confirm Overwrite",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) return false;
		}
		// Open the file
		try {
			theApp.bitStreamFile=new FileWriter(tfile);
			
		} catch (Exception e) {
			System.out.println("\nError opening the bit stream file");
			return false;
		}
		theApp.setBitStreamOut(true);
		return true;
	}
	
	// Open a Trigger modify dialog box
	void DialogTriggerModify ()	{
		TriggerModify triggerModify=new TriggerModify(this,theApp);
		// Check if any changes were made to the triggers and if so update the menu items
		if (triggerModify.isChangedTriggers()==true) updateTriggerMenuItems();
	}
	
	// Redraw the Triggers menu item
	void updateTriggerMenuItems ()	{
		// First remove anything that may be already on this menu
		triggersMenu.removeAll();
		// Get details of all the triggers
		List<Trigger> trigList=theApp.getListTriggers();
		int a;
		for (a=0;a<trigList.size();a++)	{
			JMenuItem tmenu=new JRadioButtonMenuItem(trigList.get(a).getTriggerDescription()+trigList.get(a).getTypeDescription(),trigList.get(a).isActive());
			tmenu.addActionListener(this);
			trigger_items.add(tmenu);
			triggersMenu.add(tmenu);
		}
		triggersMenu.addSeparator();
		// Add the Trigger Add/Edit menu item
		triggersMenu.add(AddEditTrigger_item=new JMenuItem("Add,Edit or Delete a Trigger"));
		AddEditTrigger_item.addActionListener(this);
		// Save these triggers
		if (theApp.saveTriggerXMLFile()==false)	{
			JOptionPane.showMessageDialog(null,"Error saving the Triggers","Rivet", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private JMenu buildAudioDevices(){
		JMenu ret=new JMenu("Audio Devices");
		ButtonGroup group=new ButtonGroup();
		ArrayList<AudioMixer> deviceList=getCompatibleDevices();
		int i;
		for (i=0; i<deviceList.size(); i++){
			//Line.Info l[]=AudioSystem.getTargetLineInfo(deviceList.get(i).lineInfo);
			JRadioButtonMenuItem dev=new JRadioButtonMenuItem(deviceList.get(i).description);
			dev.setActionCommand("mixer");
			dev.addActionListener(this);
			if (i==0) dev.setSelected(true);
			group.add(dev);
			ret.add(dev);
		}
		return ret;
	}
	
	// Provide a list of all compatable sound sources
	private ArrayList<AudioMixer> getCompatibleDevices(){
		devices=new ArrayList<AudioMixer>();
		//list the available mixers
		Mixer.Info mixers[]=AudioSystem.getMixerInfo();
		int i;
		//iterate the mixers and display TargetLines
		for (i=0;i<mixers.length;i++){
			Mixer m=AudioSystem.getMixer(mixers[i]);
			Line.Info l[]=m.getTargetLineInfo();
			// Check these exist and are "Capture" devices
			if((l.length>0)&&((m.getMixerInfo().getDescription().endsWith("Capture")==true))){
				int x;
				for (x=0;x<l.length;x++){
					if (l[0].getLineClass().getName().equals("javax.sound.sampled.TargetDataLine"))	{
						AudioMixer mc=new AudioMixer(mixers[i].getName(),m,l[x]);
						devices.add(mc);			
					}
				}
			}
		}
		return devices;
	}
	
	// Signal to the main program to change its audio mixer
	private void changeMixer(String mixerName){
		if (theApp.changeMixer(mixerName)==false)	{
			JOptionPane.showMessageDialog(null,"Error changing mixer\n"+theApp.inputThread.getMixerErrorMessage(),"Rivet",JOptionPane.ERROR_MESSAGE);
		}
	}	

	
	public void scrollDown(int v)	{
		vscrollbar.setValue(v);
	}
	
	public boolean isAdjusting()	{
		return vscrollbar.getValueIsAdjusting();
	}
	
	// Tell the status bar that this is a small screen
	public void setSmallScreen()	{
		statusBar.setSmallScreen();
	}

	
	
}
