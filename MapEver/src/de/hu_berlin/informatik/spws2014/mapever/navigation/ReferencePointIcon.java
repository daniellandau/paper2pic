/* Copyright (C) 2014,2015  Björn Stelter
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

import android.graphics.Rect;

import de.hu_berlin.informatik.spws2014.mapever.R;
import de.hu_berlin.informatik.spws2014.mapever.largeimageview.LargeImageView;
import de.hu_berlin.informatik.spws2014.mapever.largeimageview.OverlayIcon;

public class ReferencePointIcon extends OverlayIcon {

	// Resource des zu verwendenden Bildes
	private static int refPointImageResource = R.drawable.ref_punkt;
	
	// Dauer des Verblassens und letztendlich zu erreichender Alpha-Wert
	private static float hiddenAlpha = 0.4f;
	private static long fadingTimeOut = 2000;
	private static long fadingTimeIn = 200;
	
	
	// Zeitpunkt, zu dem der Referenzpunkt erstellt wurde
	private long timeStamp = 0;
	
	
	// ////////////////////////////////////////////////////////////////////////
	// //////////// CONSTRUCTORS
	// ////////////////////////////////////////////////////////////////////////


    // ////////////////////////////////////////////////////////////////////////
	// //////////// OVERLAYICON PROPERTY OVERRIDES
	// ////////////////////////////////////////////////////////////////////////
	
	@Override
	protected int getImagePositionX() {
        return 0;
	}
	
	@Override
	protected int getImagePositionY() {
        return 0;
	}
	
	@Override
	protected int getImageOffsetX() {
		return -getWidth() / 2;
	}
	
	@Override
	protected int getImageOffsetY() {
		return -getHeight() / 2;
	}
	
	@Override
	public Rect getTouchHitbox() {
		// Die Hitbox ist doppelt so groß wie das Bild, damit man die kleinen Referenzpunkte besser anklicken kann.
		// (Ist sinnvoll.)
		return new Rect(
				2 * getImageOffsetX(),
				2 * getImageOffsetY(),
				2 * (getWidth() + getImageOffsetX()),
				2 * (getHeight() + getImageOffsetY()));
	}
	

	/**
	 * Setzt den Zeitpunkt der Erzeugung des Referenzpunkts.
	 */
	public void setTimestamp(long time) {
		timeStamp = time;
	}
	
	
	// ////////////////////////////////////////////////////////////////////////
	// //////////// EVENT HANDLERS
	// ////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean onClick(float screenX, float screenY) {
		MapView mapView = (MapView) getParentLIV();
		
		// Registriere den zugehörigen Referenzpunkt als potentiellen Löschungskandidaten bei der MapView.
		// Falls dies im aktuellen Zustand nicht möglich ist, gibt die Funktion false zurück. Behandel das Event
		// dann als nicht behandelt, sodass es zur MapView weitergereicht wird.
		return mapView.registerAsDeletionCandidate(this);
	}
	
	
	// ////////////////////////////////////////////////////////////////////////
	// //////////// DARSTELLUNG
	// ////////////////////////////////////////////////////////////////////////
	
	/**
	 * Starte Verblassungsanimation des Referenzpunktes.
	 */
	public void fadeOut() {
		// von komplett sichtbar bis hiddenAlpha
		startFading(1, hiddenAlpha, fadingTimeOut);
	}
	
	/**
	 * Starte Animation, die den Referenzpunkt wieder sichtbar macht.
	 */
	public void fadeIn() {
		// von hiddenAlpha bis komplett sichtbar
		startFading(hiddenAlpha, 1, fadingTimeIn);
	}
	
}
