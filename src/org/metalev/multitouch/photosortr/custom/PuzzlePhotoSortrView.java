package org.metalev.multitouch.photosortr.custom;

import java.util.ArrayList;

import org.metalev.multitouch.controller.CustomMultiTouchController;
import org.metalev.multitouch.controller.CustomMultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.CustomMultiTouchController.PointInfo;
import org.metalev.multitouch.controller.CustomMultiTouchController.PositionAndScale;
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
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class PuzzlePhotoSortrView extends View implements
		MultiTouchObjectCanvas<PuzzlePhotoSortrView.Img> {

	private static final int[] IMAGES = { R.drawable.sample1,
			R.drawable.sample2, R.drawable.sample3 };

	private ArrayList<Img> mImages = new ArrayList<Img>();

	// --

	private CustomMultiTouchController<Img> multiTouchController = new CustomMultiTouchController<Img>(
			this);

	// --

	private PointInfo currTouchPoint = new PointInfo();

	private boolean mShowDebugInfo = false;

	private static final int UI_MODE_ROTATE = 1, UI_MODE_ANISOTROPIC_SCALE = 2;

	private int mUIMode = UI_MODE_ROTATE;

	// --

	private Paint mLinePaintTouchPointCircle = new Paint();

	// ---------------------------------------------------------------------------------------------------

	public PuzzlePhotoSortrView(Context context) {
		this(context, null);
	}

	public PuzzlePhotoSortrView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PuzzlePhotoSortrView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		Resources res = context.getResources();
		for (int i = 0; i < IMAGES.length; i++)
			mImages.add(new Img(IMAGES[i], res, i));

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

		private float SCREEN_MARGIN = 100;

		float itemHgt;

		private Path path;

		public int order;

		private int border = 10;

		public Img(int resId, Resources res, int order) {
			this.resId = resId;
			this.order = order;
			getMetrics(res);
			initPath();

		}

		private void initPath() {
			path = new Path();
			DisplayMetrics display = getContext().getResources()
					.getDisplayMetrics();
			itemHgt = (display.heightPixels - 60) / 3;
			float LTY = (itemHgt + border) * order;
			float RTX = display.widthPixels;
			path.moveTo(0, LTY);
			path.lineTo(0, LTY + itemHgt);
			path.lineTo(RTX, LTY + itemHgt);
			path.lineTo(RTX, LTY);
			path.close();
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
			this.drawable = res.getDrawable(resId);
			this.width = drawable.getIntrinsicWidth();
			this.height = drawable.getIntrinsicHeight();
			float cx, cy, sx, sy;
			cy = ((itemHgt + border) * order + itemHgt) / 2;
			cx = displayWidth / 2;
			sx = sy = 1 / caculateSampleSize(width, height, displayWidth,
					(int) itemHgt);
			setPos(cx, cy, sx, sy, 0.0f);
		}

		private float caculateSampleSize(int picWdh, int picHgt, int showWdh,
				int showHgt) {
			// 如果此时显示区域比图片大，直接返回
			if ((showWdh < picWdh) || (showHgt < picHgt)) {
				int wdhSample = picWdh / showWdh;
				int hgtSample = picHgt / showHgt;
				// 利用小的来处理
				int sample = wdhSample > hgtSample ? hgtSample : wdhSample;
				int minSample = 2;
				while (sample > minSample) {
					minSample *= 2;
				}
				return minSample >> 1;
			} else {
				return 1;
			}
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
			if (newMinX > 0 || newMaxX < displayWidth
					|| newMinY > (itemHgt + border) * order
					|| newMaxY < (itemHgt + border) * order + itemHgt) {
			}
			this.centerX = centerX;
			this.centerY = centerY;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			// this.angle = angle;
			this.minX = newMinX;
			this.minY = newMinY;
			this.maxX = newMaxX;
			this.maxY = newMaxY;
			Log.i("Position", centerX + "****" + centerY + "****" + minX
					+ "****" + maxX + "****" + minY + "****" + maxY);
			return true;
		}

		/** Return whether or not the given screen coords are inside this image */
		public boolean containsPoint(float scrnX, float scrnY) {
			// FIXME: need to correctly account for image rotation
			return contains(path, scrnX, scrnY);
		}

		private boolean contains(Path paramPath, float pointx, float pointy) {
			RectF localRectF = new RectF();
			paramPath.computeBounds(localRectF, true);
			Region localRegion = new Region();
			localRegion.setPath(paramPath, new Region((int) localRectF.left,
					(int) localRectF.top, (int) localRectF.right,
					(int) localRectF.bottom));
			return localRegion.contains((int) pointx, (int) pointy);
		}

		public void draw(Canvas canvas) {
			try {
				canvas.save();
				final DrawFilter filter = new PaintFlagsDrawFilter(
						Paint.ANTI_ALIAS_FLAG, 0);
				canvas.clipPath(path);
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

		public int getDisplayWidth() {
			return displayWidth;
		}

		public float getItemHgt() {
			return itemHgt;
		}

		public Drawable getDrawable() {
			return drawable;
		}

		public int getWidth() {
			return drawable.getIntrinsicWidth();
		}

		public int getHeight() {
			return drawable.getIntrinsicHeight();
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

		public void setCenterX(float centerX) {
			this.centerX = centerX;
		}

		public void setCenterY(float centerY) {
			this.centerY = centerY;
		}

		public void setMinX(float minX) {
			this.minX = minX;
		}

		public void setMaxX(float maxX) {
			this.maxX = maxX;
		}

		public void setMinY(float minY) {
			this.minY = minY;
		}

		public void setMaxY(float maxY) {
			this.maxY = maxY;
		}

		public int getTop() {
			return (int) ((itemHgt + border) * order);
		}

		public int getBottom() {
			return (int) ((itemHgt + border) * order + itemHgt);
		}

		public int getLeft() {
			return 0;
		}

		public int getRight() {
			return displayWidth;
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

		@Override
		public String toString() {
			return "Img [top=" + getTop() + ", bottom=" + getBottom()
					+ ", left=" + getLeft() + ", right=" + getRight() + "]";
		}

	}

	@Override
	public void actionUp() {
		Img img = getDraggableObjectAtPoint(currTouchPoint);
		if (img.getMinX() > img.getLeft()
				&& img.getMaxX() >= img.getDisplayWidth() + img.getMinX()
						- img.getLeft()) {
			float dx = img.getMinX() - img.getLeft();
			Log.i("info", "left=" + img.getLeft());
			img.setMinX(img.getMinX() - dx);
			img.setMaxX(img.getMaxX() - dx);
			img.setCenterX(img.getCenterX() - dx);
		}
		if (img.getMaxX() < img.getRight()
				&& img.getMinX() <= img.getMaxX() - img.getRight()) {
			float dx = img.getRight() - img.getMaxX();
			img.setMinX(img.getMinX() + dx);
			img.setMaxX(img.getMaxX() + dx);
			img.setCenterX(img.getCenterX() + dx);
			Log.i("info", "right" + img.getRight());
		}
		if (img.getTop() + img.getMinY() > img.getTop()
				&& img.getTop() + img.getMaxY() >= img.getBottom()
						+ img.getMinY()) {
			Log.i("info", "top=" + img.getTop());
			float dy = img.getMinY();
			img.setMinY(img.getMinY() - dy);
			img.setMaxY(img.getMaxY() - dy);
			img.setCenterY(img.getCenterY() - dy);
		}
		if (img.getTop() + img.getMaxY() < img.getBottom()
				&& img.getMinY() <= img.getTop() + img.getMaxY()
						- img.getBottom()) {
			Log.i("info", "bottom=" + img.getBottom());
			float dy = img.getBottom() - img.getMinY() - img.getTop();
			img.setMinY(img.getMinY() + dy);
			img.setMaxY(img.getMaxY() + dy);
			img.setCenterY(img.getCenterY() + dy);
		}

		Log.i("info", img.toString());
		invalidate();
	}
}
