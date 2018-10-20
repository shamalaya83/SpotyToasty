package it.dd.spotytoasty.calls;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import static com.sun.jna.platform.win32.WTypes.CLSCTX_INPROC_SERVER;

/**
 * Control Spotify volume only through WINDOWS SOUND API
 * 
 * @author shamalaya
 *
 */
public class SpotyVolume {

	/**
	 * ID
	 */
	private static final Guid.CLSID CLSID_MMDeviceEnumerator = new Guid.CLSID("BCDE0395-E52F-467C-8E3D-C4579291692E");

	private static final Guid.IID IID_IMMDeviceEnumerator = new Guid.IID("A95664D2-9614-4F35-A746-DE8DB63617E6");
	private static final Guid.IID IID_IAudioSessionManager2 = new Guid.IID("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F");
	private static final Guid.IID IID_ISimpleAudioVolume = new Guid.IID("87ce5498-68d6-44e5-9215-6da47ef883d8");

	/**
	 * Pointers
	 */
	private static final PointerByReference MMDeviceEnumerator = new PointerByReference();
	private static final PointerByReference MMDevice = new PointerByReference();
	private static final PointerByReference AudioSessionManager2 = new PointerByReference();
	private static final PointerByReference AudioSessionEnumerator = new PointerByReference();
	private static final PointerByReference SimpleAudioVolume = new PointerByReference();

	/**
	 * VTABLE
	 */
	private static final int VTABLE_IMMDeviceEnumerator_GetDefaultAudioEndpoint = 4 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IMMDevice_Activate = 3 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IAudioSessionManager2_GetSessionEnumerator = 5 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IAudioSessionEnumerator_GetCount = 3 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IAudioSessionEnumerator_GetSession = 4 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IAudioSessionControl2_GetSessionIdentifier = 12 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_IAudioSessionControl2_QueryInterface = 0 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_ISimpleAudioVolume_SetMasterVolume = 3 * WinDef.DWORDLONG.SIZE;
	private static final int VTABLE_ISimpleAudioVolume_SetMute = 5 * WinDef.DWORDLONG.SIZE;
	
	/**
	 * Singleton
	 */
	private static SpotyVolume spotyVolume;

	public static SpotyVolume getInstance() {
		if( spotyVolume==null )
			spotyVolume = new SpotyVolume();

		return spotyVolume;
	}

	/**
	 * Initialize WASAPI
	 */
	private SpotyVolume() {

		// Set MULTITHREAD Environment
		HRESULT ris = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
		if( !WinNT.S_OK.equals(ris) ) {
			throw new RuntimeException( "Ole32::CoInitializeEx() error result: " + ris.intValue() ); 
		}
		
		// device enumerator 
		if (!WinNT.S_OK.equals(Ole32.INSTANCE.CoCreateInstance(
				CLSID_MMDeviceEnumerator,
				null,
				CLSCTX_INPROC_SERVER,
				IID_IMMDeviceEnumerator,
				MMDeviceEnumerator))) {
			throw new RuntimeException("Ole32::CoCreateInstance() failed");
		}

		// device
		Pointer MMDeviceEnumeratorPointer = MMDeviceEnumerator.getValue();
		Pointer MMDeviceEnumeratorVirtualTable = MMDeviceEnumeratorPointer.getPointer(0);
		Function GetDefaultAudioEndpoint = Function.getFunction(MMDeviceEnumeratorVirtualTable.getPointer(VTABLE_IMMDeviceEnumerator_GetDefaultAudioEndpoint), Function.ALT_CONVENTION);

		int eDataFlow = 0;     // eRender
		int eRole = 1;         // eMultimedia
		if (!WinNT.S_OK.equals(GetDefaultAudioEndpoint.invoke(WinNT.HRESULT.class, new Object[]{MMDeviceEnumeratorPointer, eDataFlow, eRole, MMDevice}))) {
			throw new RuntimeException("IMMDeviceEnumerator::GetDefaultAudioEndpoint() failed");
		}

		// session manager
		Pointer MMDevicePointer = MMDevice.getValue();
		Pointer MMDeviceVirtualTable = MMDevicePointer.getPointer(0);
		Function Activate = Function.getFunction(MMDeviceVirtualTable.getPointer(VTABLE_IMMDevice_Activate), Function.ALT_CONVENTION);

		if (!WinNT.S_OK.equals(Activate.invoke(WinNT.HRESULT.class, new Object[]{MMDevicePointer, IID_IAudioSessionManager2, CLSCTX_INPROC_SERVER, null, AudioSessionManager2}))) {
			throw new RuntimeException("IMMDevice::Activate( IAudioSessionManager2 ) failed");
		}
	}

