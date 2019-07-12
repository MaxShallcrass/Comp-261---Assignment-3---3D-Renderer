package renderer;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public class Renderer extends GUI {
	Color directLightColour;
	Vector3D directLightSource;
	static ArrayList<Vector3D> directLightSources=new ArrayList<Vector3D>();
	static ArrayList<Color> directLightColors=new ArrayList<Color>();
	private Scene scene;	

	@Override
	/*
	 * Parse's the given file into a Scene object, which
	 * contains the data used to render an image.
	 */
	protected void onLoad(File file) {
		List<String> lines = null;

		// reads data
		try {
			lines = Files.readAllLines(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<Scene.Polygon> polys = new ArrayList<>();
		String[] data;

		int numPolygons = Integer.parseInt(lines.get(0));
		// stores all the data in a arraylist of polygons
		for (int lineNum = 1; lineNum <= numPolygons; lineNum++) {
			data = lines.get(lineNum).split(",");
			// gets the color
			Color color = new Color(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]));
			// gets vectors for polygon
			Vector3D a = new Vector3D(Float.parseFloat(data[3]), Float.parseFloat(data[4]), Float.parseFloat(data[5]));
			Vector3D b = new Vector3D(Float.parseFloat(data[6]), Float.parseFloat(data[7]), Float.parseFloat(data[8]));
			Vector3D c = new Vector3D(Float.parseFloat(data[9]), Float.parseFloat(data[10]),Float.parseFloat(data[11]));
			polys.add(new Scene.Polygon(a, b, c, color));
		}
		
		// gets the lightsource
		data = lines.get(numPolygons + 1).split(","); // last line of the file
		Vector3D lightSource = new Vector3D(Float.parseFloat(data[0]), Float.parseFloat(data[1]),Float.parseFloat(data[2]));
		directLightSources.add(lightSource);
		directLightColors.add(new Color(150, 150, 150));
		scene = new Scene(polys, lightSource);
	}
	
	
	/*
	 * Rotate's the user's viewpoint.
	 */
	@Override
	protected void onKeyPress(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
			scene = Pipeline.rotateScene(scene, 0, (float) (0.1 * Math.PI));
		}
		if (ev.getKeyCode() == KeyEvent.VK_RIGHT) {
			scene = Pipeline.rotateScene(scene, 0, (float) (-0.1 * Math.PI));
		}
		if (ev.getKeyCode() == KeyEvent.VK_UP) {
			scene = Pipeline.rotateScene(scene, (float) (-0.1 * Math.PI), 0);
		}
		if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
			scene = Pipeline.rotateScene(scene, (float) (0.1 * Math.PI), 0);
		}
	}

	/*
	 * Renders the image, uses a z buffer
	 * Translates and scales the image to
	 * maximise the screen size
	 */
	protected BufferedImage render() {
		if (scene == null)
			return null;
		//sets the image to fit the screen 
		scene = Pipeline.scaleScene(scene);
		scene = Pipeline.translateScene(scene);
		
		Color[][] zBuffer = new Color[CANVAS_WIDTH][CANVAS_HEIGHT];
		float[][] zDepth = new float[CANVAS_WIDTH][CANVAS_HEIGHT];
		
		//sets up zbuffer and zdepth to take on values
		for (int x = 0; x < CANVAS_WIDTH; x++) {
			for (int y = 0; y < CANVAS_HEIGHT; y++) {
				zBuffer[x][y] = Color.gray;
				zDepth[x][y] = Float.POSITIVE_INFINITY;
			}
		}

		for (Scene.Polygon p : scene.getPolygons()) {
			// determines if the polygon should be rendered.
			if (!Pipeline.isHidden(p)) {
				Color ambient = new Color(getAmbientLight()[0], getAmbientLight()[1], getAmbientLight()[2]);
				Color c = Pipeline.getShading(p, directLightSources, directLightColors, ambient);
				// gets the edgelist of the polygon.
				EdgeList edges = Pipeline.computeEdgeList(p);
				// adds the polygon's edgeList to the total zBuffer.
				Pipeline.computeZBuffer(zBuffer, zDepth, edges, c);
			}
		}
		return convertBitmapToImage(zBuffer);
	}

	/**
	 * Converts a 2D array of Colors to a BufferedImage. Assumes that bitmap is
	 * indexed by column then row and has imageHeight rows and imageWidth
	 * columns. Note that image.setRGB requires x (col) and y (row) are given in
	 * that order.
	 */
	private BufferedImage convertBitmapToImage(Color[][] bitmap) {
		BufferedImage image = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < CANVAS_WIDTH; x++) {
			for (int y = 0; y < CANVAS_HEIGHT; y++) {
				image.setRGB(x, y, bitmap[x][y].getRGB());
			}
		}
		return image;
	}

	public static void main(String[] args) {
		new Renderer();
	}
}
// code for comp261 assignments