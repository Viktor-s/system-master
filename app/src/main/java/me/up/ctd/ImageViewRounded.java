package me.up.ctd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewRounded extends ImageView {

    public ImageViewRounded(Context context) {
        super(context);
    }

    public ImageViewRounded(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewRounded(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();

        if (drawable == null) {
            return;
        }

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Bitmap fullSizeBitmap = drawable.getBitmap();

        int scaledWidth = getMeasuredWidth();
        int scaledHeight = getMeasuredHeight();

        Bitmap mScaledBitmap;
        if (scaledWidth == fullSizeBitmap.getWidth()
                && scaledHeight == fullSizeBitmap.getHeight()) {
            mScaledBitmap = fullSizeBitmap;
        } else {
            mScaledBitmap = Bitmap.createScaledBitmap(fullSizeBitmap,
                    scaledWidth, scaledHeight, true /* filter */);
        }

        // Bitmap roundBitmap = getRoundedCornerBitmap(mScaledBitmap);

        // Bitmap roundBitmap = getRoundedCornerBitmap(getContext(),
        // mScaledBitmap, 10, scaledWidth, scaledHeight, false, false,
        // false, false);
        // canvas.drawBitmap(roundBitmap, 0, 0, null);

        Bitmap circleBitmap = getCircledBitmap(mScaledBitmap);

        canvas.drawBitmap(circleBitmap, 0, 0, null);

    }

    public Bitmap getRoundedCornerBitmap(Context context, Bitmap input,
                                         int pixels, int w, int h, boolean squareTL, boolean squareTR,
                                         boolean squareBL, boolean squareBR) {

        Bitmap output = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final float densityMultiplier = context.getResources()
                .getDisplayMetrics().density;

        final int color = 0xff424242;

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, w, h);
        final RectF rectF = new RectF(rect);

        // make sure that our rounded corner is scaled appropriately
        final float roundPx = pixels * densityMultiplier;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        // draw rectangles over the corners we want to be square
        if (squareTL) {
            canvas.drawRect(0, 0, w / 2, h / 2, paint);
        }
        if (squareTR) {
            canvas.drawRect(w / 2, 0, w, h / 2, paint);
        }
        if (squareBL) {
            canvas.drawRect(0, h / 2, w / 2, h, paint);
        }
        if (squareBR) {
            canvas.drawRect(w / 2, h / 2, w, h, paint);
        }

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(input, 0, 0, paint);

        return output;
    }

    Bitmap getCircledBitmap(Bitmap bitmap) {

        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);

        int color = Color.BLUE;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
//        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getHeight()/2, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);



        return result;
    }

}