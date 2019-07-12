package renderer;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.*;


import renderer.Scene.Polygon;

/**
 * The Pipeline class has method stubs for all the major components of the
 * rendering pipeline, for you to fill in.
 * 
 * Some of these methods can get quite long, in which case you should strongly
 * consider moving them out into their own file. You'll need to update the
 * imports in the test suite if you do.
 */
public class Pipeline {

	/**
	 * Returns true if the given polygon is facing away from the camera, so it
	 * is hidden
	 */
	public static boolean isHidden(Polygon poly) {
		return getUnitNormal(poly).z>0;
	}

	/**
	 * Computes the colour of a polygon on the screen, once the lights, their
	 * angles relative to the polygon's face, and the reflectance of the polygon
	 * have been accounted for.
	 * 
	 * @param lightDirection
	 *            The Vector3D pointing to the directional light read in from
	 *            the file.
	 * @param lightColor
	 *            The color of that directional light.
	 * @param ambientLight
	 *            The ambient light in the scene, i.e. light that doesn't depend
	 *            on the direction.
	 */
	
	public static Color getShading(Polygon poly, ArrayList<Vector3D> lightDirection, ArrayList<Color> lightColor, Color ambientLight) {
		
		Color reflectance =poly.getReflectance();
		Vector3D normal=getUnitNormal(poly);
		double rAmbient =  ambientLight.getRed()/255.0;
		double gAmbient =  ambientLight.getGreen()/255.0;
		double bAmbient =  ambientLight.getBlue()/255.0;	
		
		double rFlec = reflectance.getRed();
		double gFlec = reflectance.getGreen();
		double bFlec = reflectance.getBlue();
		double r = 0;
		double g = 0;
		double b = 0;
		//for each light source
		for(int i=0; i< lightDirection.size(); i++){
			double rIncident = lightColor.get(i).getRed()/255.0;
			double gIncident = lightColor.get(i).getGreen()/255.0;
			double bIncident = lightColor.get(i).getBlue()/255.0;	
			
			float cos =lightDirection.get(i).dotProduct(normal)/(lightDirection.get(i).mag*normal.mag);
			if(cos>=0) {
				//calculates amount of light from a light source
				r+=rIncident*rFlec*cos;
				g+=gIncident*gFlec*cos;
				b+=bIncident*bFlec*cos;
			}	
		}
		//calculates ambient light
			r+=(rAmbient*rFlec);
			g+=(gAmbient*gFlec);
			b+=(bAmbient*bFlec);		
		return new Color(colorBoundary(r), colorBoundary(g), colorBoundary(b));
	}
	
	/*
	 * Just check to make sure the color value is
	 * in the range 0-255
	 */
	public static int colorBoundary(double c){
		if(c<0)
			return 0;
		if(c>255)
			return 255;
		return (int) c;
	}
	
	
	/**
	 * Gets the unit normal vector of a polygon.
	 * i.e. the direction of the vector that points directly out from the face of the polygon.
	 * Used to find the difference between the light source and the polygon's face.
	 */
	private static Vector3D getUnitNormal(Polygon poly) {
		Vector3D a = poly.getVertices()[0], b = poly.getVertices()[1], c = poly.getVertices()[2];
		return b.minus(a).crossProduct(c.minus(b)).unitVector();
	}
	
	

	/**
	 * This method should rotate the polygons and light such that the viewer is
	 * looking down the Z-axis. The idea is that it returns an entirely new
	 * Scene object, filled with new Polygons, that have been rotated.
	 * 
	 * @param scene
	 *            The original Scene.
	 * @param xRot
	 *            An angle describing the viewer's rotation in the YZ-plane (i.e
	 *            around the X-axis).
	 * @param yRot
	 *            An angle describing the viewer's rotation in the XZ-plane (i.e
	 *            around the Y-axis).
	 * @return A new Scene where all the polygons and the light source have been
	 *         rotated accordingly.
	 */
	public static Scene rotateScene(Scene scene, float xRot, float yRot) {
		if (scene == null) {
			return null;
		}
		//transformation matrice's
		Transform xTrans = Transform.newXRotation(xRot);
		Transform yTrans = Transform.newYRotation(yRot);
		List<Scene.Polygon> polygons = scene.getPolygons();
		//transforms all the polygons
		for (Polygon p : polygons) {
			Vector3D[] vec = p.getVertices();
			for (int i = 0; i < vec.length; i++) {
				if (xRot != 0.0f) {
					vec[i] = xTrans.multiply(vec[i]);
				}
				if (yRot != 0.0f) {
					vec[i] = yTrans.multiply(vec[i]);
				}
			}
		}

		//transforms the light sources
		for(int i=0; i<Renderer.directLightSources.size(); i++) {
			Vector3D v =Renderer.directLightSources.get(i);
			if (xRot != 0.0f) {
				v = xTrans.multiply(v);
			}
			if (yRot != 0.0f) {
				v = yTrans.multiply(v);
			}
			Renderer.directLightSources.set(i, v);	
		}
		
		return new Scene(polygons, scene.getLight());
	}

	/**
	 * Translates the scene so fits inside the window
	 * 
	 * @param scene
	 * @return
	 */
	public static Scene translateScene(Scene scene) {
		Rectangle area = computeBoundingBox(scene.getPolygons());
		
		float xDiff = -area.x;
		float yDiff = -area.y;
		
		Transform t = Transform.newTranslation(new Vector3D(xDiff, yDiff, 0));
		
		for (Scene.Polygon p : scene.getPolygons()) {
			for (int i=0; i<p.getVertices().length; i++) {
				p.getVertices()[i] = t.multiply(p.getVertices()[i]);
			}
		}
				
		return new Scene(scene.getPolygons(), scene.getLight());
	}

