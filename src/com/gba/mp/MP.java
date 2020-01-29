package com.gba.mp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public final class MP extends JPanel implements Runnable {
	private static final long serialVersionUID = -5961693648278726304L;

	public static final String TITLE = "Mini projet de mathématiques #14";
	public static final int ORIGIN_X = 0, ORIGIN_Y = 0, WIDTH = 900, HEIGHT = MP.WIDTH;
	public static final double REFRESH_RATE = 1000;

	public static final int MEASURE_CIRCLE_RADIUS = (MP.WIDTH / 2) - 100;
	public static final int SEED_RADIUS = 5;

	public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
	public static final double ROTATION_ANGLE = ((2 * Math.PI)) / MP.GOLDEN_RATIO;
	public static final double ROTATION_ANGLE_COS = Math.cos(MP.ROTATION_ANGLE);
	public static final double ROTATION_ANGLE_SIN = Math.sin(MP.ROTATION_ANGLE);
	public static final double SCALE_FACTOR = MP.map(MP.WIDTH, 100, 1000, 1, 0.01);

	public static final Color BACKGROUND_COLOR = Color.BLACK;
	public static final Color TEXT_COLOR = Color.WHITE;
	public static final Color INSIDE_SEEDS_COLOR = Color.YELLOW;
	public static final Color OUTSIDE_SEEDS_COLOR = Color.RED;
	public static final Color MEASURE_CIRCLE_COLOR = new Color(1f, 0f, 0f);
	public static Font TEXT_FONT = new Font("Consolas", Font.PLAIN, 14);

	static {
		System.out.println(MP.SCALE_FACTOR);
	}

	private final Thread thread;
	private final JFrame frame;
	private final BufferedImage image;

	private boolean isRunning;

	private final List<double[]> coordinatesList;
	private double currentAngle;
	private double efficiency;
	private boolean seedsOutOfBounds, calculateEfficiency, displayEfficiency;

	private int insideSeedsCount;

	public MP() {
		// Thread
		{
			this.thread = new Thread(this);
			this.isRunning = false;
		}
		this.frame = new JFrame(MP.TITLE);
		// This (canvas) properties
		{
			Dimension canvasSize = new Dimension(MP.WIDTH, MP.HEIGHT);
			this.setMinimumSize(canvasSize);
			this.setMaximumSize(canvasSize);
			this.setPreferredSize(canvasSize);
			this.image = new BufferedImage(MP.WIDTH, MP.HEIGHT, BufferedImage.TYPE_INT_ARGB);
		}
		// Frame
		{
			this.frame.add(this);
			this.frame.pack();
			this.frame.setLocationRelativeTo(null);
			this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
		// Coords
		{
			this.coordinatesList = new ArrayList<double[]>();
			this.coordinatesList.add(new double[] { MP.ORIGIN_X, MP.ORIGIN_Y });
			this.currentAngle = MP.ROTATION_ANGLE;
		}
		// Efficiency related variables
		{
			this.insideSeedsCount = 0;
			this.seedsOutOfBounds = false;
			this.calculateEfficiency = true;
			this.displayEfficiency = false;
			this.efficiency = 0;
		}
	}

	public synchronized final void start() {
		if (this.isRunning) {
			return;
		} else {
			this.isRunning = true;
			this.thread.start();
		}
	}

	public synchronized final void stop() {
		if (this.isRunning) {
			this.isRunning = false;
		} else {
			return;
		}
	}

	@Override
	public void run() {
		this.frame.setVisible(true);
		while (this.isRunning) {
			this.updateLogic();
			this.repaint();
			try {
				Thread.sleep((long) (1000 / MP.REFRESH_RATE));
			} catch (InterruptedException interruptedException) {
				return;
			}
		}
		this.frame.setVisible(false);
		this.frame.dispose();
		try {
			this.thread.join();
		} catch (InterruptedException interruptedException) {
			System.err.println("An error occurred while waiting for the thread to die");
			System.exit(-1);
		}
	}

	private void updateLogic() {
		if (!this.seedsOutOfBounds) {
			double[] lastCoords = this.coordinatesList.get(this.coordinatesList.size() - 1);
			double lastX = lastCoords[0];
			double lastY = lastCoords[1];
			double newX = ((lastX * MP.ROTATION_ANGLE_COS) - (lastY * MP.ROTATION_ANGLE_SIN));
			double newY = ((lastX * MP.ROTATION_ANGLE_SIN) + (lastY * MP.ROTATION_ANGLE_COS));
			newX += MP.SCALE_FACTOR * Math.cos(this.currentAngle);
			newY += MP.SCALE_FACTOR * Math.sin(this.currentAngle);
			this.coordinatesList.add(new double[] { newX, newY });
			this.currentAngle += MP.ROTATION_ANGLE;
			if (this.distanceFromOrigin(newX, newY) > MP.MEASURE_CIRCLE_RADIUS) {
				this.seedsOutOfBounds = true;
				return;
			} else {
				this.insideSeedsCount += 1;
			}
		}
	}

	private final double distanceFromOrigin(double x, double y) {
		return Math.sqrt((x * x) + (y * y));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics imageGraphics = this.image.getGraphics();
		Graphics2D graphics2d = (Graphics2D) imageGraphics;
		// Rendering hints
		{
			graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			graphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			graphics2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			graphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		}
		graphics2d.setColor(MP.BACKGROUND_COLOR);
		graphics2d.fillRect(0, 0, MP.WIDTH, MP.HEIGHT);
		graphics2d.setColor(MP.MEASURE_CIRCLE_COLOR);
		graphics2d.fillOval((MP.WIDTH / 2) - MP.MEASURE_CIRCLE_RADIUS, (MP.HEIGHT / 2) - MP.MEASURE_CIRCLE_RADIUS, MP.MEASURE_CIRCLE_RADIUS * 2, MP.MEASURE_CIRCLE_RADIUS * 2);
		graphics2d.setColor(MP.MEASURE_CIRCLE_COLOR.darker());
		graphics2d.drawOval((MP.WIDTH / 2) - MP.MEASURE_CIRCLE_RADIUS, (MP.HEIGHT / 2) - MP.MEASURE_CIRCLE_RADIUS, MP.MEASURE_CIRCLE_RADIUS * 2, MP.MEASURE_CIRCLE_RADIUS * 2);
		int size = this.coordinatesList.size();
		double[][] coordsListArray = this.coordinatesList.toArray(new double[size][]);
		for (double[] coords : coordsListArray) {
			int x = (int) coords[0];
			int y = (int) coords[1];
			if (this.distanceFromOrigin(x, y) > MP.MEASURE_CIRCLE_RADIUS) {
				graphics2d.setColor(MP.OUTSIDE_SEEDS_COLOR);
			} else {
				graphics2d.setColor(MP.INSIDE_SEEDS_COLOR);
			}
			graphics2d.fillOval((MP.WIDTH / 2) - MP.SEED_RADIUS - x, (MP.HEIGHT / 2) - MP.SEED_RADIUS - y, 2 * MP.SEED_RADIUS, 2 * MP.SEED_RADIUS);
			graphics2d.setColor(Color.BLACK);
			graphics2d.drawOval((MP.WIDTH / 2) - MP.SEED_RADIUS - x, (MP.HEIGHT / 2) - MP.SEED_RADIUS - y, 2 * MP.SEED_RADIUS, 2 * MP.SEED_RADIUS);
		}
		graphics2d.setColor(MP.TEXT_COLOR);
		graphics2d.setFont(MP.TEXT_FONT);
		graphics2d.drawString("Nombre de graines contenues dans la surface de mesure: " + this.insideSeedsCount, 10, 20);
		if (this.seedsOutOfBounds && this.calculateEfficiency) {
			int measurePixels = 0;
			for (int x = 0; x < MP.WIDTH; x += 1) {
				for (int y = 0; y < MP.HEIGHT; y += 1) {
					Color color = new Color(this.image.getRGB(x, y), true);
					if (color.getRGB() == MEASURE_CIRCLE_COLOR.getRGB()) {
						measurePixels += 1;
					}
				}
			}
			double circlePixels = (Math.PI * (MP.MEASURE_CIRCLE_RADIUS) * (MP.MEASURE_CIRCLE_RADIUS));
			this.efficiency = (Math.abs(measurePixels - circlePixels) / circlePixels) * 100;
			this.calculateEfficiency = false;
			this.displayEfficiency = true;
		}
		if (this.displayEfficiency) {
			graphics2d.setColor(MP.TEXT_COLOR);
			graphics2d.drawString("Angle: " + MP.ROTATION_ANGLE + " radians, efficacité: " + this.efficiency + "%", 10, 40);
		}
		graphics.drawImage(this.image, MP.ORIGIN_X, MP.ORIGIN_Y, MP.WIDTH, MP.HEIGHT, null);
	}

	public static void main(String[] args) {
		MP project14 = new MP();
		project14.start();
	}

	private static double map(double value, double minimalValue, double maximalValue, double minimalTargetValue, double maximalTargetValue) {
		return (((value - minimalValue) / (maximalValue - minimalValue)) * (maximalTargetValue - minimalTargetValue)) + minimalTargetValue;
	}
}
