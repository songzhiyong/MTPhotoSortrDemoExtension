package org.metalev.multitouch.photosortr.custom;

import java.util.ArrayList;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;
import org.metalev.multitouch.photosortr.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class PhotoSortrView extends View implements
		MultiTouchObjectCanvas<PhotoSortrView.Img> {

	private static final int[] IMAGES = { R.drawable.sample1, R.drawable.sample2,
			R.drawable.sample3 };

	private ArrayList<Img> mImages = new ArrayList<Img>();

	// --

	private MultiTouchController<Img> multiTouchController = new MultiTouchController<Img>(
			this);

	// --

	private PointInfo currTouchPoint = new PointInfo();

	private boolean mShowDebugInfo = false;

	private static final int UI_MODE_ROTATE = 1, UI_MODE_ANISOTROPIC_SCALE = 2;

	private int mUIMode = UI_MODE_ROTATE;

	// --

	private Paint mLinePaintTouchPointCircle = new Paint();

	// ---------------------------------------------------------------------------------------------------

	public PhotoSortrView(Context context) {
		this(context, null);
	}

	public PhotoSortrView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoSortrView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		Resources res = context.getResources();
		for (int i = 0; i < IMAGES.length; i++)
			mImages.add(new Img(IMAGES[i], res));

		mLinePaintTouchPointCircle.setColor(Color.YELLOW);
		mLinePaintTouchPointCircle.setStrokeWidth(5);
		mLinePaintTouchPointCircle.setStyle(Style.STROKE);
		mLinePaintTouchPointCircle.setAntiAlias(true);
		setBackgroundColor(Color.BLACK);
	}

	/** Called by activity's onResume() method to load the images */
	public void loadImages(Context context) {
		Resources res = context.getResources();
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).load(res, i);
	}

	/**
	 * Called by activity's onPause() method to free memory used for loading the
	 * images
	 */
	public void unloadImages() {
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).unload();
	}

	// ---------------------------------------------------------------------------------------------------

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).draw(canvas);
		if (mShowDebugInfo)
			drawMultitouchDebugMarks(canvas);
	}

	// ---------------------------------------------------------------------------------------------------

	public void trackballClicked() {
		mUIMode = (mUIMode + 1) % 3;
		invalidate();
	}

	private void drawMultitouchDebugMarks(Canvas canvas) {
		if (currTouchPoint.isDown()) {
			float[] xs = currTouchPoint.getXs();
			float[] ys = currTouchPoint.getYs();
			float[] pressures = currTouchPoint.getPressures();
			int numPoints = Math.min(currTouchPoint.getNumTouchPoints(), 2);
			for (int i = 0; i < numPoints; i++)
				canvas.drawCircle(xs[i], ys[i], 50 + pressures[i] * 80,
						mLinePaintTouchPointCircle);
			if (numPoints == 2)
				canvas.drawLine(xs[0], ys[0], xs[1], ys[1],
						mLinePaintTouchPointCircle);
		}
	}

	// ---------------------------------------------------------------------------------------------------

	/** Pass touch events to the MT controller */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return multiTouchController.onTouchEvent(event);
	}

	/**
	 * Get the image that is under the single-touch point, or return null
	 * (canceling the drag op) if none
	 */
	public Img getDraggableObjectAtPoint(PointInfo pt) {
		float x = pt.getX(), y = pt.getY();
		int n = mImages.size();
		for (int i = n - 1; i >= 0; i--) {
			Img im = mImages.get(i);
			if (im.containsPoint(x, y))
				return im;
		}
		return null;
	}

	/**
	 * Select an object for dragging. Called whenever an object is found to be
	 * under the point (non-null is returned by getDraggableObjectAtPoint()) and
	 * a drag operation is starting. Called with null when drag op ends.
	 */
	public void selectObject(Img img, PointInfo touchPoint) {
		currTouchPoint.set(touchPoint);
		if (img != null) {
			// Move image to the top of the stack when selected
			mImages.remove(img);
			mImages.add(img);
		} else {
			// Called with img == null when drag stops.
		}
		invalidate();
	}

	/**
	 * Get the current position and scale of the selected image. Called whenever
	 * a drag starts or is reset.
	 */
	public void getPositionAndScale(Img img, PositionAndScale objPosAndScaleOut) {
		// FIXME affine-izem (and fix the fact that the anisotropic_scale part
		// requires averaging the two scale factors)
		objPosAndScaleOut.set(img.getCenterX(), img.getCenterY(),
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) == 0,
				(img.getScaleX() + img.getScaleY()) / 2,
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0, img.getScaleX(),
				img.getScaleY(), (mUIMode & UI_MODE_ROTATE) != 0,
				img.getAngle());
	}

	/** Set the position and scale of the dragged/stretched image. */
	public boolean setPositionAndScale(Img img,
			PositionAndScale newImgPosAndScale, PointInfo touchPoint) {
		currTouchPoint.set(touchPoint);
		boolean ok = img.setPos(newImgPosAndScale);
		if (ok)
			invalidate();
		return ok;
	}

	// ----------------------------------------------------------------------------------------------

	class Img {
		private int resId;

		private Drawable drawable;

		private int width, height, displayWidth, displayHeight;

		private float centerX, centerY, scaleX, scaleY, angle;

		private float minX, maxX, minY, maxY;

		private static final float SCREEN_MARGIN = 100;

		public Img(int resId, Resources res) {
			this.resId = resId;
			getMetrics(res);
		}

		private void getMetrics(Resources res) {
			DisplayMetrics metrics = res.getDisplayMetrics();
			// The DisplayMetrics don't seem to always be updated on screen
			// rotate, so we hard code a portrait
			// screen orientation for the non-rotated screen here...
			// this.displayWidth = metrics.widthPixels;
			// this.displayHeight = metrics.heightPixels;
			this.displayWidth = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
					.max(metrics.widthPixels, metrics.heightPixels) : Math.min(
					metrics.widthPixels, metrics.heightPixels);
			this.displayHeight = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
					.min(metrics.widthPixels, metrics.heightPixels) : Math.max(
					metrics.widthPixels, metrics.heightPixels);
		}

		/** Called by activity's onResume() method to load the images */
		public void load(Resources res, int i) {
			getMetrics(res);
			Bitmap bitmap = addWhiteBorder(
					drawableToBitmap(res.getDrawable(resId)), 20);
			this.drawable = new BitmapDrawable(res, bitmap);
			this.width = drawable.getIntrinsicWidth();
			this.height = drawable.getIntrinsicHeight();
			float cx, cy, sx, sy, angle;
			float percent = 0.5f;
			if (i == 1) {
				percent = (float) (0.3 + Math.random() * 0.2);
			}
			percent = (float) (0.5 + Math.random() * 0.2);
			cx = SCREEN_MARGIN
					+ (float) (percent * (displayWidth - 2 * SCREEN_MARGIN));
			cy = SCREEN_MARGIN
					+ (float) ((2 * i + 1) / 6.0 * (displayHeight - 2 * SCREEN_MARGIN))
					- 20;
			float sc = (float) (Math.max(displayWidth, displayHeight)
					/ (float) Math.max(width, height) * 0.5 * 0.3 + 0.15);
			angle = (float) Math.random();
			sx = sy = sc;
			Log.i("info", cx + "--" + cy + "--" + sc);
			setPos(cx, cy, sx, sy, angle);
		}

		/**
		 * Called by activity's onPause() method to free memory used for loading
		 * the images
		 */
		public void unload() {
			this.drawable = null;
		}

		/** Set the position and scale of an image in screen coordinates */
		public boolean setPos(PositionAndScale newImgPosAndScale) {
			return setPos(
					newImgPosAndScale.getXOff(),
					newImgPosAndScale.getYOff(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleX() : newImgPosAndScale.getScale(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleY() : newImgPosAndScale.getScale(),
					newImgPosAndScale.getAngle());
			// FIXME: anisotropic scaling jumps when axis-snapping
			// FIXME: affine-ize
			// return setPos(newImgPosAndScale.getXOff(),
			// newImgPosAndScale.getYOff(),
			// newImgPosAndScale.getScaleAnisotropicX(),
			// newImgPosAndScale.getScaleAnisotropicY(), 0.0f);
		}

		/** Set the position and scale of an image in screen coordinates */
		private boolean setPos(float centerX, float centerY, float scaleX,
				float scaleY, float angle) {
			float ws = (width / 2) * scaleX, hs = (height / 2) * scaleY;
			float newMinX = centerX - ws, newMinY = centerY - hs, newMaxX = centerX
					+ ws, newMaxY = centerY + hs;
			if (newMinX > displayWidth - SCREEN_MARGIN
					|| newMaxX < SCREEN_MARGIN
					|| newMinY > displayHeight - SCREEN_MARGIN
					|| newMaxY < SCREEN_MARGIN)
				return false;
			this.centerX = centerX;
			this.centerY = centerY;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			Log.i("info", centerX + "--" + centerX + "--" + scaleX + "--"
					+ angle);
			this.angle = angle;
			this.minX = newMinX;
			this.minY = newMinY;
			this.maxX = newMaxX;
			this.maxY = newMaxY;
			return true;
		}

		/** Return whether or not the given screen coords are inside this image */
		public boolean containsPoint(float scrnX, float scrnY) {
			// FIXME: need to correctly account for image rotation
			return (scrnX >= minX && scrnX <= maxX && scrnY >= minY && scrnY <= maxY);
		}

		public void draw(Canvas canvas) {
			try {
				canvas.save();
				final DrawFilter filter = new PaintFlagsDrawFilter(
						Paint.ANTI_ALIAS_FLAG, 0);
				float dx = (maxX + minX) / 2;
				float dy = (maxY + minY) / 2;
				drawable.setBounds((int) minX, (int) minY, (int) maxX,
						(int) maxY);
				canvas.translate(dx, dy);
				canvas.rotate(angle * 180.0f / (float) Math.PI);
				canvas.translate(-dx, -dy);
				canvas.setDrawFilter(filter);
				setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				drawable.draw(canvas);
				canvas.restore();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Drawable getDrawable() {
			return drawable;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public float getCenterX() {
			return centerX;
		}

		public float getCenterY() {
			return centerY;
		}

		public float getScaleX() {
			return scaleX;
		}

		public float getScaleY() {
			return scaleY;
		}

		public float getAngle() {
			return angle;
		}

		// FIXME: these need to be updated for rotation
		public float getMinX() {
			return minX;
		}

		public float getMaxX() {
			return maxX;
		}

		public float getMinY() {
			return minY;
		}

		public float getMaxY() {
			return maxY;
		}

		public Bitmap drawableToBitmap(Drawable drawable) {
			if (drawable instanceof BitmapDrawable) {
				return ((BitmapDrawable) drawable).getBitmap();
			}
			final DrawFilter filter = new PaintFlagsDrawFilter(
					Paint.ANTI_ALIAS_FLAG, 0);
			Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.setDrawFilter(filter);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
			return bitmap;
		}

		private Bitmap addWhiteBorder(Bitmap bmp, int borderSize) {
			Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth()
					+ borderSize * 2, bmp.getHeight() + borderSize * 2,
					bmp.getConfig());
			Canvas canvas = new Canvas(bmpWithBorder);
			canvas.drawColor(Color.WHITE);
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			canvas.drawBitmap(bmp, borderSize, borderSize, paint);
			return bmpWithBorder;
		}
	}
}
