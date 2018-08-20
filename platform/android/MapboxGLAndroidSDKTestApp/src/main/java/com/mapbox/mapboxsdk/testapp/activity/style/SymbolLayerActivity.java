package com.mapbox.mapboxsdk.testapp.activity.style;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.testapp.R;
import com.mapbox.mapboxsdk.testapp.utils.ResourceUtils;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import timber.log.Timber;

import java.io.IOException;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textFont;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

/**
 * Test activity showcasing runtime manipulation of symbol layers.
 * <p>
 * Showcases the ability to offline render a symbol layer by using a packaged style and
 * loads the font from the assets folder.
 * </p>
 */
public class SymbolLayerActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener, OnMapReadyCallback {

  private static final String MARKER_SOURCE = "marker-source";
  private static final String MARKER_LAYER = "marker-layer";
  private static final String MARKER_ICON = "my-layers-image";

  private MapboxMap mapboxMap;
  private MapView mapView;
  private boolean initialFont;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_symbollayer);

    try {
      // Create map configuration
      MapboxMapOptions mapboxMapOptions = new MapboxMapOptions();
      mapboxMapOptions.camera(new CameraPosition.Builder().target(
        new LatLng(52.35273, 4.91638))
        .zoom(13)
        .build()
      );
      mapboxMapOptions.styleJson(ResourceUtils.readRawResource(this, R.raw.streets));

      // Create map programmatically, add to view hierarchy
      mapView = new MapView(this, mapboxMapOptions);
      mapView.getMapAsync(this);
      mapView.onCreate(savedInstanceState);
      ((ViewGroup) findViewById(R.id.container)).addView(mapView);
    } catch (IOException exception) {
      Timber.e(exception);
    }
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    // Add a sdf image for the makers
    Drawable icLayersDrawable = getResources().getDrawable(R.drawable.ic_layers);
    Bitmap icLayersBitmap = BitmapUtils.getBitmapFromDrawable(icLayersDrawable);
    mapboxMap.addImage(
      MARKER_ICON,
      icLayersBitmap,
      true
    );

    // Add a source
    FeatureCollection markers = FeatureCollection.fromFeatures(new Feature[] {
      Feature.fromGeometry(Point.fromLngLat(4.91638, 52.35673), featureProperties("Marker 1")),
      Feature.fromGeometry(Point.fromLngLat(4.91638, 52.34673), featureProperties("Marker 2"))
    });
    mapboxMap.addSource(new GeoJsonSource(MARKER_SOURCE, markers));

    // Add the symbol-layer
    mapboxMap.addLayer(
      new SymbolLayer(MARKER_LAYER, MARKER_SOURCE)
        .withProperties(
          iconImage(MARKER_ICON),
          iconIgnorePlacement(true),
          iconAllowOverlap(true),
          iconAnchor(Property.ICON_ANCHOR_BOTTOM),
          iconColor(Color.RED),
          textField(get("title")),
          textFont(new String[] {"DIN Offc Pro Regular", "Arial Unicode MS Regular"}),
          textColor(Color.RED),
          textAllowOverlap(true),
          textIgnorePlacement(true),
          textAnchor(Property.TEXT_ANCHOR_TOP),
          textSize(10f)
        )
    );

    // Set a click-listener so we can manipulate the map
    mapboxMap.addOnMapClickListener(SymbolLayerActivity.this);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    // Query which features are clicked
    PointF screenLoc = mapboxMap.getProjection().toScreenLocation(point);
    List<Feature> features = mapboxMap.queryRenderedFeatures(screenLoc, MARKER_LAYER);

    SymbolLayer layer = mapboxMap.getLayerAs(MARKER_LAYER);
    if (features.size() == 0) {
      // Reset
      layer.setProperties(iconSize(1f));
    } else {
      layer.setProperties(iconSize(3f));
    }
  }

  private void toggleTextSize() {
    SymbolLayer layer = mapboxMap.getLayerAs(MARKER_LAYER);
    layer.setProperties(layer.getTextSize().getValue() > 10 ? textSize(10f) : textSize(20f));
  }

  private void toggleTextField() {
    SymbolLayer layer = mapboxMap.getLayerAs(MARKER_LAYER);
    layer.setProperties("{title}".equals(layer.getTextField().getValue()) ? textField("āA") : textField("{title}"));
  }

  private void toggleTextFont() {
    SymbolLayer layer = mapboxMap.getLayerAs(MARKER_LAYER);
    if (initialFont) {
      layer.setProperties(textFont(new String[] {"DIN Offc Pro Bold", "Arial Unicode MS Bold"}));
    } else {
      layer.setProperties(textFont(new String[] {"DIN Offc Pro Medium", "Arial Unicode MS Regular"}));
    }
    initialFont = !initialFont;
  }

  private JsonObject featureProperties(String title) {
    JsonObject object = new JsonObject();
    object.add("title", new JsonPrimitive(title));
    return object;
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_symbol_layer, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_toggle_text_size:
        toggleTextSize();
        return true;
      case R.id.action_toggle_text_field:
        toggleTextField();
        return true;
      case R.id.action_toggle_text_font:
        toggleTextFont();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
