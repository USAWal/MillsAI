package de.zilant.mills.three;

public enum IntersectionType {
	UNOCCUPIED(0),
	OCCUPIED_BY_ME(2),
	OCCUPIED_BY_OPPONENT(1);
	
	private IntersectionType(int rawValue) { this.rawValue = rawValue; }
	
	public final int rawValue;
}
