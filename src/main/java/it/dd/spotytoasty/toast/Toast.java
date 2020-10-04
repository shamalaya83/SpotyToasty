package it.dd.spotytoasty.toast;

import static it.dd.spotytoasty.calls.SpotyControls.*;

import it.dd.spotytoasty.calls.SpotyControls;
import it.dd.spotytoasty.calls.SpotyVolume;
import it.dd.spotytoasty.calls.SpotyWeb;
import it.dd.spotytoasty.configuration.Configuration;
import it.dd.spotytoasty.utils.Utils;
import lombok.extern.log4j.Log4j;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.Border;

import com.wrapper.spotify.enums.CurrentlyPlayingType;
import com.wrapper.spotify.enums.ModelObjectType;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Episode;
import com.wrapper.spotify.model_objects.specification.ShowSimplified;
import com.wrapper.spotify.model_objects.specification.Track;

/**
 * Spotify Overlay Class
 * 
 * LAYAOUT:
 * 
 * +----------------+-----------------------------------+
 * |                |   ARTIST                          |
 * |    COVER       +-----------------------------------+
 * |                |   SONG                            |
 * +----------------+-----------------------------------+
 * |    CONTROLS    |   ALBUM                           |
 * +----------------+-----------------------------------+
 * 
 * @author shamalaya
 */
@Log4j
public class Toast extends JDialog {

	private static final long serialVersionUID = -1289062175899723990L;

	/**
	 * App Name
	 */
	private final static String APP_NAME = "SpotyToasty";

	/**
	 * Images (controls, nocover)
	 */
	private static ImageIcon button_prev;
	private static ImageIcon button_next;
	private static ImageIcon button_play;
	private static ImageIcon button_pause;
	private static ImageIcon button_love;
	private static ImageIcon button_dislike;
	private static Image nocover;

	/**
	 * Overlay Info
	 */
	private ImageIcon icon;
	private JButton prev;
	private JButton pp;
	private JButton next;
	private JButton love;
	private JLabel artist;
	private JLabel album;
	private JLabel song;

	/**
	 * Configuration and SpotyWeb
	 */
	private Configuration conf;
	private SpotyWeb spotyWeb;

	/**
	 * Mute on ads
	 */
	final private SpotyVolume spotyVolume = SpotyVolume.getInstance();

	/**
	 * Scheduler
	 */
	private static ScheduledExecutorService scheduler;
	
	/**
	 * Autohide
	 */
	private static Timer timerHide;
	
	/**
	 * Visible flag
	 */
	private boolean show = true;
	
	/**
	 * Last trak id
	 */
	private String lastTrackID = null;
	
	/**
	 * Construct Toast
	 * 
	 * @param conf
	 * @param track
	 * @throws Exception
	 */
	public Toast(Configuration conf, SpotyWeb spotyWeb) throws Exception {

		long start = System.currentTimeMillis();

		this.conf = conf;
		this.spotyWeb = spotyWeb;

		// set visibility
		this.show = conf.getOverlay().isShow();
		
		// preload image
		preloadImage(conf);		

		// main frame
		setUndecorated(true);			
		setAlwaysOnTop(true);		
		setAutoRequestFocus(false);
		setFocusable(false);
		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);	

		// set opacity
		setOpacity( conf.getOverlay().getOpacity() );

		// set location
		int[] coords = parseCoords(conf.getOverlay().getCoords());
		setLocation( coords[0], coords[1] );			

