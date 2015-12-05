package de.tisan.player;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class MP3Player {

	private Decoder decoder;
	private boolean play;
	private Thread thread;
	private boolean running;
	private float volume = -5.0F;
	private static MP3Player instance;
	private long current = System.currentTimeMillis();
	private String url;

	public static MP3Player get() {
		return (instance == null ? (instance = new MP3Player()) : instance);
	}

	private MP3Player() {
		this.decoder = new Decoder();
		this.play = false;
		this.running = false;
	}

	public void play(final String url) throws MalformedURLException {
		final URL stream = new URL(url);
		this.url = url;
		final long c = current;
		if (!running && !play) {
			this.play = true;
			this.thread = new Thread(new Runnable() {

				@Override
				public void run() {
					while (true) {
						try {
							if (c != current || (running && !play)) {
								decoder.stop();
								running = false;
								return;
							}
							if(decoder != null && decoder.volume != null){
								decoder.volume.setValue(volume);
								
							}
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
			});
			this.thread.start();
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						running = true;
						decoder.play("test", stream.openStream());
						decoder.volume.setValue(volume);
						play = false;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
 	public void playPlaylist(final File... streams) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (streams != null) {
					play = true;
					while (play) {
						for (File s : streams) {
							if (decoder == null) {
								decoder = new Decoder();
							}
							try {
								System.out.println(">> Playing next Song in playlist");

								new Thread(new Runnable() {
									@Override
									public void run() {
										while (true) {
											try {
												if (!play) {
													decoder.stop();
													return;
												}
												decoder.volume.setValue(volume);
												Thread.sleep(500);
											} catch (InterruptedException e) {

												e.printStackTrace();
											}
										}

									}
								}).start();
								Thread.sleep(1000);
								if (play) {
									FileInputStream i = new FileInputStream(s);
									decoder.volume.setValue(volume);
									decoder.play("test", i);
									i.close();

								}

							} catch (IOException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

				}
			}

		}).start();

	}

	public void volumeDown() {
		if (play) {
			volume -= decoder.volume.getPrecision() * 6;
			if (decoder.volume != null) {
				if (volume < decoder.volume.getMinimum()) {
					volume = decoder.volume.getMinimum();
					System.out.println("Correcting volume to minimum");
				}
				if (volume > decoder.volume.getMaximum()) {
					volume = decoder.volume.getMaximum();
					System.out.println("Correcting volume to maximum");
				}
				decoder.volume.setValue(volume);

			}
		}

	}

	public void volumeUp() {
		// 5.0F
		if (play) {
			volume += decoder.volume.getPrecision() * 6;
			if (decoder.volume != null) {
				if (volume < decoder.volume.getMinimum()) {
					volume = decoder.volume.getMinimum();
					System.out.println("Correcting volume to minimum");
				}
				if (volume > decoder.volume.getMaximum()) {
					volume = decoder.volume.getMaximum();
					System.out.println("Correcting volume to maximum");
				}
				decoder.volume.setValue(volume);
			}
		}

	}

	public void stop() {
		this.play = false;
		current = System.currentTimeMillis();
	}
		
	public boolean isPlaying(){
		return play;
	}
}
