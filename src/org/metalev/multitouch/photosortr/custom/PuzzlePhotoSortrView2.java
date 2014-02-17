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
import android.graphics.BitmapFactory;
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

public class PuzzlePhotoSortrView2 extends View implements
		MultiTouchObjectCanvas<PuzzlePhotoSortrView2.Img> {

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

	public PuzzlePhotoSortrView2(Context context) {
		this(context, null);
	}

	public PuzzlePhotoSortrView2(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PuzzlePhotoSortrView2(Context context, AttributeSet attrs,
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
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) == 0, img.getScale(),
				(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0, img.getScale(),
				img.getScale(), (mUIMode & UI_MODE_ROTATE) != 0, 0);
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
		private Bitmap bitmap;
		private float[] mPathLT;
		private float[] mPathOffset;
		private Path path;
		public int order;
		private int border;
		private int viewWdh, viewHgt, itemHgt;
		private float scale;

		private float centerX, centerY;

		public Img(int resId, Resources res, int order) {
			this.resId = resId;
			this.order = order;
			getMetrics(res);
			initPath();
		}

		public float[] getmPathOffset() {
			return mPathOffset;
		}

		public void setmPathOffset(float[] mPathOffset) {
			this.mPathOffset = mPathOffset;
		}

		private void initPath() {
			path = new Path();
			mPathLT = new float[2];
			mPathOffset = new float[2];
			mPathLT[0] = 0f;
			mPathLT[1] = 0f;
			mPathOffset[0] = 0f;
			mPathOffset[1] = 0f;

			mPathLT[0] = 0;
			mPathLT[1] = (itemHgt + border) * order;
			path.moveTo(mPathLT[0], mPathLT[1]);
			path.lineTo(viewWdh, mPathLT[1]);
			path.lineTo(viewWdh, mPathLT[1] + itemHgt);
			path.lineTo(0, mPathLT[1] + itemHgt);
			path.close();
		}

		private void getMetrics(Resources res) {
			DisplayMetrics metrics = res.getDisplayMetrics();
			this.viewWdh = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
					.max(metrics.widthPixels, metrics.heightPixels) : Math.min(
					metrics.widthPixels, metrics.heightPixels);
			this.viewHgt = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
					.min(metrics.widthPixels, metrics.heightPixels) : Math.max(
					metrics.widthPixels, metrics.heightPixels);
			this.itemHgt = (viewHgt - 60) / 3;
			this.border = viewWdh / 160;
		}

		public float getScale() {
			return scale;
		}

		public void setScale(float scale) {
			this.scale = scale;
		}

		public Bitmap getBitmap() {
			return bitmap;
		}

		/** Called by activity's onResume() method to load the images */
		public void load(Resources res, int i) {
			getMetrics(res);
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(getResources(), resId, opt);
			int bmpWdh = opt.outWidth;
			int bmpHgt = opt.outHeight;
			int size = caculateSampleSize(bmpWdh, bmpHgt, viewWdh, itemHgt);
			opt.inJustDecodeBounds = false;
			opt.inSampleSize = size;
			scale = (float) (1.0 / size);
			bitmap = BitmapFactory.decodeResource(getResources(), resId, opt);
		}

		private int caculateSampleSize(int picWdh, int picHgt, int showWdh,
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
			this.bitmap.recycle();
			this.bitmap = null;
		}

		public boolean setPos(PositionAndScale newImgPosAndScale) {
			return setPos(
					newImgPosAndScale.getXOff(),
					newImgPosAndScale.getYOff(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleX() : newImgPosAndScale.getScale(),
					(mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0 ? newImgPosAndScale
							.getScaleY() : newImgPosAndScale.getScale(),
					newImgPosAndScale.getAngle());
		}

		/** Set the position and scale of an image in screen coordinates */
		private boolean setPos(float centerX, float centerY, float scaleX,
				float scaleY, float angle) {
			float ws = centerX - getBitmap().getWidth() - getmPathOffset()[0];
			float hs = centerY - getBitmap().getHeight() - getmPathOffset()[1];
			this.scale = scaleX;
			mPathOffsetX = centerX - getCenterX();
			mPathOffsetY = centerY - getCenterY();
			mPathOffset[0] = ws;
			mPathOffset[1] = hs;
			this.centerX = centerX;
			this.centerY = centerY;
			return true;
		}

		public float getCenterX() {
			return centerX;
		}

		public void setCenterX(float centerX) {
			this.centerX = centerX;
		}

		public float getCenterY() {
			return centerY;
		}

		public void setCenterY(float centerY) {
			this.centerY = centerY;
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

		float mPathOffsetX, mPathOffsetY;

		public void draw(Canvas canvas) {
			try {
				Paint paint = new Paint();
				canvas.save();
				canvas.clipPath(path);
				canvas.drawColor(Color.GRAY);
				canvas.drawBitmap(bitmap, mPathLT[0] + mPathOffsetX
						+ mPathOffset[0], mPathLT[1] + mPathOffsetY
						+ mPathOffset[1], paint);
				canvas.restore();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public int getWidth() {
			return viewWdh;
		}

		public int getTop() {
			return (int) ((itemHgt + border) * order);
		}

		public int getBottom() {
			return (int) ((itemHgt + border) * order - (bitmap.getHeight() - itemHgt));
		}

		public int getLeft() {
			return 0;
		}

		public int getRight() {
			return -(bitmap.getWidth() - viewWdh);
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
		// if (img.getMinX() > img.getLeft()) {
		// Log.i("info", "left=" + img.getLeft());
		// img.setMinX(img.getLeft());
		// img.setMaxX(img.getMaxX() - img.getLeft());
		// img.setCenterX(img.getCenterX() - img.getLeft());
		// }
		// if (img.getMinX() < img.getRight()) {
		// img.setMinX(img.getRight());
		// img.setMaxX(img.getMaxX() + (img.getRight() - img.getMinX()));
		// img.setCenterX(img.getCenterX() + (img.getRight() - img.getMinX()));
		// Log.i("info", "right" + img.getRight());
		// }
		// if (img.getMinY() > img.getTop()) {
		// Log.i("info", "top=" + img.getTop());
		// img.setMinY(img.getTop());
		// img.setMaxY(img.getMaxY() - (img.getMinY() - img.getTop()));
		// img.setCenterY(img.getCenterY() - (img.getMinY() - img.getTop()));
		// }
		// if (img.getMinY() > img.getBottom()) {
		// Log.i("info", "bottom=" + img.getBottom());
		// img.setMinY(img.getBottom());
		// img.setMaxY(img.getMaxY() + (img.getMinY() - img.getBottom()));
		// img.setCenterY(img.getCenterY() + (img.getMinY() - img.getBottom()));
		// }

		Log.i("info", img.toString());
		invalidate();
	}
}
