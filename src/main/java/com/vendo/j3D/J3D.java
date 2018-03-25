//original from: http://show.docjava.com/book/cgij/exportToHTML/j3d/SwingingCubeApp.java.html

package com.vendo.j3D;

/*
 *	@(#)RotPosPathApp.java 1.1 00/09/22 14:37
 *
 * Copyright (c) 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

import com.sun.j3d.utils.applet.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.vp.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.applet.*;
import java.awt.*;


public class J3D extends Applet {

	public BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		// axes
		Transform3D tr = new Transform3D();
		tr.setScale(0.25);
//		tr.setTranslation(new Vector3d(-0.8, -0.7, -0.5));
		TransformGroup tg = new TransformGroup(tr);
		objRoot.addChild(tg);
		Axes axes = new Axes();
		tg.addChild(axes);

		//lights
		AmbientLight aLight = new AmbientLight(true, new Color3f(Color.blue));
		aLight.setInfluencingBounds(new BoundingSphere());
		objRoot.addChild(aLight);
		PointLight ptlight1 = new PointLight (new Color3f (Color.red), new Point3f (0f,0f,2f), new Point3f (1f,1f,0f));
		ptlight1.setInfluencingBounds(new BoundingSphere());
		objRoot.addChild(ptlight1);
		PointLight ptlight2 = new PointLight (new Color3f (Color.red), new Point3f (0f,0f,-2f), new Point3f (1f,1f,0f));
		ptlight2.setInfluencingBounds(new BoundingSphere());
		objRoot.addChild(ptlight2);

		Transform3D axisOfRotPos = new Transform3D();
		AxisAngle4f axis = new AxisAngle4f(1.0f, 0.0f, 0.0f, 0.0f);
		axisOfRotPos.set(axis);

		final int Num = 50;

		//rotation
		Quat4f[] quats = new Quat4f[Num + 1];
		for (int ii = 0; ii < Num; ii++) {
//			quats[ii] = new Quat4f (0.0f, 0.0f, 0.0f, 0.0f);
			quats[ii] = new Quat4f (0.0f, 1.0f, 1.0f, 0.0f);
		}
		quats[Num] = quats[0];

/*
	quats[0] = new Quat4f(0.0f, 1.0f, 1.0f, 0.0f);
	quats[1] = new Quat4f(1.0f, 0.0f, 0.0f, 0.0f);
	quats[2] = new Quat4f(0.0f, 1.0f, 0.0f, 0.0f);
	quats[3] = new Quat4f(0.0f, 1.0f, 1.0f, 0.0f);
	quats[4] = new Quat4f(0.0f, 0.0f, 1.0f, 0.0f);
	quats[5] = new Quat4f(0.0f, 1.0f, 1.0f, 0.0f);
	quats[6] = new Quat4f(1.0f, 1.0f, 0.0f, 0.0f);
	quats[7] = new Quat4f(1.0f, 0.0f, 0.0f, 0.0f);
	quats[8] = quats[0];
*/

		//follow an ellipse
		Point3f[] positions = new Point3f[Num + 1];
		for (int ii = 0; ii < Num; ii++) {
			float theta = ii * 2f * (float) Math.PI / (float) Num;
			float x = .9f * (float) Math.cos (theta);
			float z = .6f * (float) Math.sin (theta);
			positions[ii] = new Point3f(x, 0f, z);
		}
		positions[Num] = positions[0];

		float[] knots = new float[Num + 1];
		for (int ii = 0; ii < Num; ii++) {
			knots[ii] = ii / (float) Num;
		}
		knots[Num] = 1f;

		TransformGroup target = new TransformGroup();
		target.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		Alpha alpha = new Alpha(-1, 5 * 1000);
		RotPosPathInterpolator rotPosPath = new RotPosPathInterpolator(
				alpha, target, axisOfRotPos, knots, quats, positions);
		rotPosPath.setSchedulingBounds(new BoundingSphere());

		objRoot.addChild(target);
		objRoot.addChild(rotPosPath);
		target.addChild(new ColorCube(0.05));
//		target.addChild(new Sphere(0.025f));

		Background background = new Background();
		background.setColor(1.0f, 1.0f, 1.0f);
		background.setApplicationBounds(new BoundingSphere());
		objRoot.addChild(background);

		PointArray point_geom = new PointArray(Num + 1, GeometryArray.COORDINATES);
		point_geom.setCoordinates(0, positions);
		Appearance points_appear = new Appearance();
		ColoringAttributes points_coloring = new ColoringAttributes();
		points_coloring.setColor(1.0f, 0.0f, 0.0f);
		points_appear.setColoringAttributes(points_coloring);
		PointAttributes points_points = new PointAttributes(4.0f, true);
		points_appear.setPointAttributes(points_points);
		Shape3D points = new Shape3D(point_geom, points_appear);
		objRoot.addChild(points);

