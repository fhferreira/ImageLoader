package com.novoda.lib.imageloader;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.novoda.lib.imageloader.cache.ImageCache;
import com.novoda.lib.imageloader.cache.SoftMapCache;
import com.novoda.lib.imageloader.file.FileUtil;
import com.novoda.lib.imageloader.service.CacheCleaner;
import com.novoda.lib.imageloader.util.BitmapUtil;

public class BaseImageLoader implements ImageManager {

	private static final String TAG = "ImageLoader";

	private BitmapUtil bitmapUtil;
	private CacheManager cacheManager;
	private Settings settings;

	public BaseImageLoader(Context context, Settings settings) {
		this.bitmapUtil = new BitmapUtil();
		this.settings = settings;
		this.cacheManager = new CacheManager(this, createCache(),
				bitmapUtil.decodeImageResourceAndScaleBitmap(context, settings));
	}

	@Override
	public void load(String url, Context activity, ImageView imageView) {
		try {
			if (cacheManager.hasImageInCache(url)) {
				Bitmap b = cacheManager.getImageFromCache(url);
				if (b != null) {
					imageView.setImageBitmap(b);
					return;
				}
			}
			cacheManager.push(new CacheManager.Image(url, imageView));
		} catch (Throwable t) {
			Log.e(TAG, t.getMessage(), t);
		}
	}

	@Override
	public Bitmap getBitmap(String url) {
		return getBitmap(url, false);
	}
	
	@Override
	public String getFilePath(String imageUrl) {
		File f = getFile(imageUrl);
		if(f.exists()) {
			return f.getAbsolutePath();	
		}
		return null;
	}

	@Override
	public Bitmap getBitmap(String url, boolean scale) {
		if (url != null && url.length() >= 0) {
			File f = getFile(url);
			if (f.exists()) {
				Bitmap b = bitmapUtil.decodeFileAndScale(f, scale, settings);
				if (b != null) {
					return b;
				}
			}
			new FileUtil().retrieveImage(url, f);
			return bitmapUtil.decodeFileAndScale(f, scale, settings);
		}
		return null;
	}
	
	private File getFile(String url) {
		String filename = String.valueOf(url.hashCode());
		File f = new File(settings.getCacheDir(), filename + ".jpg");
		return f;
	}

	@Override
	public void deleteFileCache(Context context) {
		sendCacheCleanUpBroadcast(context, 0);
	}
	
	@Override
	public void reduceFileCache(Context context) {
		long expirationPeriod = settings.getExpirationPeriod();
		sendCacheCleanUpBroadcast(context, expirationPeriod);
	}

	@Override
	public void cleanCache() {
		cacheManager.resetCache(createCache());
	}

	protected ImageCache createCache() {
		return new SoftMapCache();
	}
	
	private void sendCacheCleanUpBroadcast(Context context, long expirationPeriod) {
		String path = settings.getCacheDir().getAbsolutePath();
		Intent i = CacheCleaner.getCleanCacheIntent(path, expirationPeriod);
		i.setPackage(context.getPackageName());
		context.startService(i);
	}

}
