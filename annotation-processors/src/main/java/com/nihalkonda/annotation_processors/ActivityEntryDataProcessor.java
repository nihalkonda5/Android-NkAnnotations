package com.nihalkonda.annotation_processors;

import android.app.Activity;
import android.util.Log;

import com.nihalkonda.annotations.ActivityEntryData;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ActivityEntryDataProcessor {

    private static final String TAG = "ActivityEntryDataProces";

    private static <T> T getIntentValue(Activity activity,String key, Class<T> classObject){
        Object o = activity.getIntent().getExtras().get(key);
        Log.i(TAG, "getIntentValue: "+key+" "+o);
        return classObject.cast(o);
    }

    private static boolean hasIntentValue(Activity activity,String key){
        try {
            return activity.getIntent().getExtras().containsKey(key);
        }catch (Exception e){
            return false;
        }
    }

    public static <T> void process(Activity activity, T defaultValue){
        Field[] fields = activity.getClass().getDeclaredFields();
        Log.i(TAG, "process: "+ Arrays.toString(fields));
        for (Field f:fields) {
            //Log.i(TAG, "process: "+ f);
            if(f.isAnnotationPresent(ActivityEntryData.class)){
                ActivityEntryData annotation = f.getAnnotation(ActivityEntryData.class);
                f.setAccessible(true);
                String key = f.getName();
                try {
                    if(hasIntentValue(activity,key)){
                        f.set(activity,getIntentValue(activity,key,f.getType()));
                    }else if(annotation.value()){
                        f.set(activity,defaultValue);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

}
