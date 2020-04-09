package com.gba.mp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public final class MP extends JPanel implements Runnable {
	private static final long serialVersionUID = -5961693648278726304L;

	public static final String TITLE = "Mini projet de mathématiques #14";
	public static final int ORIGIN_X = 0, ORIGIN_Y = 0, WIDTH = 1200, HEIGHT = 800;
	public static final double REFRESH_RATE = 300;

	public static final int MEASURE_CIRCLE_RADIUS = 300;
	public static final int SEED_RADIUS = 5;

	public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
	public static final double SCALE_FACTOR = 0.15;
	public static final Color BACKGROUND_COLOR = Color.decode("#2F2F2F");
	public static final Color TEXT_COLOR = Color.decode("#FDFDD8");
	public static final Color INSIDE_SEEDS_COLOR = Color.decode("#594731");
	public static final Color OUTSIDE_SEEDS_COLOR = Color.RED;
	public static final Color MEASURE_CIRCLE_COLOR = Color.decode("#FDFDD7");
	public static Font TEXT_FONT = new Font("Liberation Serif", Font.PLAIN, 20);

	private final Thread thread;
	private final JFrame frame;
	private final JPanel panel;
	private final JTextField field;
	private final JButton button;
	private final BufferedImage image;
	private final JTable table;

	private boolean isRunning;
	private boolean hasCalculatedCirclePixels;
	private boolean shouldDraw;
	private boolean addValueToTable;

	private final List<double[]> coordinatesList;
	private double currentAngle;
	private double efficiency;
	private boolean seedsOutOfBounds, calculateEfficiency, displayEfficiency;
	private int circlePixels;

	private int insideSeedsCount;
	private double rotationAngle;
	private String rotationString;

	public static final void drawCenteredString(Graphics graphics, String string, int x, int y, int width, int height, Font font, Color color) {
		graphics.setFont(font);
		FontMetrics fm = graphics.getFontMetrics();
		int cx = ((width - fm.stringWidth(string)) / 2);
		int cy = ((height - fm.getHeight()) / 2) + fm.getAscent();
		graphics.setColor(color);
		graphics.drawString(string, x + cx, y + cy);
	}

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
		// Panel
		{
			this.panel = new JPanel();
			this.field = new JTextField();
			this.button = new JButton("Lancer");
			this.field.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					MP.this.button.doClick();
				}
			});
			this.button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String text = MP.this.field.getText();
					if (!text.isEmpty()) {
						boolean proceed = true;
						try {
							String angle = "";
							if (text.toLowerCase().equals("nombre d'or")) {
								angle = "2*PI / Nombre d'or";
								rotationAngle = ((2 * Math.PI)) / MP.GOLDEN_RATIO;
							} else if (text.toLowerCase().equals("racine(2)")) {
								angle = "2*PI / Racine(2)";
								rotationAngle = ((2 * Math.PI)) / Math.sqrt(2);
							} else if (text.toLowerCase().equals("e")) {
								angle = "2*PI / e";
								rotationAngle = ((2 * Math.PI)) / Math.exp(1);
							} else {
								String[] parts = text.split("PI/");
								if (parts.length == 2) {
									rotationAngle = Math.PI / Double.valueOf(parts[1]);
								} else {
									rotationAngle = ((Integer.valueOf(parts[0]) * Math.PI) / Integer.valueOf(parts[1]));
								}
								angle = text;
							}
							rotationString = angle;

						} catch (Exception exception) {
							// exception.printStackTrace();
							proceed = false;
							MP.this.field.setText("");
						}
						if (proceed) {
							MP.this.field.setText("");
							MP.this.seedsOutOfBounds = false;
							MP.this.coordinatesList.clear();
							MP.this.coordinatesList.add(new double[] { MP.ORIGIN_X, MP.ORIGIN_Y });
							MP.this.isRunning = true;
							MP.this.calculateEfficiency = true;
							MP.this.insideSeedsCount = 0;
							MP.this.displayEfficiency = false;
							MP.this.shouldDraw = true;
						}
					}
				}
			});
			this.panel.setLayout(new BorderLayout());
			this.panel.add(this.field, BorderLayout.CENTER);
			this.panel.add(this.button, BorderLayout.EAST);
			String[] columnNames = { "Angle", "Efficacité" };
			this.table = new JTable();
			this.table.setModel(new DefaultTableModel(columnNames, 1));
		}
		// Frame
		{
			this.frame.setLayout(new BorderLayout());
			this.frame.add(this, BorderLayout.CENTER);
			this.frame.add(this.panel, BorderLayout.SOUTH);
			JScrollPane scrollPane = new JScrollPane(this.table);
			this.table.setFillsViewportHeight(true);
			this.frame.add(scrollPane, BorderLayout.EAST);
			this.frame.pack();
			this.frame.setResizable(false);
			this.frame.setLocationRelativeTo(null);
			this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
		// Coords
		{
			this.coordinatesList = new ArrayList<double[]>();
			this.coordinatesList.add(new double[] { MP.ORIGIN_X, MP.ORIGIN_Y });
			this.currentAngle = MP.this.rotationAngle;
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
		this.globalUpdate();
	}

	public void globalUpdate() {
		this.frame.setVisible(true);
		this.field.requestFocus();
		while (this.isRunning) {
			this.updateLogic();
			this.repaint();
			try {
				Thread.sleep((long) (1000 / MP.REFRESH_RATE));
			} catch (InterruptedException interruptedException) {
				return;
			}
		}
	}

	private void updateLogic() {
		if (!this.seedsOutOfBounds) {
			double[] lastCoords = this.coordinatesList.get(this.coordinatesList.size() - 1);
			double lastX = lastCoords[0];
			double lastY = lastCoords[1];
			double newX = ((lastX * Math.cos(this.rotationAngle))) - (lastY * Math.sin(this.rotationAngle));
			double newY = ((lastX * Math.sin(this.rotationAngle)) + (lastY * Math.cos(this.rotationAngle)));
			newX += MP.SCALE_FACTOR * Math.cos(this.currentAngle);
			newY += MP.SCALE_FACTOR * Math.sin(this.currentAngle);
			this.coordinatesList.add(new double[] { newX, newY });
			this.currentAngle += this.rotationAngle;
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
		MP.drawCenteredString(graphics2d, "Projet de Mathématiques: Simulation", 0, 0, MP.WIDTH, MP.HEIGHT / 8, MP.TEXT_FONT.deriveFont(40f), MP.TEXT_COLOR);
		graphics2d.setColor(MP.MEASURE_CIRCLE_COLOR);
		graphics2d.fillOval((MP.WIDTH / 2) - MP.MEASURE_CIRCLE_RADIUS, (MP.HEIGHT / 2) - MP.MEASURE_CIRCLE_RADIUS, MP.MEASURE_CIRCLE_RADIUS * 2, MP.MEASURE_CIRCLE_RADIUS * 2);
		graphics2d.setColor(MP.MEASURE_CIRCLE_COLOR.darker());
		graphics2d.drawOval((MP.WIDTH / 2) - MP.MEASURE_CIRCLE_RADIUS, (MP.HEIGHT / 2) - MP.MEASURE_CIRCLE_RADIUS, MP.MEASURE_CIRCLE_RADIUS * 2, MP.MEASURE_CIRCLE_RADIUS * 2);

		if (!this.hasCalculatedCirclePixels) {
			for (int i = 0; i < MP.WIDTH; i++) {
				for (int j = 0; j < MP.HEIGHT; j++) {
					Color color = new Color(this.image.getRGB(i, j), true);
					if (color.getRGB() == MP.MEASURE_CIRCLE_COLOR.getRGB()) {
						this.circlePixels++;
					}
				}
			}

			this.hasCalculatedCirclePixels = true;
		}

		if (this.shouldDraw) {
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
			}
			graphics2d.setColor(MP.TEXT_COLOR);
			graphics2d.setFont(MP.TEXT_FONT);
			graphics2d.drawString("Nombre de graines contenues dans la surface de mesure: " + this.insideSeedsCount, 10, MP.HEIGHT - 35);
			if (this.seedsOutOfBounds && this.calculateEfficiency) {
				int measurePixels = 0;
				for (int x = 0; x < MP.WIDTH; x += 1) {
					for (int y = 0; y < MP.HEIGHT; y += 1) {
						Color color = new Color(this.image.getRGB(x, y), true);
						if (color.getRGB() == MP.MEASURE_CIRCLE_COLOR.getRGB()) {
							measurePixels += 1;
						}
					}
				}
				this.efficiency = 100 - ((100 * measurePixels) / (double) this.circlePixels);
				this.calculateEfficiency = false;
				this.displayEfficiency = true;
				this.addValueToTable = true;
			}
			if (this.displayEfficiency) {
				graphics2d.setColor(MP.TEXT_COLOR);
				graphics2d.drawString("Angle: " + this.rotationAngle + " radians, efficacité: " + this.efficiency + "%", 10, MP.HEIGHT - 15);
			}
		}
		if (this.addValueToTable) {
			DefaultTableModel defaultTableModel = (DefaultTableModel) this.table.getModel();
			defaultTableModel.addRow(new Object[] { rotationString + " rad", String.valueOf(this.efficiency) + "%" });
			this.addValueToTable = false;
		}
		graphics.drawImage(this.image, MP.ORIGIN_X, MP.ORIGIN_Y, MP.WIDTH, MP.HEIGHT, null);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		MP project14 = new MP();
		project14.start();
	}
}