	/**
	 * Session enumerator
	 */
	private void getSessionEnumerator() {
		Pointer AudioSessionManager2Pointer = AudioSessionManager2.getValue();
		Pointer AudioSessionManager2PointerVirtualTable = AudioSessionManager2Pointer.getPointer(0);
		Function sessionEnumerator = Function.getFunction(AudioSessionManager2PointerVirtualTable.getPointer(VTABLE_IAudioSessionManager2_GetSessionEnumerator), Function.ALT_CONVENTION);

		Object ris = sessionEnumerator.invoke(WinNT.HRESULT.class, new Object[]{AudioSessionManager2.getValue(), AudioSessionEnumerator});
		
		if ( ! WinNT.S_OK.equals(ris) ) {
			throw new RuntimeException("IAudioSessionManager2::GetSessionEnumerator() failed: " + ((WinNT.HRESULT)ris).intValue());
		}
	}

	/**
	 * Session enumerator counter
	 * 
	 * @return
	 */
	private int getCount() {
		Pointer AudioSessionEnumeratorPointer = AudioSessionEnumerator.getValue();
		Pointer AudioSessionEnumeratorVirtualTable = AudioSessionEnumeratorPointer.getPointer(0);
		Function GetCout = Function.getFunction(AudioSessionEnumeratorVirtualTable.getPointer(VTABLE_IAudioSessionEnumerator_GetCount), Function.ALT_CONVENTION);

		IntByReference resultPointer = new IntByReference();
		if (!WinNT.S_OK.equals(GetCout.invoke(WinNT.HRESULT.class, new Object[]{AudioSessionEnumerator.getValue(), resultPointer}))) {
			throw new RuntimeException("IAudioSessionEnumerator::GetCount() failed");
		}

		return resultPointer.getValue();
	}

	/**
	 * Retrieve session
	 * 
	 * @param index
	 * @param IAudioSessionControl2
	 */
	private void getSession( int index, PointerByReference IAudioSessionControl2 ) {
		Pointer AudioSessionEnumeratorPointer = AudioSessionEnumerator.getValue();
		Pointer AudioSessionEnumeratorVirtualTable = AudioSessionEnumeratorPointer.getPointer(0);
		Function GetSession = Function.getFunction(AudioSessionEnumeratorVirtualTable.getPointer(VTABLE_IAudioSessionEnumerator_GetSession), Function.ALT_CONVENTION);

		if (!WinNT.S_OK.equals(GetSession.invoke(WinNT.HRESULT.class, new Object[]{AudioSessionEnumerator.getValue(), index, IAudioSessionControl2}))) {
			throw new RuntimeException("IAudioSessionEnumerator::GetSession() failed");
		}
	}

	/**
	 * Session identifier
	 * 
	 * @param AudioSessionControl2
	 * @return
	 */
	private String GetSessionIdentifier( PointerByReference AudioSessionControl2 ) {
		Pointer AudioSessionControl2Pointer = AudioSessionControl2.getValue();
		Pointer AudioSessionControl2VirtualTable = AudioSessionControl2Pointer.getPointer(0);
		Function DisplayName = Function.getFunction(AudioSessionControl2VirtualTable.getPointer(VTABLE_IAudioSessionControl2_GetSessionIdentifier), Function.ALT_CONVENTION);

		PointerByReference ris = new PointerByReference();
		if (!WinNT.S_OK.equals(DisplayName.invoke(WinNT.HRESULT.class, new Object[]{AudioSessionControl2.getValue(), ris}))) {
			throw new RuntimeException("IAudioSessionControl2::GetSessionIdentifier() failed");
		}

		return ris.getValue().getWideString(0);
	}

