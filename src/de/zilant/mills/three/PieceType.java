package de.zilant.mills.three;

public enum PieceType {
	MINE      (2),
	OPPONENTS (1),
	NONE      (0);
	
	public final int VALUE;
	
	private PieceType(int value) { this.VALUE = value; }
}
