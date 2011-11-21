package org.jimhopp.GPSvsNetwork;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.jimhopp.GPSvsNetwork.model.PhoneLocationModel;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import org.jimhopp.GPSvsNetwork.R;

public class GPSvsNetworkMap extends MapActivity {
	PhoneLocationModel model;
    MapView mapview;

	private Timer timer;
	
	private class Timer implements Runnable {

		private volatile boolean done = false;
		
		Handler handler = new Handler();
		
		private Runnable updateLocation = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				updateMarkers();
			}
		};
		
		public Timer() {
			// TODO Auto-generated constructor stub
		}
		
		public void done() { done = true; }
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!done) {
				handler.post(updateLocation);
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	void updateMarkers() {
		    Location gps = model.getGPSLocation();
		    Location network = model.getNetworkLocation();
	        if (gps != null && network != null) {
	            Drawable gDrawable = this.getResources().getDrawable(R.drawable.gps_marker);
	        	LocationOverlay gOverlay = new LocationOverlay(gDrawable);
	        	GeoPoint gPoint = new GeoPoint((int) (gps.getLatitude() * 1e6),
	        			(int) (gps.getLongitude() * 1e6));
	        	OverlayItem gOverlayitem = new OverlayItem(gPoint, "", "");
	        	gOverlay.addOverlay(gOverlayitem);
	        	
	        	Drawable nDrawable = this.getResources().getDrawable(R.drawable.network_marker);
	        	LocationOverlay nOverlay = new LocationOverlay(nDrawable);
	        	GeoPoint nPoint = new GeoPoint((int) (network.getLatitude() * 1e6),
	        			(int) (network.getLongitude() * 1e6));
	        	OverlayItem nOverlayitem = new OverlayItem(nPoint, "", "");
	        	nOverlay.addOverlay(nOverlayitem);
	        	
	            List<Overlay> mapOverlays = mapview.getOverlays();
		        if (mapOverlays != null) { 
		        	mapOverlays.clear();
		        	mapOverlays.add(gOverlay);
		        	mapOverlays.add(nOverlay);
		        }
		        final MapController mc = mapview.getController();
	        	double diffLate6 =  Math.abs(gps.getLatitude()  - network.getLatitude()) * 1e6;
	        	double diffLone6 =  Math.abs(gps.getLongitude()  - network.getLongitude()) * 1e6;

	    		double avgLate6 = (gps.getLatitude() + network.getLatitude())/2 * 1e6;
	    		double avgLone6 = (gps.getLongitude() + network.getLongitude())/2 * 1e6;
	    		mc.animateTo(new GeoPoint((int)avgLate6, (int)avgLone6));
	        	//add a padding factor
	    		mc.zoomToSpan((int)(diffLate6 * 1.1), (int)(diffLone6 * 1.1));
	        }
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	
        mapview = (MapView) findViewById(R.id.mapview);
        mapview.setBuiltInZoomControls(true);
        mapview.setClickable(true); 
        mapview.setEnabled(true); 
        mapview.setSatellite(false); 
        mapview.setTraffic(false); 
     // start out with a general zoom 
        final MapController mc = mapview.getController();
        mc.setZoom(16);
        
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        model = new PhoneLocationModel(lm);
        
        Location gps = model.getGPSLocation();
        Location network = model.getNetworkLocation();
        
        if (gps != null && network != null) {
        	updateMarkers();
        }
        
        timer = new Timer();
        new Thread(timer).start();
    }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	    menu.add(Menu.NONE, 1, Menu.NONE, "Email Location").setAlphabeticShortcut('e');
	    menu.add(Menu.NONE, 2, Menu.NONE, "Exit").setAlphabeticShortcut('x');
	    return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case 1:
			Location locG = model.getGPSLocation();
			Location locN = model.getNetworkLocation();
			String body;
            List<Overlay> mapOverlays = mapview.getOverlays();
            int itemCnt = mapOverlays.size();
			if (locG != null && locN != null) {
				String text = "My current locations:\n" +
						"GPS    : "	+ (locG != null ? locG.getLatitude() : -999)
						+ ", " + (locG != null ? locG.getLongitude() : -999) + "\n" 
						+ "Network: "	+ (locN != null ? locN.getLatitude() : -999)
						+ ", " + (locN != null ? locN.getLongitude() : -999) + "\n"
						+ "Number of items in the list: " + itemCnt;
				String qmap_link;
				try {
					String query_string = "size=512x512"
					     + "&markers=" 
						 + URLEncoder.encode("color:blue|label:G|" 
					             + locG.getLatitude() + "," + locG.getLongitude(), "UTF-8")
					     + "&markers="
					     + URLEncoder.encode("color:green|label:N|" 
					             + locN.getLatitude() + "," + locN.getLongitude(), "UTF-8")
					     + "&sensor=true";
					qmap_link = "http://maps.googleapis.com/maps/api/staticmap?" + query_string;
				} catch (UnsupportedEncodingException e) {
					qmap_link = e.getMessage();
				}
				body = text + "\n " + qmap_link;
			}
			else {
				body = "(location unknown!)";
			}			
			Uri addr = Uri.fromParts("mailto", "jimhopp@gmail.com", null);
			Intent email = new Intent(Intent.ACTION_SENDTO, addr);
			email.putExtra(Intent.EXTRA_TEXT, body);
			email.putExtra(Intent.EXTRA_SUBJECT, "my location");
			PackageManager pm = getPackageManager();
			if (pm.resolveActivity(email, PackageManager.MATCH_DEFAULT_ONLY) != null) {
				startActivity(email);
			}
			else {
				Toast toast = Toast.makeText(this, "Sorry, email not configured on this device",
						Toast.LENGTH_SHORT);
				toast.show();			
			}
			return true;
			
		case 2: 
			timer.done();
			finish();
			return true;

		default:
				break;
		}
		return false;
	}
}