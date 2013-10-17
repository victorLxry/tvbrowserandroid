package org.tvbrowser.tvbrowser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class UiUtils {
  public static final SimpleDateFormat LONG_DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.getDefault());
  
  public static void showProgramInfo(Activity activity, long id) {

    Cursor c = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    c.moveToFirst();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
    View layout = activity.getLayoutInflater().inflate(R.layout.detail_layout, null);
    
    TextView date = (TextView)layout.findViewById(R.id.detail_date_channel);
    TextView title = (TextView)layout.findViewById(R.id.detail_title);
    TextView genre = (TextView)layout.findViewById(R.id.detail_genre);
    TextView episode = (TextView)layout.findViewById(R.id.detail_episode_title);
    TextView shortDescription = (TextView)layout.findViewById(R.id.detail_short_description);
    
    Date start = new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
    SimpleDateFormat day = new SimpleDateFormat("EEE",Locale.getDefault());
    
    long channelID = c.getLong(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
    
    Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
    
    channel.moveToFirst();
    
    date.setText(day.format(start) + " " + java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(start) + " " + DateFormat.getTimeFormat(activity).format(start) + " - " + DateFormat.getTimeFormat(activity)/*.getTimeInstance(java.text.DateFormat.SHORT)*/.format(new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)))) + " " + channel.getString(0));
    
    channel.close();
    
    title.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
      genre.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)));
    }
    else {
      genre.setVisibility(View.GONE);
    }
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
      episode.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)));
    }
    else {
      episode.setVisibility(View.GONE);
    }
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
      shortDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)));
    }
    else {
      shortDescription.setVisibility(View.GONE);
    }
    
    c.close();
        
    builder.setView(layout);
    builder.show();
  }
  
  public static void createContextMenu(Activity activity, ContextMenu menu, long id) {
    activity.getMenuInflater().inflate(R.menu.program_context, menu);
    
    Cursor cursor = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null, null);
    
    if(Build.VERSION.SDK_INT < 14) {
      menu.findItem(R.id.prog_create_calendar_entry).setVisible(false);
    }
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      boolean showMark = cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES)) || cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES)).trim().length() == 0;
      
      menu.findItem(R.id.prog_mark_item).setVisible(showMark);
      menu.findItem(R.id.prog_unmark_item).setVisible(!showMark);
      
      if(!showMark) {
        if(cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES)).contains(SettingConstants.MARK_VALUE_FAVORITE)) {
          menu.findItem(R.id.create_favorite_item).setVisible(false);
        }
      }
    }
    
    cursor.close();
  }
  
  @SuppressLint("NewApi")
  public static boolean handleContextMenuSelection(Activity activity, MenuItem item, long programID, View menuView) {
    Cursor info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.DATA_KEY_TITLE}, null, null,null);
    
    String current = null;
    String title = null;
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
        current = info.getString(0);
      }
      
      title = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));
    }
    
    info.close();
    
    ContentValues values = new ContentValues();
    
    if(item.getItemId() == R.id.create_favorite_item) {
      UiUtils.editFavorite(null, activity, title);
      return true;
    }
    else if(item.getItemId() == R.id.prog_mark_item) {
      if(current != null && current.contains(SettingConstants.MARK_VALUE)) {
        return true;
      }
      else if(current == null || current.trim().length() == 0) {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, SettingConstants.MARK_VALUE);
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";"+SettingConstants.MARK_VALUE);
      }
      
      if(menuView != null) {
        info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME}, null, null,null);
        info.moveToFirst();
        
        UiUtils.handleMarkings(activity, info, menuView, current);
        
        info.close();   
      }
    }
    else if(item.getItemId() == R.id.prog_unmark_item){
      if(current == null || current.trim().length() == 0) {
        return true;
      }
      
      current = "";
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      
      if(menuView != null) {
        menuView.setBackgroundResource(android.R.drawable.list_selector_background);
      }
    }
    else if(item.getItemId() == R.id.prog_create_calendar_entry) {
      info = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = activity.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        
        if(channel.getCount() > 0) {
          channel.moveToFirst();
          
          // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT);
          
          addCalendarEntry.setType(activity.getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
          String desc = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            desc = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
            
            if(desc != null && desc.trim().toLowerCase().equals("null")) {
              desc = null;
            }
          }
          
          String episode = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            episode = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
            
            if(episode != null && episode.trim().toLowerCase().equals("null")) {
              episode = null;
            }
          }
          addCalendarEntry.putExtra(Events.EVENT_LOCATION, channel.getString(0));
          // Add the calendar event details
          addCalendarEntry.putExtra(Events.TITLE, info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          String description = null;
          
          if(episode != null) {
            description = episode;
          }
          
          if(desc != null) {
            if(description != null) {
              description += "\n\n" + desc;
            }
            else {
              description = desc;
            }
          }
          
          if(description != null) {
            addCalendarEntry.putExtra(Events.DESCRIPTION, description);
          }
          
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)));
          
          // Use the Calendar app to add the new event.
          activity.startActivity(addCalendarEntry);
          
          if(current != null && current.contains(SettingConstants.MARK_VALUE_CALENDAR)) {
            return true;
          }
          else if(current == null || current.trim().length() == 0) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, SettingConstants.MARK_VALUE_CALENDAR);
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";"+SettingConstants.MARK_VALUE_CALENDAR);
          }
          
          if(menuView != null) {
            menuView.setBackgroundColor(activity.getResources().getColor(SettingConstants.MARK_COLOR_MAP.get(SettingConstants.MARK_VALUE_CALENDAR)));
          }
        }
        
        channel.close();
      }
      
      info.close();
    }
    
    if(values.size() > 0) {
      if(menuView != null) {
        menuView.invalidate();
      }
      
      activity.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
      
      Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
      intent.putExtra(SettingConstants.MARKINGS_ID, programID);
      
      LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }
    
    return true;
  }
  
  private static class RunningDrawable extends Drawable {
    private Paint mBase;
    private Paint mSecond;
    private long mStartTime;
    private long mEndTime;
    
    public RunningDrawable(Paint base, Paint second, long startTime, long endTime) {
      mBase = base;
      mSecond = second;
      mStartTime = startTime;
      mEndTime = endTime;
    }
    
    @Override
    public void draw(Canvas canvas) {
      if(mStartTime <= System.currentTimeMillis() && System.currentTimeMillis() < mEndTime) {
        long expiredSeconds = System.currentTimeMillis() - mStartTime;
        float percent = expiredSeconds/(float)(mEndTime - mStartTime);
        
        int leftWidth = (int)(getBounds().width() * percent);
        
        canvas.drawRect(0, 0, leftWidth, getBounds().height(), mBase);
        canvas.drawRect(leftWidth, 0, getBounds().width(), getBounds().height(), mSecond);
      }
    }

    @Override
    public int getOpacity() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void setAlpha(int alpha) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  public static void handleMarkings(Activity activity, Cursor cursor, View view, String markingValues) {
    String value = markingValues;
    
    if(value == null && cursor != null && !cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
      value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
    }
    
    long startTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) : 0;
    long endTime = cursor != null ? cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) : 0;
    
    Paint base = new Paint();
    base.setStyle(Paint.Style.FILL_AND_STROKE);
    
    Paint second = null;
        
    if(startTime <= System.currentTimeMillis() && System.currentTimeMillis() <= endTime) {
      base.setColor(0x400000FF);
      second = new Paint();
      second.setStyle(Paint.Style.FILL_AND_STROKE);
      second.setColor(0x150000FF);
    }
    else {
      base = null;
    }
    
    ArrayList<Drawable> draw = new ArrayList<Drawable>();
    
    if(base != null) {
      RunningDrawable running = new RunningDrawable(base, second, startTime, endTime);
      draw.add(running);
    }
    
    if(value != null && value.trim().length() > 0) {
      if(value.startsWith(";")) {
        value = value.substring(1);
      }
      
      String[] markings = value.split(";");
      
      if(markings.length > 1) {
        int[] colors = new int[markings.length];
        
        for(int i = 0; i < markings.length; i++) {
          Integer color = SettingConstants.MARK_COLOR_MAP.get(markings[i]);
          
          if(color != null) {
            colors[i] = activity.getResources().getColor(color);
          }
        }
        
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,colors);
        gd.setCornerRadius(0f);
        
        draw.add(gd);
      }
      else {
        Integer color = SettingConstants.MARK_COLOR_MAP.get(value);
        
        if(color != null) {
          draw.add(new ColorDrawable(activity.getResources().getColor(color)));
        }
      }
      /*
      else if(value.contains(SettingConstants.MARK_VALUE_CALENDAR)) {
        draw.add(new ColorDrawable(activity.getResources().getColor(R.color.mark_color_calendar)));
        //view.setBackgroundResource(R.color.mark_color_calendar);
      }
      else if(value.contains(SettingConstants.MARK_VALUE_FAVORITE)) {
        draw.add(new ColorDrawable(activity.getResources().getColor(R.color.mark_color_favorite)));
        //view.setBackgroundResource(R.color.mark_color_favorite);
      }
      else if(value.contains(SettingConstants.MARK_VALUE_SYNC_FAVORITE)) {
        draw.add(new ColorDrawable(activity.getResources().getColor(R.color.mark_color_sync_favorite)));
        //view.setBackgroundResource(R.color.mark_color);
      }
      else {
        draw.add(new ColorDrawable(activity.getResources().getColor(R.color.mark_color)));
        //view.setBackgroundResource(R.color.mark_color);
      }*/
    }
    else {
      draw.add(activity.getResources().getDrawable(android.R.drawable.list_selector_background));
    }
    
    view.setBackgroundDrawable(new LayerDrawable(draw.toArray(new Drawable[draw.size()])));
    /*
    else {
      view.setBackgroundResource(android.R.drawable.list_selector_background);
    }*/
  }
  
  public static void editFavorite(final Favorite fav, final Activity activity, String searchString) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    
    final View input = activity.getLayoutInflater().inflate(R.layout.add_favorite_layout, null);
    
    if(fav != null) {
      ((EditText)input.findViewById(R.id.favorite_name)).setText(fav.getName());
      ((EditText)input.findViewById(R.id.favorite_search_value)).setText(fav.getSearchValue());
      ((CheckBox)input.findViewById(R.id.favorite_only_title)).setChecked(fav.searchOnlyTitle());
    }
    else if(searchString != null) {
      ((EditText)input.findViewById(R.id.favorite_search_value)).setText(searchString);
    }
    
    builder.setView(input);
    
    builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        new Thread() {
          public void run() {
            String name = ((EditText)input.findViewById(R.id.favorite_name)).getText().toString();
            String search = ((EditText)input.findViewById(R.id.favorite_search_value)).getText().toString();
            boolean onlyTitle = ((CheckBox)input.findViewById(R.id.favorite_only_title)).isChecked();
            
            if(name == null || name.trim().length() == 0) {
              name = search;
            }
            
            if(search != null) {
              Intent intent = new Intent(SettingConstants.FAVORITES_CHANGED);
                       
              if(fav != null) {
                intent.putExtra(Favorite.OLD_NAME_KEY, fav.getName());
              }
              
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
              
              Set<String> favoritesSet = prefs.getStringSet(SettingConstants.FAVORITE_LIST, new HashSet<String>());
              HashSet<String> newFavoriteList = new HashSet<String>();
              
              Favorite dummy = new Favorite(name, search, onlyTitle);
              boolean added = false;
              
              for(String favorite : favoritesSet) {
                String[] values = favorite.split(";;");
                
                if(fav != null && values[0].equals(fav.getName())) {                
                  newFavoriteList.add(dummy.getSaveString());
                  added = true;
                }
                else {
                  newFavoriteList.add(favorite);
                }
              }
                
              if(!added) {
                newFavoriteList.add(dummy.getSaveString());
              }
              
              Editor edit = prefs.edit();
              edit.putStringSet(SettingConstants.FAVORITE_LIST, newFavoriteList);
              edit.commit();
              
              intent.putExtra(Favorite.NAME_KEY, name);
              intent.putExtra(Favorite.SEARCH_KEY, search);
              intent.putExtra(Favorite.ONLY_TITLE_KEY, onlyTitle);
              
              Favorite.updateFavoriteMarking(activity.getApplicationContext(), activity.getContentResolver(), dummy);
              
              LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
            }
          }
        }.start();
      }
    });
    
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        
      }
    });
    
    AlertDialog dialog = builder.create();
    dialog.show();
  }
  
  public static void formatDayView(Activity activity, Cursor cursor, View view, int startDayLabelID) {try {
    long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
    
    TextView text = (TextView)view;
    SimpleDateFormat day = new SimpleDateFormat("EEE", Locale.getDefault());
    
    Calendar progDate = Calendar.getInstance();
    progDate.setTimeInMillis(date);
    
    Calendar today = Calendar.getInstance();
    
    TextView startDay = (TextView)((View)view.getParent()).findViewById(/*R.id.startDayLabelPL*/startDayLabelID);
    startDay.setText(day.format(new Date(date)));
    
    if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
      startDay.setText(activity.getText(R.string.today));
    }
    else {
      today.add(Calendar.DAY_OF_YEAR, 1);
      
      if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
        startDay.setText(activity.getText(R.string.tomorrow));
      }
      else {
        today.add(Calendar.DAY_OF_YEAR, -2);
        
        if(progDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
          startDay.setText(activity.getText(R.string.yesterday));
        }
      }
    }
    
    SimpleDateFormat df = (SimpleDateFormat)java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
    String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
      
    SimpleDateFormat mdf = new SimpleDateFormat(pattern);
    
    text.setText(mdf.format(progDate.getTime()));
    
    UiUtils.handleMarkings(activity, cursor, ((RelativeLayout)view.getParent()), null);}catch(Throwable t) {Log.d("Exception", "", t);}
  }
}