		// main panel
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());	
		mainPanel.setBackground( parseColor(conf.getOverlay().getOverlay_background()) );
		mainPanel.setBorder( parseBorder( conf.getOverlay().getOverlay_border() ));

		// left panel
		final JPanel leftPanel = new JPanel(); 
		leftPanel.setLayout( new BoxLayout(leftPanel, BoxLayout.Y_AXIS) );
		leftPanel.setBackground( parseColor( conf.getOverlay().getOverlay_cover_background()) );
				
		// create cover image
		icon = new ImageIcon( parseImage( null ) );
		final JLabel cover = new JLabel(icon);
		cover.setAlignmentX( Component.CENTER_ALIGNMENT );
		cover.setBorder( parseBorder( conf.getOverlay().getOverlay_cover_border() ));
		leftPanel.add(cover);

		// control panel
		if( conf.getControls().isShow() ) {

			final JPanel controlPanel = new JPanel();
			controlPanel.setBackground( parseColor(conf.getOverlay().getOverlay_control_background()) );
			controlPanel.setBorder( parseBorder( conf.getOverlay().getOverlay_control_border() ));
			controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));	

			// control panel
			if( conf.getControls().isShow_prev() ) {				
				prev = createControlButton();
				prev.setIcon( button_prev );
				prev.setEnabled(false);				
				prev.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SpotyControls.pressKey(VK_MEDIA_PREV_TRACK);
						executeWorker();
					}
				});

				controlPanel.add(prev);
			}

			if( conf.getControls().isShow_pp() ) {
				pp = createControlButton();
				pp.setIcon( button_pause );
				pp.setEnabled(false);			
				pp.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {

						// fire command
						SpotyControls.pressKey(VK_MEDIA_PLAY_PAUSE);

						// switch icon
						final String desc = ((ImageIcon) pp.getIcon()).getDescription();
						if( "pause".equals(desc) ) {
							pp.setIcon( button_play );
							cancelScheduler();

						} else {
							pp.setIcon( button_pause );
							executeWorker();
						}
					}				
				});

				controlPanel.add(pp);
			}

			if( conf.getControls().isShow_next() ) {
				next = createControlButton();
				next.setIcon( button_next );
				next.setEnabled(false);				
				next.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SpotyControls.pressKey(VK_MEDIA_NEXT_TRACK);
						executeWorker();
					}
				});

				controlPanel.add(next);
			}

			if( conf.getControls().isShow_love() ) {
				love = createControlButton();
				love.setIcon( button_love );	
				love.setEnabled(false);							
				love.addActionListener( new BookmarkActionListner() );
				controlPanel.add(love);
			}		

			// add controls panel
			leftPanel.add(controlPanel);
		}

		// add cover
		mainPanel.add( leftPanel, BorderLayout.WEST );

		// set track panel
		final JPanel trackPanel = new JPanel(); 
		trackPanel.setLayout(new BorderLayout());
		trackPanel.setBackground( parseColor(conf.getOverlay().getOverlay_info_background()) );
		trackPanel.setBorder(parseBorder( conf.getOverlay().getOverlay_info_border() ));				

		// create artist
		if( conf.getArtist().isShow() ) {
			artist = new JLabel( concatArtists( null ) );
			artist.setForeground( Color.decode( conf.getArtist().getColor()) );
			artist.setFont(new Font( conf.getArtist().getFont(), conf.getArtist().getWeight(), conf.getArtist().getSize()) );
			trackPanel.add(artist, conf.getArtist().getPosition());
		}

		// create song
		if( conf.getSong().isShow() ) {
			song = new JLabel( parseSong( null ) );
			song.setForeground( Color.decode( conf.getSong().getColor()) );
			song.setFont(new Font( conf.getSong().getFont(), conf.getSong().getWeight(), conf.getSong().getSize()) );
			trackPanel.add(song, conf.getSong().getPosition());
		}

		// create album
		if( conf.getAlbum().isShow() ) {
			album = new JLabel( parseAlbum( null ) );
			album.setForeground( Color.decode( conf.getAlbum().getColor()) );
			album.setFont(new Font( conf.getAlbum().getFont(), conf.getAlbum().getWeight(), conf.getAlbum().getSize()) );
			trackPanel.add(album, conf.getAlbum().getPosition());
		}		

		// add track info
		mainPanel.add( trackPanel, BorderLayout.CENTER );

		// show the layer
		add(mainPanel);

		// timer autohide
		if( conf.getAutohide().isAutohide() ) {
			setupTimerAutohide();
		}
		
		log.info("Dialog configured in: " + (System.currentTimeMillis() - start) + " ms");
	}

	/**
	 * Draw Overlay
	 */
	public void draw() {
		SwingUtilities.invokeLater( new Runnable() {			
			public void run() {
				try {
					// set tryicon					
					setTryIcon();
					// show overlay
					setToastyVisible();
					pack();
				} catch (AWTException e) {
					log.error("Error while showing overlay", e);
					System.exit(-1);
				}							
			}
		});
	}

	/**
	 * Update current song info
	 * 
	 * @param spotifyObj
	 * @throws Exception 
	 */
	public void setCurrentSongInfo( CurrentlyPlayingContext spotifyObj ) throws Exception {
		
		String id = getModelTypeID(spotifyObj);
			
		// check if the current song is already displayed
		if( id.equals(lastTrackID) )
			return;
		else
			lastTrackID = id;
		
		long start = System.currentTimeMillis();

		// muted?
		unmute();

		// set icon
		icon.setImage( parseImage( spotifyObj ) );

		// set controls
		if( conf.getControls().isShow() ) {					

			if( conf.getControls().isShow_prev() ) {		
				if( !prev.isEnabled() )
					prev.setEnabled(true);
			}

			if( conf.getControls().isShow_next() ) {		
				if( !next.isEnabled() )
					next.setEnabled(true);
			}

			if( conf.getControls().isShow_pp() ) {

				// reset icon
				pp.setIcon( button_pause );

				if( !pp.isEnabled() )
					pp.setEnabled(true);
			}

			if( conf.getControls().isShow_love() ) {
				
				// check if is an episode
				if( spotifyObj.getCurrentlyPlayingType() == CurrentlyPlayingType.TRACK ) {
					
					if( !love.isVisible() )
						love.setVisible(true);
					
					// set new track
					((BookmarkActionListner)love.getActionListeners()[0]).setTrack(spotifyObj);
					
					// check if track is already saved
					boolean flag_loved = spotyWeb.checkTracksForUser(id);
					
					if( !flag_loved ) {
						love.setIcon( button_love );
					} else {
						love.setIcon( button_dislike );
					}
					
					love.setEnabled(true);
					
				} else {
					love.setVisible(false);
				}
			}
		}

		// set artist
		if( conf.getArtist().isShow() ) {
			artist.setText( concatArtists( spotifyObj ) );					
		}

		// set song
		if( conf.getSong().isShow() ) {
			song.setText( parseSong( spotifyObj ) );
		}

		// set album
		if( conf.getAlbum().isShow() ) {
			album.setText( parseAlbum( spotifyObj ) );
		}								

		// set visible
		setToastyVisible();								

		// repack overlay		
		pack();

		// set max width
		if( conf.getOverlay().getMax_width() > 0 ) {
			Dimension dim = getSize();
			setSize( Math.min(dim.width, conf.getOverlay().getMax_width()), dim.height);
		}			

		// restart timerhide
		if( conf.getAutohide().isAutohide() )
			timerHide.restart();
		
		log.info("Dialog updated in: " + (System.currentTimeMillis() - start) + " ms");
	}

	public void mute() {
		if( conf.isAutomute() ) {
			spotyVolume.muteTargetApplication(true);			
		}
	}

	public void unmute() {
		if( conf.isAutomute() ) {
			spotyVolume.muteTargetApplication(false);				
		}
	}

	/**
	 * Create a SwingWorker to retry song info, update UI & schedule new task 
	 */
	public void executeWorker() {

		new SwingWorker<CurrentlyPlayingContext, Void>() {

			/**
			 * retrieve song info (outside EDT)
			 */
			@Override
			protected CurrentlyPlayingContext doInBackground() throws Exception {

				try {

					// cancel task
					cancelScheduler();

					// delay before
					Utils.sleep( conf.getOverlay().getRefresh_ms() );

					// get current song
					return spotyWeb.getInformationAboutUsersCurrentPlayback();				

				} catch( Exception e) {
					log.error("SwingWorker error while fetching track info: ", e );				
				}

				return null;
			}

			/**
			 * EDT Safe update UI & schedule next update task
			 */
			@Override
			protected void done() {

				// default wait time
				long wait = conf.getOverlay().getRefresh_ms();

				try {

					// retrieve background result
					CurrentlyPlayingContext track = get();

					// is playing?
					if( track != null && track.getIs_playing() ) {

						if( track.getItem() != null ) {

							// update info						
							setCurrentSongInfo( track );		
							
							// pooling or wait end?
							if( !conf.getOverlay().isPooling() && track.getProgress_ms() != null ) {
								
								// calculate wait time
								wait = getModelTypeDuration( track ) - track.getProgress_ms();
								
								//check wait
								if( wait < 0) 
									wait = conf.getOverlay().getRefresh_ms();
							}
							
						} else {

							// hide overlay 
							setToastyHidden();

							// mute
							mute();						
						}
					}

				} catch (Exception e) {
					log.error("SwingWorker error while updating overlay: ", e);
				}

				// schedule refresh task
				log.info("next scheduled update task delay: " + wait + " ms");
				scheduler = Executors.newSingleThreadScheduledExecutor();
				scheduler.schedule( new UpdateTask(), wait, TimeUnit.MILLISECONDS );
			}
		}.execute();
	}

	/**
	 * Try to cancel UpdateTask
	 */
	private void cancelScheduler() {
		if( scheduler != null ) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Update task for new song 
	 * @author daniele.dagnese
	 *
	 */
	class UpdateTask implements Runnable {
		public void run() {
			executeWorker();
		}
	}

	/**
	 * Add / Remove song from bookmark
	 * @author shamalaya
	 *
	 */
	class BookmarkActionListner implements ActionListener {

		private CurrentlyPlayingContext track;

		public void setTrack(CurrentlyPlayingContext track) {			
			this.track = track;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				String desc = ((ImageIcon) love.getIcon()).getDescription();
				if( "love".equals(desc) ) {
					spotyWeb.saveTracksForUser( getModelTypeID( track ) );
					love.setIcon( button_dislike );
				} else if( "dislike".equals(desc) ) {
					spotyWeb.removeTracksForUser( getModelTypeID( track ) );
					love.setIcon( button_love );
				}

			} catch (Exception ex) {						
				log.error( "saveTracksForUser() failed!", ex );
			}
		}	
	}
	
	/**
	 * Setup Autohide effect
	 */
	private void setupTimerAutohide() {
		timerHide = new Timer(conf.getAutohide().getHide_after_ms(), new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
				
				float saveOp = getOpacity();					
				float curOp = saveOp;
				
				do {
					// set lower opacity
					curOp = curOp - 0.2f;
					if( curOp > 0 ) {
						setOpacity(curOp);
						Utils.sleep(100);
					}
					
				} while(curOp > 0);
				
				// hide & reset original opacity value
				setVisible(false);
				setOpacity(saveOp);
			}
		});
		timerHide.setRepeats(false);
	}
	
	/**
	 * Stop timer
	 */
	private void stopTimerAutohide() {
		if( timerHide != null ) {
			timerHide.stop();	
		}
	}
	
	/**
	 * Preload image
	 * @throws IOException 
	 */
	private void preloadImage(final Configuration conf) throws IOException {
		button_prev = new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/prev.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0 ), "prev");
		button_next = new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/next.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0 ), "next");
		button_play = new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/play.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0 ), "play");
		button_pause= new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/pause.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0), "pause");
		button_love	= new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/love.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0), "love");
		button_dislike	= new ImageIcon( ImageIO.read( ClassLoader.getSystemResource("img/dislike.png") ).getScaledInstance( conf.getControls().getIcons_px(), conf.getControls().getIcons_px(), 0), "dislike");
		nocover = ImageIO.read( ClassLoader.getSystemResource("img/album-art-placeholder.png") ).getScaledInstance( conf.getCover().getImage_width(), conf.getCover().getImage_height(), 0);
	}	

	/**
	 * Create tryicon
	 * @throws AWTException
	 */
	private void setTryIcon() throws AWTException {

		// Check the SystemTray is supported
		if (SystemTray.isSupported()) {

			final PopupMenu popup = new PopupMenu();
			final TrayIcon trayIcon = new TrayIcon( Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("img/spotytoasty.png")), APP_NAME );
			trayIcon.setImageAutoSize(true);
			final SystemTray tray = SystemTray.getSystemTray();

			// Show / Hide
			final MenuItem showHide = new MenuItem( conf.getOverlay().isShow() ? "Show/Hide (Always on)" : "Show/Hide (Hidden)" );
			showHide.addActionListener(new ActionListener() {			
				public void actionPerformed(ActionEvent e) {
					if( show ) {
						show = false;
						setVisible(false);
						showHide.setLabel("Show/Hide (Hidden)");
					} else {
						show = true;
						setVisible(true);
						showHide.setLabel("Show/Hide (Always on)");
					}
				}
			});

			// Enable / Disable Automute
			final MenuItem automute = new MenuItem( conf.isAutomute() ? "Automute (Enabled)" : "Automute (Disabled)" );
			automute.addActionListener(new ActionListener() {			
				public void actionPerformed(ActionEvent e) {
					if( conf.isAutomute() ) {
						conf.setAutomute(false);
						automute.setLabel("Automute (Disabled)");
					} else {
						conf.setAutomute(true);
						automute.setLabel("Automute (Enabled)");
					}
				}
			});
			
			// Enable / Disable Autohide
			final MenuItem autohide = new MenuItem( conf.getAutohide().isAutohide() ? "Autohide (Enabled)" : "Autohide (Disabled)");
			autohide.addActionListener(new ActionListener() {							
				public void actionPerformed(ActionEvent e) {
					if( conf.getAutohide().isAutohide() ) {
						conf.getAutohide().setAutohide(false);
						// stop autohide timer
						stopTimerAutohide();	
						// show?
						if( show )
							setVisible(true);
						autohide.setLabel("Autohide (Disabled)");						
					} else {
						conf.getAutohide().setAutohide(true);
						// setup & start timerAutoHide
						setupTimerAutohide();
						timerHide.restart();
						autohide.setLabel("Autohide (Enabled)");						
					}
				}
			});
			
			// Enable / Disable Pooling mode
			final MenuItem pooling = new MenuItem( conf.getOverlay().isPooling() ? "Pooling mode (Enabled)" : "Pooling mode (Disabled)");
			pooling.addActionListener(new ActionListener() {			
				public void actionPerformed(ActionEvent e) {
					if( conf.getOverlay().isPooling() ) {
						conf.getOverlay().setPooling(false);
						pooling.setLabel("Pooling mode (Disabled)");						
					} else {
						conf.getOverlay().setPooling(true);
						
						// remove pending schedule						
						cancelScheduler();
						
						// restart schedule
						executeWorker();
						
						pooling.setLabel("Pooling mode (Enabled)");						
					}
				}
			});
			
			// Exit
			final MenuItem exitItem = new MenuItem("Exit");
			exitItem.addActionListener(new ActionListener() {			
				public void actionPerformed(ActionEvent e) {
					tray.remove(trayIcon);
					System.exit(0);
				}
			});

			//Add components to pop-up menu
			popup.add(showHide);
			popup.add(automute);
			popup.add(autohide);
			popup.add(pooling);
			popup.addSeparator();
			popup.add(exitItem);
			trayIcon.setPopupMenu(popup);
			tray.add(trayIcon);
		}
	}
	
	private void setToastyVisible() {
		if( show && !isVisible() )
			setVisible(true);
	}
	
	private void setToastyHidden() {
		if( show && isVisible() )
			setVisible(false);
	}

	private static Color parseColor(String color) {
		try {
			return Color.decode(color);
		} catch(Exception e) {
			return new Color(0, 0, 0, 0);
		}
	}

	private static Border parseBorder(String border) {
		try {
			final String[] values = border.replaceAll("\\s+", "").split(",");
			final int[] borders = { 
					Integer.parseInt(values[0]), 
					Integer.parseInt(values[1]),
					Integer.parseInt(values[2]),
					Integer.parseInt(values[3]) 
			};
			return BorderFactory.createEmptyBorder( borders[0],borders[1],borders[2],borders[3] ); 
		} catch(Exception e)  {
			return BorderFactory.createEmptyBorder( 0,0,0,0 );
		}
	}

	private static int[] parseCoords(String coord) {
		try {
			final String[] values = coord.replaceAll("\\s+", "").split(",");
			final int[] coords = { 
					Integer.parseInt(values[0]), 
					Integer.parseInt(values[1])
			};
			return coords;
		} catch(Exception e) {
			return new int[] {0,0};
		}
	}

	private static String concatArtists(CurrentlyPlayingContext cpc) {	
		return getModelTypeArtists(cpc);
	}

	private static String parseSong(CurrentlyPlayingContext cpc) {	
		return getModelTypeName( cpc );
	}

	private static String parseAlbum(CurrentlyPlayingContext cpc) {
		return getModelTypeAlbumName(cpc);
	}

	private Image parseImage( CurrentlyPlayingContext cpc ) {
		try {
			final String path = getModelTypeAlbumCover( cpc );
			final URL url = new URL(path);	
			return ImageIO.read(url).getScaledInstance( this.conf.getCover().getImage_width(), this.conf.getCover().getImage_height(), 0 );
		} catch(Exception e) {
			return nocover;
		}
	}

	private static JButton createControlButton() {
		final JButton button = new JButton();
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setContentAreaFilled(false);	
		button.setBorderPainted(false);
		button.setRolloverEnabled(false);
		button.setFocusable(false);
		return button;
	}
	
	/**
	 * get ID
	 * @throws IOException 
	 */
	private static String getModelTypeID( CurrentlyPlayingContext obj ) throws IOException {
		if( obj.getItem().getType() == ModelObjectType.TRACK )
			return ((Track)obj.getItem()).getId();
		else if( obj.getItem().getType() == ModelObjectType.EPISODE )
			return ((Episode)obj.getItem()).getId();	
		else
			throw new IOException("Unknown spotify modeltype");
	}
	
	/**
	 * get durationMs
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	private static int getModelTypeDuration( CurrentlyPlayingContext obj ) throws IOException {
		if( obj.getItem().getType() == ModelObjectType.TRACK )
			return ((Track)obj.getItem()).getDurationMs();
		else if( obj.getItem().getType() == ModelObjectType.EPISODE )
			return ((Episode)obj.getItem()).getDurationMs();	
		else
			throw new IOException("Unknown spotify modeltype");
	}
	
	/**
	 * get name
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	private static String getModelTypeName( CurrentlyPlayingContext obj ) {
		try {
			if( obj.getItem().getType() == ModelObjectType.TRACK )
				return ((Track)obj.getItem()).getName();
			else if( obj.getItem().getType() == ModelObjectType.EPISODE )
				return ((Episode)obj.getItem()).getName();	
			else
				return "N.A.";
		} catch(Exception e) {
			return "N.A.";
		}
	}
	
	/**
	 * get artist/show
	 * 
	 * @param obj
	 * @return
	 */
	private static String getModelTypeArtists( CurrentlyPlayingContext obj ) {
		try {
			String ris = "N.A.";
			if( obj.getItem().getType() == ModelObjectType.TRACK ) {
				ArtistSimplified[] artists = ((Track)obj.getItem()).getArtists();
				if( artists != null ) {
					ris = "";
					for(int i=0;i<artists.length;i++) {
						if(i>0)	ris += ", ";
						ris += artists[i].getName();
					}
				}
			}
			else if( obj.getItem().getType() == ModelObjectType.EPISODE ) {
				ShowSimplified shows = ((Episode)obj.getItem()).getShow();
				if( shows != null )
					ris = shows.getName();
			}

			return ris;
			
		} catch(Exception e) {
			return "N.A.";
		}
	}
	
	/**
	 * get album name
	 * 
	 * @param obj
	 * @return
	 */
	private static String getModelTypeAlbumName( CurrentlyPlayingContext obj ) {
		try {
			if( obj.getItem().getType() == ModelObjectType.TRACK )
				return ((Track)obj.getItem()).getAlbum().getName();
			else if( obj.getItem().getType() == ModelObjectType.EPISODE )
				return ((Episode)obj.getItem()).getDescription();
			else
				return "N.A.";
		} catch(Exception e) {
			return "N.A.";
		}
	}
	
	/**
	 * get album cover
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	private static String getModelTypeAlbumCover( CurrentlyPlayingContext obj ) throws IOException {
		if( obj.getItem().getType() == ModelObjectType.TRACK )
			return ((Track)obj.getItem()).getAlbum().getImages()[0].getUrl();
		else if( obj.getItem().getType() == ModelObjectType.EPISODE )
			return ((Episode)obj.getItem()).getShow().getImages()[0].getUrl();
		else
			throw new IOException("Unknown spotify modeltype");
	}
}
