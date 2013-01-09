package de.zilant.mills.three;

public enum BoardState {
	IMMEDIATE_WIN(2),
	IMMEDIATE_LOSS(-2),
	WIN(1),
	LOSS(-1),
	DRAW(0);
	
	public static BoardState defineState(int rawState) {
		switch (rawState) {
		case -2: return IMMEDIATE_LOSS;
		case -1: return LOSS;
		case  1: return WIN;
		case  2: return IMMEDIATE_WIN;
		default: return DRAW;
		}
	}
	
	private BoardState(int rawValue) { this.rawValue = rawValue; }
	
	@Override
	public String toString() { return representations[rawValue + 2]; }
	
	public int rawValue;
	
	private static final String[] representations = new String[] {"IMMEDIATE LOSS", "LOSS", "DRAW", "WIN", "IMMEDIATE WIN"};
}