	/**
	 * Scales the scene so it fits perfectly in the window
	 * 
	 * Scales by whichever is longer, length or width
	 * @param scene
	 * @return
	 */
	public static Scene scaleScene(Scene scene) {
		Rectangle area = computeBoundingBox(scene.getPolygons());
		float width = (float) area.getWidth();
		float height = (float) area.getHeight();
		float scale;
		// finds which direction to scale
		boolean scaleWidth = width - GUI.CANVAS_WIDTH > height - GUI.CANVAS_HEIGHT;
		if (scaleWidth) {
			scale = GUI.CANVAS_WIDTH / width;
		} else {
			scale = GUI.CANVAS_HEIGHT / height;
		}
		Transform scaler = Transform.newScale(new Vector3D(scale, scale, scale));

		for (Polygon p : scene.getPolygons()) {
			for (int i = 0; i < p.getVertices().length; i++)
				p.getVertices()[i] = scaler.multiply(p.getVertices()[i]);
		}
		
		return new Scene(scene.getPolygons(), scene.getLight());
	}

	/**
	 * Computes the edgelist of a single provided polygon, as per the lecture
	 * slides.
	 */
	public static EdgeList computeEdgeList(Polygon poly) {
		Vector3D[] vectors = Arrays.copyOf(poly.getVertices(), 3);
		int yMin=Integer.MAX_VALUE;
		int yMax=Integer.MIN_VALUE;
	
		for (Vector3D v : vectors) {
			if (v.y > yMax) {
				yMax = Math.round(v.y);
			}
			if (v.y < yMin) {
				yMin = Math.round(v.y);
			}
		}
		EdgeList edgeList = new EdgeList(yMin, yMax);
		
		Vector3D a=poly.getVertices()[0];
		Vector3D b=poly.getVertices()[1];
		Vector3D c=poly.getVertices()[2];
	
		setValues(a, b, edgeList);
		setValues(b, c, edgeList);
		setValues(c, a, edgeList);
		return edgeList;	
	}
	/*
	 * Sets the pixel values on the screen in which will be the given polygon
	 */
	public static void setValues(Vector3D a, Vector3D b, EdgeList edge){	
		float slope= (b.x - a.x) / (b.y - a.y);
		float slopeZ = (b.z - a.z) / (b.y - a.y);
		
		float x=a.x;
		float z=a.z;
		int y=Math.round(a.y);
		if(y<=b.y) {
			while(y<=Math.round(b.y)) {
			edge.setLeftX(y, x);
			edge.setLeftZ(y, z);
			x+=slope;
			z+=slopeZ;
			y++;
			}
		}else {		
		 while(y>=Math.round(b.y)) {
				edge.setRightX(y, x);	
				edge.setRightZ(y, z);
				x-=slope;
				z-=slopeZ;
				y--;
				}
		}
	}
	/*
	 * Creates a bounding box of the a scene, 
	 */
	public static Rectangle computeBoundingBox(List<Scene.Polygon> polygons){
		float minY= Float.MAX_VALUE;
		float maxY=Float.MIN_VALUE;
		
		float minX= Float.MAX_VALUE;
		float maxX=Float.MIN_VALUE;
		
		
		for(Scene.Polygon poly: polygons) {	
			for(Vector3D v : poly.getVertices()) {
				minY=Math.min(minY, v.y);
				maxY=Math.max(maxY, v.y);
				
				minX=Math.min(minX, v.x);
				maxX=Math.max(maxX, v.x);		
			}	
		}
		return new Rectangle(Math.round(minX), Math.round(minY), Math.round(maxX - minX), Math.round(maxY - minY));
	}
	

	/**
	 * Fills a zbuffer with the contents of a single edge list according to the
	 * lecture slides.
	 * 
	 * The idea here is to make zbuffer and zdepth arrays in your main loop, and
	 * pass them into the method to be modified.
	 * 
	 * @param zbuffer
	 *            A double array of colours representing the Color at each pixel
	 *            so far.
	 * @param zdepth
	 *            A double array of floats storing the z-value of each pixel
	 *            that has been coloured in so far.
	 * @param polyEdgeList
	 *            The edgelist of the polygon to add into the zbuffer.
	 * @param polyColor
	 *            The colour of the polygon to add into the zbuffer.
	 */
	public static void computeZBuffer(Color[][] zbuffer, float[][] zdepth, EdgeList polyEdgeList, Color polyColor) {
		
		for (int y = polyEdgeList.getStartY(); y < polyEdgeList.getEndY(); y++) {
			float slope = (polyEdgeList.getRightZ(y) - polyEdgeList.getLeftZ(y))
					/ (polyEdgeList.getRightX(y) - polyEdgeList.getLeftZ(y));

			float z = polyEdgeList.getLeftZ(y);
			int x = Math.round(polyEdgeList.getLeftX(y));
			while (x < Math.round(polyEdgeList.getRightX(y))) {
				
				if (withinBounds(x, y) &&z < zdepth[x][y]) {
					zbuffer[x][y] = polyColor;
					zdepth[x][y] = z;
				}
				z += slope;
				x++;
			}
		}		
	}
	/*
	 * Makes sure that the x y coordinates are on the screen
	 */
	public static boolean withinBounds(int x, int y) {
		return y >= 0 && x >= 0 && y < GUI.CANVAS_HEIGHT && x < GUI.CANVAS_WIDTH;
	}
}

// code for comp261 assignments
