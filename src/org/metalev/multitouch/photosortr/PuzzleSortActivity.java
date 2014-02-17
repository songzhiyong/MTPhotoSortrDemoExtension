/**
 * PhotoSorterActivity.java
 * 
 * (c) Luke Hutchison (luke.hutch@mit.edu)
 * 
 * --
 * 
 * Released under the MIT license (but please notify me if you use this code, so that I can give your project credit at
 * http://code.google.com/p/android-multitouch-controller ).
 * 
 * MIT license: http://www.opensource.org/licenses/MIT
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.metalev.multitouch.photosortr;

import org.metalev.multitouch.photosortr.custom.LeanPhotoSortrView;
import org.metalev.multitouch.photosortr.custom.PhotoSortrView;
import org.metalev.multitouch.photosortr.custom.PuzzlePhotoSortrView;
import org.metalev.multitouch.photosortr.custom.UnRegularPhotoSortrView;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class PuzzleSortActivity extends Activity {
	View photoSorter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setTitle(R.string.instructions);
		int module = getIntent().getIntExtra("template_index", 0);
		switch (module) {
		case 0:
			photoSorter = new PhotoSortrView(this);
			setContentView(photoSorter);
			((PhotoSortrView) photoSorter).loadImages(this);
			break;
		case 1:
			photoSorter = new PuzzlePhotoSortrView(this);
			setContentView(photoSorter);
			((PuzzlePhotoSortrView) photoSorter).loadImages(this);
			break;
		case 2:
			photoSorter = new LeanPhotoSortrView(this);
			setContentView(photoSorter);
			((LeanPhotoSortrView) photoSorter).loadImages(this);
			break;
		case 3:
			photoSorter = new UnRegularPhotoSortrView(this);
			setContentView(photoSorter);
			((UnRegularPhotoSortrView) photoSorter).loadImages(this);
			break;
		case 4:
			photoSorter = new LeanPhotoSortrView(this);
			setContentView(photoSorter);
			((LeanPhotoSortrView) photoSorter).loadImages(this);
			break;
		default:
			break;
		}

	}
}