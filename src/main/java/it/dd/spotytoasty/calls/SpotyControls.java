package it.dd.spotytoasty.calls;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Control Spotify using media keys
 * 
 * @author shamalaya
 *
 */
public class SpotyControls {
	
	/**
	 * MEDIA KEYS CODE
	 */
	public final static int VK_MEDIA_NEXT_TRACK	= 0xB0;	
	public final static int VK_MEDIA_PREV_TRACK	= 0xB1;
	public final static int VK_MEDIA_PLAY_PAUSE	= 0xB3;		

	/**
	 * KEY EVENTS
	 */
	private static final int KEYEVENTF_KEYDOWN = 0;
	private static final int KEYEVENTF_KEYUP = 2;

	/**
	 * Press and release a key.
	 * Using JNA to call native windows sendinput()
	 * @param c
	 */
	public static void pressKey(int c) {

		WinUser.INPUT input = new WinUser.INPUT();
		input.type = new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD );
		input.input.setType("ki");

		input.input.ki.wScan = new WinDef.WORD( 0 );
		input.input.ki.time = new WinDef.DWORD( 0 );
		input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR( 0 );

		// Press
		input.input.ki.wVk  = new WinDef.WORD( c );
		input.input.ki.dwFlags = new WinDef.DWORD( KEYEVENTF_KEYDOWN );  
		User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );

		// Release
		input.input.ki.wVk  = new WinDef.WORD( c );
		input.input.ki.dwFlags = new WinDef.DWORD( KEYEVENTF_KEYUP );  
		User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );
	}
}