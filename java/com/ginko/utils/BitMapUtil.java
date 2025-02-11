package com.ginko.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class BitMapUtil {
    private static final Size ZERO_SIZE = new Size(0, 0);
    private static final Options OPTIONS_GET_SIZE = new Options();
    private static final Options OPTIONS_DECODE = new Options();
    private static final byte[] LOCKED = new byte[0];
    //Mantain the recycle sequence, to make sure the fist image is recycled firstly.
    private static final LinkedList<String> CACHE_ENTRIES = new LinkedList<String>();
    //Threads which are request image
    private static final Queue<QueueEntry> TASK_QUEUE = new LinkedList<QueueEntry>();
    //save the keys of images which is processing, to avoid duplicated request
    private static final Set<String> TASK_QUEUE_INDEX = new HashSet<String>();
    // cache Bitmap
    private static final Map<String,Bitmap> IMG_CACHE_INDEX = new HashMap<String,Bitmap>();       // image path:bitmap
    private static int CACHE_SIZE = 20; // numbers of images to cache.
    static {
        OPTIONS_GET_SIZE.inJustDecodeBounds = true;
        new Thread() {
            {
                setDaemon(true);
            }
            public void run() {
                while (true) {
                    synchronized (TASK_QUEUE) {
                        if (TASK_QUEUE.isEmpty()) {
                            try {
                                TASK_QUEUE.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    QueueEntry entry = TASK_QUEUE.poll();
                    String key = createKey(entry.path, entry.width,
                            entry.height);
                    TASK_QUEUE_INDEX.remove(key);
                    createBitmap(entry.path, entry.width, entry.height);
                }
            }
        }.start();
    }


    public static Bitmap getBitmap(String path, int width, int height) {
        if(path==null){
            return null;
        }
        Bitmap bitMap = null;
        try {
            if (CACHE_ENTRIES.size() >= CACHE_SIZE) {
                destoryLast();
            }
            bitMap = useBitmap(path, width, height);
            if (bitMap != null && !bitMap.isRecycled()) {
                return bitMap;
            }
            bitMap = createBitmap(path, width, height);
            String key = createKey(path, width, height);
            synchronized (LOCKED) {
                IMG_CACHE_INDEX.put(key, bitMap);
                CACHE_ENTRIES.addFirst(key);
            }
        } catch (OutOfMemoryError err) {
            destoryLast();
            System.out.println(CACHE_SIZE);
            return createBitmap(path, width, height);
        }
        return bitMap;
    }


    public static Size getBitMapSize(String path) {
        File file = new File(path);
        if (file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                BitmapFactory.decodeStream(in, null, OPTIONS_GET_SIZE);
                return new Size(OPTIONS_GET_SIZE.outWidth,
                        OPTIONS_GET_SIZE.outHeight);
            } catch (FileNotFoundException e) {
                return ZERO_SIZE;
            } finally {
                closeInputStream(in);
            }
        }
        return ZERO_SIZE;
    }

    private static Bitmap useBitmap(String path, int width, int height) {
        Bitmap bitMap = null;
        String key = createKey(path, width, height);
        synchronized (LOCKED) {
            bitMap = IMG_CACHE_INDEX.get(key);
            if (null != bitMap) {
                if (CACHE_ENTRIES.remove(key)) {
                    CACHE_ENTRIES.addFirst(key);
                }
            }
        }
        return bitMap;
    }

    private static void destoryLast() {
        synchronized (LOCKED) {
            String key = CACHE_ENTRIES.removeLast();
            if (key.length() > 0) {
                Bitmap bitMap = IMG_CACHE_INDEX.remove(key);
                if (bitMap != null && !bitMap.isRecycled()) {
                    bitMap.recycle();
                    bitMap = null;
                }
            }
        }
    }

    private static String createKey(String path, int width, int height) {
        if (null == path || path.length() == 0) {
            return "";
        }
        return path + "_" + width + "_" + height;
    }

    private static Bitmap createBitmap(String path, int width, int height) {
        File file = new File(path);
        if (file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                Size size = getBitMapSize(path);
                if (size.equals(ZERO_SIZE)) {
                    return null;
                }
                int scale = 1;
                int a = size.getWidth() / width;
                int b = size.getHeight() / height;
                scale = Math.max(a, b);
                synchronized (OPTIONS_DECODE) {
                    OPTIONS_DECODE.inSampleSize = scale;
                    Bitmap bitMap = BitmapFactory.decodeStream(in, null,
                            OPTIONS_DECODE);
                    return bitMap;
                }
            } catch (FileNotFoundException e) {
                Log.v("BitMapUtil","createBitmap=="+e.toString());
            } finally {
                closeInputStream(in);
            }
        }
        return null;
    }

    private static void closeInputStream(InputStream in) {
        if (null != in) {
            try {
                in.close();
            } catch (IOException e) {
                Log.v("BitMapUtil", "closeInputStream==" + e.toString());
            }
        }
    }

    static class Size {
        private int width, height;
        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }
    }

    static class QueueEntry {
        public String path;
        public int width;
        public int height;
    }
}