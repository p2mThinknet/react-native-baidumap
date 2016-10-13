package com.yiyang.reactnativebaidumap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.util.Log;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.ReadableMap;

/**
 * Created by yiyang on 16/3/1.
 */
public class ReactMapMarker {
    private Marker mMarker;
    private MarkerOptions mOptions;

    private String id;

    private Context mContext;

    public static BitmapDescriptor defaultIcon = BitmapDescriptorFactory.fromResource(R.drawable.location);


    private BitmapDescriptor iconBitmapDescriptor;
    private final DraweeHolder mLogoHolder;
    private DataSource<CloseableReference<CloseableImage>> dataSource;

    private final ControllerListener<ImageInfo> mLogoControllerListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    CloseableReference<CloseableImage> imageReference = null;
                    try {
                        imageReference = dataSource.getResult();
                        if (imageReference != null) {
                            CloseableImage image = imageReference.get();
                            if (image != null && image instanceof CloseableStaticBitmap) {
                                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap)image;
                                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                if (bitmap != null) {
                                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                                }
                            }
                        }
                    } finally {
                        dataSource.close();
                        if (imageReference != null) {
                            CloseableReference.closeSafely(imageReference);
                        }
                    }
                    update();
                }
            };

    public ReactMapMarker(Context context) {
        this.mContext = context;
        mLogoHolder = DraweeHolder.create(createDraweeHierarchy(), null);
        mLogoHolder.onAttach();
    }

    public void buildMarker(ReadableMap annotation) throws Exception{
        if (annotation == null) {
            throw new Exception("marker annotation must not be null");
        }
        id = annotation.getString("id");
        MarkerOptions options = new MarkerOptions();
        double latitude = annotation.getDouble("latitude");
        double longitude = annotation.getDouble("longitude");

        options.position(new LatLng(latitude, longitude));
        if (annotation.hasKey("draggable")) {

            boolean draggable = annotation.getBoolean("draggable");
            options.draggable(draggable);
        }

        if (annotation.hasKey("title")) {
            options.title(annotation.getString("title"));
        }

        options.icon(defaultIcon);
        this.mOptions = options;

        if (annotation.hasKey("image")) {
            String imgUri = annotation.getMap("image").getString("uri");
            if (imgUri != null && imgUri.length() > 0) {
                if (imgUri.startsWith("http://") || imgUri.startsWith("https://")) {
                    ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imgUri)).build();
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    dataSource = imagePipeline.fetchDecodedImage(imageRequest,this);
                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                            .setImageRequest(imageRequest)
                            .setControllerListener(mLogoControllerListener)
                            .setOldController(mLogoHolder.getController())
                            .build();
                    mLogoHolder.setController(controller);
                } else {
                    this.mOptions.icon(getBitmapDescriptorByName(imgUri));
                }
            }
        } else {
            Bitmap bitmap3 = null;
            Bitmap bitmap4 = null;
            try {
                bitmap3 =drawbitmap();
                bitmap4 =drawtext(bitmap3,annotation.getString("subtile"));
                iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap4);
                options.icon(iconBitmapDescriptor);
            }
            catch (Exception e) {
                Log.e("RCTBaiduMap", e.toString());
            }
            finally{
                bitmap3.recycle();
                bitmap4.recycle();
                bitmap3=null;
                bitmap4=null;
                //options.icon(defaultIcon);
            }
            ////////options.icon(defaultIcon);
        }

        this.mOptions = options;
    }
    //firegnu
    private static Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.4f,0.4f);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }


    private Bitmap drawtext(Bitmap bitmap3,String info) {
        int width = bitmap3.getWidth(), hight = bitmap3.getHeight();
        Bitmap icon = Bitmap.createBitmap(width, hight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint photoPaint = new Paint();
        photoPaint.setDither(true);
        photoPaint.setFilterBitmap(true);
        Rect src = new Rect(0, 0, bitmap3.getWidth(), bitmap3.getHeight());
        Rect dst = new Rect(0, 0, width, hight);
        canvas.drawBitmap(bitmap3, src, dst, photoPaint);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
        textPaint.setTextSize(60.0f);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setColor(Color.rgb(26, 179, 0));
        //textPaint.setShadowLayer(3f, 1, 1,this.getResources().getColor(android.R.color.background_dark));//影音的设置
        canvas.drawText(info, 40, 100, textPaint);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        Bitmap newIcon = small(icon);
        return newIcon;
    }
    private Bitmap drawbitmap() {
        Bitmap photo = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.pop);
        int width = photo.getWidth();
        int hight = photo.getHeight();
        Bitmap newb = Bitmap.createBitmap(width, hight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newb);
        Paint photoPaint = new Paint();
        canvas.drawBitmap(photo, 0, 0, photoPaint);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return newb;
    }
    //

    private GenericDraweeHierarchy createDraweeHierarchy() {
        return new GenericDraweeHierarchyBuilder(this.mContext.getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .setFadeDuration(0)
                .build();
    }

    public String getId() {return this.id;}
    public Marker getMarker() {return this.mMarker;}
    public MarkerOptions getOptions() {return this.mOptions;}

    public void addToMap(BaiduMap map) {
        if (this.mMarker == null) {
            this.mMarker = (Marker)map.addOverlay(this.getOptions());
        }
    }

    private int getDrawableResourceByName(String name) {
        return this.mContext.getResources().getIdentifier(name, "drawable", this.mContext.getPackageName());
    }

    private BitmapDescriptor getBitmapDescriptorByName(String name) {
        return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
    }

    private BitmapDescriptor getIcon() {
        if (iconBitmapDescriptor != null) {
            return iconBitmapDescriptor;
        } else {
            return defaultIcon;
        }
    }

    public void update() {
        if (this.mMarker != null) {
            this.mMarker.setIcon(getIcon());
        } else if (this.mOptions != null){
            this.mOptions.icon(getIcon());
        }
    }



}
