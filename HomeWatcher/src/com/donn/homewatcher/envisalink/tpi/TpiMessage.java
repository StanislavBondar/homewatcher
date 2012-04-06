package com.donn.homewatcher.envisalink.tpi;

import com.donn.homewatcher.Event;

import android.content.SharedPreferences;

public class TpiMessage {
	
	private String originalMessage = "";
	private String trimmedMessage = "";
	private String generalData = "";
	private String checkSum = "";
	private Event panelEvent;
	private String englishDescription = "";
	private int code = -1;
	private int partition = -1;
	private int zone = -1;
	private int user = -1;
	private int mode = -1;
	
	private SharedPreferences sharedPreferences;
	
	/**
	 * @param trimmedMessage
	 * @param preferences - the preferences file from which zone and other English information will be read
	 */
	public TpiMessage(Event event, SharedPreferences preferences) {
		this.panelEvent = event;
		this.originalMessage = event.getMessage();
		this.sharedPreferences = preferences;
		parseMessage();
	}
	
	/**
	 * Based on EnvisalinkTPI-1-03.PDF - 04-06-2012
	 */
	private void parseMessage() {
		
		try {
			verifyChecksum(originalMessage);
			
			if (originalMessage.length() > 3) {
				//TODO: Check last two digits of checksum - verify instead of ignoring
				trimmedMessage = originalMessage.substring(0, originalMessage.length() - 2);
				checkSum = originalMessage.substring(originalMessage.length() - 2);
				code = Integer.parseInt(trimmedMessage.substring(0, 3));
			}
			else {			
				englishDescription = "ERROR Processing Message: " + originalMessage;
				code = 000;
			}
		}
		catch (Exception e) {
			englishDescription = "ERROR Processing Message: " + e.getMessage() + ": " + originalMessage;
			code = 000;
		}
		
		switch (code) {
		
			case 500: 
				englishDescription = "Command Acknowledge";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 501:
				englishDescription = "Command Error";
				break;
				
			case 502: 
				englishDescription = "System Error";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 505:
				englishDescription = "Login Response (ok=0, fail=1, timeout=2, login=3)";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 510:
				englishDescription = "Keypad LED State - Partition 1 Only";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 511:
				englishDescription = "Keypad LED Flash State - Partition 1 Only";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 550: 
				englishDescription = "Time-Date Broadcast";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 560:
				englishDescription = "Ring Detected";
				break;
				
			case 561: 
				englishDescription = "Indoor Temperature Broadcast";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 562:
				englishDescription = "Outdoor Temperature Broadcast";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 601:
				englishDescription = "Zone Alarm";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				zone = Integer.parseInt(trimmedMessage.substring(4));
				break;
				
			case 602:
				englishDescription = "Zone Alarm Restore";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				zone = Integer.parseInt(trimmedMessage.substring(4));
				break;
				
			case 603:
				englishDescription = "Zone Tamper";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				zone = Integer.parseInt(trimmedMessage.substring(4));
				break;
			
			case 604:
				englishDescription = "Zone Tamper Restore";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				zone = Integer.parseInt(trimmedMessage.substring(4));
				break;
				
			case 605:
				englishDescription = "Zone Fault";
				zone = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 606:
				englishDescription = "Zone Fault Restore";
				zone = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 609:
				englishDescription = "Zone Open";
				zone = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 610:
				englishDescription = "Zone Restored";
				zone = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 615:
				englishDescription = "Envisalink Zone Timer Dump";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 620:
				englishDescription = "Duress Alarm";
				generalData = trimmedMessage.substring(3);
				break;
			
			case 621:
				englishDescription = "Fire Key Alarm";
				break;
			
			case 622:
				englishDescription = "Fire Key Alarm Restoral";
				break;
				
			case 623:
				englishDescription = "Aux Key Alarm";
				break;
				
			case 624:
				englishDescription = "Aux Key Alarm Restoral";
				break;
				
			case 625:
				englishDescription = "Panic Alarm";
				break;
				
			case 626:
				englishDescription = "Panic Alarm Restoral";
				break;
				
			case 650:
				englishDescription = "Partition Ready";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 651:
				englishDescription = "Partition Not Ready";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 652:
				englishDescription = "Partition Armed (0=Away/1=Stay/2=ZEA/3=ZES)";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				mode = Integer.parseInt(trimmedMessage.substring(4));
				break;
				
			case 653:
				englishDescription = "Partition Armed - Force Arming Enabled";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 654:
				englishDescription = "Partition In Alarm";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 655:
				englishDescription = "Partition Disarmed";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 656:
				englishDescription = "Exit Delay In Progress";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 657:
				englishDescription = "Entry Delay In Progress";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 658:
				englishDescription = "Keypad Logout";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 659:
				englishDescription = "Partition Failed to Arm";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 660:
				englishDescription = "PGM Output is in Progress";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 663:
				englishDescription = "Chime Enabled";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 664:
				englishDescription = "Chime Disabled";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 670:
				englishDescription = "Invalid Access Code";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 671:
				englishDescription = "Function Not Available";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 672:
				englishDescription = "Failure to Arm";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 673:
				englishDescription = "Partition is Busy";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 674:
				englishDescription = "System Arming in Progress";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 700:
				englishDescription = "User Closing";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				generalData = trimmedMessage.substring(4);
				break;
				
			case 701:
				englishDescription = "Special Closing";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 702:
				englishDescription = "Partial Closing";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 750:
				englishDescription = "User Opening";
				partition = Integer.parseInt(trimmedMessage.substring(3, 4));
				generalData = trimmedMessage.substring(4);
				break;
				
			case 751:
				englishDescription = "Special Opening";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 800:
				englishDescription = "Panel Battery Trouble";
				break;
				
			case 801:
				englishDescription = "Panel Battery Trouble Restore";
				break;

			case 802:
				englishDescription = "Panel AC Trouble";
				break;

			case 803:
				englishDescription = "Panel AC Trouble Restore";
				break;

			case 806:
				englishDescription = "System Bell Trouble";
				break;

			case 807:
				englishDescription = "System Bell Trouble Restore";
				break;

			case 814:
				englishDescription = "FTC Trouble";
				break;

			case 816:
				englishDescription = "Buffer Near Full";
				break;

			case 829:
				englishDescription = "General System Tamper";
				break;

			case 830:
				englishDescription = "General System Tamper Restore";
				break;
				
			case 840:
				englishDescription = "Trouble LED ON";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 841:
				englishDescription = "Trouble LED OFF";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 842:
				englishDescription = "Fire Trouble Alarm";
				break;
				
			case 843:
				englishDescription = "Fire Trouble Alarm Restore";
				break;
				
			case 849:
				englishDescription = "Verbose Trouble Status";
				generalData = trimmedMessage.substring(3);
				break;
				
			case 850:
				englishDescription = "Partition Busy";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 851:
				englishDescription = "Partition Busy Restore";
				partition = Integer.parseInt(trimmedMessage.substring(3));
				break;
				
			case 900:
				englishDescription = "Code Required";
				break;
				
			case 921:
				englishDescription = "Master Code Required";
				break;
				
			case 922:
				englishDescription = "Installers Code Required";
				break;
				
			default:
				englishDescription = "Unknown Code: " + code;
				break;
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(englishDescription);
		sb.append(" --");
		
		if (partition > -1) {
			sb.append(" partition: ");
			sb.append(partition);
		}
		if (zone > -1) {
			//Maps zones to the actual "english" named zones, from preferences
			sb.append(" zone " + zone + ": ");
			sb.append(sharedPreferences.getString("z" + zone, ""));
		}
		if (generalData != null && generalData.length() > 0) {
			sb.append(" data: ");
			sb.append(generalData);
		}
//		if (code > 0) {
//			sb.append(" code: ");
//			sb.append(code);
//		}
		if (user > -1) {
			sb.append(" user: ");
			sb.append(user);
		}
		if (mode > -1) {
			sb.append(" mode: ");
			sb.append(mode);
		}
//		if (checkSum != null) {
//			sb.append(" checkSum :");
//			sb.append(checkSum);
//		}
		
		return sb.toString();
	}

	public String getGeneralData() {
		return generalData;
	}

	public String getEnglishDescription() {
		return englishDescription;
	}

	public int getPartition() {
		return partition;
	}

	public int getZone() {
		return zone;
	}

	public int getUser() {
		return user;
	}

	public int getMode() {
		return mode;
	}
	
	public int getCode() {
		return code;
	}
	
	public Event getPanelEvent() {
		return panelEvent;
	}
	
	private void verifyChecksum(String completeString) throws Exception {
		int checkSum;
		int runningTotal = 0;
		String dataString = completeString.substring(0, completeString.length() - 2);
		String checkSumString = completeString.substring(completeString.length() - 2);
		
		for(byte s : dataString.getBytes()) {
			runningTotal = s + runningTotal;
		}
		
		checkSum = runningTotal;
		String hexCheckSum = Integer.toHexString(checkSum);
		hexCheckSum = hexCheckSum.substring(hexCheckSum.length() - 2).toUpperCase();

		if (checkSumString.equals(hexCheckSum)) {
			//Do nothing, it checks out
		}
		else {
			throw new Exception("Invalid checksum. Received checksum: " + 
					checkSumString + " Calc'd checksum: " + hexCheckSum);
		}
	}
}
