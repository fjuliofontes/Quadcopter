package com.example.juliofontes.quadcopterremotecontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Vector;
import java.util.jar.Attributes;

/**
 * Created by juliofontes on 29/01/18.
 */

public class JoystickView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {

    private float centerX, centerY;
    private float finalx, finaly;
    //private float baseRadius;
    private float hatRadius;
    public static final int scale = 1250;

    private JoystickListener joystickCallback;
    //private final int ratio = 5; //the smaller, the more shading will occur

    private void setupDimensions(){
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        //baseRadius = Math.min(getWidth(),getHeight()) / 2;
        hatRadius = Math.min(getWidth(),getHeight()) / 8;
    }

    public JoystickView(Context context){
        super(context);
        getHolder().addCallback(this); //Setup the surface holder callback so that we know when its ready
        setOnTouchListener(this); // Allow this class to handle touch input
        if(context instanceof JoystickListener) // setup the callback in this
            joystickCallback = (JoystickListener) context;
    }

    public JoystickView(Context context, AttributeSet attributes, int style){
        super(context,attributes,style);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener) // setup the callback in this
            joystickCallback = (JoystickListener) context;
    }

    public JoystickView(Context context,AttributeSet attributes){
        super(context,attributes);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener) // setup the callback in this
            joystickCallback = (JoystickListener) context;
    }

    private void drawJoystick(float newX, float newY){
        if(getHolder().getSurface().isValid()) {
            /*Init Stuff*/
            Canvas myCanvas = this.getHolder().lockCanvas(); // Stuff to draw
            Paint colors = new Paint();
            float default_width = colors.getStrokeWidth();
            myCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Clear the BG
            /*Center Lines : color = red*/
            colors.setStrokeWidth(10);
            colors.setARGB(255,255,0,0); // color joystick base
            myCanvas.drawLine(0,getHeight()/2,getWidth(),getHeight()/2,colors);
            myCanvas.drawLine(getWidth()/2,0,getWidth()/2,getHeight(),colors);
            /*Horizontal and Vertical Lines : color = green*/
            colors.setARGB(255,0,255,0); // color joystick base
            colors.setStrokeWidth(20);
            myCanvas.drawLine(0,0,getWidth(),0,colors); // horizontal line down
            myCanvas.drawLine(0,getHeight(),getWidth(),getHeight(),colors); // horizontal line up
            myCanvas.drawLine(0,0,0,getHeight(),colors); // vertical line left
            myCanvas.drawLine(getWidth(),0,getWidth(),getHeight(),colors); // vertical line right
            /*Joystick drawing : color = blue */
            colors.setARGB(255,0,0,255); // color of joysitck itself
            myCanvas.drawCircle(newX,newY,hatRadius,colors); // Draw the joystick hat
            /*Joystick inside circle : color = white */
            colors.setARGB(255,255,255,255); // color of joysitck itself
            myCanvas.drawCircle(newX,newY,hatRadius/8,colors); // Draw the joystick hat
            /*Write Process*/
            getHolder().unlockCanvasAndPost(myCanvas); // write the new drawing to the surface view

        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        setupDimensions();
        drawJoystick(centerX,centerY);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public boolean onTouch(View v, MotionEvent e){
        if(v.equals(this)){
            if(e.getAction()!=e.ACTION_UP) {
                /*
                float displacement = (float) Math.sqrt(Math.pow(e.getX() - centerX, 2) + Math.pow(e.getY() - centerY, 2));
                if(displacement < baseRadius){
                    drawJoystick(e.getX(), e.getY());
                    joystickCallback.onJoystickMoved((e.getX() - centerX)/baseRadius, (e.getY() - centerY)/baseRadius,getId());
                }
                else{
                    float ratio = baseRadius/displacement;
                    float constrainedX = centerX + (e.getX() - centerX) * ratio;
                    float constrainedY = centerY + (e.getY() - centerY) * ratio;
                    drawJoystick(constrainedX,constrainedY);
                }
                */

                if ((e.getX() >= 0) && (e.getY() >= 0) && (e.getX() < getWidth()) && (e.getY() < getHeight())) { // Inside the square
                    drawJoystick(e.getX(), e.getY());
                    sendCoords(e.getX(),e.getY());
                }
                else{
                    computeCoords(e.getX(),e.getY()); //Outside the square
                }

            }
            else {
                if(getId() == R.id.joystickRight){ // we want that right joystick return always to the center
                    drawJoystick(centerX, centerY); // return to center
                    sendCoords(centerX,centerY); //this is a ensure that the coords will be proprely send
                }
                else{
                    if(e.getY() >= 0 && e.getY() < getHeight()){
                        drawJoystick(centerX,e.getY()); // return only the X coordenate to the center
                        sendCoords(centerX,e.getY()); //this is a ensure that the coords will be propely send
                    }
                    else{
                        drawJoystick(centerX,finaly); // return only the X coordenate to the center
                        sendCoords(centerX,finaly); //this is a ensure that the coords will be propely send
                    }
                }
            }
        }
        return true;
    }

    public interface JoystickListener{
        void onJoystickMoved(int xPercent, int yPercent, int id);
    }

    public void sendCoords(float valX, float valY){
        joystickCallback.onJoystickMoved((int)((valX*scale)/getWidth()),(int)((valY*scale)/getHeight()),getId());
    }

    /*  -------------
        |     |     |
        | z3  |  z2 |
        |-----|-----|
        | z4  |  z1 |
        |     |     |
        -------------
    */
    public void computeCoords(float valx, float valy){
        if(( valx >= centerX) && (valy <= centerY) ){ // zone 2
            if((valx >= getWidth()) && (valy >= 0)){ // Aresta Vertical Right
                //drawJoystick(getWidth(), valy);
                finalx = getWidth();
                finaly = valy;
            }
            else if((valy < 0) && (valx < getWidth())){ // Aresta Horizontal Up
                //drawJoystick(valx, 0);
                finalx = valx;
                finaly = 0;
            }
            else { //vertice
                //drawJoystick(getWidth(), 0);
                finalx = getWidth();
                finaly = 0;
            }
        }
        else if((valx >= centerX) && (valy > centerY)){ // zone 1
            if(valx >= getWidth() && (valy < getHeight())){ // Aresta Vertical Right
                //drawJoystick(getWidth(), valy);
                finalx = getWidth();
                finaly = valy;
            }
            else if ((valy >= getHeight()) && (valx < getWidth()) ){ // Aresta Horizontal Down
                //drawJoystick(valx, getHeight());
                finalx = valx;
                finaly = getHeight();
            }
            else { // vertice
                //drawJoystick(getWidth(),getHeight());
                finalx = getWidth();
                finaly = getHeight();
            }
        }
        else if((valx < centerX) && (valy>centerY)){ // zone 4
            if((valy >= getHeight()) && (valx >= 0)){ // Aresta Horizontal Down
                //drawJoystick(valx, getHeight());
                finalx = valx;
                finaly = getHeight();
            }
            else if((valx < 0) && (valy < getHeight())){ // Aresta Vertical Left
                //drawJoystick(0, valy);
                finalx = 0;
                finaly = valy;
            }
            else {// vertice
                //drawJoystick(0, getHeight());
                finalx = 0;
                finaly = getHeight();
            }
        }
        else if((valx < centerX) && (valy <= centerY)){ // zone 3
            if((valy < 0) && (valx >= 0)){ // Aresta Horizontal Up
                //drawJoystick(valx, 0);
                finalx = valx;
                finaly = 0;
            }
            else if((valx < 0) && (valy >= 0)){ // Aresta Vertical Left
                //drawJoystick(0, valy);
                finalx = 0;
                finaly = valy;
            }
            else { // Aresta
                //drawJoystick(0, 0);
                finalx = 0;
                finaly = 0;
            }
        }
        else{
            drawJoystick(centerX, centerY); // return to center because something anomolos happned
        }
        drawJoystick(finalx,finaly); // send the computed value
        sendCoords(finalx,finaly);
    }
}