//		objRoot.compile();

		return objRoot;
	} // end of CreateSceneGraph method of RotPosPathApp

	// Create a simple scene and attach it to the virtual universe

	public J3D() {
		setLayout(new BorderLayout());
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		Canvas3D canvas3D = new Canvas3D(config);
		add("Center", canvas3D);

		BranchGroup scene = createSceneGraph();

		// SimpleUniverse is a Convenience Utility class
		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);

		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
		simpleU.getViewingPlatform().setNominalViewingTransform();

		// viewplatform motion via mouse
		OrbitBehavior orbit = new OrbitBehavior(canvas3D);
		orbit.setSchedulingBounds(new BoundingSphere());
		simpleU.getViewingPlatform().setViewPlatformBehavior(orbit);

		TransformGroup tgView = simpleU.getViewingPlatform().getViewPlatformTransform();
		_cameraEffect = new CameraBehavior(tgView);
		_cameraEffect.setSchedulingBounds(new BoundingSphere());

		scene.addChild(_cameraEffect);

		Transform3D t3d = new Transform3D();
		tgView.getTransform(t3d);
		Vector3f cameraInitialPos = new Vector3f();
		t3d.get(cameraInitialPos);
		System.out.println("cameraInitialPos="+cameraInitialPos.toString ());

		simpleU.addBranchGraph(scene);
	}

	public void setTarget (Point3d target, long duration_ms, boolean wait) {
		Point3d lookAt = new Point3d (0, 0, 0);
		Vector3d up = new Vector3d (0, 1, 0);

		_cameraEffect.setTarget (target, lookAt, up, duration_ms);

		if (wait) {
			System.out.print ("waiting for camera to finish moving ");
			while (_cameraEffect.isMoving ()) {
				try {
				  Thread.sleep (200L); //ms
				} catch (InterruptedException ex) {}
				System.out.print(".");
			}
			System.out.println(" done");
		}
	}

	public void setPath (Point3d[] path, boolean wait) {
		Point3d lookAt = new Point3d (0, 0, 0);
		Vector3d up = new Vector3d (0, 1, 0);

		_cameraEffect.setPath (path, lookAt, up);//, duration_ms);

		if (wait) {
			System.out.print ("waiting for camera to finish moving ");
			while (_cameraEffect.isMoving ()) {
				try {
				  Thread.sleep (200L); //ms
				} catch (InterruptedException ex) {}
				System.out.print(".");
			}
			System.out.println(" done");
		}
	}

	public void setPath (Point3d[] path, Point3d[] lookAt, boolean wait) {
//		Point3d lookAt = new Point3d (0, 0, 0);
		Vector3d up = new Vector3d (0, 1, 0);

		_cameraEffect.setPath (path, lookAt, up);//, duration_ms);

		if (wait) {
			System.out.print ("waiting for camera to finish moving ");
			while (_cameraEffect.isMoving ()) {
				try {
				  Thread.sleep (200L); //ms
				} catch (InterruptedException ex) {}
				System.out.print(".");
			}
			System.out.println(" done");
		}
	}

	public static void main(String[] args) {
/*
		System.out.print("RotPosPathApp.java \n- a demonstration of using the RotPosPathInterpolator ");
		System.out.println("and Alpha classes to provide animation in a Java 3D scene.");
		System.out.println("The RotPosPathInterpolator changes the target TransformGroup");
		System.out.println("to change the POSition and ROTation along a PATH.  The positions");
		System.out.println("are marked by the red dots in the scene.");
		System.out.println("This is a simple example progam from The Java 3D API Tutorial.");
		System.out.println("The Java 3D Tutorial is available on the web at:");
		System.out.println("http://java.sun.com/products/java-media/3D/collateral");
*/

		int mode = 1;

		J3D j3d = new J3D ();
		Frame frame = new MainFrame(j3d, 512, 512);

		if (mode == 1) {
			final int NumPoints = 1000;
			Point3d path[] = new Point3d [NumPoints];
			Point3d lookAt[] = new Point3d [NumPoints];
			for (int ii = 0; ii < path.length; ii++) {
				double theta = ii * 2 * Math.PI / (double) NumPoints;
				double dist = 2.5;
				double x = 1.1 * dist * Math.cos (theta);
				double y = 1;
				double z = 1.2 * dist * Math.sin (theta);
				path[ii] = new Point3d (x, y, z);

//				double x2 = -0.5 * dist * Math.cos (2 * theta);
				double x2 = -1 + 2 * ii / (double) NumPoints;
				lookAt[ii] = new Point3d (x2, 0, 0);
			}

			j3d.setPath (path, lookAt, true);
			while (true) {
				j3d.setPath (path, lookAt, true);
			}
		} else if (mode == 2) {
			final int NumPoints = 500;
			Point3d path[] = new Point3d [NumPoints];
			for (int ii = 0; ii < path.length; ii++) {
				double theta = ii * 2 * Math.PI / (double) NumPoints;
				double dist = 2.5;
				double x = 1.1 * dist * Math.cos (theta);
				double y = 1;
				double z = 1.2 * dist * Math.sin (theta);
				path[ii] = new Point3d (x, y, z);
			}

			j3d.setPath (path, true);
			while (true) {
				j3d.setPath (path, true);
			}
		} else if (mode == 3) {
			double nonZero = 0.0000001; //avoid exception in invert
			double d = 2;
			j3d.setTarget (new Point3d (nonZero, d, d), 1500L, true);
			while (true) {
				j3d.setTarget (new Point3d (nonZero, d, -d), 6000L, true);
				j3d.setTarget (new Point3d (nonZero, -d, -d), 6000L, true);
				j3d.setTarget (new Point3d (nonZero, -d, d), 6000L, true);
				j3d.setTarget (new Point3d (nonZero, d, d), 6000L, true);
			}
		}
	}

	//members
	CameraBehavior _cameraEffect;
}
