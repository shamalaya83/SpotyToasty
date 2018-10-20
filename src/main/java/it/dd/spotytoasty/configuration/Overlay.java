package it.dd.spotytoasty.configuration;

import lombok.Data;

@Data
public class Overlay {
	private boolean show;
	private int	refresh_ms;
	private boolean pooling;
	private float opacity;
	private int max_width;
	private String coords;
	private String overlay_background;
	private String overlay_border;
	private String overlay_cover_background;
	private String overlay_cover_border;
	private String overlay_control_background;
	private String overlay_control_border;
	private String overlay_info_background;
	private String overlay_info_border;
}
