//original from: http://forums.java.net/jive/thread.jspa?messageID=344244

package com.vendo.j3D;

//import com.sun.j3d.utils.geometry.*;
//import com.sun.j3d.utils.universe.*;
//import com.sun.j3d.utils.behaviors.vp.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.*;
//import java.applet.*;
//import java.awt.*;

/*
		ViewingPlatform vp = su.getViewingPlatform(); // su = SimpleUniverse instance
		TransformGroup tg = vp.getViewPlatformTransform();
		cameraEffect = new CameraBehavior(tg);
		cameraEffect.setSchedulingBounds(bounds); // the scene bounds
		sceneBG.addChild(cameraEffect); // add to the scene branch group
*/

public class CameraBehavior extends Behavior
{
	// Add this class to the scene graph, and call setTarget() to
	// force the camera to move smoothly

	private enum Mode {ByTarget, ByPath1, ByPath2};

	private boolean _isMoving;
	private Mode _mode;
	private WakeupCondition _wc;
	private long _startTime;
	private double _duration;
	private TransformGroup _viewTG;
	private Point3d[] _path;
	private Point3d[] _lookAt;
	private int _pathIndex;

	private Point3d _startPos;
	private Point3d _stopPos;
	private Point3d _stopLookAt;
	private Vector3d _stopUp;
	private Vector3d _stopUp1;

	public CameraBehavior (TransformGroup viewTG)
	{
		_isMoving = false;
		_viewTG = viewTG;
		_wc = new WakeupOnElapsedFrames (0);
	}

	public void initialize ()
	{
		wakeupOn (_wc);
	}

	public void processStimulus (Enumeration criteria)
	{
		WakeupCriterion wakeup;
		if (_isMoving) {
			while (criteria.hasMoreElements ()) {
				wakeup = (WakeupCriterion) criteria.nextElement ();
				if (wakeup instanceof WakeupOnElapsedFrames)
					if (_mode == Mode.ByTarget)
						moveCamera1 ();
					else if (_mode == Mode.ByPath1)
						moveCamera2 ();
					else
						moveCamera3 ();
			}
		}
		wakeupOn (_wc);
	}

	public void setTarget (Point3d target, Point3d lookAt, Vector3d up, long duration_ms)
	{
		_mode = Mode.ByTarget;
		_duration = (double) duration_ms;
		_stopPos = new Point3d (target);
		_stopLookAt = new Point3d (lookAt);
		_stopUp = new Vector3d (up);
		_stopUp1 = new Vector3d (up);
		_stopUp1.y = -_stopUp1.y;
		System.out.println ("_stopLookAt="+_stopLookAt.toString ());
		System.out.println ("_stopUp="+_stopUp.toString ());
		System.out.println ("_stopUp1="+_stopUp1.toString ());

		_startTime = System.currentTimeMillis ();

		// calculate the current camera position
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);
		Vector3f currPos=new Vector3f ();
		t3d.get (currPos);
		_startPos = new Point3d (currPos.x, currPos.y, currPos.z);
		System.out.println ("_startPos="+_startPos.toString ());
		_isMoving = true;
	}

	public void setPath (Point3d[] path, Point3d lookAt, Vector3d up)//, long duration_ms)
	{
		_mode = Mode.ByPath1;
		_path = path;
		_pathIndex = 0;
//		_duration = (double) duration_ms;
//		_stopPos = new Point3d (target);
		_stopLookAt = new Point3d (lookAt);
		_stopUp = new Vector3d (up);
		_stopUp1 = new Vector3d (up);
		_stopUp1.y = -_stopUp1.y;
		System.out.println ("_stopLookAt="+_stopLookAt.toString ());
		System.out.println ("_stopUp="+_stopUp.toString ());
		System.out.println ("_stopUp1="+_stopUp1.toString ());

/*
		_startTime = System.currentTimeMillis ();

		// calculate the current camera position
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);
		Vector3f currPos=new Vector3f ();
		t3d.get (currPos);
		_startPos = new Point3d (currPos.x, currPos.y, currPos.z);
		System.out.println ("_startPos="+_startPos.toString ());
*/
		_isMoving = true;
	}

	public void setPath (Point3d[] path, Point3d[] lookAt, Vector3d up)//, long duration_ms)
	{
		_mode = Mode.ByPath2;
		_path = path;
		_lookAt = lookAt;
		_pathIndex = 0;
//		_duration = (double) duration_ms;
//		_stopPos = new Point3d (target);
//		_stopLookAt = new Point3d (lookAt);
		_stopUp = new Vector3d (up);
		_stopUp1 = new Vector3d (up);
		_stopUp1.y = -_stopUp1.y;
//		System.out.println ("_stopLookAt="+_stopLookAt.toString ());
		System.out.println ("_stopUp="+_stopUp.toString ());
		System.out.println ("_stopUp1="+_stopUp1.toString ());

/*
		_startTime = System.currentTimeMillis ();

		// calculate the current camera position
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);
		Vector3f currPos=new Vector3f ();
		t3d.get (currPos);
		_startPos = new Point3d (currPos.x, currPos.y, currPos.z);
		System.out.println ("_startPos="+_startPos.toString ());
*/
		_isMoving = true;
	}

	public boolean isMoving ()
	{
		return _isMoving;
	}

	private void moveCamera1 ()
	{
		Point3d newPos;
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);

		long t = System.currentTimeMillis () - _startTime;
		double fraction = t / _duration;

		if (fraction > 1.0) {
			_isMoving = false;
			newPos = _stopPos;
		} else {
			// Calculate the new camera position
			newPos = new Point3d ((_stopPos.x - _startPos.x) * fraction + _startPos.x,
								  (_stopPos.y - _startPos.y) * fraction + _startPos.y,
								  (_stopPos.z - _startPos.z) * fraction + _startPos.z);
		}

		// Set camera position