	/**
	 * Retrieve SimpleAudioVolume from AudioSessionControl
	 * @param AudioSessionControl2
	 */
	private void getSimpleAudioVolume( PointerByReference AudioSessionControl2 ) {
		Pointer AudioSessionControl2Pointer = AudioSessionControl2.getValue();
		Pointer AudioSessionControl2VirtualTable = AudioSessionControl2Pointer.getPointer(0);
		Function DisplayName = Function.getFunction(AudioSessionControl2VirtualTable.getPointer(VTABLE_IAudioSessionControl2_QueryInterface), Function.ALT_CONVENTION);

		if (!WinNT.S_OK.equals(DisplayName.invoke(WinNT.HRESULT.class, new Object[]{AudioSessionControl2.getValue(), IID_ISimpleAudioVolume, SimpleAudioVolume }))) {
			throw new RuntimeException("IAudioSessionControl2::queryInterface() failed");
		}
	}

	/**
	 * Set master volume
	 * 
	 * @param level
	 */
	private void setVolume( float level ) {
		Pointer SimpleAudioVolumePointer = SimpleAudioVolume.getValue();
		Pointer SimpleAudioVolumeVirtualTable = SimpleAudioVolumePointer.getPointer(0);
		Function SetVOlume = Function.getFunction(SimpleAudioVolumeVirtualTable.getPointer(VTABLE_ISimpleAudioVolume_SetMasterVolume), Function.ALT_CONVENTION);

		if (!WinNT.S_OK.equals(SetVOlume.invoke(WinNT.HRESULT.class, new Object[]{SimpleAudioVolume.getValue(), level, null} ))) {
			throw new RuntimeException("ISimpleAudioVolume::SetMasterVolume() failed");
		}			
	}
	
	/**
	 * Set mute/unmute
	 * 
	 * @param mute
	 */
	private void setMute( boolean mute ) {
		Pointer SimpleAudioVolumePointer = SimpleAudioVolume.getValue();
		Pointer SimpleAudioVolumeVirtualTable = SimpleAudioVolumePointer.getPointer(0);
		Function SetVOlume = Function.getFunction(SimpleAudioVolumeVirtualTable.getPointer(VTABLE_ISimpleAudioVolume_SetMute), Function.ALT_CONVENTION);

		BOOL m = new BOOL(mute);
		if (!WinNT.S_OK.equals(SetVOlume.invoke(WinNT.HRESULT.class, new Object[]{SimpleAudioVolume.getValue(), m, null} ))) {
			throw new RuntimeException("ISimpleAudioVolume::SetMute() failed");
		}
	}
	
	/**
	 * Get Spotify application reference
	 */
	private void getTargetApplication() {
		
		// get all sessions
		getSessionEnumerator();
		int count = getCount();

		// find spotify session
		for(int i=0;i<count;i++) {

			// get session
			PointerByReference AudioSessionControl2 = new PointerByReference();
			getSession(i, AudioSessionControl2);

			// get id session
			String id = GetSessionIdentifier(AudioSessionControl2);

			// spotify?
			if( id.contains("Spotify") ) {
				
				// set SimpleAudioVolume pointer
				getSimpleAudioVolume(AudioSessionControl2);	
				break;
			}
		}
	}

	/**
	 * Mute/Unmute Spotify
	 * @param mute
	 */
	public void setSpotifyMute(boolean mute) {
		getTargetApplication();
		setMute( mute );	
	}	
	
	/**
	 * Set Spotify Volume level
	 * 
	 * @param level
	 */
	public void setSpotifyVolume(float level) {
		getTargetApplication();
		setVolume( level );	
	}	
}