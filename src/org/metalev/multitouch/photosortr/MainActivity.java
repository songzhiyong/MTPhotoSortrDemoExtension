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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.google.android.gms.common.SignInButton;

public class MainActivity extends Activity {
	private SignInButton sInButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setTitle(R.string.instructions);
		setContentView(R.layout.layout_main);

	}

	public void onTemplate0(View view) {
		int mode = 0;
		startTemplate(mode);
	}

	public void onTemplate1(View view) {
		int mode = 1;
		startTemplate(mode);
	}

	public void onTemplate2(View view) {
		int mode = 2;
		startTemplate(mode);
	}

	public void onTemplate3(View view) {
		int mode = 3;
		startTemplate(mode);
	}

	public void onTemplate4(View view) {
		int mode = 4;
		startTemplate(mode);
	}

	private void startTemplate(int mode) {
		Intent intent = new Intent(this, PuzzleSortActivity.class);
		intent.putExtra("template_index", mode);
		startActivity(intent);
	}

}