//		System.out.println("newPos.z="+newPos.z);
		if (newPos.z > 0)
			t3d.lookAt(newPos, _stopLookAt, _stopUp);
		else
			t3d.lookAt(newPos, _stopLookAt, _stopUp1);

		try	{
//			System.out.println(t3d.toString ());
			t3d.invert ();
		} catch (Exception ee) {
			System.out.println (t3d.toString ());
			System.out.println (ee);
			t3d = new Transform3D ();
		}

		_viewTG.setTransform (t3d);
	}

	private void moveCamera2 ()
	{
		Point3d newPos;
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);

/*
		long t = System.currentTimeMillis () - _startTime;
		double fraction = t / _duration;

		if(fraction > 1.0) {
			_isMoving = false;
			newPos = _stopPos;
		}
		else {
			// Calculate the new camera position
			newPos = new Point3d ((_stopPos.x - _startPos.x) * fraction + _startPos.x,
								  (_stopPos.y - _startPos.y) * fraction + _startPos.y,
								  (_stopPos.z - _startPos.z) * fraction + _startPos.z);
		}
*/
		newPos = _path[_pathIndex];
//		System.out.println ("newPos = " + newPos);

		// Set camera position
//		System.out.println("newPos.z="+newPos.z);
		if (true)
//		if (newPos.z > 0)
			t3d.lookAt(newPos, _stopLookAt, _stopUp);
		else
			t3d.lookAt(newPos, _stopLookAt, _stopUp1);

		try	{
//			System.out.println(t3d.toString ());
			t3d.invert ();
		} catch (Exception ee) {
			System.out.println (t3d.toString ());
			System.out.println (ee);
			t3d = new Transform3D ();
		}

		_viewTG.setTransform (t3d);
		_pathIndex++;
		if (_pathIndex >= _path.length)
			_isMoving = false;
	}

	private void moveCamera3 ()
	{
		Point3d newPos;
		Transform3D t3d = new Transform3D ();
		_viewTG.getTransform (t3d);

/*
		long t = System.currentTimeMillis () - _startTime;
		double fraction = t / _duration;

		if(fraction > 1.0) {
			_isMoving = false;
			newPos = _stopPos;
		}
		else {
			// Calculate the new camera position
			newPos = new Point3d ((_stopPos.x - _startPos.x) * fraction + _startPos.x,
								  (_stopPos.y - _startPos.y) * fraction + _startPos.y,
								  (_stopPos.z - _startPos.z) * fraction + _startPos.z);
		}
*/

/*
		newPos = _path[_pathIndex];
//		System.out.println ("newPos = " + newPos);

		// Set camera position
//		System.out.println("newPos.z="+newPos.z);
		if (true)
//		if (newPos.z > 0)
			t3d.lookAt(newPos, _stopLookAt, _stopUp);
		else
			t3d.lookAt(newPos, _stopLookAt, _stopUp1);
*/

		t3d.lookAt (_path[_pathIndex], _lookAt[_pathIndex], _stopUp);
		try	{
//			System.out.println(t3d.toString ());
			t3d.invert ();
		} catch (Exception ee) {
			System.out.println (t3d.toString ());
			System.out.println (ee);
			t3d = new Transform3D ();
		}

		_viewTG.setTransform (t3d);
		_pathIndex++;
		if (_pathIndex >= _path.length)
			_isMoving = false;
	}
}
