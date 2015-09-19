/* Copyright (C) 2014,2015  Björn Stelter, Hagen Sparka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package de.hu_berlin.informatik.spws2014.mapever.navigation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;

import de.hu_berlin.informatik.spws2014.mapever.BaseActivity;
import de.hu_berlin.informatik.spws2014.mapever.MapEverApp;
import de.hu_berlin.informatik.spws2014.mapever.R;
import de.hu_berlin.informatik.spws2014.mapever.Start;

public class Navigation extends BaseActivity implements LocationListener {
	
	// ////// KEYS F�R ZU SPEICHERNDE DATEN IM SAVEDINSTANCESTATE-BUNDLE

	// Key f�r ID der geladenen Karte (long)
	private static final String SAVEDCURRENTMAPID = "savedCurrentMapID";
	
	// Key f�r UserPosition (Point2D)
	private static final String SAVEDUSERPOS = "savedUserPosition";
	
	// Key f�r den Zustand der Navigation (NavigationState)
	private static final String SAVEDSTATE = "savedState";
	
	// Key f�r den Zustand der Hilfe (boolean)
	private static final String SAVEDHELPSTATE = "savedHelpState";
	
	// Key f�r den Zustand der Positionszentrierungsfunktion (boolean)
	private static final String SAVEDTRACKPOSITION = "savedTrackPosition";
	
	
	// ////// STATIC VARIABLES AND CONSTANTS
	
	/**
	 * INTENT_LOADMAPID: ID der zu ladenden Map als long
	 */
	public static final String INTENT_LOADMAPID = "de.hu_berlin.informatik.spws2014.mapever.navigation.Navigation.LoadMapID";
	public static final String INTENT_POS = "de.hu_berlin.informatik.spws2014.mapever.navigation.Navigation.IntentPos";
	
	
	// ////// VIEWS
	
	// unsere Karte
	private MapView mapView;
	
	// ////// KARTEN- UND NAVIGATIONSINFORMATIONEN
	
	// ID der aktuell geladenen Karte (zugleich Dateiname des Kartenbildes)
	private long currentMapID;
	
	// Position des Benutzers auf der Karte in Pixeln
	private double[] intentPos = null;
	
	// Soll die aktuelle Position verfolgt (= zentriert) werden?
	private boolean trackPosition = false;
	
	
	// ////// LOKALISIERUNG
	
	// GPS-Lokalisierung
	private LocationManager locationManager;
	
	// Lokalisierungsalgorithmus

	// LocationDataManagerListener, der das Eintreffen neuer Positionen handled
	private LocationDataManagerListener locDatManListener;
	
	// Umbenennen der Karte
	private String newMapName = "";
	
	// TODO bitte das hier drunter alles mal oben einsortieren
	
	// soll der Zustand gespeichert werden?
	private boolean saveState = true;
	
	// der aktuelle Zustand der Navigation, der Anfangszustand ist RUNNING
	public NavigationStates state = NavigationStates.RUNNING;
	
	// "SuperLayout", in welches die anderen Layouts eingebunden werden, erm�glicht einfache Umsetzung von Overlays
	private FrameLayout layoutFrame;
	
	// XXX besser L�sen per Zustands�bergang
	private boolean quickTutorial = false;
	
	// unser Men�
	private Menu menu;
	

	// ////////////////////////////////////////////////////////////////////////
	// //////////// ACTIVITY LIFECYCLE UND INITIALISIERUNG
	// ////////////////////////////////////////////////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Wenn die Activity in Folge einer Rotation neu erzeugt wird, gibt es einen gespeicherten Zustand in
		// savedInstanceState, sonst normal starten.
		Log.d("Navigation", "onCreate..." + (savedInstanceState != null ? " with savedInstanceState" : ""));
		
		// ////// LAYOUT UND VIEWS
		
		// Layout aufbauen
		layoutFrame = new FrameLayout(getBaseContext());
		setContentView(layoutFrame);
		getLayoutInflater().inflate(R.layout.activity_navigation, layoutFrame);
		
		// Initialisieren der ben�tigten Komponenten
		mapView = (MapView) findViewById(R.id.map);
		mapView.setFocusable(true);
		mapView.setFocusableInTouchMode(true);

		// Im Zweifelsfall kennen wir den Kartennamen nicht (sonst wird der aus der Datenbank geladen)
		setTitle(getString(R.string.navigation_const_name_of_unnamed_maps));
		
		// ////// PARAMETER ERMITTELN UND ZUSTANDSVARIABLEN INITIALISIEREN (GGF. AUS STATE LADEN)
		
		intentPos = null;
		if (savedInstanceState == null) {
			// -- Frischer Start --
			
			Intent intent = getIntent();
			
			// ID der Karte (= Dateiname des Bildes)
			currentMapID = intent.getExtras().getLong(INTENT_LOADMAPID);
			
			// Initialisiere Startzustand der Navigation
			changeState(NavigationStates.RUNNING);
			saveState = true;
			
			intentPos = intent.getDoubleArrayExtra(INTENT_POS);
		}
		else {
			// -- Gespeicherter Zustand --
			
			// ID der Karte (= Dateiname des Bildes)
			currentMapID = savedInstanceState.getLong(SAVEDCURRENTMAPID);
			
			// ist das Kurztutorial aktiviert?
			quickTutorial = savedInstanceState.getBoolean(SAVEDHELPSTATE);
			
			// ist die Positionszentrierung aktiviert?
			trackPosition = savedInstanceState.getBoolean(SAVEDTRACKPOSITION);
			
			// Wiederherstellung des Zustands
			NavigationStates restoredState = NavigationStates.values()[savedInstanceState.getInt(SAVEDSTATE)];
			
			if (restoredState.isHelpState()) {
				// Sonderbehandlung beim Drehen im Bildschirm der Schnellhilfe
				// XXX besser machen
				state = restoredState;
				getLayoutInflater().inflate(R.layout.navigation_help_running, layoutFrame);
				endQuickHelp();
				startQuickHelp();
				
			}
			else {
				changeState(restoredState);
			}
		}
		
		// ////// KARTE LADEN UND KOMPONENTEN INITIALISIEREN
		
		// Lade Karte und erstelle gegebenenfalls einen neuen Eintrag
		Log.d("onCreate", "Loading map: " + currentMapID + (currentMapID == -1 ? " (new map!)" : ""));
		initLoadMap();
		
		// Initialisiere GPS-Modul
		initGPSModule();
		
		// Initialiales update(), damit alles korrekt dargestellt wird
		mapView.update();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("Navigation", "onDestroy...");
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("Navigation", "onStart...");
		
		//Prompt user to activate GPS
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			DialogInterface.OnClickListener gpsPromptListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        if (which == DialogInterface.BUTTON_POSITIVE) {
			        	Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(intent);
			        }
			        dialog.dismiss();
			    }
			};

			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setMessage(R.string.navigation_gps_activation_popup_question)
				.setPositiveButton(android.R.string.yes, gpsPromptListener)
			    .setNegativeButton(android.R.string.no, gpsPromptListener)
			    .show();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d("Navigation", "onStop...");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d("Navigation", "onResume...");
		
		// Abonniere GPS Updates
		// TODO Genauigkeit (Parameter 2, 3)? default aus Tutorial (400, 1)
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, (float) 0.2, this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.d("Navigation", "onPause...");
		
		// Deabonniere GPS Updates
		locationManager.removeUpdates(this);
    }
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d("Navigation", "onSaveInstanceState...");
		
		if (saveState) {
			// ////// SAVE THE CURRENT STATE
			
			// ID der Karte (= Dateiname des Bildes)
			savedInstanceState.putLong(SAVEDCURRENTMAPID, currentMapID);

			// Zustand der Navigation speichern
			savedInstanceState.putInt(SAVEDSTATE, state.ordinal());
			
			// Zustand der Hilfe speichern
			savedInstanceState.putBoolean(SAVEDHELPSTATE, quickTutorial);
			
			// Zustand der Hilfe speichern
			savedInstanceState.putBoolean(SAVEDTRACKPOSITION, trackPosition);
			
			// neu erstellt werden:
			// LocationManager
			// LocationDataManager
			// LocationDataManagerListener
			// LocationDataManagerListenerExpertenWohnungsvermittlungFachangestellter
		}
		
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}
	
	/**
	 * Beim Bet�tigen der Zur�cktaste gelangen wir wieder zum Startbildschirm.
	 */
	@Override
	public void onBackPressed() {
		// Mit der Zur�ck-Taste kann das L�schen von Referenzpunkten abgebrochen werden
		if (state == NavigationStates.DELETE_REFPOINT) {
			refPointDeleteBack(null);
			return;
		}
		
		// ... genauso wie das Setzen von Referenzpunkten
		if (state == NavigationStates.MARK_REFPOINT || state == NavigationStates.ACCEPT_REFPOINT) {
			cancelReferencePoint(null);
			return;
		}
		
		// ... und die Schnellhilfe
		if (state.isHelpState()) {
			
			endQuickHelp();
			return;
		}
		
		// Wenn wir zur�ckgehen, muss der Zustand nicht gespeichert werden
		saveState = false;
		
		// Startbildschirm aufrufen und Activity finishen
		Intent intent = new Intent(getApplicationContext(), Start.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (intentPos != null) {
			intentPos = null;
			intent.putExtra(Start.INTENT_EXIT, true);
		}
		startActivity(intent);
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation, menu);
		
		this.menu = menu;
		
		// Aktiviere Debug-Optionen, falls Debugmode aktiviert
		if (MapEverApp.isDebugModeEnabled(this)) {
			menu.findItem(R.id.action_debugmode_mockgps).setVisible(true);
		}

		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		
			case R.id.action_rename_map:
				changeState(NavigationStates.RENAME_MAP);
				return true;
				
			case R.id.action_quick_help:
				// Schnellhilfe-Button
				startQuickHelp();
				return true;
				
			case R.id.action_debugmode_mockgps:
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	// //////////// INITIALISIERUNG VON KARTE UND GPS-MODUL
	// ////////////////////////////////////////////////////////////////////////
	
	// //////// KARTE LADEN / NEUE KARTE ERSTELLEN
	
	static final int LOAD_TEST_MAP = 0;
	static final int CREATE_NEW_MAP = -1;
	
	private void initLoadMap() {
		// ////// LDM-IO-HANDLER INITIALISIEREN
		
		Log.i("Nav" ,"currentMapID:" + currentMapID);
		// ////// BILD IN DIE MAPVIEW LADEN
		
		// (bei currentMapID == 0 wird die Testkarte geladen)
		try {
			mapView.loadMap(currentMapID);
		} catch (FileNotFoundException e) {
			Log.e("Navigation/initLoadMap", "Konnte Karte " + currentMapID + " nicht laden!");

			// ResourceID der Fehlermeldung an den Startbildschirm geben, dieser zeigt Fehlermeldung an
			setResult(R.string.navigation_image_not_found);
			finish();
			return;
		}
		
		// Bilddimensionen m�ssen erkannt worden sein
		if (mapView.getImageWidth() == 0 || mapView.getImageHeight() == 0) {
			Log.e("Navigation/initLoadMap", "Bilddimensionen sind " + mapView.getImageWidth() + "x" + mapView.getImageHeight());
			
			// (Sollte eigentlich eh nie vorkommen, also gib "Error" aus...)
			setResult(R.string.general_error_title);
			finish();
			return;
		}
		
		// ////// LOCATIONDATAMANAGER INITIALISIEREN
		
		// Listener f�r neue Userkoordinaten erstellen
		locDatManListener = new LocationDataManagerListener(this);
		

		// ////// GELADENE REFERENZPUNKTE DARSTELLEN

		// TODO Check for corrupted maps! (LDM returns lots of nulls)
		
	}
	
	
	// //////// GPS-MODUL INITIALISIEREN
	
	private void initGPSModule() {
		
		// Initialisiere GPS-Modul
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, (float) 0.2, this);
		Log.d("initGPSModule", "Location provider: " + LocationManager.GPS_PROVIDER);
		
		// Wir machen nichts mit der lastKnownLocation, siehe #169
	}
	
	
	// ////////////////////////////////////////////////////////////////////////
	// //////////// HANDLING VON USERINPUT
	// ////////////////////////////////////////////////////////////////////////
	
	// //////// HILFE
	
	/**
	 * Startet die Schnellhilfe, wenn nicht bereits aktiv.
	 */
	public void startQuickHelp() {
		// Bis RUNNING wieder erreicht ist, wird der Hilfebildschirm immer angezeigt
		quickTutorial = true;
		
		// Ansonsten zeigen wir abh�ngig vom aktuellen Zustand einen passendes Hilfs-Overlay an
		// und deaktivieren die entsprechenden Buttons
		switch (state) {
			case ACCEPT_REFPOINT:
				getLayoutInflater().inflate(R.layout.navigation_help_accept_refpoint, layoutFrame);
				changeState(NavigationStates.HELP_ACCEPT_REFPOINT);
				break;
			case DELETE_REFPOINT:
				getLayoutInflater().inflate(R.layout.navigation_help_delete_refpoint, layoutFrame);
				changeState(NavigationStates.HELP_DELETE_REFPOINT);
				break;
			case MARK_REFPOINT:
				getLayoutInflater().inflate(R.layout.navigation_help_mark_refpoint, layoutFrame);
				changeState(NavigationStates.HELP_MARK_REFPOINT);
				break;
			case RUNNING:
				getLayoutInflater().inflate(R.layout.navigation_help_running, layoutFrame);
				changeState(NavigationStates.HELP_RUNNING);
				break;
			
			// Wenn wir in der Schnellhilfe sind, beenden wir sie jetzt
			case HELP_ACCEPT_REFPOINT:
			case HELP_DELETE_REFPOINT:
			case HELP_MARK_REFPOINT:
			case HELP_RUNNING:
				endQuickHelp();
				break;
			default:
				Log.e("startQuickHelp", "Schnellhilfe f�r diesen Zustand fehlt noch!");
				return;
				
		}
		
	}
	
	/**
	 * Beendet die Schnellhilfe und stellt den vorherigen Zustand wieder her.
	 */
	public void endQuickHelp() {
		// alle Layer bis auf den untersten aus dem FrameLayout entfernen
		if (layoutFrame.getChildCount() > 1) {
			layoutFrame.removeViews(layoutFrame.getChildCount() - 1, layoutFrame.getChildCount() - 1);
		}
		
		// zur�ck zum vorherigen Zustand
		switch (state) {
			case HELP_ACCEPT_REFPOINT:
				changeState(NavigationStates.ACCEPT_REFPOINT);
				break;
			case HELP_DELETE_REFPOINT:
				changeState(NavigationStates.DELETE_REFPOINT);
				break;
			case HELP_MARK_REFPOINT:
				changeState(NavigationStates.MARK_REFPOINT);
				break;
			case HELP_RUNNING:
				changeState(NavigationStates.RUNNING);
				break;
			default:
				Log.e("endQuickHelp", "Diesen Text sollte man nie angezeigt bekommen! Zustand: " + state);
				break;
		
		}
	}
	
	// //////// BUTTONS
	
	/**
	 * Der User hat auf den "Referenzpunkt setzen" Button gedr�ckt.
	 * 
	 * @param view
	 */
	public void setRefPoint(View view) {
		if (state != NavigationStates.RUNNING) {
			Log.w("setRefPoint", "Inkonsistenter Zustand: state != RUNNING");
		}
		
		// Zustands�nderung zum "Referenzpunkt Setzen" Zustand
		changeState(NavigationStates.MARK_REFPOINT);
		
	}
	
	/**
	 * Der Nutzer will den neu gesetzten Referenzpunkt behalten.
	 * 
	 * @param view
	 */
	public void acceptReferencePoint(View view) {
		mapView.acceptReferencePoint();
		
		// Anschlie�end befinden wir uns wieder im Anfangszustand
		changeState(NavigationStates.RUNNING);
	}
	
	/**
	 * Der Nutzer will den neu gesetzten Referenzpunkt verwerfen.
	 * 
	 * @param view
	 */
	public void cancelReferencePoint(View view) {
		// kann damit auch von onBackPressed aufgerufen werden, wenn wir noch in MARK_REFPOINT sind
		if (state == NavigationStates.ACCEPT_REFPOINT) {
			mapView.cancelReferencePoint();
		}
		
		// Anschlie�end befinden wir uns wieder im Anfangszustand
		changeState(NavigationStates.RUNNING);
	}
	
	/**
	 * Der Button zum L�schen des Referenzpunkts wurde bet�tigt.
	 * 
	 * @param view
	 */
	public void deleteReferencePoint(View view) {
		mapView.deleteReferencePoint();
		
		// Anschlie�end befinden wir uns wieder im Anfangszustand
		changeState(NavigationStates.RUNNING);
	}
	
	/**
	 * Der ausgew�hlte Referenzpunkt soll doch nicht gel�scht werden.
	 * 
	 * @param view
	 */
	public void refPointDeleteBack(View view) {
		mapView.dontDeleteReferencePoint();
		
		// Anschlie�end befinden wir uns wieder im Anfangszustand
		changeState(NavigationStates.RUNNING);
	}
	
	/**
	 * Aktiviere Zentrierung des Locationmarkers.
	 * 
	 * @param view
	 */
	public void trackPosition(View view) {
		trackPosition = true;
		view.setVisibility(View.GONE);
		mapView.centerCurrentLocation();
	}
	
	// ////////////////////////////////////////////////////////////////////////
	// //////////// HILFSFUNKTIONEN
	// ////////////////////////////////////////////////////////////////////////
	
	// //////// ZUSTANDS�NDERUNGEN
	
	/**
	 * Simpler Zustands�bergang ohne gro�e Rafinesse.
	 * 
	 * @param nextState
	 */
	public void changeState(NavigationStates nextState) {
		
		boolean changeToHelp = false;
		boolean changeFromHelp = false;
		
		NavigationStates oldState = state;
		state = nextState;
		
		Log.d("changeState", "Zustands�bergang von " + oldState + " zu " + state);
		
		if (oldState.isHelpState()) {
			changeFromHelp = true;
		}
		else if (oldState == NavigationStates.RENAME_MAP) {
			
			// wenn ein neuer Name eingegeben wurde, so ist er in newMapName gespeichert
			if (newMapName != "") {
				this.getSupportActionBar().setTitle(newMapName);

				newMapName = "";
			}
			
		}
		
		// alle Layer bis auf den untersten aus dem FrameLayout entfernen
		// TODO nee, oder? siehe Bug #231
		// if (layoutFrame.getChildCount() > 1) {
		// layoutFrame.removeViews(layoutFrame.getChildCount() - 1, layoutFrame.getChildCount() - 1);
		// }
		
		// Rename Men�option deaktivieren
		if (menu != null) {
			menu.findItem(R.id.action_rename_map).setVisible(false);
		}
		
		// und dann je nach Zustand die passenden Buttons reaktivieren
		switch (state) {
			case ACCEPT_REFPOINT:
				break;
			
			case DELETE_REFPOINT:
				break;
			
			case HELP_ACCEPT_REFPOINT:
				// Es handelt sich um einen Wechsel zur Schnellhilfe
				changeToHelp = true;
				break;
			
			case HELP_DELETE_REFPOINT:
				changeToHelp = true;
				break;
			
			case HELP_MARK_REFPOINT:
				// hier gibt es aktuell keine Buttons anzuzeigen
				
				changeToHelp = true;
				break;
			
			case HELP_RUNNING:

				changeToHelp = true;
				break;
			
			case MARK_REFPOINT:
				// hier gibt es aktuell keine Buttons anzuzeigen
				break;
			
			case RENAME_MAP:
				
				// TODO das wirkt irgendwie alles so umst�ndlich... xD
				
				// erstelle AlertDialog f�r die schickere Umbenennung
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				builder.setTitle(R.string.navigation_rename_map);
				
				// Dr�cken des Umbenennen-Buttons benennt die Karte um
				builder.setPositiveButton(R.string.navigation_rename_map_rename, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						changeState(NavigationStates.RUNNING);
						
					}
				});
				
				// Dr�cken des Cancel-Buttons beendet die Umbenennung
				builder.setNegativeButton(R.string.navigation_rename_map_cancel, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// der Kartenname soll nicht ge�ndert werden!
						newMapName = "";
						changeState(NavigationStates.RUNNING);
						
						
					}
				});
				
				// TODO was ist hier die Alternative zu null?
				View renameDialogLayout = getLayoutInflater().inflate(R.layout.navigation_rename_map, null);
				
				// bindet das Layout navigation_rename_map ein
				builder.setView(renameDialogLayout);
				
				AlertDialog dialog = builder.create();
				
				// Behandelt alle Arten den AlertDialog abzubrechen, ohne die Kn�pfe zu verwenden
				dialog.setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						// der Kartenname soll nicht ge�ndert werden!
						newMapName = "";
						changeState(NavigationStates.RUNNING);
					}
				});
				
				// Anzeigen des Dialog
				dialog.show();
				
				EditText input = (EditText) renameDialogLayout.findViewById(R.id.editTextToNameMap);
				
				// Wenn der Text ge�ndert wird, wird der String newMapName angepasst
				input.addTextChangedListener(new TextWatcher() {
					
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						
					}
					
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						
					}
					
					@Override
					public void afterTextChanged(Editable s) {
						newMapName = s.toString();
						
					}
				});
				
				// der Aktuelle Kartenname wird dem Benutzer angezeigt
				input.setText(getTitle());
				break;
			
			case RUNNING:
				// nur in RUNNING kann man die Karte umbenennen
				if (menu != null) {
					menu.findItem(R.id.action_rename_map).setVisible(true);
				}
				
				if (intentPos != null) {
					onBackPressed();
					return;
				}
				break;
			
			default:
				break;
		}
		
		// Das Kurztutorial soll nur angezeigt werden, wenn es
		// a) aktiviert ist
		// und b) wir nicht zu einem Hilfebildschirm wechseln oder von einem solchen kommen
		if (quickTutorial && !changeFromHelp && !changeToHelp) {
			// wenn wir wieder bei RUNNING angekommen sind, wird die Hilfe erst wieder auf Nutzerwunsch ausgel�st
			if (state == NavigationStates.RUNNING) {
				quickTutorial = false;
				return;
			}
			else {
				// Ansonsten zeigen wir den Hilfe-Bildschirm an
				startQuickHelp();
			}
		}
	}
	

	// ////////////////////////////////////////////////////////////////////////
	// //////////// LOKALISIERUNG
	// ////////////////////////////////////////////////////////////////////////
	
	// //////// GPS
	
	@Override
	public void onLocationChanged(Location location) {}
	
	// TODO bin mir nicht sicher, ob hier was gemacht werden muss...
	@Override
	public void onProviderEnabled(String provider) {
		Log.d("Navigation", "Provider enabled: " + provider);
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		Log.d("Navigation", "Provider disabled: " + provider);
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d("Navigation", "Status changed: " + provider + ", status = " + status);
	}
}
